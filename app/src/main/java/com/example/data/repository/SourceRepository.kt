package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.SiteEntity
import com.example.data.source.MangaSourceAdapterRegistry
import com.example.data.source.SourceCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SourceConnectionTestResult(
    val isReachable: Boolean,
    val httpCode: Int,
    val latencyMs: Long,
    val statusLabel: String,
    val capabilities: SourceCapabilities
) {
    val message: String get() = statusLabel
}

/**
 * Dedicated repository for managing user-added manga sources (SiteEntity),
 * testing connection status, tracking the last visited WebView URL per source,
 * and reporting source adapter capabilities.
 */
class SourceRepository(
    private val dao: AppDao,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    val allSources: Flow<List<SiteEntity>> = dao.getAllSites()
    val enabledSources: Flow<List<SiteEntity>> = dao.getEnabledSites()

    fun getCapabilities(site: SiteEntity): SourceCapabilities {
        return MangaSourceAdapterRegistry.getAdapterForSite(site).capabilities
    }

    suspend fun getSourceById(id: Long): SiteEntity? = withContext(Dispatchers.IO) {
        dao.getSiteById(id)
    }

    suspend fun addSource(
        name: String,
        url: String,
        domain: String = "",
        iconKey: String = "globe",
        supportsChapterDownload: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        val normalizedUrl = if (trimmedUrl.startsWith("http://", ignoreCase = true) ||
            trimmedUrl.startsWith("https://", ignoreCase = true)
        ) {
            trimmedUrl
        } else {
            "https://$trimmedUrl"
        }
        val resolvedDomain = domain.ifBlank {
            try {
                URI(normalizedUrl).host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: trimmedUrl
            } catch (_: Exception) {
                trimmedUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
            }
        }
        val current = dao.getAllSitesOnce()
        val existing = current.find {
            it.domain.equals(resolvedDomain, ignoreCase = true) ||
                it.url.equals(normalizedUrl, ignoreCase = true)
        }
        if (existing != null) {
            val updated = existing.copy(
                name = name.ifBlank { existing.name },
                isEnabled = true,
                supportsChapterDownload = supportsChapterDownload
            )
            dao.updateSite(updated)
            return@withContext existing.id
        }
        val nextSortOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.insertSite(
            SiteEntity(
                name = name.ifBlank { resolvedDomain },
                url = normalizedUrl,
                domain = resolvedDomain,
                isEnabled = true,
                sortOrder = nextSortOrder,
                iconKey = iconKey,
                supportsChapterDownload = supportsChapterDownload
            )
        )
    }

    suspend fun updateSource(site: SiteEntity) = withContext(Dispatchers.IO) {
        dao.updateSite(site)
    }

    suspend fun deleteSource(site: SiteEntity) = withContext(Dispatchers.IO) {
        dao.deleteSite(site)
    }

    suspend fun updateLastVisitedUrl(siteId: Long, url: String) = withContext(Dispatchers.IO) {
        if (siteId <= 0L || url.isBlank()) return@withContext
        val existing = dao.getSiteById(siteId) ?: return@withContext
        dao.updateSite(existing.copy(lastVisitedUrl = url))
    }

    suspend fun updateLastVisitedUrl(site: SiteEntity, url: String) = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext
        dao.updateSite(site.copy(lastVisitedUrl = url))
    }

    suspend fun testSourceConnection(url: String): SourceConnectionTestResult = withContext(Dispatchers.IO) {
        val cleanUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val domain = cleanUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
        val tempSite = SiteEntity(name = domain, url = cleanUrl, domain = domain)
        testSourceConnection(tempSite)
    }

    suspend fun testSourceConnection(site: SiteEntity): SourceConnectionTestResult = withContext(Dispatchers.IO) {
        val capabilities = getCapabilities(site)
        val start = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(site.url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                .build()
            client.newCall(request).execute().use { response ->
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                val code = response.code
                val bodySample = response.body?.string()?.take(4096) ?: ""
                val isCloudflare = code in listOf(403, 503) ||
                    bodySample.contains("cf-browser-verification", ignoreCase = true) ||
                    bodySample.contains("Just a moment...", ignoreCase = true)

                val statusLabel = when {
                    response.isSuccessful -> "متصل (${elapsed}ms)"
                    isCloudflare -> "يتطلب التحقق من حماية الموقع (Cloudflare)"
                    else -> "استجابة الخادم: HTTP $code"
                }
                if (site.id > 0L) {
                    dao.updateSite(site.copy(connectionStatus = statusLabel))
                }
                SourceConnectionTestResult(
                    isReachable = response.isSuccessful || isCloudflare,
                    httpCode = code,
                    latencyMs = elapsed,
                    statusLabel = statusLabel,
                    capabilities = capabilities
                )
            }
        } catch (_: Exception) {
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            val statusLabel = "تعذر الاتصال: تحقق من الشبكة أو الرابط"
            if (site.id > 0L) {
                dao.updateSite(site.copy(connectionStatus = statusLabel))
            }
            SourceConnectionTestResult(
                isReachable = false,
                httpCode = 0,
                latencyMs = elapsed,
                statusLabel = statusLabel,
                capabilities = capabilities
            )
        }
    }
}
