package com.eventradar.sources

import com.eventradar.data.Interest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Reads a public iCalendar (.ics / webcal://) feed attached to an interest — e.g. a racing series'
 * season calendar — and turns its entries into events. Free, no key needed.
 */
class CalendarFeedSource : EventSource {
    override val name = "Calendar feed"

    override fun appliesTo(interest: Interest) = !interest.feedUrl.isNullOrBlank()

    override suspend fun search(interest: Interest, from: LocalDate, to: LocalDate): List<FoundEvent> =
        withContext(Dispatchers.IO) {
            val url = normaliseFeedUrl(interest.feedUrl ?: return@withContext emptyList())
            val req = Request.Builder().url(url)
                .header("User-Agent", Http.USER_AGENT)
                .header("Accept", "text/calendar, text/plain, */*")
                .build()
            val text = Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw SourceException("Feed for ${interest.name}: HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            if (!text.contains("BEGIN:VCALENDAR")) {
                throw SourceException("Feed for ${interest.name} isn't a calendar (.ics) link")
            }
            parse(text)
                .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
                .filter { !interest.ukOnly || looksUk(it) }
        }

    // --- iCalendar parsing ---------------------------------------------------------------------

    private data class Prop(val name: String, val params: Map<String, String>, val value: String)

    private fun parse(ics: String): List<FoundEvent> {
        // Lines starting with a space/tab continue the previous line ("folding").
        val lines = ics.replace("\r\n", "\n").replace(Regex("\n[ \t]"), "").split('\n')
        val out = mutableListOf<FoundEvent>()
        var current: MutableList<Prop>? = null
        for (raw in lines) {
            val line = raw.trimEnd()
            when {
                line == "BEGIN:VEVENT" -> current = mutableListOf()
                line == "END:VEVENT" -> {
                    current?.let { props -> toEvent(props)?.let(out::add) }
                    current = null
                }
                current != null -> {
                    val prop = parseLine(line)
                    if (prop != null) current.add(prop)
                }
            }
        }
        return out
    }

    private fun parseLine(line: String): Prop? {
        // Find the first ':' that isn't inside a quoted parameter value.
        var inQuotes = false
        var colon = -1
        for ((i, c) in line.withIndex()) {
            if (c == '"') inQuotes = !inQuotes
            if (c == ':' && !inQuotes) { colon = i; break }
        }
        if (colon <= 0) return null
        val head = line.substring(0, colon).split(';')
        val params = head.drop(1).mapNotNull { p ->
            val eq = p.indexOf('=')
            if (eq > 0) p.substring(0, eq).uppercase() to p.substring(eq + 1).trim('"') else null
        }.toMap()
        return Prop(head[0].uppercase(), params, line.substring(colon + 1))
    }

    private fun unescape(s: String) = s.replace("\\n", " ").replace("\\N", " ")
        .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\").trim()

    private fun toEvent(props: List<Prop>): FoundEvent? {
        fun get(n: String) = props.firstOrNull { it.name == n }
        if (get("STATUS")?.value?.uppercase() == "CANCELLED") return null
        val title = get("SUMMARY")?.value?.let(::unescape)?.takeIf { it.isNotBlank() } ?: return null
        val start = get("DTSTART") ?: return null
        val (date, time) = parseStart(start) ?: return null
        val description = get("DESCRIPTION")?.value?.let(::unescape)
        val location = get("LOCATION")?.value?.let(::unescape)?.takeIf { it.isNotBlank() }

        val link = get("URL")?.value?.trim()?.takeIf { it.startsWith("http") }
            ?: description?.let { Regex("""https?://[^\s<>"]+""").find(it)?.value?.trimEnd('.', ',', ')') }
            ?: "https://www.google.com/search?q=" + URLEncoder.encode("$title $date", "UTF-8")

        // RFC 7986 IMAGE, or an ATTACH that's an image.
        val image = props.firstOrNull {
            (it.name == "IMAGE" || (it.name == "ATTACH" && it.params["FMTTYPE"]?.startsWith("image") == true)) &&
                it.value.startsWith("http")
        }?.value?.trim()

        // Locations are often "Circuit, Town, Country" — first part as venue, rest as city.
        val parts = location?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        return FoundEvent(
            title = title,
            date = date,
            time = time,
            price = null,
            venue = parts.firstOrNull(),
            city = parts.drop(1).joinToString(", ").ifBlank { null },
            imageUrl = image,
            url = link,
            source = name,
        )
    }

    private val london = ZoneId.of("Europe/London")
    private val dateTimeFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    /** Returns the start date and (if timed) the London-local start time. */
    private fun parseStart(p: Prop): Pair<LocalDate, String?>? = try {
        val v = p.value.trim()
        when {
            p.params["VALUE"] == "DATE" || v.length == 8 ->
                LocalDate.parse(v.take(8), DateTimeFormatter.BASIC_ISO_DATE) to null
            v.endsWith("Z") -> {
                val local = LocalDateTime.parse(v.dropLast(1).take(15), dateTimeFmt)
                    .atOffset(ZoneOffset.UTC).atZoneSameInstant(london)
                local.toLocalDate() to local.format(timeFmt)
            }
            else -> {
                val zone = p.params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: london
                val local = LocalDateTime.parse(v.take(15), dateTimeFmt).atZone(zone).withZoneSameInstant(london)
                local.toLocalDate() to local.format(timeFmt)
            }
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        fun normaliseFeedUrl(raw: String): String {
            val u = raw.trim()
            return when {
                u.startsWith("webcal://", ignoreCase = true) -> "https://" + u.substring(9)
                u.startsWith("webcals://", ignoreCase = true) -> "https://" + u.substring(10)
                u.startsWith("http") -> u
                else -> "https://$u"
            }
        }

        private val ukWords = listOf(
            // countries / regions
            "united kingdom", "great britain", "england", "scotland", "wales", "northern ireland",
            "britain", "british", " uk", "uk ", "(uk)", ", uk", " gb",
            // circuits & venues
            "silverstone", "brands hatch", "donington", "oulton park", "snetterton", "thruxton", "croft",
            "knockhill", "cadwell", "castle combe", "goodwood", "anglesey", "pembrey", "mallory park",
            "lydden", "kirkistown", "bishopscourt", "three sisters", "blyton", "curborough", "shelsley",
            "prescott", "harewood", "gurston", "loton park", "doune", "santa pod", "rockingham",
            "elvington", "llandow", "aintree", "ty croes", "trac mon",
            // rally regions often used as event names
            "ulster", "grampian", "dukeries", "malcolm wilson", "carlisle", "kielder", "dyfi",
            "harry flatters", "nicky grist", "trackrod", "jim clark", "galloway", "mull", "isle of man",
            "rally north wales", "cambrian", "east riding", "plains", "tour of epynt", "epynt",
        )

        fun looksUk(e: FoundEvent): Boolean {
            val hay = " " + listOfNotNull(e.title, e.venue, e.city).joinToString(" ").lowercase() + " "
            return ukWords.any { it in hay }
        }
    }
}
