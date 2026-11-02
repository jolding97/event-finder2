package com.eventradar.sources

import com.eventradar.data.Interest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.LocalDate

/** Ticketmaster Discovery API v2 — strong for UK concerts, sport, comedy and theatre. Free key. */
class TicketmasterSource(private val apiKey: String) : EventSource {
    override val name = "Ticketmaster"

    override suspend fun search(interest: Interest, from: LocalDate, to: LocalDate): List<FoundEvent> =
        withContext(Dispatchers.IO) {
            val url = "https://app.ticketmaster.com/discovery/v2/events.json".toHttpUrl().newBuilder()
                .addQueryParameter("apikey", apiKey.trim())
                .addQueryParameter("keyword", interest.name)
                .addQueryParameter("countryCode", "GB")
                .addQueryParameter("size", "50")
                .addQueryParameter("sort", "date,asc")
                .addQueryParameter("startDateTime", "${from}T00:00:00Z")
                .addQueryParameter("endDateTime", "${to}T23:59:59Z")
                .build().toString()

            val root = Http.getJson(url, name)
            val events = root.obj("_embedded")?.arr("events") ?: return@withContext emptyList()

            events.mapNotNull { el ->
                val e = el.asObj() ?: return@mapNotNull null
                val start = e.obj("dates")?.obj("start")
                val date = parseDateOrNull(start?.str("localDate")) ?: return@mapNotNull null
                val link = e.str("url") ?: return@mapNotNull null
                val venue = e.obj("_embedded")?.arr("venues")?.firstOrNull().asObj()

                FoundEvent(
                    title = e.str("name") ?: return@mapNotNull null,
                    date = date,
                    time = normaliseTime(start?.str("localTime")),
                    price = price(e),
                    venue = venue?.str("name"),
                    city = venue?.obj("city")?.str("name"),
                    imageUrl = bestImage(e),
                    url = link,
                    source = name,
                )
            }
        }

    private fun price(e: kotlinx.serialization.json.JsonObject): String? {
        val pr = e.arr("priceRanges")?.firstOrNull().asObj() ?: return null
        val cur = pr.str("currency")
        val min = pr["min"]?.jsonPrimitive?.doubleOrNull
        val max = pr["max"]?.jsonPrimitive?.doubleOrNull
        return when {
            min != null && max != null && max > min -> "${formatMoney(min, cur)} – ${formatMoney(max, cur)}"
            min != null -> formatMoney(min, cur)
            max != null -> formatMoney(max, cur)
            else -> null
        }
    }

    /** Prefer a wide 16:9 image of decent size for the thumbnails. */
    private fun bestImage(e: kotlinx.serialization.json.JsonObject): String? {
        val images = e.arr("images")?.mapNotNull { it.asObj() } ?: return null
        fun width(o: kotlinx.serialization.json.JsonObject) = o["width"]?.jsonPrimitive?.intOrNull ?: 0
        val wide = images.filter { it.str("ratio") == "16_9" && width(it) in 500..1400 }
        return (wide.maxByOrNull(::width) ?: images.maxByOrNull(::width))?.str("url")
    }
}
