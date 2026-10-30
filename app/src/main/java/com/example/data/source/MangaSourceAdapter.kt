package com.example.data.source

import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaChapter
import com.example.data.MangaLatestUpdateItem
import com.example.data.MangaWork
import com.example.data.local.SiteEntity
import java.util.Locale

data class SourceCapabilities(
    val adapterId: String,
    val adapterName: String,
    val supportsWebViewBrowsing: Boolean = true,
    val supportsCatalogExtraction: Boolean,
    val supportsDetailsExtraction: Boolean,
    val supportsChapterExtraction: Boolean,
    val supportsPageExtraction: Boolean,
    val supportsOfflineDownload: Boolean,
    val supportsLatestUpdates: Boolean = supportsCatalogExtraction
) {
    val supportsChapterDownload: Boolean get() = supportsOfflineDownload
    val supportsChapterDetection: Boolean get() = supportsChapterExtraction
    val supportsOfflineReader: Boolean get() = supportsOfflineDownload
}

data class AdapterScrapeResult<T>(
    val data: T,
    val requiresCloudflareChallenge: Boolean = false,
    val errorMessage: String? = null,
    val hasNextPage: Boolean = false
)

/**
 * Unified interface for source connectors (MangaSourceAdapter).
 * Allows adding dedicated site support without modifying the core storage or reader architecture,
 * and explicitly reports source capabilities when a site only supports WebView browsing.
 */
interface MangaSourceAdapter {
    val capabilities: SourceCapabilities

    fun canHandle(site: SiteEntity): Boolean

    suspend fun fetchCatalog(
        site: SiteEntity,
        page: Int = 1,
        searchQuery: String = "",
        genre: String? = null,
        sortMode: String = "آخر الاضافات"
    ): AdapterScrapeResult<List<MangaWork>>

    suspend fun fetchLatestUpdates(
        site: SiteEntity,
        page: Int = 1
    ): AdapterScrapeResult<List<MangaLatestUpdateItem>>

