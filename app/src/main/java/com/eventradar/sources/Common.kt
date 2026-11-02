package com.eventradar.sources

import com.eventradar.data.Interest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** An event as returned by a source, before de-duplication and storage. */
data class FoundEvent(
    val title: String,
    val date: LocalDate,
    val time: String?,
    val price: String?,
    val venue: String?,
    val city: String?,
    val imageUrl: String?,
    val url: String,
    val source: String,
)

interface EventSource {
    val name: String
    /** Whether this source should be asked about [interest] at all. */
    fun appliesTo(interest: Interest): Boolean = true
    suspend fun search(interest: Interest, from: LocalDate, to: LocalDate): List<FoundEvent>
}

class SourceException(message: String) : IOException(message)

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    fun getJson(url: String, sourceName: String): JsonObject {
        val req = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw SourceException("$sourceName: HTTP ${resp.code}")
            return Json.parseToJsonElement(body) as? JsonObject
                ?: throw SourceException("$sourceName: unexpected response")
        }
    }
}

// --- Small helpers for picking values out of loosely structured JSON ---------------------------

fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray
fun JsonElement?.asObj(): JsonObject? = this as? JsonObject

fun parseDateOrNull(s: String?): LocalDate? = try {
    s?.take(10)?.let { LocalDate.parse(it) }
} catch (_: Exception) {
    null
}

/** Normalises "19:30:00", "7:30", "19.30" etc. to "HH:mm"; anything else becomes null. */
fun normaliseTime(s: String?): String? {
    if (s.isNullOrBlank()) return null
    val m = Regex("""(\d{1,2})[:.](\d{2})""").find(s) ?: return null
    val h = m.groupValues[1].toInt()
    val min = m.groupValues[2].toInt()
    if (h !in 0..23 || min !in 0..59) return null
    if (h == 0 && min == 0) return null // usually means "time unknown"
    return "%02d:%02d".format(h, min)
}

fun formatMoney(amount: Double, currency: String?): String {
    val symbol = when (currency?.uppercase()) {
        null, "", "GBP" -> "£"; "EUR" -> "€"; "USD" -> "$"; else -> "$currency "
    }
    return if (amount % 1.0 == 0.0) "$symbol${amount.toInt()}" else "$symbol${"%.2f".format(amount)}"
}
