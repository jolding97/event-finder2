package com.eventradar.sources

import com.eventradar.data.Interest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.LocalDate

/** Skiddle Events API — UK-focused: gigs, club nights, festivals, comedy. Free key. */
class SkiddleSource(private val apiKey: String) : EventSource {
    override val name = "Skiddle"

    override suspend fun search(interest: Interest, from: LocalDate, to: LocalDate): List<FoundEvent> =
        withContext(Dispatchers.IO) {
            val url = "https://www.skiddle.com/api/v1/events/search/".toHttpUrl().newBuilder()
                .addQueryParameter("api_key", apiKey.trim())
                .addQueryParameter("keyword", interest.name)
                .addQueryParameter("minDate", from.toString())
                .addQueryParameter("maxDate", to.toString())
                .addQueryParameter("limit", "50")
                .addQueryParameter("order", "0")
                .build().toString()

            val root = Http.getJson(url, name)
            if (root.str("error")?.let { it != "0" } == true) {
                throw SourceException("Skiddle: ${root.str("errormessage") ?: "error"}")
            }
            val results = root.arr("results") ?: return@withContext emptyList()

            results.mapNotNull { el ->
                val e = el.asObj() ?: return@mapNotNull null
                val date = parseDateOrNull(e.str("date") ?: e.str("startdate")) ?: return@mapNotNull null
                val venue = e.obj("venue")
                val entry = e.str("entryprice")
                FoundEvent(
                    title = e.str("eventname") ?: return@mapNotNull null,
                    date = date,
                    time = normaliseTime(e.obj("openingtimes")?.str("doorsopen") ?: e.str("startdate")?.drop(10)),
                    price = entry?.let { p ->
                        p.toDoubleOrNull()?.let { if (it == 0.0) "Free" else formatMoney(it, "GBP") } ?: p
                    },
                    venue = venue?.str("name"),
                    city = venue?.str("town"),
                    imageUrl = e.str("largeimageurl") ?: e.str("xlargeimageurl") ?: e.str("imageurl"),
                    url = e.str("link") ?: return@mapNotNull null,
                    source = name,
                )
            }
        }
}
