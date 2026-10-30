package com.example.data

import android.webkit.CookieManager
import com.example.data.local.SiteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CatalogScrapeResult(
    val works: List<MangaWork>,
    val requiresCloudflare: Boolean = false,
    val errorMessage: String? = null
)

data class DetailsScrapeResult(
    val work: MangaWork,
    val requiresCloudflare: Boolean = false,
    val errorMessage: String? = null
)

data class PageCandidateDiagnostic(
    val rawUrl: String,
    val resolvedUrl: String,
    val sourceElement: String,
    val attributeUsed: String,
    val accepted: Boolean,
    val reason: String
)

data class ChapterPagesScrapeResult(
    val pageUrls: List<String>,
    val nextChapterUrl: String? = null,
    val prevChapterUrl: String? = null,
    val requiresCloudflare: Boolean = false,
    val errorMessage: String? = null,
    val diagnostics: List<PageCandidateDiagnostic> = emptyList()
)

data class ParsedChapterNumber(
    val intNumber: Int,
    val doubleValue: Double,
    val label: String,
    val isSpecial: Boolean = false
)

data class LatestUpdatesScrapeResult(
    val updates: List<MangaLatestUpdateItem>,
    val hasNextPage: Boolean = false,
    val requiresCloudflare: Boolean = false,
    val errorMessage: String? = null
)

object LiveMangaSiteScraper {

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    @Volatile
    var activeUserAgent: String = DEFAULT_USER_AGENT

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getCookiesForUrl(url: String): String {
        if (url.isBlank()) return ""
        return try {
            CookieManager.getInstance()?.getCookie(url) ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * يدمج ملفات تعريف الارتباط (Cookies) من رابط الصورة (CDN) ورابط الصفحة المرجعية (Referer) ورابط الموقع الأصلي
     * لضمان نجاح تحميل الصور التي تتطلب جلسة Cloudflare أو جلسة موقع نشطة.
     */
    fun getCombinedCookies(
        targetUrl: String,
        refererUrl: String? = null,
        siteUrl: String? = null
    ): String {
        val cookiePairs = LinkedHashMap<String, String>()
        listOfNotNull(siteUrl, refererUrl, targetUrl).forEach { url ->
            val rawCookie = getCookiesForUrl(url)
            if (rawCookie.isNotBlank()) {
                rawCookie.split(";").forEach { segment ->
                    val trimmed = segment.trim()
                    if (trimmed.isNotBlank()) {
                        val key = trimmed.substringBefore("=").trim()
                        if (key.isNotBlank()) {
                            cookiePairs[key] = trimmed
                        }
                    }
                }
            }
        }
        return cookiePairs.values.joinToString("; ")
    }

    fun buildRequest(url: String, referer: String? = null, postFormBody: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", activeUserAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
        if (!referer.isNullOrBlank()) {
            builder.header("Referer", referer)
        }
        val cookie = getCombinedCookies(targetUrl = url, refererUrl = referer)
        if (cookie.isNotBlank()) {
            builder.header("Cookie", cookie)
        }
        if (postFormBody != null) {
            builder.header("X-Requested-With", "XMLHttpRequest")
            builder.post(postFormBody.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
        }
        return builder.build()
    }

    fun isCloudflareChallengeHtml(html: String, httpCode: Int = 200): Boolean {
        if (httpCode == 403 || httpCode == 503) {
            if (html.contains("cloudflare", ignoreCase = true) ||
                html.contains("cf-browser-verification", ignoreCase = true) ||
                html.contains("Just a moment", ignoreCase = true) ||
                html.contains("challenge-platform", ignoreCase = true)
            ) {
                return true
            }
        }
        return html.contains("<title>Just a moment...</title>", ignoreCase = true) ||
            html.contains("cf-challenge-running", ignoreCase = true) ||
            html.contains("Enable JavaScript and cookies to continue", ignoreCase = true) ||
            html.contains("تحقق من أنك إنسان", ignoreCase = true)
    }

    fun resolveUrl(baseUrl: String, rawHref: String): String {
        val cleaned = rawHref.trim()
            .replace("&amp;", "&")
            .removePrefix("url(")
            .removeSuffix(")")
            .trim('\'', '"', ' ')
        if (cleaned.isEmpty() || cleaned.startsWith("javascript:") || cleaned.startsWith("data:") || cleaned.startsWith("#")) {
            return ""
        }
        if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return cleaned.replace(" ", "%20")
        }
        if (cleaned.startsWith("//")) {
            return "https:${cleaned.replace(" ", "%20")}"
        }
        return try {
            val baseUri = URI(baseUrl.trimEnd('/'))
            val scheme = baseUri.scheme ?: "https"
            val host = baseUri.host ?: return cleaned.replace(" ", "%20")
            val portPart = if (baseUri.port != -1 && baseUri.port != 80 && baseUri.port != 443) ":${baseUri.port}" else ""
            val origin = "$scheme://$host$portPart"
            val normPath = cleaned.replace(" ", "%20")
            if (normPath.startsWith("/")) {
                "$origin$normPath"
            } else {
                "$origin/$normPath"
            }
        } catch (_: Exception) {
            cleaned.replace(" ", "%20")
        }
    }

    fun buildCatalogTargetUrl(
        site: SiteEntity,
        searchQuery: String = "",
        selectedGenre: String? = null
    ): String {
        val base = site.url.trimEnd('/')
        if (searchQuery.isNotBlank()) {
            val encoded = try {
                URLEncoder.encode(searchQuery.trim(), "UTF-8")
            } catch (_: Exception) {
                searchQuery.trim()
            }
            return when {
                base.contains("olympustaff", ignoreCase = true) || base.contains("team", ignoreCase = true) ->
                    "$base/series?search=$encoded"
                else ->
                    "$base/?s=$encoded&post_type=wp-manga"
            }
        }
        if (!selectedGenre.isNullOrBlank() && selectedGenre != "التصنيفات") {
            val encoded = try {
                URLEncoder.encode(selectedGenre.trim(), "UTF-8")
            } catch (_: Exception) {
                selectedGenre.trim()
            }
            return when {
                base.contains("olympustaff", ignoreCase = true) || base.contains("team", ignoreCase = true) ->
                    "$base/series?genre=$encoded"
                else ->
                    "$base/manga-genre/$encoded/"
            }
        }
        return base
    }

    suspend fun fetchSiteCatalog(
        site: SiteEntity,
        searchQuery: String = "",
        selectedGenre: String? = null
    ): CatalogScrapeResult = withContext(Dispatchers.IO) {
        val targetUrl = buildCatalogTargetUrl(site, searchQuery, selectedGenre)
        try {
            val request = buildRequest(targetUrl, referer = site.url)
            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: ""
                if (isCloudflareChallengeHtml(html, response.code)) {
                    return@withContext CatalogScrapeResult(
                        works = emptyList(),
                        requiresCloudflare = true,
                        errorMessage = "الموقع محمي بواسطة Cloudflare، جاري فتح جسر الحماية التلقائي..."
                    )
                }
                var works = parseCatalogHtml(html, site, targetUrl)
                // If homepage had few works and site is olympustaff/team-x or madara, also try /series or /manga
                if (works.size < 3 && searchQuery.isBlank() && selectedGenre == null) {
                    val altPaths = listOf("/series", "/manga", "/series/")
                    for (path in altPaths) {
                        val altUrl = site.url.trimEnd('/') + path
                        try {
                            httpClient.newCall(buildRequest(altUrl, referer = site.url)).execute().use { altResp ->
                                val altHtml = altResp.body?.string() ?: ""
                                val altWorks = parseCatalogHtml(altHtml, site, altUrl)
                                if (altWorks.size > works.size) {
                                    works = altWorks
                                }
                            }
                        } catch (_: Exception) {
                        }
                        if (works.size >= 6) break
                    }
                }
                CatalogScrapeResult(works = works, requiresCloudflare = false)
            }
        } catch (e: Exception) {
            CatalogScrapeResult(
                works = emptyList(),
                requiresCloudflare = false,
                errorMessage = e.localizedMessage ?: "تعذر الاتصال بالموقع"
            )
        }
    }

    fun parseCatalogHtml(html: String, site: SiteEntity, pageUrl: String = site.url): List<MangaWork> {
        if (html.isBlank()) return emptyList()
        val results = LinkedHashMap<String, MangaWork>()

        // Clean script and style blocks to speed up regex scanning
        val bodyHtml = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")

        // Strategy 1: Scan structured card blocks (Team-X .bs/.bsx/.box, Madara .page-item-detail/.c-tabs-item__content, MangaStream .utao/.bs)
        val blockRegex = Regex(
            """<(?:div|article|li)[^>]+class=["'][^"']*(?:page-item-detail|c-tabs-item__content|bsx|bs|manga__item|post-item|series-card|animepost|item-summary|utao|box)[^"']*["'][^>]*>([\s\S]{40,3500}?)</(?:div|article|li)>""",
            RegexOption.IGNORE_CASE
        )
        blockRegex.findAll(bodyHtml).forEach { match ->
            val block = match.groupValues[1]
            val work = extractWorkFromCardFragment(block, site, pageUrl)
            if (work != null && work.mangaUrl.isNotBlank() && !results.containsKey(work.mangaUrl)) {
                results[work.mangaUrl] = work
            }
        }

        // Strategy 2: Scan all anchor tags wrapping an <img> that point to manga/series/work paths
        val anchorWithImgRegex = Regex(
            """<a[^>]+href=["']([^"']+)["'][^>]*>([\s\S]{10,1800}?)</a>""",
            RegexOption.IGNORE_CASE
        )
        anchorWithImgRegex.findAll(bodyHtml).forEach { match ->
            val rawHref = match.groupValues[1]
            val inner = match.groupValues[2]
            if (!inner.contains("<img", ignoreCase = true) && !inner.contains("background-image", ignoreCase = true)) {
                return@forEach
            }
            val resolvedHref = resolveUrl(pageUrl, rawHref)
            if (!isLikelyMangaWorkUrl(resolvedHref, site.url)) return@forEach

            val imgUrl = extractBestImageUrl(inner, pageUrl)
            if (imgUrl.isBlank() || isIgnoredAssetImage(imgUrl)) return@forEach

            val rawTitle = extractAttribute(match.value, "title")
                .ifBlank { extractAttribute(inner, "alt") }
                .ifBlank { stripHtmlTags(inner) }
                .let { cleanMangaTitle(it) }

            if (rawTitle.length >= 2 && !results.containsKey(resolvedHref)) {
                val id = slugFromUrl(resolvedHref)
                results[resolvedHref] = MangaWork(
                    id = id,
                    title = rawTitle,
                    mangaUrl = resolvedHref,
                    siteName = site.name,
                    siteUrl = site.url,
                    siteIconKey = site.iconKey,
                    coverImageUrl = imgUrl
                )
            } else if (results.containsKey(resolvedHref)) {
                val existing = results[resolvedHref]!!
                if (existing.coverImageUrl.isNullOrBlank() && imgUrl.isNotBlank()) {
                    results[resolvedHref] = existing.copy(coverImageUrl = imgUrl)
                }
            }
        }

        // Strategy 3: Scan nearby titles for works whose title was only in a sibling heading
        return results.values.toList()
    }

    private fun extractWorkFromCardFragment(fragment: String, site: SiteEntity, baseUrl: String): MangaWork? {
        val hrefRegex = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val hrefs = hrefRegex.findAll(fragment)
            .map { resolveUrl(baseUrl, it.groupValues[1]) }
            .filter { isLikelyMangaWorkUrl(it, site.url) }
            .toList()

        val mangaUrl = hrefs.firstOrNull() ?: return null
        val imgUrl = extractBestImageUrl(fragment, baseUrl)
        if (imgUrl.isBlank() || isIgnoredAssetImage(imgUrl)) return null

        val headingRegex = Regex("""<(?:h2|h3|h4|h5|div|span)[^>]*class=["'][^"']*(?:post-title|tt|title|series-title|name)[^"']*["'][^>]*>([\s\S]*?)</(?:h2|h3|h4|h5|div|span)>""", RegexOption.IGNORE_CASE)
        val headingTitle = headingRegex.find(fragment)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) } ?: ""

        val titleAttr = extractAttribute(fragment, "title")
        val altAttr = extractAttribute(fragment, "alt")

        val title = listOf(headingTitle, titleAttr, altAttr)
            .map { cleanMangaTitle(it) }
            .firstOrNull { it.length >= 2 && !it.equals("مانجا", ignoreCase = true) }
            ?: cleanMangaTitle(slugFromUrl(mangaUrl).replace("-", " "))

        if (title.isBlank()) return null

