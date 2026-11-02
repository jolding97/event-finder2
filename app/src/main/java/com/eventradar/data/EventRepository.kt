package com.eventradar.data

import com.eventradar.sources.CalendarFeedSource
import com.eventradar.sources.EventSource
import com.eventradar.sources.FoundEvent
import com.eventradar.sources.ImageFinder
import com.eventradar.sources.SkiddleSource
import com.eventradar.sources.TicketmasterSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.Normalizer
import java.time.LocalDate

data class RefreshResult(val found: Int, val errors: List<String>) {
    val summary: String
        get() = buildString {
            append(if (found == 1) "1 event found" else "$found events found")
            if (errors.isNotEmpty()) append(" · ${errors.distinct().take(2).joinToString("; ")}")
        }
}

class EventRepository(private val db: AppDatabase, private val settings: SettingsStore) {

    fun observeInterests(): Flow<List<Interest>> = db.interests().observeAll()
    fun observeEvents(): Flow<List<EventEntity>> = db.events().observeUpcoming(LocalDate.now().toString())

    suspend fun addInterest(name: String, category: String, feedUrl: String?, ukOnly: Boolean): Interest {
        val i = Interest(name = name.trim(), category = category, feedUrl = feedUrl?.trim()?.ifBlank { null }, ukOnly = ukOnly)
        return i.copy(id = db.interests().insert(i))
    }

    suspend fun updateInterest(interest: Interest) = db.interests().update(interest)

    /** Removes the interest and any events that were only on the calendar because of it. */
    suspend fun deleteInterest(interest: Interest) {
        db.interests().delete(interest)
        val delete = mutableListOf<String>()
        val update = mutableListOf<EventEntity>()
        for (e in db.events().getAll()) {
            if (interest.name !in e.interests) continue
            val remaining = e.interests - interest.name
            if (remaining.isEmpty()) delete += e.id else update += e.copy(interestTags = tags(remaining))
        }
        db.events().replace(delete, update)
    }

    suspend fun clearEvents() = db.events().clear()

    private fun sources(): List<EventSource> {
        val s = settings.current
        return buildList {
            if (s.ticketmasterKey.isNotBlank()) add(TicketmasterSource(s.ticketmasterKey))
            if (s.skiddleKey.isNotBlank()) add(SkiddleSource(s.skiddleKey))
            add(CalendarFeedSource())
        }
    }

    /**
     * Searches every interest for events between today and [months] months ahead.
     * Events from a source that failed this time are kept as they were.
     * [onProgress] reports (searches done, total searches).
     */
    suspend fun refreshAll(
        months: Int = settings.current.monthsAhead,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): RefreshResult = refresh(db.interests().getAll(), fullRefresh = true, months, onProgress)

    /** Searches just one interest (used right after it's added or edited) and merges the results in. */
    suspend fun refreshInterest(interest: Interest): RefreshResult =
        refresh(listOf(interest), fullRefresh = false, settings.current.monthsAhead) { _, _ -> }

    private suspend fun refresh(
        interests: List<Interest>,
        fullRefresh: Boolean,
        months: Int,
        onProgress: (Int, Int) -> Unit,
    ): RefreshResult = coroutineScope {
        if (interests.isEmpty()) return@coroutineScope RefreshResult(0, listOf("Add some interests first"))
        val sources = sources()
        val jobs = interests.flatMap { i -> sources.filter { it.appliesTo(i) }.map { it to i } }
        if (jobs.isEmpty()) {
            return@coroutineScope RefreshResult(0, listOf("Add a free API key in Settings, or a calendar feed to an interest"))
        }

        val from = LocalDate.now()
        val to = from.plusMonths(months.toLong())
        val gate = Semaphore(4)
        val done = java.util.concurrent.atomic.AtomicInteger(0)
        onProgress(0, jobs.size)

        data class Outcome(val source: String, val interest: Interest, val events: List<FoundEvent>?, val error: String?)

        val outcomes = jobs.map { (source, interest) ->
            async {
                gate.withPermit {
                    val o = try {
                        Outcome(source.name, interest, source.search(interest, from, to), null)
                    } catch (e: Exception) {
                        Outcome(source.name, interest, null, e.message ?: "${source.name} failed")
                    }
                    onProgress(done.incrementAndGet(), jobs.size)
                    o
                }
            }
        }.awaitAll()

        // Merge everything found into one map keyed by de-duplication id.
        val merged = LinkedHashMap<String, EventEntity>()
        for (o in outcomes) {
            for (f in o.events.orEmpty()) {
                if (f.date.isBefore(from) || f.date.isAfter(to)) continue
                val entity = f.toEntity(o.interest.name)
                merged[entity.id] = merged[entity.id]?.mergedWith(entity) ?: entity
            }
        }

        val existing = db.events().getAll().associateBy { it.id }
        val failedSources = outcomes.filter { it.error != null }.map { it.source }.toSet()

        // Carry over interest tags and thumbnails we already had.
        val upserts = merged.values.map { new ->
            val old = existing[new.id]
            when {
                old == null -> new
                fullRefresh -> new.copy(imageUrl = new.imageUrl ?: old.imageUrl)
                else -> old.mergedWith(new)
            }
        }.toMutableList()

        // Thumbnails: fill gaps from the event page's share image.
        val imageGate = Semaphore(6)
        upserts.indices.filter { upserts[it].imageUrl == null && "google.com/search" !in upserts[it].url }.map { idx ->
            async {
                imageGate.withPermit {
                    ImageFinder.find(upserts[idx].url)?.let { upserts[idx] = upserts[idx].copy(imageUrl = it) }
                }
            }
        }.awaitAll()

        // Full refresh: drop events no longer found (unless their source failed this time,
        // or they're beyond the window we just searched).
        val deletes = if (fullRefresh) {
            existing.values.filter {
                it.id !in merged && it.source !in failedSources && !LocalDate.parse(it.startDate).isAfter(to)
            }.map { it.id }
        } else emptyList()

        db.events().replace(deletes, upserts)
        db.events().deletePast(from.toString())
        if (fullRefresh) settings.update { it.copy(lastRefresh = System.currentTimeMillis()) }

        RefreshResult(found = merged.size, errors = outcomes.mapNotNull { it.error })
    }

    companion object {
        fun tags(names: Collection<String>) = names.distinct().joinToString("|", prefix = "|", postfix = "|")

        private fun normalise(s: String): String =
            Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("""\p{M}"""), "")
                .replace(Regex("""\b(the|tickets?|live|presents?|uk|tour|\d{4})\b"""), " ")
                .replace(Regex("""[^a-z0-9]+"""), "")
                .take(40)

        fun FoundEvent.toEntity(interestName: String) = EventEntity(
            id = "${date}_${normalise(title).ifBlank { url.hashCode().toString() }}",
            title = title,
            startDate = date.toString(),
            startTime = time,
            price = price,
            venue = venue,
            city = city,
            imageUrl = imageUrl,
            url = url,
            source = source,
            interestTags = tags(listOf(interestName)),
        )

        /** Combine two records of the same event, keeping whichever fields are filled in. */
        fun EventEntity.mergedWith(other: EventEntity) = copy(
            startTime = startTime ?: other.startTime,
            price = price ?: other.price,
            venue = venue ?: other.venue,
            city = city ?: other.city,
            imageUrl = imageUrl ?: other.imageUrl,
            interestTags = tags(interests + other.interests),
            fetchedAt = maxOf(fetchedAt, other.fetchedAt),
        )
    }
}