    suspend fun fetchMangaDetails(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<MangaWork>

    suspend fun fetchChapterList(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<List<MangaChapter>>

    suspend fun fetchChapterPageUrls(
        site: SiteEntity,
        work: MangaWork,
        chapter: MangaChapter
    ): AdapterScrapeResult<List<String>>
}

/**
 * Adapter for WordPress Madara / Arabic Manga sites (e.g., Mangalek, Azora, Manga-Alarab, etc.).
 */
class MadaraMangaSourceAdapter : MangaSourceAdapter {
    override val capabilities = SourceCapabilities(
        adapterId = "madara",
        adapterName = "موصل Madara للمانجا والمانهوا",
        supportsWebViewBrowsing = true,
        supportsCatalogExtraction = true,
        supportsDetailsExtraction = true,
        supportsChapterExtraction = true,
        supportsPageExtraction = true,
        supportsOfflineDownload = true,
        supportsLatestUpdates = true
    )

    override fun canHandle(site: SiteEntity): Boolean {
        val domain = site.domain.lowercase(Locale.ROOT)
        return site.adapterKey == "madara" ||
            site.adapterType == "madara" ||
            domain.contains("mangalik") ||
            domain.contains("mangalek") ||
            domain.contains("azora") ||
            domain.contains("manga") ||
            domain.contains("lek") ||
            domain.contains("manhwa")
    }

    override suspend fun fetchCatalog(
        site: SiteEntity,
        page: Int,
        searchQuery: String,
        genre: String?,
        sortMode: String
    ): AdapterScrapeResult<List<MangaWork>> {
        val pagedSite = if (page > 1 && searchQuery.isBlank()) {
            site.copy(url = "${site.url.trimEnd('/')}/page/$page/")
        } else {
            site
        }
        val res = LiveMangaSiteScraper.fetchSiteCatalog(
            site = pagedSite,
            searchQuery = searchQuery,
            selectedGenre = genre
        )
        return AdapterScrapeResult(
            data = res.works,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage,
            hasNextPage = res.works.size >= 8
        )
    }

    override suspend fun fetchLatestUpdates(
        site: SiteEntity,
        page: Int
    ): AdapterScrapeResult<List<MangaLatestUpdateItem>> {
        val res = LiveMangaSiteScraper.fetchLatestUpdates(site, page)
        return AdapterScrapeResult(
            data = res.updates,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage,
            hasNextPage = res.hasNextPage
        )
    }

    override suspend fun fetchMangaDetails(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<MangaWork> {
        val res = LiveMangaSiteScraper.fetchMangaDetails(work)
        return AdapterScrapeResult(
            data = res.work,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage
        )
    }

    override suspend fun fetchChapterList(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<List<MangaChapter>> {
        val details = LiveMangaSiteScraper.fetchMangaDetails(work)
        return AdapterScrapeResult(
            data = details.work.chapters,
            requiresCloudflareChallenge = details.requiresCloudflare,
            errorMessage = details.errorMessage
        )
    }

    override suspend fun fetchChapterPageUrls(
        site: SiteEntity,
        work: MangaWork,
        chapter: MangaChapter
    ): AdapterScrapeResult<List<String>> {
        val res = LiveMangaSiteScraper.fetchChapterPages(work, chapter)
        return AdapterScrapeResult(
            data = res.pageUrls,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage
        )
    }
}

/**
 * Adapter for MangaStream / Team-X / Olympus-style sites.
 */
class MangaStreamSourceAdapter : MangaSourceAdapter {
    override val capabilities = SourceCapabilities(
        adapterId = "mangastream",
        adapterName = "موصل MangaStream / Team-X",
        supportsWebViewBrowsing = true,
        supportsCatalogExtraction = true,
        supportsDetailsExtraction = true,
        supportsChapterExtraction = true,
        supportsPageExtraction = true,
        supportsOfflineDownload = true,
        supportsLatestUpdates = true
    )

    override fun canHandle(site: SiteEntity): Boolean {
        val domain = site.domain.lowercase(Locale.ROOT)
        return site.adapterKey == "mangastream" ||
            site.adapterType == "mangastream" ||
            domain.contains("olympu") ||
            domain.contains("teamx") ||
            domain.contains("team-x") ||
            domain.contains("swat") ||
            domain.contains("prochan") ||
            domain.contains("ares")
    }

    override suspend fun fetchCatalog(
        site: SiteEntity,
        page: Int,
        searchQuery: String,
        genre: String?,
        sortMode: String
    ): AdapterScrapeResult<List<MangaWork>> {
        val pagedSite = if (page > 1 && searchQuery.isBlank()) {
            site.copy(url = "${site.url.trimEnd('/')}/series?page=$page")
        } else {
            site
        }
        val res = LiveMangaSiteScraper.fetchSiteCatalog(
            site = pagedSite,
            searchQuery = searchQuery,
            selectedGenre = genre
        )
        return AdapterScrapeResult(
            data = res.works,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage,
            hasNextPage = res.works.size >= 8
        )
    }

    override suspend fun fetchLatestUpdates(
        site: SiteEntity,
        page: Int
    ): AdapterScrapeResult<List<MangaLatestUpdateItem>> {
        val res = LiveMangaSiteScraper.fetchLatestUpdates(site, page)
        return AdapterScrapeResult(
            data = res.updates,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage,
            hasNextPage = res.hasNextPage
        )
    }

    override suspend fun fetchMangaDetails(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<MangaWork> {
        val res = LiveMangaSiteScraper.fetchMangaDetails(work)
        return AdapterScrapeResult(
            data = res.work,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage
        )
    }

    override suspend fun fetchChapterList(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<List<MangaChapter>> {
        val details = LiveMangaSiteScraper.fetchMangaDetails(work)
        return AdapterScrapeResult(
            data = details.work.chapters,
            requiresCloudflareChallenge = details.requiresCloudflare,
            errorMessage = details.errorMessage
        )
    }

    override suspend fun fetchChapterPageUrls(
        site: SiteEntity,
        work: MangaWork,
        chapter: MangaChapter
    ): AdapterScrapeResult<List<String>> {
        val res = LiveMangaSiteScraper.fetchChapterPages(work, chapter)
        return AdapterScrapeResult(
            data = res.pageUrls,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage
        )
    }
}

/**
 * Adapter for sites marked as WebView-only (where direct extraction/download is not supported).
 * Ensures the app clearly communicates that only WebView browsing is available for this source.
 */
class WebViewOnlySourceAdapter : MangaSourceAdapter {
    override val capabilities = SourceCapabilities(
        adapterId = "webview_only",
        adapterName = "تصفح مباشر عبر WebView فقط",
        supportsWebViewBrowsing = true,
        supportsCatalogExtraction = false,
        supportsDetailsExtraction = false,
        supportsChapterExtraction = false,
        supportsPageExtraction = false,
        supportsOfflineDownload = false,
        supportsLatestUpdates = false
    )

    override fun canHandle(site: SiteEntity): Boolean {
        return site.adapterKey == "webview_only" ||
            site.adapterType == "webview_only" ||
            !site.supportsChapterDownload
    }

    override suspend fun fetchCatalog(
        site: SiteEntity,
        page: Int,
        searchQuery: String,
        genre: String?,
        sortMode: String
    ): AdapterScrapeResult<List<MangaWork>> {
        return AdapterScrapeResult(
            data = emptyList(),
            errorMessage = "هذا المصدر يدعم التصفح داخل WebView فقط ولا يدعم استخراج القوائم تلقائيًا."
        )
    }

    override suspend fun fetchLatestUpdates(
        site: SiteEntity,
        page: Int
    ): AdapterScrapeResult<List<MangaLatestUpdateItem>> {
        return AdapterScrapeResult(
            data = emptyList(),
            errorMessage = "هذا المصدر (${site.name}) يدعم التصفح داخل WebView فقط ولا يدعم استخراج آخر التحديثات تلقائيًا."
        )
    }

    override suspend fun fetchMangaDetails(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<MangaWork> {
        return AdapterScrapeResult(
            data = work,
            errorMessage = "هذا المصدر يدعم التصفح داخل WebView فقط."
        )
    }

    override suspend fun fetchChapterList(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<List<MangaChapter>> {
        return AdapterScrapeResult(
            data = emptyList(),
            errorMessage = "استخراج الفصول غير مدعوم لهذا المصدر. يمكنك قراءته عبر WebView."
        )
    }

    override suspend fun fetchChapterPageUrls(
        site: SiteEntity,
        work: MangaWork,
        chapter: MangaChapter
    ): AdapterScrapeResult<List<String>> {
        return AdapterScrapeResult(
            data = emptyList(),
            errorMessage = "تنزيل وعرض الصور المحلي غير مدعوم لهذا المصدر. افتح الفصل في WebView."
        )
    }
}

/**
 * Default HTML connector that attempts standard manga HTML discovery and reports accurately
 * if a site does not expose manga/chapter structures.
 */
class GenericMangaSourceAdapter : MangaSourceAdapter {
    override val capabilities = SourceCapabilities(
        adapterId = "generic_manga",
        adapterName = "موصل مواقع المانجا العام",
        supportsWebViewBrowsing = true,
        supportsCatalogExtraction = true,
        supportsDetailsExtraction = true,
        supportsChapterExtraction = true,
        supportsPageExtraction = true,
        supportsOfflineDownload = true,
        supportsLatestUpdates = true
    )

    override fun canHandle(site: SiteEntity): Boolean = true

    override suspend fun fetchCatalog(
        site: SiteEntity,
        page: Int,
        searchQuery: String,
        genre: String?,
        sortMode: String
    ): AdapterScrapeResult<List<MangaWork>> {
        val pagedSite = if (page > 1 && searchQuery.isBlank()) {
            site.copy(url = "${site.url.trimEnd('/')}/page/$page/")
        } else {
            site
        }
        val res = LiveMangaSiteScraper.fetchSiteCatalog(
            site = pagedSite,
            searchQuery = searchQuery,
            selectedGenre = genre
        )
        return AdapterScrapeResult(
            data = res.works,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage,
            hasNextPage = res.works.size >= 8
        )
    }

    override suspend fun fetchLatestUpdates(
        site: SiteEntity,
        page: Int
    ): AdapterScrapeResult<List<MangaLatestUpdateItem>> {
        val res = LiveMangaSiteScraper.fetchLatestUpdates(site, page)
        return AdapterScrapeResult(
            data = res.updates,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage,
            hasNextPage = res.hasNextPage
        )
    }

    override suspend fun fetchMangaDetails(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<MangaWork> {
        val res = LiveMangaSiteScraper.fetchMangaDetails(work)
        return AdapterScrapeResult(
            data = res.work,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage
        )
    }

    override suspend fun fetchChapterList(
        site: SiteEntity,
        work: MangaWork
    ): AdapterScrapeResult<List<MangaChapter>> {
        val details = LiveMangaSiteScraper.fetchMangaDetails(work)
        return AdapterScrapeResult(
            data = details.work.chapters,
            requiresCloudflareChallenge = details.requiresCloudflare,
            errorMessage = details.errorMessage
        )
    }

    override suspend fun fetchChapterPageUrls(
        site: SiteEntity,
        work: MangaWork,
        chapter: MangaChapter
    ): AdapterScrapeResult<List<String>> {
        val res = LiveMangaSiteScraper.fetchChapterPages(work, chapter)
        return AdapterScrapeResult(
            data = res.pageUrls,
            requiresCloudflareChallenge = res.requiresCloudflare,
            errorMessage = res.errorMessage
        )
    }
}

object MangaSourceAdapterRegistry {
    private val webViewOnlyAdapter = WebViewOnlySourceAdapter()
    private val madaraAdapter = MadaraMangaSourceAdapter()
    private val mangaStreamAdapter = MangaStreamSourceAdapter()
    private val genericAdapter = GenericMangaSourceAdapter()

    fun getAdapterForSite(site: SiteEntity): MangaSourceAdapter {
        return when {
            webViewOnlyAdapter.canHandle(site) -> webViewOnlyAdapter
            mangaStreamAdapter.canHandle(site) -> mangaStreamAdapter
            madaraAdapter.canHandle(site) -> madaraAdapter
            else -> genericAdapter
        }
    }

    fun getCapabilitiesForSite(site: SiteEntity): SourceCapabilities {
        return getAdapterForSite(site).capabilities
    }

    fun getAdapterForWork(work: MangaWork, sites: List<SiteEntity>): MangaSourceAdapter {
        val matchedSite = sites.find {
            it.id == work.sourceId ||
                it.url.equals(work.siteUrl, ignoreCase = true) ||
                it.name.equals(work.siteName, ignoreCase = true)
        } ?: SiteEntity(
            id = work.sourceId,
            name = work.siteName,
            url = work.siteUrl,
            domain = work.siteUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
        )
        return getAdapterForSite(matchedSite)
    }
}
