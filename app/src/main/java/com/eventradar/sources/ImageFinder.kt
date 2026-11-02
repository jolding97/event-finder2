package com.eventradar.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * Finds a thumbnail for events that came back without one, by reading the event page's
 * og:image / twitter:image tag (the same picture shown when the link is shared).
 */
object ImageFinder {
    private val metaTag = Regex("""<meta\s[^>]*>""", RegexOption.IGNORE_CASE)
    private val keyAttr = Regex("""(?:property|name)\s*=\s*["'](og:image(?::secure_url)?|twitter:image(?::src)?)["']""", RegexOption.IGNORE_CASE)
    private val contentAttr = Regex("""content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    suspend fun find(pageUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(pageUrl)
                .header("User-Agent", Http.USER_AGENT)
                .header("Accept", "text/html")
                .build()
            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                // The tags live in <head>; no need to read the whole page.
                val html = resp.body?.source()?.let { src ->
                    src.request(300_000)
                    src.buffer.clone().readUtf8()
                } ?: return@use null
                val finalUrl = resp.request.url
                metaTag.findAll(html)
                    .filter { keyAttr.containsMatchIn(it.value) }
                    .mapNotNull { contentAttr.find(it.value)?.groupValues?.get(1) }
                    .map { it.replace("&amp;", "&") }
                    .mapNotNull { finalUrl.resolve(it)?.toString() ?: it.toHttpUrlOrNull()?.toString() }
                    .firstOrNull()
            }
        }.getOrNull()
    }
}