        return MangaWork(
            id = slugFromUrl(mangaUrl),
            title = title,
            mangaUrl = mangaUrl,
            siteName = site.name,
            siteUrl = site.url,
            siteIconKey = site.iconKey,
            coverImageUrl = imgUrl
        )
    }

    suspend fun fetchMangaDetails(work: MangaWork): DetailsScrapeResult = withContext(Dispatchers.IO) {
        val targetUrl = work.mangaUrl.ifBlank { "${work.siteUrl.trimEnd('/')}/series/${work.id}" }
        try {
            val request = buildRequest(targetUrl, referer = work.siteUrl)
            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: ""
                if (isCloudflareChallengeHtml(html, response.code)) {
                    return@withContext DetailsScrapeResult(
                        work = work,
                        requiresCloudflare = true,
                        errorMessage = "يتطلب الموقع تخطي حماية Cloudflare لعرض الفصول"
                    )
                }
                var parsedWork = parseMangaDetailsHtml(html, work, targetUrl)

                // Madara WordPress AJAX chapters fallback if chapters weren't in initial HTML
                if (parsedWork.chapters.isEmpty()) {
                    val ajaxUrl = targetUrl.trimEnd('/') + "/ajax/chapters/"
                    try {
                        httpClient.newCall(buildRequest(ajaxUrl, referer = targetUrl, postFormBody = "")).execute().use { ajaxResp ->
                            val ajaxHtml = ajaxResp.body?.string() ?: ""
                            val ajaxChapters = parseChaptersFromHtmlFragment(ajaxHtml, work.id, targetUrl)
                            if (ajaxChapters.isNotEmpty()) {
                                parsedWork = parsedWork.copy(
                                    chapters = ajaxChapters,
                                    chaptersCount = ajaxChapters.size,
                                    isDetailsLoaded = true
                                )
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                // Discover additional chapter list pagination pages if the work page paginates chapters
                val paginationUrls = extractChapterPaginationUrls(html, targetUrl)
                if (paginationUrls.isNotEmpty()) {
                    val mergedByUrl = LinkedHashMap<String, MangaChapter>()
                    parsedWork.chapters.forEach { ch ->
                        mergedByUrl[normalizeChapterUrlKey(ch.chapterUrl)] = ch
                    }
                    for (pageLink in paginationUrls.take(5)) {
                        try {
                            httpClient.newCall(buildRequest(pageLink, referer = targetUrl)).execute().use { pageResp ->
                                val pageHtml = pageResp.body?.string() ?: ""
                                if (pageResp.isSuccessful && pageHtml.isNotBlank() && !isCloudflareChallengeHtml(pageHtml, pageResp.code)) {
                                    val extraChapters = parseChaptersFromHtmlFragment(pageHtml, work.id, targetUrl)
                                    extraChapters.forEach { extra ->
                                        val normKey = normalizeChapterUrlKey(extra.chapterUrl)
                                        if (normKey.isNotBlank() && !mergedByUrl.containsKey(normKey)) {
                                            mergedByUrl[normKey] = extra
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    val finalChapters = deduplicateAndSortChapters(mergedByUrl.values.toList(), work.id)
                    parsedWork = parsedWork.copy(
                        chapters = finalChapters,
                        chaptersCount = finalChapters.size,
                        isDetailsLoaded = true
                    )
                }

                DetailsScrapeResult(work = parsedWork, requiresCloudflare = false)
            }
        } catch (e: Exception) {
            DetailsScrapeResult(
                work = work,
                requiresCloudflare = false,
                errorMessage = e.localizedMessage ?: "تعذر جلب تفاصيل المانجا"
            )
        }
    }

    fun parseMangaDetailsHtml(html: String, baseWork: MangaWork, pageUrl: String): MangaWork {
        if (html.isBlank()) return baseWork

        // Title extraction
        val h1Title = Regex("""<h1[^>]*>([\s\S]*?)</h1>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.let { cleanMangaTitle(stripHtmlTags(it)) }
            ?.takeIf { it.isNotBlank() }
        val ogTitle = extractMetaProperty(html, "og:title")
            .let { cleanMangaTitle(it) }
            .takeIf { it.isNotBlank() }

        // Cover extraction
        val summaryImgBlock = Regex(
            """<(?:div|figure)[^>]+class=["'][^"']*(?:summary_image|thumb|white-bg|series-thumb|manga-cover|poster)[^"']*["'][^>]*>([\s\S]{10,1200}?)</(?:div|figure)>""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.getOrNull(1) ?: ""
        val coverUrl = extractBestImageUrl(summaryImgBlock, pageUrl)
            .ifBlank { extractMetaProperty(html, "og:image") }
            .let { resolveUrl(pageUrl, it) }
            .takeIf { it.isNotBlank() && !isIgnoredAssetImage(it) }
            ?: (baseWork.coverImageUrl ?: "")

        // Description / Story extraction
        val descRegex = Regex(
            """<(?:div|p)[^>]+class=["'][^"']*(?:summary__content|description-summary|entry-content-single|review-content|story|manga-excerpt|desc|synopsis)[^"']*["'][^>]*>([\s\S]{15,6000}?)</(?:div|p)>""",
            RegexOption.IGNORE_CASE
        )
        val rawDesc = descRegex.find(html)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
            ?.ifBlank { extractMetaProperty(html, "og:description") }
            ?.ifBlank { extractMetaName(html, "description") }
            ?: baseWork.description

        // Status extraction
        val status = when {
            html.contains("مكتملة") || html.contains("Completed", ignoreCase = true) -> "مكتملة"
            html.contains("متوقفة") || html.contains("Hiatus", ignoreCase = true) -> "متوقفة"
            else -> "مستمرة"
        }

        // Type extraction
        val type = when {
            html.contains("مانها") || html.contains("Manhua", ignoreCase = true) || html.contains("صيني") -> "مانها\nصيني"
            html.contains("مانهوا") || html.contains("Manhwa", ignoreCase = true) || html.contains("كوري") -> "مانهوا\nكورية"
            html.contains("مانجا") || html.contains("Manga", ignoreCase = true) || html.contains("ياباني") -> "مانجا\nياباني"
            else -> baseWork.type
        }

        // Rating extraction
        val ratingMatch = Regex("""(?:post-total-rating|rating-prc|numscore|score)[^>]*>[\s\S]{0,120}?([0-9]\.[0-9])""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1) ?: baseWork.rating

        // Genres extraction
        val genres = mutableListOf<String>()
        val genreSectionRegex = Regex(
            """<(?:div|span|ul)[^>]+class=["'][^"']*(?:genres-content|mgen|wp-manga-tags-list|genre-info|review-author-info|seriestugenre)[^"']*["'][^>]*>([\s\S]{10,2500}?)</(?:div|span|ul)>""",
            RegexOption.IGNORE_CASE
        )
        genreSectionRegex.findAll(html).forEach { section ->
            Regex("""<a[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
                .findAll(section.groupValues[1])
                .forEach { aMatch ->
                    val g = stripHtmlTags(aMatch.groupValues[1]).trim()
                    if (g.length in 2..24 && g !in genres) {
                        genres.add(g)
                    }
                }
        }

        // Chapters extraction
        val chapters = parseChaptersFromHtmlFragment(html, baseWork.id, pageUrl)

        return baseWork.copy(
            title = h1Title ?: ogTitle ?: baseWork.title,
            coverImageUrl = coverUrl.ifBlank { baseWork.coverImageUrl },
            description = rawDesc.ifBlank { "لا يوجد وصف متاح لهذا العمل على الموقع." },
            status = status,
            type = type,
            rating = ratingMatch,
            genres = if (genres.isNotEmpty()) genres.take(8) else baseWork.genres,
            chapters = chapters,
            chaptersCount = chapters.size,
            isDetailsLoaded = true
        )
    }

    /**
     * يستخرج قائمة الفصول مع فصل صارم بين:
     * 1. تاريخ النشر (مثل 03/10/2026 أو 2026-10-03 أو منذ 3 ساعات)
     * 2. رقم الفصل الفعلي (بما يشمل الفصول العشرية مثل 20.1 و 20.5 والخاصة)
     * 3. عنوان الفصل النظيف بدون إقحام التاريخ داخله
     * كما يمنع تحويل العناوين المشوهة بالتاريخ مثل 201/10/2026 إلى الفصل 201.
     */
    fun parseChaptersFromHtmlFragment(html: String, workId: String, pageUrl: String): List<MangaChapter> {
        if (html.isBlank()) return emptyList()
        val rawChapters = mutableListOf<MangaChapter>()
        val seenUrlKeys = HashSet<String>()

        val cleanHtml = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")

        // Strategy 1: Structured chapter items (<li>, <tr>, <div class="chbox/eph-num/chapter-item">)
        val structuredItemRegex = Regex(
            """<(?:li|tr|div)[^>]*(?:class=["'][^"']*(?:wp-manga-chapter|ch-main-anchor|eph-num|chapter-li|chbox|chapter-item|a-h)[^"']*["']|data-num=["'][^"']+["'])[^>]*>([\s\S]{10,2000}?)</(?:li|tr|div)>""",
            RegexOption.IGNORE_CASE
        )

        structuredItemRegex.findAll(cleanHtml).forEach { match ->
            val fullTag = match.value
            val innerBlock = match.groupValues[1]
            val parsed = parseSingleChapterBlock(
                fullBlockHtml = fullTag,
                innerBlockHtml = innerBlock,
                workId = workId,
                pageUrl = pageUrl,
                fallbackOrder = rawChapters.size + 1
            )
            if (parsed != null) {
                val urlKey = normalizeChapterUrlKey(parsed.chapterUrl)
                if (urlKey.isNotBlank() && seenUrlKeys.add(urlKey)) {
                    rawChapters.add(parsed)
                }
            }
        }

        // Strategy 2: Scan inside explicit chapter list containers (.eplister, #chapterlist, .clstyle, .listing-chapters_wrap, .chapters-list)
        val containerRegex = Regex(
            """<(?:div|ul|section)[^>]+(?:id|class)=["'][^"']*(?:eplister|chapterlist|clstyle|listing-chapters_wrap|chapters-list|version-chap|page-content-listing|series-chapters)[^"']*["'][^>]*>([\s\S]{20,180000}?)</(?:div|ul|section)>""",
            RegexOption.IGNORE_CASE
        )
        val containerMatches = containerRegex.findAll(cleanHtml).toList()
        val anchorScopeHtml = if (containerMatches.isNotEmpty()) {
            containerMatches.joinToString("\n") { it.groupValues[1] }
        } else {
            cleanHtml
        }

        // Scan <li> blocks inside chapter containers even if they don't have a specific class
        if (containerMatches.isNotEmpty()) {
            Regex("""<li[^>]*>([\s\S]{10,1500}?)</li>""", RegexOption.IGNORE_CASE)
                .findAll(anchorScopeHtml)
                .forEach { liMatch ->
                    val parsed = parseSingleChapterBlock(
                        fullBlockHtml = liMatch.value,
                        innerBlockHtml = liMatch.groupValues[1],
                        workId = workId,
                        pageUrl = pageUrl,
                        fallbackOrder = rawChapters.size + 1
                    )
                    if (parsed != null) {
                        val urlKey = normalizeChapterUrlKey(parsed.chapterUrl)
                        if (urlKey.isNotBlank() && seenUrlKeys.add(urlKey)) {
                            rawChapters.add(parsed)
                        }
                    }
                }
        }

        // Strategy 3: Scan chapter anchors if not already captured
        // Note: If structured items were already found, only accept anchors that belong to the same work path
        // and aren't "First/Last chapter" summary buttons (which have classes like 'first-chapter', 'last-chapter', 'btn').
        val anchorRegex = Regex("""<a([^>]+href=["']([^"']+)["'][^>]*)>([\s\S]{1,800}?)</a>""", RegexOption.IGNORE_CASE)
        anchorRegex.findAll(anchorScopeHtml).forEach { match ->
            val aAttrs = match.groupValues[1]
            val rawHref = match.groupValues[2]
            val innerHtml = match.groupValues[3]
            if (aAttrs.contains("btn", ignoreCase = true) ||
                aAttrs.contains("first-chapter", ignoreCase = true) ||
                aAttrs.contains("last-chapter", ignoreCase = true) ||
                aAttrs.contains("bookmark", ignoreCase = true)
            ) {
                return@forEach
            }
            val chUrl = resolveUrl(pageUrl, rawHref)
            val urlKey = normalizeChapterUrlKey(chUrl)
            if (chUrl.isBlank() || urlKey.isBlank() || seenUrlKeys.contains(urlKey)) return@forEach

            val looksLikeChapterUrl = isLikelyChapterUrl(chUrl, pageUrl)
            val strippedCheck = stripHtmlTags(innerHtml)
            val looksLikeChapterText = strippedCheck.contains("فصل") ||
                strippedCheck.contains("الفصل") ||
                strippedCheck.contains("Chapter", ignoreCase = true) ||
                strippedCheck.contains("Ch.", ignoreCase = true)

            if (looksLikeChapterUrl || (looksLikeChapterText && isSameWorkContext(chUrl, pageUrl))) {
                val parsed = parseSingleChapterBlock(
                    fullBlockHtml = match.value,
                    innerBlockHtml = match.value,
                    workId = workId,
                    pageUrl = pageUrl,
                    fallbackOrder = rawChapters.size + 1
                )
                if (parsed != null && seenUrlKeys.add(urlKey)) {
                    rawChapters.add(parsed)
                }
            }
        }

        return deduplicateAndSortChapters(rawChapters, workId)
    }

    private fun parseSingleChapterBlock(
        fullBlockHtml: String,
        innerBlockHtml: String,
        workId: String,
        pageUrl: String,
        fallbackOrder: Int
    ): MangaChapter? {
        val aMatch = Regex("""<a([^>]+href=["']([^"']+)["'][^>]*)>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
            .find(innerBlockHtml) ?: return null
        val aAttrs = aMatch.groupValues[1]
        val chUrl = resolveUrl(pageUrl, aMatch.groupValues[2])
        if (chUrl.isBlank() || !isLikelyChapterUrl(chUrl, pageUrl)) return null

        // 1. Extract structural data-num / data-chapter if present on <li>, <div>, or <a>
        val structuralNumAttr = extractAttribute(fullBlockHtml, "data-num")
            .ifBlank { extractAttribute(fullBlockHtml, "data-chapter") }
            .ifBlank { extractAttribute(aAttrs, "data-num") }
            .trim()

        // 2. Extract release date from explicit date elements BEFORE stripping HTML tags for the title
        val dateElementRegex = Regex(
            """<(?:span|i|time|div|td)[^>]+class=["'][^"']*(?:chapter-release-date|chapterdate|date|post-on|time|released|ch-date)[^"']*["'][^>]*>([\s\S]*?)</(?:span|i|time|div|td)>|<time[^>]*>([\s\S]*?)</time>""",
            RegexOption.IGNORE_CASE
        )
        var extractedDate = ""
        dateElementRegex.findAll(fullBlockHtml).forEach { dMatch ->
            val candidate = stripHtmlTags(dMatch.groupValues[1].ifBlank { dMatch.groupValues[2] }).trim()
            if (candidate.isNotBlank() && extractedDate.isBlank()) {
                extractedDate = candidate
            }
        }

        // 3. Remove date elements, view counts, and download buttons from the anchor inner HTML before extracting title
        val cleanedAnchorInner = aMatch.groupValues[3]
            .replace(dateElementRegex, " ")
            .replace(
                Regex("""<(?:span|div|i)[^>]+class=["'][^"']*(?:view|views|download|dl|status|new|up)[^"']*["'][^>]*>[\s\S]*?</(?:span|div|i)>""", RegexOption.IGNORE_CASE),
                " "
            )

        // Prefer dedicated chapter title span (.chapternum, .chapter-title, .name, .epxs) if present
        val dedicatedTitleRegex = Regex(
            """<(?:span|div|p)[^>]+class=["'][^"']*(?:chapternum|chapter-title|chapter-number|ch-num|epxs|name)[^"']*["'][^>]*>([\s\S]*?)</(?:span|div|p)>""",
            RegexOption.IGNORE_CASE
        )
        val rawTitleCandidate = dedicatedTitleRegex.find(cleanedAnchorInner)?.groupValues?.getOrNull(1)
            ?.let { stripHtmlTags(it).trim() }
            ?.takeIf { it.isNotBlank() }
            ?: stripHtmlTags(cleanedAnchorInner).trim()

        // 4. Separate any remaining embedded dates (e.g. "03/10/2026" or "201/10/2026") from the title text
        val separated = separateTitleAndEmbeddedDate(
            rawTitle = rawTitleCandidate,
            existingDate = extractedDate,
            chapterUrl = chUrl,
            mangaPageUrl = pageUrl,
            structuralNumAttr = structuralNumAttr
        )

        // 5. Parse chapter number (supporting decimals like 20.1, 20.5 and verifying against URL)
        val numberInfo = parseChapterNumberInfo(
            cleanTitle = separated.cleanTitle,
            rawTitleBeforeDateStrip = rawTitleCandidate,
            chapterUrl = chUrl,
            mangaPageUrl = pageUrl,
            structuralNumAttr = structuralNumAttr,
            fallback = fallbackOrder
        )

        val finalTitle = separated.cleanTitle.ifBlank {
            "الفصل ${numberInfo.label}"
        }

        return MangaChapter(
            id = "${workId}_ch_${numberInfo.label.replace('.', '_')}_$fallbackOrder",
            number = numberInfo.intNumber,
            title = finalTitle,
            dateText = separated.dateText,
            chapterUrl = chUrl,
            chapterNumberValue = numberInfo.doubleValue,
            numberLabel = numberInfo.label
        )
    }

    internal data class TitleDateSeparation(
        val cleanTitle: String,
        val dateText: String
    )

    /**
     * يفصل التواريخ المدمجة داخل نص عنوان الفصل مثل:
     * - "الفصل 25 03/10/2026" -> cleanTitle="الفصل 25", dateText="03/10/2026"
     * - "201/10/2026" (الناتج عن التئام رقم الفصل 20 أو 20.1 مع التاريخ 01/10/2026 أو 10/2026)
     */
    internal fun separateTitleAndEmbeddedDate(
        rawTitle: String,
        existingDate: String,
        chapterUrl: String,
        mangaPageUrl: String,
        structuralNumAttr: String = ""
    ): TitleDateSeparation {
        var workingTitle = rawTitle.replace(Regex("\\s+"), " ").trim()
        var foundDate = existingDate.trim()

        // Case A: Corrupted merged number+date token like "201/10/2026" or "2003/10/2026"
        val mergedDateRegex = Regex("""(?<!\d)(\d{3,4})[/-](\d{1,2})[/-](20\d{2}|19\d{2})(?!\d)""")
        val mergedMatch = mergedDateRegex.find(workingTitle)
        if (mergedMatch != null) {
            val firstGroup = mergedMatch.groupValues[1] // e.g. "201" or "2003"
            val monthGroup = mergedMatch.groupValues[2]
            val yearGroup = mergedMatch.groupValues[3]
            val urlNumStr = extractChapterNumberTokenFromUrl(chapterUrl, mangaPageUrl)
                .ifBlank { structuralNumAttr }

            // Check if firstGroup isn't a valid 4-digit year (YYYY/MM/DD)
            val isIsoYear = firstGroup.length == 4 && (firstGroup.startsWith("20") || firstGroup.startsWith("19"))
            if (!isIsoYear) {
                if (urlNumStr.isNotBlank()) {
                    // Use the verified number from URL / data-num (e.g. "20.1" or "20")
                    if (foundDate.isBlank()) {
                        val possibleDay = firstGroup.removePrefix(urlNumStr.replace(".", ""))
                            .takeIf { it.length in 1..2 && (it.toIntOrNull() ?: 0) in 1..31 }
                        foundDate = if (possibleDay != null) {
                            "${possibleDay.padStart(2, '0')}/${monthGroup.padStart(2, '0')}/$yearGroup"
                        } else {
                            "${monthGroup.padStart(2, '0')}/$yearGroup"
                        }
                    }
                    workingTitle = workingTitle.replace(mergedMatch.value, "الفصل $urlNumStr").trim()
                } else if (firstGroup.length == 3) {
                    // e.g. "201/10/2026" where "20" + "1/10/2026" or "20.1" + "/10/2026"
                    val chPart = firstGroup.substring(0, 2)
                    val dayPart = firstGroup.substring(2)
                    if (foundDate.isBlank()) {
                        foundDate = "${dayPart.padStart(2, '0')}/${monthGroup.padStart(2, '0')}/$yearGroup"
                    }
                    workingTitle = workingTitle.replace(mergedMatch.value, "الفصل $chPart").trim()
                }
            }
        }

        // Case B: Standard embedded date patterns (DD/MM/YYYY, YYYY-MM-DD, YYYY/MM/DD)
        val standardDateRegex = Regex(
            """\b(\d{1,2}[/-]\d{1,2}[/-](?:20\d{2}|19\d{2}|\d{2})|(?:20\d{2}|19\d{2})[/-]\d{1,2}[/-]\d{1,2})\b"""
        )
        standardDateRegex.findAll(workingTitle).toList().forEach { match ->
            val dateVal = match.value.trim()
            if (foundDate.isBlank()) {
                foundDate = dateVal
            }
            workingTitle = workingTitle.replace(match.value, " ")
        }

        // Case C: Relative Arabic/English date expressions accidentally inside the anchor text
        val relativeDateRegex = Regex(
            """((?:منذ|قبل)\s+(?:الآن|لحظات|ثوان(?:ي)?|دقيقة|دقيقتين|\d+\s*دقائق|ساعة|ساعتين|\d+\s*ساعات|يوم|يومين|\d+\s*أيام|أسبوع|أسبوعين|\d+\s*أسابيع|شهر|شهرين|\d+\s*أشهر|سنة|\d+\s*سنوات)|\b\d+\s+(?:mins?|minutes?|hours?|days?|weeks?|months?)\s+ago\b)""",
            RegexOption.IGNORE_CASE
        )
        val relMatch = relativeDateRegex.find(workingTitle)
        if (relMatch != null) {
            if (foundDate.isBlank()) {
                foundDate = relMatch.value.trim()
            }
            workingTitle = workingTitle.replace(relMatch.value, " ")
        }

        workingTitle = workingTitle
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '|', ':', '•', '،')
            .trim()

        return TitleDateSeparation(cleanTitle = workingTitle, dateText = foundDate)
    }

    /**
     * يحلل رقم الفصل الفعلي مع دعم الفصول العشرية (مثل 20.1 و 20.5) والفصول الخاصة،
     * ويتحقق من التطابق بين العنوان والرابط وسمة data-num لمنع الأخطاء مثل (20..29 ثم 201).
     */
    internal fun parseChapterNumberInfo(
        cleanTitle: String,
        rawTitleBeforeDateStrip: String,
        chapterUrl: String,
        mangaPageUrl: String,
        structuralNumAttr: String = "",
        fallback: Int = 1
    ): ParsedChapterNumber {
        val isSpecial = cleanTitle.contains("خاص") ||
            cleanTitle.contains("إضافي") ||
            cleanTitle.contains("فرعي") ||
            cleanTitle.contains("extra", ignoreCase = true) ||
            cleanTitle.contains("special", ignoreCase = true) ||
            cleanTitle.contains("omake", ignoreCase = true)

        // 1. Check structural attribute (data-num) if valid
        val attrToken = normalizeChapterNumberToken(structuralNumAttr)
        val urlToken = extractChapterNumberTokenFromUrl(chapterUrl, mangaPageUrl)
        val titleToken = extractChapterNumberTokenFromTitle(cleanTitle)

        // Determine the most reliable token:
        // If rawTitle had a slash-corrupted token or titleToken disagrees wildly with urlToken (e.g. title=201, url=20.1 or 20), trust urlToken or attrToken!
        val chosenToken = when {
            attrToken.isNotBlank() -> attrToken
            urlToken.isNotBlank() && titleToken.isNotBlank() -> {
                val urlDouble = urlToken.toDoubleOrNull()
                val titleDouble = titleToken.toDoubleOrNull()
                if (urlDouble != null && titleDouble != null && kotlin.math.abs(urlDouble - titleDouble) > 0.001) {
                    // E.g. URL says 20.1 (or 20) while title parsed 201 from a malformed string
                    if (urlToken.replace(".", "") == titleToken || rawTitleBeforeDateStrip.contains("/")) {
                        urlToken
                    } else {
                        titleToken
                    }
                } else {
                    titleToken
                }
            }
            titleToken.isNotBlank() -> titleToken
            urlToken.isNotBlank() -> urlToken
            else -> ""
        }

        val doubleVal = chosenToken.toDoubleOrNull() ?: fallback.toDouble()
        val intVal = doubleVal.toInt()
        val label = when {
            chosenToken.isNotBlank() -> chosenToken
            else -> fallback.toString()
        }

        return ParsedChapterNumber(
            intNumber = intVal,
            doubleValue = doubleVal,
            label = label,
            isSpecial = isSpecial
        )
    }

    private fun extractChapterNumberTokenFromTitle(cleanTitle: String): String {
        if (cleanTitle.isBlank()) return ""
        // First look for explicit keyword + number: "الفصل 20.5", "فصل 20", "Chapter 20.1", "Ch. 20"
        val explicitRegex = Regex(
            """(?:الفصل|فصل|chapter|ch\.?|ep\.?|#)\s*(\d+(?:[._-]\d+)?)""",
            RegexOption.IGNORE_CASE
        )
        val explicitMatch = explicitRegex.find(cleanTitle)
        if (explicitMatch != null) {
            return normalizeChapterNumberToken(explicitMatch.groupValues[1])
        }

        // Next look for standalone number token that is NOT part of a date (no adjacent '/')
        val standaloneRegex = Regex("""(?<![/\d])(\d+(?:\.\d+)?)(?![/\d])""")
        val standaloneMatch = standaloneRegex.find(cleanTitle)
        if (standaloneMatch != null) {
            return normalizeChapterNumberToken(standaloneMatch.groupValues[1])
        }
        return ""
    }

    private fun extractChapterNumberTokenFromUrl(chapterUrl: String, mangaPageUrl: String): String {
        if (chapterUrl.isBlank()) return ""
        val cleanUrl = chapterUrl.substringBefore("?").substringBefore("#").trimEnd('/')
        val baseManga = mangaPageUrl.substringBefore("?").substringBefore("#").trimEnd('/')

        val relativePart = if (baseManga.isNotBlank() && cleanUrl.startsWith(baseManga, ignoreCase = true)) {
            cleanUrl.substring(baseManga.length).trim('/')
        } else {
            cleanUrl.substringAfterLast('/')
        }

        // Match patterns like "chapter-20-1", "chapter-20.5", "20-1", "20.1", "20"
        val urlExplicitRegex = Regex(
            """(?:chapter|ch|فصل|الفصل)[-_/\s]*(\d+(?:[._-]\d+)?)|^(\d+(?:[._-]\d+)?)$""",
            RegexOption.IGNORE_CASE
        )
        val match = urlExplicitRegex.find(relativePart)
            ?: urlExplicitRegex.find(cleanUrl.substringAfterLast('/'))
        if (match != null) {
            val raw = match.groupValues[1].ifBlank { match.groupValues[2] }
            return normalizeChapterNumberToken(raw)
        }
        return ""
    }

    private fun normalizeChapterNumberToken(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val normalized = trimmed.replace('_', '.').replace('-', '.')
        val asDouble = normalized.toDoubleOrNull() ?: return ""
        return if (asDouble % 1.0 == 0.0) {
            asDouble.toInt().toString()
        } else {
            asDouble.toString().trimEnd('0').trimEnd('.')
        }
    }

    private fun deduplicateAndSortChapters(
        chapters: List<MangaChapter>,
        workId: String
    ): List<MangaChapter> {
        if (chapters.isEmpty()) return emptyList()
        val uniqueMap = LinkedHashMap<String, MangaChapter>()
        for (ch in chapters) {
            val urlKey = normalizeChapterUrlKey(ch.chapterUrl)
            // Deduplicate by normalized URL key
            val dedupKey = urlKey.ifBlank { "${workId}_${ch.displayNumber}_${ch.title}" }
            val existing = uniqueMap[dedupKey]
            if (existing == null) {
                uniqueMap[dedupKey] = ch
            } else {
                // Keep the entry that has a non-empty dateText or richer title
                val mergedDate = existing.dateText.ifBlank { ch.dateText }
                val mergedTitle = if (existing.title.length >= ch.title.length) existing.title else ch.title
                uniqueMap[dedupKey] = existing.copy(title = mergedTitle, dateText = mergedDate)
            }
        }
        val list = uniqueMap.values.toList()
        return if (list.size > 1) {
            list.sortedWith(
                compareBy<MangaChapter> { it.chapterNumberValue }
                    .thenBy { it.number }
                    .thenBy { it.displayNumber }
            )
        } else {
            list
        }
    }

    private fun extractChapterPaginationUrls(html: String, baseWorkUrl: String): List<String> {
        val urls = LinkedHashSet<String>()
        val cleanBase = baseWorkUrl.substringBefore("?").substringBefore("#").trimEnd('/')
        val pageLinkRegex = Regex(
            """<a[^>]+href=["']([^"']*(?:\?(?:page|chpage|cpage)=\d+|/page/\d+/?)[^"']*)["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        pageLinkRegex.findAll(html).forEach { m ->
            val resolved = resolveUrl(baseWorkUrl, m.groupValues[1])
            if (resolved.startsWith(cleanBase, ignoreCase = true) &&
                normalizeChapterUrlKey(resolved) != normalizeChapterUrlKey(baseWorkUrl)
            ) {
                urls.add(resolved)
            }
        }
        return urls.toList()
    }

    suspend fun fetchChapterPages(
        work: MangaWork,
        chapter: MangaChapter
    ): ChapterPagesScrapeResult = withContext(Dispatchers.IO) {
        val targetUrl = chapter.chapterUrl.ifBlank {
            "${work.siteUrl.trimEnd('/')}/series/${work.id}/${chapter.displayNumber}"
        }
        try {
            val request = buildRequest(targetUrl, referer = work.mangaUrl.ifBlank { work.siteUrl })
            httpClient.newCall(request).execute().use { response ->
                val httpCode = response.code
                val html = response.body?.string() ?: ""
                if (isCloudflareChallengeHtml(html, httpCode)) {
                    DiagnosticLogger.logWarn(
                        category = DiagnosticCategory.SCRAPER,
                        message = "صفحة الفصل تتطلب تخطي حماية Cloudflare",
                        sourceId = work.sourceId.toString(),
                        sourceName = work.siteName,
                        workId = work.id,
                        chapterId = chapter.id,
                        url = targetUrl,
                        httpCode = httpCode
                    )
                    return@withContext ChapterPagesScrapeResult(
                        pageUrls = emptyList(),
                        requiresCloudflare = true,
                        errorMessage = "يتطلب الفصل تخطي حماية Cloudflare"
                    )
                }
                if (!response.isSuccessful && html.isBlank()) {
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.SCRAPER,
                        message = "فشل جلب صفحة الفصل (HTTP $httpCode)",
                        sourceId = work.sourceId.toString(),
                        sourceName = work.siteName,
                        workId = work.id,
                        chapterId = chapter.id,
                        url = targetUrl,
                        httpCode = httpCode
                    )
                    return@withContext ChapterPagesScrapeResult(
                        pageUrls = emptyList(),
                        requiresCloudflare = false,
                        errorMessage = "فشل الاتصال بصفحة الفصل (HTTP $httpCode)"
                    )
                }

                var extraction = parseChapterPagesWithDiagnostics(html, targetUrl)
                var pages = extraction.pageUrls.toMutableList()
                val allDiagnostics = extraction.diagnostics.toMutableList()

                // Check if Madara site is in single-page mode (?style=paged) and retry with ?style=list
                if (pages.size <= 1 && !targetUrl.contains("style=list", ignoreCase = true) &&
                    (html.contains("wp-manga", ignoreCase = true) || html.contains("reading-content", ignoreCase = true))
                ) {
                    val listModeUrl = if (targetUrl.contains("?")) "$targetUrl&style=list" else "$targetUrl?style=list"
                    try {
                        httpClient.newCall(buildRequest(listModeUrl, referer = targetUrl)).execute().use { listResp ->
                            val listHtml = listResp.body?.string() ?: ""
                            if (listResp.isSuccessful && listHtml.isNotBlank()) {
                                val listExtraction = parseChapterPagesWithDiagnostics(listHtml, listModeUrl)
                                if (listExtraction.pageUrls.size > pages.size) {
                                    pages = listExtraction.pageUrls.toMutableList()
                                    allDiagnostics.addAll(listExtraction.diagnostics)
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                // Check for multi-part / paginated sub-pages of the same chapter
                val subPageUrls = extractChapterSubPageUrls(html, targetUrl)
                if (subPageUrls.isNotEmpty() && pages.size <= 3) {
                    val seenPages = LinkedHashSet(pages)
                    for (subUrl in subPageUrls.take(25)) {
                        try {
                            httpClient.newCall(buildRequest(subUrl, referer = targetUrl)).execute().use { subResp ->
                                val subHtml = subResp.body?.string() ?: ""
                                if (subResp.isSuccessful && subHtml.isNotBlank()) {
                                    val subExtraction = parseChapterPagesWithDiagnostics(subHtml, subUrl)
                                    subExtraction.pageUrls.forEach { seenPages.add(it) }
                                    allDiagnostics.addAll(subExtraction.diagnostics)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    pages = seenPages.toMutableList()
                }

                val rejectedCount = allDiagnostics.count { !it.accepted }
                DiagnosticLogger.logInfo(
                    category = DiagnosticCategory.SCRAPER,
                    message = "تم استخراج ${pages.size} صفحة صالحة للفصل ${chapter.displayNumber} (تم استبعاد $rejectedCount عنصر غير صالح مثل الإيموجي/الأيقونات)",
                    sourceId = work.sourceId.toString(),
                    sourceName = work.siteName,
                    workId = work.id,
                    chapterId = chapter.id,
                    url = targetUrl,
                    httpCode = httpCode,
                    details = allDiagnostics.take(6).joinToString(" ; ") {
                        "${if (it.accepted) "ACCEPT" else "REJECT"}[${it.attributeUsed}]: ${it.reason} (${it.resolvedUrl.takeLast(45)})"
                    }
                )
                ChapterPagesScrapeResult(
                    pageUrls = pages,
                    requiresCloudflare = false,
                    errorMessage = if (pages.isEmpty()) {
                        "تعذر استخراج صفحات الفصل الصالحة من المصدر (تم استبعاد الصور التعبيرية والعناصر غير الصالحة)"
                    } else null,
                    diagnostics = allDiagnostics
                )
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(
                category = DiagnosticCategory.SCRAPER,
                message = "استثناء أثناء جلب صفحات الفصل: ${e.localizedMessage}",
                sourceId = work.sourceId.toString(),
                sourceName = work.siteName,
                workId = work.id,
                chapterId = chapter.id,
                url = targetUrl
            )
            ChapterPagesScrapeResult(
                pageUrls = emptyList(),
                requiresCloudflare = false,
                errorMessage = e.localizedMessage ?: "تعذر تحميل صور الفصل"
            )
        }
    }

    /**
     * يستخرج جميع روابط صور الفصل بالترتيب الأصلي مع إزالة الروابط المكررة،
     * ويعزل حاوية القارئ الفعلية ويستبعد الإيموجي (مثل wp-smiley / twemoji / s.w.org / reactions)
     * والشعارات والإعلانات والصور البديلة الصغيرة.
     */
    fun parseChapterPagesHtml(html: String, chapterUrl: String): List<String> {
        return parseChapterPagesWithDiagnostics(html, chapterUrl).pageUrls
    }

    fun parseChapterPagesWithDiagnostics(html: String, chapterUrl: String): ChapterPagesScrapeResult {
        if (html.isBlank()) return ChapterPagesScrapeResult(emptyList())
        val images = LinkedHashSet<String>()
        val diagnostics = mutableListOf<PageCandidateDiagnostic>()

        // 1. Check ts_reader.run({... "images": [...] }), chapter_preloaded_images, or JSON arrays of page URLs
        val jsonArrayPatterns = listOf(
            Regex("""["'](?:images|pages|page_urls|chapter_images)["']\s*:\s*\[([\s\S]*?)\]""", RegexOption.IGNORE_CASE),
            Regex("""(?:chapter_preloaded_images|chapterImages|pagesList|image_list)\s*=\s*\[([\s\S]*?)\]""", RegexOption.IGNORE_CASE)
        )
        for (pattern in jsonArrayPatterns) {
            pattern.findAll(html).forEach { match ->
                val arrayContent = match.groupValues[1]
                Regex("""["']((?:https?:)?//[^"']+|/[^"']+\.(?:jpg|jpeg|png|webp)(?:\?[^"']*)?)["']""", RegexOption.IGNORE_CASE)
                    .findAll(arrayContent)
                    .forEach { urlMatch ->
                        val rawUrl = urlMatch.groupValues[1].replace("\\/", "/").trim()
                        val resolved = resolveUrl(chapterUrl, rawUrl)
                        val rejection = getCandidateRejectionReason(resolved, tagHtml = "<script json>", isInsideReaderContainer = true)
                        if (rejection == null) {
                            images.add(resolved)
                            diagnostics.add(
                                PageCandidateDiagnostic(
                                    rawUrl = rawUrl,
                                    resolvedUrl = resolved,
                                    sourceElement = "script-json-array",
                                    attributeUsed = "json",
                                    accepted = true,
                                    reason = "رابط صفحة فصل صالح داخل مصفوفة القارئ"
                                )
                            )
                        } else {
                            diagnostics.add(
                                PageCandidateDiagnostic(
                                    rawUrl = rawUrl,
                                    resolvedUrl = resolved,
                                    sourceElement = "script-json-array",
                                    attributeUsed = "json",
                                    accepted = false,
                                    reason = rejection
                                )
                            )
                        }
                    }
            }
        }

        // 2. Isolate dedicated reader containers first (excluding generic .entry-content unless no dedicated container exists)
        val dedicatedReaderRegex = Regex(
            """<(?:div|section|main|article)[^>]+(?:id|class)=["'][^"']*(?:reading-content|readerarea|chapter-content|images_chapter|vung-doc|container-chapter-reader|rdminimal|chapter-reader|reader-area|chapter-images)[^"']*["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        val fallbackEntryContentRegex = Regex(
            """<(?:div|section|main|article)[^>]+(?:id|class)=["'][^"']*\bentry-content\b[^"']*["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )

        val readerStartMatch = dedicatedReaderRegex.find(html) ?: fallbackEntryContentRegex.find(html)
        if (readerStartMatch != null) {
            val startIndex = readerStartMatch.range.first
            val tailHtml = html.substring(startIndex)
            val footerBoundaryRegex = Regex(
                """<(?:div|section|footer|aside|nav)[^>]+(?:id|class)=["'][^"']*(?:comments-area|disqus_thread|wpdiscuz|wp-reactions|reactions-container|c-blog__heading|related-manga|related-posts|manga-discussion|wp-manga-nav|site-footer|footer-area|sharedaddy|post-ratings)[^"']*["']""",
                RegexOption.IGNORE_CASE
            )
            val endOffset = footerBoundaryRegex.find(tailHtml)?.range?.first ?: minOf(tailHtml.length, 350_000)
            val readerSectionHtml = stripNonReaderBlocksFromFragment(tailHtml.substring(0, endOffset))
            extractAndValidateImagesFromFragment(
                fragment = readerSectionHtml,
                baseUrl = chapterUrl,
                sourceContainerName = "reader-container",
                isInsideReaderContainer = true,
                images = images,
                diagnostics = diagnostics
            )
        }

        // 3. Scan explicit .page-break / .chapter-image blocks across the document
        val pageBreakRegex = Regex(
            """<div[^>]+class=["'][^"']*(?:page-break|image-item|chapter-image|page-item)[^"']*["'][^>]*>([\s\S]*?)</div>""",
            RegexOption.IGNORE_CASE
        )
        pageBreakRegex.findAll(html).forEach { match ->
            extractAndValidateImagesFromFragment(
                fragment = match.groupValues[1],
                baseUrl = chapterUrl,
                sourceContainerName = "page-break",
                isInsideReaderContainer = true,
                images = images,
                diagnostics = diagnostics
            )
        }

        // 4. Scan explicit reader <img> tags (NEVER match generic 'image-' which matches wp-image / emoji!)
        val imgTagRegex = Regex("""<img[^>]+>""", RegexOption.IGNORE_CASE)
        imgTagRegex.findAll(html).forEach { match ->
            val tag = match.value
            val isExplicitReaderImg = tag.contains("wp-manga-chapter-img", ignoreCase = true) ||
                tag.contains("chapter-img", ignoreCase = true) ||
                tag.contains("page-img", ignoreCase = true) ||
                tag.contains("ts-main-image", ignoreCase = true) ||
                tag.contains("data-page=", ignoreCase = true) ||
                tag.contains("data-index=", ignoreCase = true)

            val extracted = extractBestImageUrlWithAttr(tag, chapterUrl)
            if (isExplicitReaderImg) {
                val rejection = getCandidateRejectionReason(extracted.first, tagHtml = tag, isInsideReaderContainer = true)
                if (rejection == null) {
                    images.add(extracted.first)
                    diagnostics.add(
                        PageCandidateDiagnostic(
                            rawUrl = extracted.first,
                            resolvedUrl = extracted.first,
                            sourceElement = "explicit-reader-img",
                            attributeUsed = extracted.second,
                            accepted = true,
                            reason = "وسم صورة فصل صريح"
                        )
                    )
                } else {
                    diagnostics.add(
                        PageCandidateDiagnostic(
                            rawUrl = extracted.first,
                            resolvedUrl = extracted.first,
                            sourceElement = "explicit-reader-img",
                            attributeUsed = extracted.second,
                            accepted = false,
                            reason = rejection
                        )
                    )
                }
            } else if (extracted.first.isNotBlank()) {
                // Record diagnostic if an outside image was an emoji/reaction so Stage 1 audit logs it clearly
                val rejection = getCandidateRejectionReason(extracted.first, tagHtml = tag, isInsideReaderContainer = false)
                if (rejection != null) {
                    diagnostics.add(
                        PageCandidateDiagnostic(
                            rawUrl = extracted.first,
                            resolvedUrl = extracted.first,
                            sourceElement = "outside-img",
                            attributeUsed = extracted.second,
                            accepted = false,
                            reason = rejection
                        )
                    )
                }
            }
        }

        return ChapterPagesScrapeResult(
            pageUrls = images.toList(),
            diagnostics = diagnostics
        )
    }

    private fun stripNonReaderBlocksFromFragment(fragment: String): String {
        return fragment
            .replace(
                Regex("""<(?:div|section|ul|aside)[^>]+(?:id|class)=["'][^"']*(?:wp-reactions|reactions|wpdiscuz|comments|respond|social|share|related|ads|banner|widget|author-box)[^"']*["'][^>]*>[\s\S]*?</(?:div|section|ul|aside)>""", RegexOption.IGNORE_CASE),
                ""
            )
    }

    private fun extractAndValidateImagesFromFragment(
        fragment: String,
        baseUrl: String,
        sourceContainerName: String,
        isInsideReaderContainer: Boolean,
        images: LinkedHashSet<String>,
        diagnostics: MutableList<PageCandidateDiagnostic>
    ) {
        Regex("""<img[^>]+>""", RegexOption.IGNORE_CASE).findAll(fragment).forEach { m ->
            val tag = m.value
            val (resolvedUrl, attrUsed) = extractBestImageUrlWithAttr(tag, baseUrl)
            val rejection = getCandidateRejectionReason(
                imgUrl = resolvedUrl,
                tagHtml = tag,
                isInsideReaderContainer = isInsideReaderContainer
            )
            if (rejection == null) {
                images.add(resolvedUrl)
                diagnostics.add(
                    PageCandidateDiagnostic(
                        rawUrl = resolvedUrl,
                        resolvedUrl = resolvedUrl,
                        sourceElement = sourceContainerName,
                        attributeUsed = attrUsed,
                        accepted = true,
                        reason = "صورة صفحة فصل صالحة داخل حاوية القارئ"
                    )
                )
            } else if (resolvedUrl.isNotBlank()) {
                diagnostics.add(
                    PageCandidateDiagnostic(
                        rawUrl = resolvedUrl,
                        resolvedUrl = resolvedUrl,
                        sourceElement = sourceContainerName,
                        attributeUsed = attrUsed,
                        accepted = false,
                        reason = rejection
                    )
                )
            }
        }
    }

    /**
     * يفحص عدة مؤشرات (الرابط، اسم الصنف class، النص البديل alt، الأبعاد الصريحة width/height، موقع العنصر)
     * لتحديد ما إذا كانت الصورة إيموجي (مثل Magic emperor emoji)، أو تفاعل، أو شعار، أو صورة placeholder.
     */
    internal fun getCandidateRejectionReason(
        imgUrl: String,
        tagHtml: String = "",
        isInsideReaderContainer: Boolean = true
    ): String? {
        if (imgUrl.isBlank()) return "رابط الصورة فارغ"
        val lowerUrl = imgUrl.lowercase(Locale.ROOT)
        val lowerTag = tagHtml.lowercase(Locale.ROOT)

        if (lowerUrl.startsWith("data:image") || lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("about:")) {
            return "رابط مضمن أو صورة مؤقتة غير صالحة (data/javascript URI)"
        }

        // 1. Check Emoji / Reaction / Smiley patterns (Root cause of Magic Emperor emoji issue!)
        val emojiUrlMarkers = listOf(
            "s.w.org/images/core/emoji",
            "twemoji",
            "wp-smiley",
            "/emoji/",
            "/emojis/",
            "/smilies/",
            "/reactions/",
            "wp-reactions",
            "reaction",
            "/emote",
            "/sticker",
            "1f600", "1f602", "1f60d", "1f62d", "1f44d", "1f525", "1f496"
        )
        if (emojiUrlMarkers.any { lowerUrl.contains(it) }) {
            return "صورة تعبيرية (Emoji / Reaction) مستبعدة من صفحات الفصل"
        }

        val tagClass = extractAttribute(tagHtml, "class").lowercase(Locale.ROOT)
        val tagAlt = extractAttribute(tagHtml, "alt").trim()
        val tagRole = extractAttribute(tagHtml, "role").lowercase(Locale.ROOT)

        if (tagClass.contains("emoji") ||
            tagClass.contains("wp-smiley") ||
            tagClass.contains("reaction") ||
            tagClass.contains("emote") ||
            tagClass.contains("sticker") ||
            tagClass.contains("avatar") ||
            tagClass.contains("site-logo") ||
            tagClass.contains("custom-logo")
        ) {
            return "عنصر HTML يحمل تصنيف إيموجي أو تفاعل أو شعار ($tagClass)"
        }

        // Single Unicode emoji character in alt attribute with role="img" (WordPress core emoji replacement)
        if (tagRole == "img" && tagAlt.isNotBlank() && tagAlt.codePoints().count() <= 2L &&
            tagAlt.any { Character.getType(it) == Character.SURROGATE.toInt() || Character.getType(it) == Character.OTHER_SYMBOL.toInt() }
        ) {
            return "عنصر صورة تعبيرية (WordPress Emoji alt=$tagAlt)"
        }

        // 2. Check UI / Logo / Placeholder / Ad / Tracking patterns
        val assetMarkers = listOf(
            "logo", "favicon", "avatar", "gravatar", "spinner", "loading",
            "preloader", "placeholder", "spacer", "blank.", "1x1", "pixel",
            "doubleclick", "googlesyndication", "adserver", "/ads/", "/banners/",
            "author-avatar", "discord-banner", "telegram-banner"
        )
        if (assetMarkers.any { lowerUrl.contains(it) } ||
            lowerUrl.endsWith(".svg") ||
            lowerUrl.contains(".svg?") ||
            (lowerUrl.endsWith(".gif") && (lowerUrl.contains("lazy") || lowerUrl.contains("load") || lowerUrl.contains("blank")))
        ) {
            return "صورة واجهة أو شعار أو عنصر نائب (Placeholder / Asset)"
        }

        // 3. Check explicit tiny dimensions in HTML attributes (e.g. width="24" height="24" or 72x72 emoji)
        val widthAttr = extractAttribute(tagHtml, "width").filter { it.isDigit() }.toIntOrNull()
        val heightAttr = extractAttribute(tagHtml, "height").filter { it.isDigit() }.toIntOrNull()
        if (widthAttr != null && widthAttr in 1..140) {
            return "أبعاد الصورة الصريحة صغيرة جداً (width=$widthAttr) لا تصلح لصفحة فصل"
        }
        if (heightAttr != null && heightAttr in 1..140) {
            return "ارتفاع الصورة الصريح صغير جداً (height=$heightAttr) لا يصلح لصفحة فصل"
        }
        // Check URL dimension pattern like /72x72/ or -150x150.png (WordPress square thumbnails / emojis)
        val urlDimMatch = Regex("""[-/](\d{2,3})x(\d{2,3})(?:[./?]|$)""").find(lowerUrl)
        if (urlDimMatch != null) {
            val w = urlDimMatch.groupValues[1].toIntOrNull() ?: 999
            val h = urlDimMatch.groupValues[2].toIntOrNull() ?: 999
            if (w <= 150 && h <= 150) {
                return "صورة مصغرة أو إيموجي بأبعاد صغيرة في الرابط (${w}x${h})"
            }
        }

        // 4. Require container membership or explicit chapter image indicator
        if (!isInsideReaderContainer) {
            return "الصورة خارج حاوية قارئ الفصل ولا تحمل وسم صفحة فصل"
        }

        if (!isLikelyChapterPageImage(imgUrl)) {
            return "الرابط لا يطابق أنماط صور صفحات المانجا المعتمدة"
        }

        return null
    }

    private fun extractChapterSubPageUrls(html: String, chapterUrl: String): List<String> {
        val urls = LinkedHashSet<String>()
        val cleanBase = chapterUrl.substringBefore("?").substringBefore("#").trimEnd('/')
        val subPageRegex = Regex(
            """<a[^>]+href=["']([^"']+)["'][^>]*>\s*\d+\s*</a>""",
            RegexOption.IGNORE_CASE
        )
        subPageRegex.findAll(html).forEach { m ->
            val resolved = resolveUrl(chapterUrl, m.groupValues[1])
            if (resolved.startsWith(cleanBase, ignoreCase = true) &&
                normalizeChapterUrlKey(resolved) != normalizeChapterUrlKey(chapterUrl)
            ) {
                urls.add(resolved)
            }
        }
        return urls.toList()
    }

    // ==================== LATEST UPDATES EXTRACTION (آخر التحديثات من مصدر الموقع) ====================

    fun buildLatestUpdatesTargetUrl(site: SiteEntity, page: Int = 1): String {
        val base = site.url.trimEnd('/')
        return if (page <= 1) {
            base
        } else {
            when {
                base.contains("olympustaff", ignoreCase = true) || base.contains("team", ignoreCase = true) ->
                    "$base/?page=$page"
                else ->
                    "$base/page/$page/"
            }
        }
    }

    suspend fun fetchLatestUpdates(
        site: SiteEntity,
        page: Int = 1
    ): LatestUpdatesScrapeResult = withContext(Dispatchers.IO) {
        val targetUrl = buildLatestUpdatesTargetUrl(site, page)
        try {
            val request = buildRequest(targetUrl, referer = site.url)
            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: ""
                if (isCloudflareChallengeHtml(html, response.code)) {
                    return@withContext LatestUpdatesScrapeResult(
                        updates = emptyList(),
                        requiresCloudflare = true,
                        errorMessage = "الموقع محمي بواسطة Cloudflare، يرجى فتح البوابة للمزامنة"
                    )
                }
                if (!response.isSuccessful && html.isBlank()) {
                    return@withContext LatestUpdatesScrapeResult(
                        updates = emptyList(),
                        requiresCloudflare = false,
                        errorMessage = "فشل جلب آخر التحديثات من ${site.name} (HTTP ${response.code})"
                    )
                }

                var updates = parseLatestUpdatesHtml(html, site, targetUrl)
                // If homepage didn't list chapter updates directly, try /manga/?m_orderby=latest or /series
                if (updates.isEmpty() && page == 1) {
                    val altPaths = listOf(
                        "/manga/?m_orderby=latest",
                        "/series/?sort=latest",
                        "/latest-manga/"
                    )
                    for (alt in altPaths) {
                        val altUrl = site.url.trimEnd('/') + alt
                        try {
                            httpClient.newCall(buildRequest(altUrl, referer = site.url)).execute().use { altResp ->
                                val altHtml = altResp.body?.string() ?: ""
                                if (altResp.isSuccessful && altHtml.isNotBlank()) {
                                    val altUpdates = parseLatestUpdatesHtml(altHtml, site, altUrl)
                                    if (altUpdates.isNotEmpty()) {
                                        updates = altUpdates
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                        if (updates.isNotEmpty()) break
                    }
                }

                val hasNext = html.contains("page/${page + 1}", ignoreCase = true) ||
                    html.contains("page=${page + 1}", ignoreCase = true) ||
                    html.contains("next page-numbers", ignoreCase = true) ||
                    html.contains("rel=\"next\"", ignoreCase = true)

                LatestUpdatesScrapeResult(
                    updates = updates,
                    hasNextPage = hasNext || updates.size >= 8,
                    requiresCloudflare = false,
                    errorMessage = if (updates.isEmpty()) "لم يتم العثور على تحديثات فصول حديثة في ${site.name}" else null
                )
            }
        } catch (e: Exception) {
            LatestUpdatesScrapeResult(
                updates = emptyList(),
                hasNextPage = false,
                requiresCloudflare = false,
                errorMessage = e.localizedMessage ?: "تعذر جلب آخر التحديثات من ${site.name}"
            )
        }
    }

    /**
     * يستخرج بطاقات "آخر التحديثات" من صفحة المصدر الحقيقية:
     * الغلاف، اسم العمل، رابط العمل، أحدث فصل (أو فصول البطاقة)، رقم الفصل، تاريخ/وقت النشر، واسم المصدر.
     */
    fun parseLatestUpdatesHtml(
        html: String,
        site: SiteEntity,
        pageUrl: String = site.url,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): List<MangaLatestUpdateItem> {
        if (html.isBlank()) return emptyList()
        val cleanHtml = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")

        val updatesMap = LinkedHashMap<String, MangaLatestUpdateItem>()
        var orderCounter = 0

        val cardOpenRegex = Regex(
            """<(?:div|article|li)[^>]+class=["'][^"']*\b(?:page-item-detail|c-tabs-item__content|utao|bsx|bs|manga__item|post-item|series-card|animepost|latest-item|update-item|box)\b[^"']*["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )

        val cardStarts = cardOpenRegex.findAll(cleanHtml).map { it.range.first }.toList()
        for (i in cardStarts.indices) {
            val startIdx = cardStarts[i]
            val nextStartIdx = if (i + 1 < cardStarts.size) cardStarts[i + 1] else cleanHtml.length
            val endIdx = minOf(nextStartIdx, startIdx + 5000)
            val block = cleanHtml.substring(startIdx, endIdx)

            val work = extractWorkFromCardFragment(block, site, pageUrl) ?: continue
            if (work.mangaUrl.isBlank() || work.title.isBlank()) continue

            // Find chapter links inside this update card
            val cardChapters = extractChaptersFromUpdateCard(
                cardHtml = block,
                work = work,
                site = site,
                pageUrl = pageUrl,
                startOrderIndex = orderCounter,
                nowEpochMillis = nowEpochMillis
            )
            cardChapters.forEach { item ->
                orderCounter++
                if (!updatesMap.containsKey(item.updateKey)) {
                    updatesMap[item.updateKey] = item
                }
            }
        }

        return sortLatestUpdates(updatesMap.values.toList())
    }

    private fun extractChaptersFromUpdateCard(
        cardHtml: String,
        work: MangaWork,
        site: SiteEntity,
        pageUrl: String,
        startOrderIndex: Int,
        nowEpochMillis: Long
    ): List<MangaLatestUpdateItem> {
        val items = mutableListOf<MangaLatestUpdateItem>()
        val seenKeys = HashSet<String>()

        // 1. Check structured chapter rows inside card (.chapter-item, .list-chapter li, .luf ul li, .adds)
        val subItemOpenRegex = Regex(
            """<(?:div|li)[^>]+class=["'][^"']*\b(?:chapter-item|epxs|lchx|adds|ch-item)\b[^"']*["'][^>]*>|<li\b[^>]*>""",
            RegexOption.IGNORE_CASE
        )

        val subStarts = subItemOpenRegex.findAll(cardHtml).map { it.range.first }.toList()
        for (idx in subStarts.indices) {
            val startPos = subStarts[idx]
            val nextPos = if (idx + 1 < subStarts.size) subStarts[idx + 1] else cardHtml.length
            val subBlock = cardHtml.substring(startPos, minOf(nextPos, startPos + 1400))
            val aMatch = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
                .find(subBlock) ?: continue
            val chUrl = resolveUrl(pageUrl, aMatch.groupValues[1])
            val normWorkUrl = normalizeChapterUrlKey(work.mangaUrl)
            val normChUrl = normalizeChapterUrlKey(chUrl)
            if (chUrl.isBlank() || normChUrl == normWorkUrl || !isLikelyChapterUrl(chUrl, work.mangaUrl)) {
                continue
            }

            // Look for date inside subBlock or sibling post-on/time span
            val dateRegex = Regex(
                """<(?:span|i|time|div)[^>]+class=["'][^"']*(?:post-on|chapter-release-date|chapterdate|date|time|ago)[^"']*["'][^>]*>([\s\S]*?)</(?:span|i|time|div)>|<time[^>]*>([\s\S]*?)</time>""",
                RegexOption.IGNORE_CASE
            )
            val dateInSub = dateRegex.find(subBlock)?.let {
                stripHtmlTags(it.groupValues[1].ifBlank { it.groupValues[2] }).trim()
            } ?: ""

            val cleanedLinkInner = aMatch.groupValues[2].replace(dateRegex, " ")
            val rawChText = stripHtmlTags(cleanedLinkInner).trim()
            val separated = separateTitleAndEmbeddedDate(
                rawTitle = rawChText,
                existingDate = dateInSub,
                chapterUrl = chUrl,
                mangaPageUrl = work.mangaUrl
            )
            val numInfo = parseChapterNumberInfo(
                cleanTitle = separated.cleanTitle,
                rawTitleBeforeDateStrip = rawChText,
                chapterUrl = chUrl,
                mangaPageUrl = work.mangaUrl,
                fallback = 1
            )
            val chTitle = separated.cleanTitle.ifBlank { "الفصل ${numInfo.label}" }
            val pubMillis = parsePublishDateToEpochMillis(separated.dateText, nowEpochMillis)
            val updateKey = "${site.id}_$normChUrl"
            if (seenKeys.add(updateKey)) {
                items.add(
                    MangaLatestUpdateItem(
                        updateKey = updateKey,
                        sourceId = site.id,
                        sourceName = site.name,
                        sourceUrl = site.url,
                        siteIconKey = site.iconKey,
                        workId = work.id,
                        workTitle = work.title,
                        mangaUrl = work.mangaUrl,
                        coverImageUrl = work.effectiveCoverUrl,
                        chapterId = "${work.id}_ch_${numInfo.label.replace('.', '_')}",
                        chapterTitle = chTitle,
                        chapterNumber = numInfo.intNumber,
                        chapterNumberValue = numInfo.doubleValue,
                        chapterNumberLabel = numInfo.label,
                        chapterUrl = chUrl,
                        publishDateText = separated.dateText,
                        publishedAtEpochMillis = pubMillis,
                        isDateAvailable = separated.dateText.isNotBlank(),
                        originalOrderIndex = startOrderIndex + items.size,
                        syncedAt = nowEpochMillis
                    )
                )
            }
        }

        // 2. Fallback: scan any chapter anchor inside cardHtml if no subItem matched
        if (items.isEmpty()) {
            val anchorRegex = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>([\s\S]{1,500}?)</a>""", RegexOption.IGNORE_CASE)
            val cardDateRegex = Regex(
                """<(?:span|i|time|div)[^>]+class=["'][^"']*(?:post-on|chapter-release-date|chapterdate|date|time|ago)[^"']*["'][^>]*>([\s\S]*?)</(?:span|i|time|div)>""",
                RegexOption.IGNORE_CASE
            )
            val cardLevelDate = cardDateRegex.find(cardHtml)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it).trim() } ?: ""

            anchorRegex.findAll(cardHtml).forEach { aMatch ->
                val chUrl = resolveUrl(pageUrl, aMatch.groupValues[1])
                val normWorkUrl = normalizeChapterUrlKey(work.mangaUrl)
                val normChUrl = normalizeChapterUrlKey(chUrl)
                if (chUrl.isBlank() || normChUrl == normWorkUrl || !isLikelyChapterUrl(chUrl, work.mangaUrl)) {
                    return@forEach
                }
                val rawText = stripHtmlTags(aMatch.groupValues[2]).trim()
                val separated = separateTitleAndEmbeddedDate(
                    rawTitle = rawText,
                    existingDate = cardLevelDate,
                    chapterUrl = chUrl,
                    mangaPageUrl = work.mangaUrl
                )
                val numInfo = parseChapterNumberInfo(
                    cleanTitle = separated.cleanTitle,
                    rawTitleBeforeDateStrip = rawText,
                    chapterUrl = chUrl,
                    mangaPageUrl = work.mangaUrl,
                    fallback = 1
                )
                val chTitle = separated.cleanTitle.ifBlank { "الفصل ${numInfo.label}" }
                val pubMillis = parsePublishDateToEpochMillis(separated.dateText, nowEpochMillis)
                val updateKey = "${site.id}_$normChUrl"
                if (seenKeys.add(updateKey)) {
                    items.add(
                        MangaLatestUpdateItem(
                            updateKey = updateKey,
                            sourceId = site.id,
                            sourceName = site.name,
                            sourceUrl = site.url,
                            siteIconKey = site.iconKey,
                            workId = work.id,
                            workTitle = work.title,
                            mangaUrl = work.mangaUrl,
                            coverImageUrl = work.effectiveCoverUrl,
                            chapterId = "${work.id}_ch_${numInfo.label.replace('.', '_')}",
                            chapterTitle = chTitle,
                            chapterNumber = numInfo.intNumber,
                            chapterNumberValue = numInfo.doubleValue,
                            chapterNumberLabel = numInfo.label,
                            chapterUrl = chUrl,
                            publishDateText = separated.dateText,
                            publishedAtEpochMillis = pubMillis,
                            isDateAvailable = separated.dateText.isNotBlank(),
                            originalOrderIndex = startOrderIndex + items.size,
                            syncedAt = nowEpochMillis
                        )
                    )
                }
            }
        }

        // Take the newest chapter per card (or top 2 if multiple chapters are published together)
        return items.take(2)
    }

    /**
     * يحول نص تاريخ النشر (النسبي بالعربية/الإنجليزية أو التاريخ المطلق) إلى طابع زمني (Epoch Millis)
     * مع مراعاة المنطقة الزمنية، ويعيد null إذا كان التاريخ غير متاح بدلاً من اختلاق تاريخ وهمي.
     */
    internal fun parsePublishDateToEpochMillis(
        dateText: String,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Long? {
        val cleaned = dateText.trim()
        if (cleaned.isBlank() || cleaned == "التاريخ غير متاح") return null
        val lower = cleaned.lowercase(Locale.ROOT)

        val minuteMs = 60_000L
        val hourMs = 3_600_000L
        val dayMs = 86_400_000L

        // 1. Arabic & English relative timestamps
        if (lower.contains("الآن") || lower.contains("لحظات") || lower.contains("just now") || lower.contains("ثوان")) {
            return nowEpochMillis
        }
        if (lower.contains("أمس") || lower.contains("البارحة") || lower.contains("yesterday")) {
            return nowEpochMillis - dayMs
        }
        if (lower.contains("دقيقة") && !lower.contains("دقيقتين")) {
            return nowEpochMillis - minuteMs
        }
        if (lower.contains("دقيقتين")) {
            return nowEpochMillis - 2 * minuteMs
        }
        if (lower.contains("ساعة") && !lower.contains("ساعتين")) {
            return nowEpochMillis - hourMs
        }
        if (lower.contains("ساعتين")) {
            return nowEpochMillis - 2 * hourMs
        }
        if ((lower.contains("يوم") && !lower.contains("يومين")) && (lower.contains("منذ") || lower.contains("قبل"))) {
            return nowEpochMillis - dayMs
        }
        if (lower.contains("يومين")) {
            return nowEpochMillis - 2 * dayMs
        }
        if (lower.contains("أسبوع") && !lower.contains("أسبوعين") && !lower.contains("أسابيع")) {
            return nowEpochMillis - 7 * dayMs
        }
        if (lower.contains("أسبوعين")) {
            return nowEpochMillis - 14 * dayMs
        }

        val numMatch = Regex("""(\d+)""").find(lower)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (numMatch != null && (
                lower.contains("منذ") || lower.contains("قبل") || lower.contains("ago") ||
                    lower.contains("ساع") || lower.contains("دق") || lower.contains("أيام") || lower.contains("ايام")
                )
        ) {
            return when {
                lower.contains("ثان") || lower.contains("sec") -> nowEpochMillis - numMatch * 1000L
                lower.contains("دق") || lower.contains("min") -> nowEpochMillis - numMatch * minuteMs
                lower.contains("ساع") || lower.contains("hour") || lower.contains("hr") -> nowEpochMillis - numMatch * hourMs
                lower.contains("أيام") || lower.contains("ايام") || lower.contains("يوم") || lower.contains("day") -> nowEpochMillis - numMatch * dayMs
                lower.contains("أسب") || lower.contains("اسب") || lower.contains("week") -> nowEpochMillis - numMatch * 7L * dayMs
                lower.contains("شهر") || lower.contains("أشهر") || lower.contains("month") -> nowEpochMillis - numMatch * 30L * dayMs
                lower.contains("سن") || lower.contains("year") -> nowEpochMillis - numMatch * 365L * dayMs
                else -> null
            }
        }

        // 2. Absolute dates: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD, YYYY/MM/DD
        val formats = listOf(
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
        )
        for (pattern in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(cleaned)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    fun sortLatestUpdates(items: List<MangaLatestUpdateItem>): List<MangaLatestUpdateItem> {
        if (items.size <= 1) return items
        return items.sortedWith { a, b ->
            val aTs = a.publishedAtEpochMillis
            val bTs = b.publishedAtEpochMillis
            when {
                aTs != null && bTs != null && aTs != bTs -> bTs.compareTo(aTs) // Newest first
                aTs != null && bTs == null -> -1
                aTs == null && bTs != null -> 1
                else -> a.originalOrderIndex.compareTo(b.originalOrderIndex)
            }
        }
    }

    // ==================== JAVASCRIPT DOM BRIDGE EXTRACTORS ====================

    val JS_EXTRACT_CATALOG = """
        (function() {
            var title = document.title || "";
            var isCf = title.indexOf("Just a moment") !== -1 ||
                       !!document.querySelector("#cf-wrapper, .cf-browser-verification, iframe[src*='challenges.cloudflare.com']");
            var items = [];
            var seen = {};
            var selectors = [
                ".page-item-detail", ".c-tabs-item__content", ".bs .bsx", ".bs", ".listupd .utao",
                ".box", ".manga__item", ".series-card", ".animepost", "article", ".item", ".post-item"
            ];
            var cards = document.querySelectorAll(selectors.join(","));
            function getImgUrl(el) {
                if (!el) return "";
                var img = el.tagName === "IMG" ? el : el.querySelector("img");
                if (img) {
                    var src = img.getAttribute("data-src") || img.getAttribute("data-lazy-src") ||
                              img.getAttribute("data-original") || img.currentSrc || img.src || "";
                    if (src && src.indexOf("data:image") === -1) return src;
                }
                var bgEl = el.querySelector("[style*='background-image']") || el;
                var style = window.getComputedStyle(bgEl).backgroundImage;
                if (style && style !== "none" && style.indexOf("url(") !== -1) {
                    return style.replace(/^url\(["']?/, "").replace(/["']?\)$/, "");
                }
                return "";
            }
            for (var i = 0; i < cards.length; i++) {
                var c = cards[i];
                var a = c.querySelector("a[href]");
                if (!a || !a.href) continue;
                var href = a.href;
                if (seen[href]) continue;
                var imgUrl = getImgUrl(c);
                if (!imgUrl) continue;
                var heading = c.querySelector("h2, h3, h4, h5, .post-title, .tt, .title, .name");
                var t = (heading ? heading.innerText : (a.getAttribute("title") || a.innerText || "")).trim();
                if (!t) {
                    var im = c.querySelector("img[alt]");
                    if (im) t = im.getAttribute("alt").trim();
                }
                if (t && t.length >= 2) {
                    seen[href] = true;
                    items.push({ title: t.split("\n")[0].trim(), url: href, cover: imgUrl });
                }
            }
            return JSON.stringify({ cf: isCf, count: items.length, items: items.slice(0, 80) });
        })();
    """.trimIndent()

    val JS_EXTRACT_DETAILS = """
        (function() {
            var title = document.title || "";
            var isCf = title.indexOf("Just a moment") !== -1 ||
                       !!document.querySelector("#cf-wrapper, .cf-browser-verification, iframe[src*='challenges.cloudflare.com']");
            var h1 = document.querySelector("h1, .post-title h1, .entry-title");
            var workTitle = h1 ? h1.innerText.trim() : title;
            var coverEl = document.querySelector(".summary_image img, .thumb img, .white-bg img, .series-thumb img, .poster img, meta[property='og:image']");
            var cover = "";
            if (coverEl) {
                cover = coverEl.getAttribute("content") || coverEl.getAttribute("data-src") || coverEl.getAttribute("data-lazy-src") || coverEl.currentSrc || coverEl.src || "";
            }
            var descEl = document.querySelector(".summary__content, .description-summary, .review-content, .entry-content-single, .story, .desc, .synopsis");
            var desc = descEl ? descEl.innerText.trim() : "";
            var genres = [];
            var gLinks = document.querySelectorAll(".genres-content a, .mgen a, .wp-manga-tags-list a, .genre-info a, .review-author-info a, a[href*='genre']");
            for (var i = 0; i < gLinks.length; i++) {
                var gt = gLinks[i].innerText.trim();
                if (gt && gt.length >= 2 && gt.length <= 24 && genres.indexOf(gt) === -1) {
                    genres.push(gt);
                }
            }
            var chapters = [];
            var seenCh = {};
            var chSelectors = [
                ".wp-manga-chapter a", ".eplister ul li a", "#chapterlist li a",
                ".clstyle li a", ".chapters-list a", ".ch-main-anchor a"
            ];
            var chNodes = document.querySelectorAll(chSelectors.join(","));
            if (chNodes.length === 0) {
                chNodes = document.querySelectorAll("a[href*='/chapter'], a[href*='/%d8%a7%d9%84%d9%81%d8%b5%d9%84']");
            }
            for (var j = 0; j < chNodes.length; j++) {
                var a = chNodes[j];
                var href = a.href;
                if (!href || seenCh[href] || href === window.location.href) continue;
                var li = a.closest("li, tr, .chapter-item, .chbox, .eph-num");
                var dateEl = (li || a).querySelector(".chapter-release-date, .chapterdate, .date, time, .post-on");
                var dateTxt = dateEl ? dateEl.innerText.trim() : "";
                var numSpan = a.querySelector(".chapternum, .chapter-title, .name, .epxs");
                var txt = "";
                if (numSpan) {
                    txt = numSpan.innerText.trim();
                } else {
                    var clone = a.cloneNode(true);
                    var dNodes = clone.querySelectorAll(".chapter-release-date, .chapterdate, .date, time, .post-on");
                    for (var k = 0; k < dNodes.length; k++) dNodes[k].remove();
                    txt = (clone.innerText || "").replace(/\s+/g, " ").trim();
                }
                var dataNum = (li && li.getAttribute("data-num")) || a.getAttribute("data-num") || "";
                seenCh[href] = true;
                chapters.push({ title: txt, url: href, date: dateTxt, num: dataNum });
            }
            return JSON.stringify({
                cf: isCf,
                title: workTitle,
                cover: cover,
                desc: desc,
                genres: genres.slice(0, 8),
                chapters: chapters.slice(0, 1000)
            });
        })();
    """.trimIndent()

    val JS_EXTRACT_CHAPTER_PAGES = """
        (function() {
            var title = document.title || "";
            var isCf = title.indexOf("Just a moment") !== -1 ||
                       !!document.querySelector("#cf-wrapper, .cf-browser-verification, iframe[src*='challenges.cloudflare.com']");
            var urls = [];
            var seen = {};
            function isInvalidImg(u, el) {
                if (!u || typeof u !== "string") return true;
                var lower = u.toLowerCase();
                if (lower.indexOf("data:image") === 0 || lower.indexOf("javascript:") === 0) return true;
                var blocked = [
                    "logo", "avatar", "favicon", "gravatar", "spinner", "loading", ".svg",
                    "emoji", "wp-smiley", "twemoji", "s.w.org/images/core/emoji", "reaction",
                    "wp-reactions", "wpdiscuz", "sticker", "smilies", "placeholder", "spacer", "1x1"
                ];
                for (var b = 0; b < blocked.length; b++) {
                    if (lower.indexOf(blocked[b]) !== -1) return true;
                }
                if (el) {
                    var cls = (el.className || "").toLowerCase();
                    if (cls.indexOf("emoji") !== -1 || cls.indexOf("wp-smiley") !== -1 || cls.indexOf("reaction") !== -1 || cls.indexOf("avatar") !== -1) {
                        return true;
                    }
                    if (el.closest && el.closest(".comments-area, #comments, .wp-reactions, .reactions, .wpdiscuz, footer, header, .related-manga")) {
                        return true;
                    }
                    var w = el.naturalWidth || parseInt(el.getAttribute("width") || "0", 10) || 0;
                    var h = el.naturalHeight || parseInt(el.getAttribute("height") || "0", 10) || 0;
                    if ((w > 0 && w <= 150) || (h > 0 && h <= 150)) return true;
                }
                return false;
            }
            function addUrl(raw, el) {
                if (!raw || typeof raw !== "string") return;
                var u = raw.trim();
                if (isInvalidImg(u, el)) return;
                if (!seen[u]) {
                    seen[u] = true;
                    urls.push(u);
                }
            }
            try {
                if (window.chapter_preloaded_images && Array.isArray(window.chapter_preloaded_images)) {
                    for (var p = 0; p < window.chapter_preloaded_images.length; p++) {
                        addUrl(window.chapter_preloaded_images[p], null);
                    }
                }
                if (window.chapterImages && Array.isArray(window.chapterImages)) {
                    for (var c = 0; c < window.chapterImages.length; c++) {
                        addUrl(window.chapterImages[c], null);
                    }
                }
            } catch (e) {}
            var readerSelectors = [
                ".reading-content img", "#readerarea img", ".page-break img",
                ".chapter-content img", "#images_chapter img", "img.wp-manga-chapter-img",
                ".vung-doc img", ".container-chapter-reader img", ".rdminimal img",
                ".chapter-reader img"
            ];
            var readerImgs = document.querySelectorAll(readerSelectors.join(","));
            for (var i = 0; i < readerImgs.length; i++) {
                var im = readerImgs[i];
                var src = im.getAttribute("data-src") || im.getAttribute("data-lazy-src") ||
                          im.getAttribute("data-original") || im.getAttribute("data-url") ||
                          im.getAttribute("data-cfsrc") || im.getAttribute("data-pagespeed-lazy-src") ||
                          im.currentSrc || im.src || "";
                addUrl(src, im);
            }
            return JSON.stringify({ cf: isCf, pages: urls });
        })();
    """.trimIndent()

    fun parseCatalogFromDomJson(rawJsonResult: String, site: SiteEntity): CatalogScrapeResult {
        val unwrapped = unwrapEvaluateJsString(rawJsonResult)
        if (unwrapped.isBlank() || unwrapped == "null") {
            return CatalogScrapeResult(emptyList())
        }
        return try {
            val obj = JSONObject(unwrapped)
            val isCf = obj.optBoolean("cf", false)
            val arr = obj.optJSONArray("items") ?: JSONArray()
            val list = mutableListOf<MangaWork>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val url = resolveUrl(site.url, item.optString("url"))
                val cover = resolveUrl(site.url, item.optString("cover"))
                val title = cleanMangaTitle(item.optString("title"))
                if (url.isNotBlank() && title.isNotBlank() && isLikelyMangaWorkUrl(url, site.url)) {
                    list.add(
                        MangaWork(
                            id = slugFromUrl(url),
                            title = title,
                            mangaUrl = url,
                            siteName = site.name,
                            siteUrl = site.url,
                            siteIconKey = site.iconKey,
                            coverImageUrl = cover.ifBlank { null }
                        )
                    )
                }
            }
            CatalogScrapeResult(works = list, requiresCloudflare = isCf && list.isEmpty())
        } catch (_: Exception) {
            CatalogScrapeResult(emptyList())
        }
    }

    fun parseDetailsFromDomJson(rawJsonResult: String, baseWork: MangaWork): DetailsScrapeResult {
        val unwrapped = unwrapEvaluateJsString(rawJsonResult)
        if (unwrapped.isBlank() || unwrapped == "null") {
            return DetailsScrapeResult(baseWork)
        }
        return try {
            val obj = JSONObject(unwrapped)
            val isCf = obj.optBoolean("cf", false)
            val title = cleanMangaTitle(obj.optString("title")).ifBlank { baseWork.title }
            val cover = resolveUrl(baseWork.siteUrl, obj.optString("cover")).ifBlank { baseWork.coverImageUrl ?: "" }
            val desc = obj.optString("desc").trim().ifBlank { baseWork.description }

            val genres = mutableListOf<String>()
            val gArr = obj.optJSONArray("genres")
            if (gArr != null) {
                for (i in 0 until gArr.length()) {
                    val g = gArr.optString(i).trim()
                    if (g.isNotBlank()) genres.add(g)
                }
            }

            val chList = mutableListOf<MangaChapter>()
            val cArr = obj.optJSONArray("chapters")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val cObj = cArr.getJSONObject(i)
                    val chUrl = resolveUrl(baseWork.siteUrl, cObj.optString("url"))
                    val rawTitle = cObj.optString("title").trim()
                    val rawDate = cObj.optString("date").trim()
                    val dataNum = cObj.optString("num").trim()
                    if (chUrl.isNotBlank()) {
                        val separated = separateTitleAndEmbeddedDate(
                            rawTitle = rawTitle,
                            existingDate = rawDate,
                            chapterUrl = chUrl,
                            mangaPageUrl = baseWork.mangaUrl,
                            structuralNumAttr = dataNum
                        )
                        val numInfo = parseChapterNumberInfo(
                            cleanTitle = separated.cleanTitle,
                            rawTitleBeforeDateStrip = rawTitle,
                            chapterUrl = chUrl,
                            mangaPageUrl = baseWork.mangaUrl,
                            structuralNumAttr = dataNum,
                            fallback = i + 1
                        )
                        chList.add(
                            MangaChapter(
                                id = "${baseWork.id}_ch_${numInfo.label.replace('.', '_')}_$i",
                                number = numInfo.intNumber,
                                title = separated.cleanTitle.ifBlank { "الفصل ${numInfo.label}" },
                                dateText = separated.dateText,
                                chapterUrl = chUrl,
                                chapterNumberValue = numInfo.doubleValue,
                                numberLabel = numInfo.label
                            )
                        )
                    }
                }
            }

            val sortedChapters = deduplicateAndSortChapters(chList, baseWork.id)
            val updated = baseWork.copy(
                title = title,
                coverImageUrl = cover.ifBlank { baseWork.coverImageUrl },
                description = desc.ifBlank { "لا يوجد وصف متاح لهذا العمل." },
                genres = if (genres.isNotEmpty()) genres else baseWork.genres,
                chapters = if (sortedChapters.isNotEmpty()) sortedChapters else baseWork.chapters,
                chaptersCount = if (sortedChapters.isNotEmpty()) sortedChapters.size else baseWork.chaptersCount,
                isDetailsLoaded = true
            )
            DetailsScrapeResult(work = updated, requiresCloudflare = isCf && sortedChapters.isEmpty())
        } catch (_: Exception) {
            DetailsScrapeResult(baseWork)
        }
    }

    fun parseChapterPagesFromDomJson(rawJsonResult: String, chapterUrl: String): ChapterPagesScrapeResult {
        val unwrapped = unwrapEvaluateJsString(rawJsonResult)
        if (unwrapped.isBlank() || unwrapped == "null") {
            return ChapterPagesScrapeResult(emptyList())
        }
        return try {
            val obj = JSONObject(unwrapped)
            val isCf = obj.optBoolean("cf", false)
            val pArr = obj.optJSONArray("pages") ?: JSONArray()
            val urls = LinkedHashSet<String>()
            val diagnostics = mutableListOf<PageCandidateDiagnostic>()
            for (i in 0 until pArr.length()) {
                val u = resolveUrl(chapterUrl, pArr.optString(i))
                val rejection = getCandidateRejectionReason(u, tagHtml = "<dom-bridge>", isInsideReaderContainer = true)
                if (rejection == null) {
                    urls.add(u)
                    diagnostics.add(PageCandidateDiagnostic(u, u, "dom-bridge", "js", true, "صورة فصل صالحة من DOM"))
                } else if (u.isNotBlank()) {
                    diagnostics.add(PageCandidateDiagnostic(u, u, "dom-bridge", "js", false, rejection))
                }
            }
            ChapterPagesScrapeResult(
                pageUrls = urls.toList(),
                requiresCloudflare = isCf && urls.isEmpty(),
                diagnostics = diagnostics
            )
        } catch (_: Exception) {
            ChapterPagesScrapeResult(emptyList())
        }
    }

    private fun unwrapEvaluateJsString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return try {
                JSONArray("[$trimmed]").getString(0)
            } catch (_: Exception) {
                trimmed.substring(1, trimmed.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            }
        }
        return trimmed
    }

    // ==================== HELPER UTILITIES ====================

    private fun extractBestImageUrl(fragment: String, baseUrl: String): String {
        return extractBestImageUrlWithAttr(fragment, baseUrl).first
    }

    private fun extractBestImageUrlWithAttr(fragment: String, baseUrl: String): Pair<String, String> {
        val singleUrlAttributes = listOf(
            "data-src",
            "data-lazy-src",
            "data-original",
            "data-url",
            "data-cfsrc",
            "data-pagespeed-lazy-src",
            "data-echo",
            "data-full-url"
        )
        for (attr in singleUrlAttributes) {
            val raw = extractAttribute(fragment, attr).trim()
            if (raw.isNotBlank() && !raw.startsWith("data:image", ignoreCase = true)) {
                val resolved = resolveUrl(baseUrl, raw)
                if (resolved.isNotBlank()) return resolved to attr
            }
        }
        val srcsetAttributes = listOf("data-srcset", "srcset")
        for (attr in srcsetAttributes) {
            val raw = extractAttribute(fragment, attr).trim()
            if (raw.isNotBlank() && !raw.startsWith("data:image", ignoreCase = true)) {
                val bestCandidate = selectBestUrlFromSrcset(raw)
                val resolved = resolveUrl(baseUrl, bestCandidate)
                if (resolved.isNotBlank()) return resolved to attr
            }
        }
        val rawSrc = extractAttribute(fragment, "src").trim()
        if (rawSrc.isNotBlank() && !rawSrc.startsWith("data:image", ignoreCase = true)) {
            val resolved = resolveUrl(baseUrl, rawSrc)
            if (resolved.isNotBlank()) return resolved to "src"
        }
        val bgRegex = Regex("""background-image\s*:\s*url\(([^)]+)\)""", RegexOption.IGNORE_CASE)
        val bgMatch = bgRegex.find(fragment)?.groupValues?.getOrNull(1)
        if (!bgMatch.isNullOrBlank()) {
            return resolveUrl(baseUrl, bgMatch) to "background-image"
        }
        return "" to ""
    }

    /**
     * يختار الرابط الأعلى دقة من سمة srcset (مثلاً 1024w أو 2x) بدلاً من أخذ أول صورة مصغرة.
     */
    internal fun selectBestUrlFromSrcset(srcset: String): String {
        val candidates = srcset.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("data:image", ignoreCase = true) }
        if (candidates.isEmpty()) return ""
        var bestUrl = ""
        var bestScore = -1.0
        for (entry in candidates) {
            val parts = entry.split(Regex("\\s+"))
            val urlPart = parts.firstOrNull()?.trim().orEmpty()
            if (urlPart.isBlank()) continue
            val descriptor = parts.getOrNull(1)?.lowercase(Locale.ROOT).orEmpty()
            val score = when {
                descriptor.endsWith("w") -> descriptor.removeSuffix("w").toDoubleOrNull() ?: 1.0
                descriptor.endsWith("x") -> (descriptor.removeSuffix("x").toDoubleOrNull() ?: 1.0) * 1000.0
                else -> 1.0
            }
            if (score >= bestScore) {
                bestScore = score
                bestUrl = urlPart
            }
        }
        return bestUrl
    }

    private fun extractAttribute(tagOrFragment: String, attrName: String): String {
        val regex = Regex("""\b$attrName\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(tagOrFragment)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractMetaProperty(html: String, prop: String): String {
        val r1 = Regex("""<meta[^>]+property=["']$prop["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val r2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']$prop["']""", RegexOption.IGNORE_CASE)
        return (r1.find(html) ?: r2.find(html))?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractMetaName(html: String, name: String): String {
        val r1 = Regex("""<meta[^>]+name=["']$name["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val r2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+name=["']$name["']""", RegexOption.IGNORE_CASE)
        return (r1.find(html) ?: r2.find(html))?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun stripHtmlTags(input: String): String {
        return input
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&#8217;", "'")
            .replace("&#8211;", "-")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanMangaTitle(raw: String): String {
        return raw
            .replace(Regex("\\s+"), " ")
            .removePrefix("مانجا ")
            .removePrefix("مانهوا ")
            .substringBefore(" - مانجا ليك")
            .substringBefore(" - أزورا")
            .substringBefore(" | Team-X")
            .substringBefore(" - Team-X")
            .trim()
    }

    private fun isLikelyMangaWorkUrl(url: String, siteUrl: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        val baseLower = siteUrl.trimEnd('/').lowercase(Locale.ROOT)
        if (lower == baseLower || lower == "$baseLower/") return false
        val rejectedSegments = listOf(
            "/wp-login", "/wp-admin", "/login", "/register", "/contact", "/privacy",
            "/terms", "/dmca", "/discord", "/facebook", "/twitter", "/tag/", "/manga-genre/",
            "/genre/", "/page/", "?s=", "/author/", "/artist/", "/bookmark", "/history"
        )
        if (rejectedSegments.any { lower.contains(it) }) return false
        if (Regex("""/(?:chapter|ch|فصل|الفصل)[-_/\d]""").containsMatchIn(lower)) return false
        return lower.contains("/manga/") ||
            lower.contains("/series/") ||
            lower.contains("/work/") ||
            lower.contains("/comic/") ||
            lower.contains("/manhwa/") ||
            lower.startsWith(baseLower)
    }

    private fun isSameWorkContext(chapterUrl: String, mangaPageUrl: String): Boolean {
        val workSlug = slugFromUrl(mangaPageUrl).lowercase(Locale.ROOT)
        val lowerCh = chapterUrl.lowercase(Locale.ROOT)
        return workSlug.length >= 3 && lowerCh.contains(workSlug)
    }

    private fun isLikelyChapterUrl(url: String, mangaPageUrl: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        val basePage = mangaPageUrl.substringBefore("?").substringBefore("#").trimEnd('/').lowercase(Locale.ROOT)
        if (lower == basePage || lower == "$basePage/") return false
        val rejectedSegments = listOf(
            "/wp-login", "/wp-admin", "/login", "/register", "/contact", "/privacy",
            "/terms", "/dmca", "/tag/", "/manga-genre/", "/genre/", "/author/", "/artist/",
            "?page=", "?chpage=", "/page/", "#respond", "#comments", "javascript:"
        )
        if (rejectedSegments.any { lower.contains(it) }) return false

        if (basePage.isNotBlank() && lower.startsWith("$basePage/") && lower.length > basePage.length + 1) {
            return true
        }
        val workSlug = slugFromUrl(basePage)
        if (workSlug.length >= 3 && lower.contains(workSlug)) {
            if (lower.contains("chapter") || lower.contains("فصل") || lower.contains("الفصل") ||
                Regex("""/\d+(?:[._-]\d+)?/?$""").containsMatchIn(lower)
            ) {
                return true
            }
        }
        return lower.contains("/chapter") ||
            lower.contains("الفصل") ||
            Regex("""/series/[^/]+/\d+""").containsMatchIn(lower) ||
            Regex("""/manga/[^/]+/(?:chapter|ch|فصل|الفصل|\d+)""").containsMatchIn(lower)
    }

    internal fun normalizeChapterUrlKey(url: String): String {
        if (url.isBlank()) return ""
        return url.trim()
            .substringBefore("#")
            .replace(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
            .trimEnd('/')
            .lowercase(Locale.ROOT)
    }

    internal fun isIgnoredAssetImage(imgUrl: String): Boolean {
        return getCandidateRejectionReason(imgUrl, tagHtml = "", isInsideReaderContainer = true) != null
    }

    private fun isLikelyChapterPageImage(imgUrl: String): Boolean {
        val lower = imgUrl.lowercase(Locale.ROOT)
        return lower.contains(".jpg") ||
            lower.contains(".jpeg") ||
            lower.contains(".png") ||
            lower.contains(".webp") ||
            lower.contains("wp-content/uploads") ||
            lower.contains("chapter") ||
            lower.contains("manga") ||
            lower.contains("cdn")
    }

    private fun slugFromUrl(url: String): String {
        val cleaned = url.trimEnd('/').substringBefore("?").substringAfterLast("/")
        return cleaned.ifBlank { "work_${url.hashCode()}" }
    }
}
