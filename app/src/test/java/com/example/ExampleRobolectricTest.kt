package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaCatalogData
import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.download.MangaDownloadManager
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadedChapterEntity
import com.example.data.local.DownloadedWorkEntity
import com.example.data.local.SiteEntity
import com.example.data.repository.ReadingProgressRepository
import com.example.data.repository.SourceRepository
import com.example.data.source.MangaSourceAdapterRegistry
import com.example.data.storage.LocalStorageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyZeroPreAddedSitesAndLiveScraperExtraction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مكتبة المانجا والمواقع", appName)

        // Ensure no pre-added demo sites or demo storage works exist
        assertTrue(MangaCatalogData.defaultAddedSites.isEmpty())
        assertTrue(MangaCatalogData.storageLibraryWorks.isEmpty())

        // Drawer is open initially
        composeTestRule.onNodeWithTag("navigation_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithText("انقر هنا لتسجيل الدخول").assertIsDisplayed()

        // Navigate to Manage Sites ("إدارة المواقع")
        composeTestRule.onNodeWithTag("drawer_item_manage_sites")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("manage_sites_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("لم تضف أي مواقع بعد").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_add_site_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_browser_button").assertIsDisplayed()

        // Verify live website scraper extracts real covers, details, chapters, and page images
        val site = SiteEntity(
            id = 1L,
            name = "Team-X",
            url = "https://olympustaff.com",
            domain = "olympustaff.com",
            iconKey = "teamx"
        )

        val sampleCatalogHtml = """
            <html>
            <body>
                <div class="bs">
                    <div class="bsx">
                        <a href="https://olympustaff.com/series/high-martial-world" title="بيد واحدة يكتسح آلاف الأنماط">
                            <div class="limit">
                                <img data-src="https://olympustaff.com/uploads/covers/high-martial.jpg" alt="بيد واحدة يكتسح آلاف الأنماط" />
                            </div>
                            <div class="tt">بيد واحدة يكتسح آلاف الأنماط</div>
                        </a>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val catalogWorks = LiveMangaSiteScraper.parseCatalogHtml(
            html = sampleCatalogHtml,
            site = site,
            pageUrl = site.url
        )
        assertEquals(1, catalogWorks.size)
        assertEquals("بيد واحدة يكتسح آلاف الأنماط", catalogWorks.first().title)
        assertEquals("https://olympustaff.com/uploads/covers/high-martial.jpg", catalogWorks.first().coverImageUrl)
        assertEquals("https://olympustaff.com/series/high-martial-world", catalogWorks.first().mangaUrl)

        val sampleDetailsHtml = """
            <html>
            <head>
                <meta property="og:title" content="بيد واحدة يكتسح آلاف الأنماط" />
                <meta property="og:image" content="https://olympustaff.com/uploads/covers/high-martial.jpg" />
            </head>
            <body>
                <div class="entry-content"><p>قصة المانهوا الحقيقية مباشرة من الموقع.</p></div>
                <div class="mgen"><a href="/genre/action">أكشن</a></div>
                <div class="eplister">
                    <ul>
                        <li>
                            <a href="https://olympustaff.com/series/high-martial-world/chapter-2">
                                <span class="chapternum">الفصل 2</span>
                                <span class="chapterdate">2026/10/30</span>
                            </a>
                        </li>
                        <li>
                            <a href="https://olympustaff.com/series/high-martial-world/chapter-1">
                                <span class="chapternum">الفصل 1</span>
                                <span class="chapterdate">2026/10/29</span>
                            </a>
                        </li>
                    </ul>
                </div>
            </body>
            </html>
        """.trimIndent()

        val enrichedWork = LiveMangaSiteScraper.parseMangaDetailsHtml(
            html = sampleDetailsHtml,
            baseWork = catalogWorks.first(),
            pageUrl = catalogWorks.first().mangaUrl
        )
        assertEquals(2, enrichedWork.chapters.size)
        assertEquals("https://olympustaff.com/series/high-martial-world/chapter-1", enrichedWork.chapters.first().chapterUrl)

        val sampleChapterHtml = """
            <html>
            <body>
                <div class="reading-content">
                    <div class="page-break no-gaps">
                        <img id="image-0" data-src="https://olympustaff.com/uploads/chapters/ch1/01.jpg" class="wp-manga-chapter-img" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-1" data-lazy-src="https://olympustaff.com/uploads/chapters/ch1/02.jpg" class="wp-manga-chapter-img" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-2" data-original="https://olympustaff.com/uploads/chapters/ch1/03.jpg" class="wp-manga-chapter-img" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-3" src="https://olympustaff.com/uploads/chapters/ch1/04.webp" class="wp-manga-chapter-img" />
                    </div>
                </div>
                <div class="comments-area">
                    <img src="https://olympustaff.com/uploads/avatars/user1.jpg" class="avatar" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val pageUrls = LiveMangaSiteScraper.parseChapterPagesHtml(
            html = sampleChapterHtml,
            chapterUrl = enrichedWork.chapters.first().chapterUrl
        )
        assertEquals(4, pageUrls.size)
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/01.jpg", pageUrls[0])
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/02.jpg", pageUrls[1])
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/03.jpg", pageUrls[2])
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/04.webp", pageUrls[3])
    }

    @Test
    fun verifySourceRepositoryLocalStorageOfflineReaderProgressAndDeletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.appDao()
        val storageManager = LocalStorageManager(context)
        val sourceRepository = SourceRepository(dao)
        val progressRepository = ReadingProgressRepository(dao)

        // 1. Add a new source and verify persistence, capabilities, and lastVisitedUrl
        val siteId = sourceRepository.addSource(
            name = "Azora",
            url = "azoramoon.com",
            domain = "azoramoon.com",
            iconKey = "azora",
            supportsChapterDownload = true
        )
        assertTrue(siteId > 0L)
        val savedSite = sourceRepository.getSourceById(siteId)
        assertNotNull(savedSite)
        assertEquals("https://azoramoon.com", savedSite?.url)
        assertEquals("azoramoon.com", savedSite?.domain)

        sourceRepository.updateLastVisitedUrl(siteId, "https://azoramoon.com/series/solo-leveling")
        val updatedSite = sourceRepository.getSourceById(siteId)
        assertEquals("https://azoramoon.com/series/solo-leveling", updatedSite?.lastVisitedUrl)

        val capabilities = MangaSourceAdapterRegistry.getCapabilitiesForSite(updatedSite!!)
        assertTrue(capabilities.supportsWebViewBrowsing)
        assertTrue(capabilities.supportsChapterDetection)
        assertTrue(capabilities.supportsChapterDownload)
        assertTrue(capabilities.supportsOfflineReader)

        // 2. Save real local chapter page images and cover on disk via LocalStorageManager
        val workId = "azora_solo_leveling"
        val ch1Key = "${workId}_ch_1"
        val ch2Key = "${workId}_ch_2"

        // Clean any prior test artifacts
        storageManager.deleteWorkFiles(workId)

        val fakeJpegBytes = storageManager.generateValidJpegBytes("Solo Leveling", "Ch 1", 1)
        val coverPath = storageManager.saveCoverImage(workId, fakeJpegBytes)
        assertTrue(File(coverPath).exists())

        val ch1Page1 = storageManager.saveChapterPageImage(workId, ch1Key, 0, fakeJpegBytes)
        val ch1Page2 = storageManager.saveChapterPageImage(workId, ch1Key, 1, fakeJpegBytes)
        val ch1Page3 = storageManager.saveChapterPageImage(workId, ch1Key, 2, fakeJpegBytes)

        val ch2Page1 = storageManager.saveChapterPageImage(workId, ch2Key, 0, fakeJpegBytes)
        val ch2Page2 = storageManager.saveChapterPageImage(workId, ch2Key, 1, fakeJpegBytes)

        // 3. Verify complete vs incomplete chapter file verification
        assertTrue(
            storageManager.verifyChapterFilesComplete(
                workId = workId,
                chapterKey = ch1Key,
                expectedPages = 3,
                pagePaths = listOf(ch1Page1, ch1Page2, ch1Page3)
            )
        )
        // If expectedPages is 4 but only 3 files exist, verification MUST return false (INCOMPLETE)
        assertFalse(
            storageManager.verifyChapterFilesComplete(
                workId = workId,
                chapterKey = ch1Key,
                expectedPages = 4,
                pagePaths = listOf(ch1Page1, ch1Page2, ch1Page3)
            )
        )

        // 4. Store DownloadedWorkEntity and DownloadedChapterEntity in Room (COMPLETED & INCOMPLETE)
        dao.upsertDownloadedWork(
            DownloadedWorkEntity(
                workId = workId,
                sourceId = siteId,
                sourceName = "Azora",
                sourceUrl = "https://azoramoon.com",
                title = "ارتقاء المستوى المنفرد",
                subtitle = "Solo Leveling",
                author = "Chugong",
                statusText = "مكتمل",
                status = "مكتمل",
                synopsis = "قصة صياد يرتقي بمفرده.",
                genresCsv = "أكشن,خيال",
                mangaUrl = "https://azoramoon.com/series/solo-leveling",
                remoteCoverUrl = "https://azoramoon.com/uploads/solo.jpg",
                localCoverPath = coverPath,
                savedChaptersCount = 1,
                totalChaptersOnSite = 2,
                totalStorageBytes = 1536L
            )
        )

        dao.upsertDownloadedChapter(
            DownloadedChapterEntity(
                chapterKey = ch1Key,
                workId = workId,
                sourceId = siteId,
                sourceName = "Azora",
                workTitle = "ارتقاء المستوى المنفرد",
                chapterId = "ch_1",
                chapterNumber = 1,
                chapterTitle = "الفصل 1",
                dateText = "2026/10/29",
                chapterUrl = "https://azoramoon.com/series/solo-leveling/1",
                localPagesJson = storageManager.encodeLocalPathsJson(listOf(ch1Page1, ch1Page2, ch1Page3)),
                pageCount = 3,
                downloadedPagesCount = 3,
                totalBytes = 1536L,
                status = MangaDownloadManager.STATUS_COMPLETED
            )
        )

        dao.upsertDownloadedChapter(
            DownloadedChapterEntity(
                chapterKey = ch2Key,
                workId = workId,
                sourceId = siteId,
                sourceName = "Azora",
                workTitle = "ارتقاء المستوى المنفرد",
                chapterId = "ch_2",
                chapterNumber = 2,
                chapterTitle = "الفصل 2",
                dateText = "2026/10/30",
                chapterUrl = "https://azoramoon.com/series/solo-leveling/2",
                localPagesJson = storageManager.encodeLocalPathsJson(listOf(ch2Page1, ch2Page2)),
                pageCount = 3, // Expected 3 pages, only 2 downloaded -> INCOMPLETE
                downloadedPagesCount = 2,
                totalBytes = 1024L,
                status = MangaDownloadManager.STATUS_INCOMPLETE
            )
        )

        val storedCh1 = dao.getDownloadedChapterByKey(ch1Key)
        val storedCh2 = dao.getDownloadedChapterByKey(ch2Key)
        assertNotNull(storedCh1)
        assertNotNull(storedCh2)
        assertEquals(MangaDownloadManager.STATUS_COMPLETED, storedCh1?.status)
        assertEquals(MangaDownloadManager.STATUS_INCOMPLETE, storedCh2?.status)

        // 5. Save and restore reading progress in ReadingProgressRepository
        progressRepository.saveProgress(
            chapterKey = ch1Key,
            workId = workId,
            chapterNumber = 1,
            chapterTitle = "الفصل 1",
            lastPageIndex = 2,
            scrollOffset = 140,
            totalPages = 3
        )

        val restoredProgress = progressRepository.getChapterProgress(ch1Key)
        assertNotNull(restoredProgress)
        assertEquals(2, restoredProgress?.lastPageIndex)
        assertEquals(140, restoredProgress?.scrollOffset)
        assertTrue(restoredProgress?.isCompleted == true)

        val latestWorkProgress = progressRepository.getLatestWorkProgress(workId)
        assertNotNull(latestWorkProgress)
        assertEquals(1, latestWorkProgress?.chapterNumber)
        val formattedProgress = progressRepository.formatRelativeProgressText(latestWorkProgress!!)
        assertTrue(formattedProgress.contains("الصفحة 3"))

        // 6. Delete Chapter 2 only and verify Chapter 1 files and cover remain intact
        storageManager.deleteChapterFiles(workId, ch2Key)
        dao.deleteDownloadedChapterByKey(ch2Key)

        assertNull(dao.getDownloadedChapterByKey(ch2Key))
        assertFalse(File(ch2Page1).exists())
        assertTrue(File(ch1Page1).exists())
        assertTrue(File(ch1Page2).exists())
        assertTrue(File(ch1Page3).exists())
        assertTrue(File(coverPath).exists())

        // Clean up test files
        storageManager.deleteWorkFiles(workId)
        db.close()
    }

    @Test
    fun verifyMagicEmperorEmojiRejectionAndChapterPagesExtraction() {
        val chapterUrl = "https://olympustaff.com/series/magic-emperor/chapter-540"

        // Case 1: Page has WordPress emoji, reaction faces, comments, AND real chapter pages inside .reading-content
        val magicEmperorHtml = """
            <html>
            <body>
                <div class="entry-content">
                    <p>ملاحظة المترجم <img src="https://s.w.org/images/core/emoji/15.0.3/72x72/1f600.png" class="wp-smiley emoji" width="72" height="72" /></p>
                    <div class="reading-content">
                        <div class="page-break">
                            <img id="image-1"
                                 src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
                                 data-src="https://olympustaff.com/uploads/magic-emperor/540/01.webp"
                                 srcset="https://olympustaff.com/uploads/magic-emperor/540/01-300x420.webp 300w, https://olympustaff.com/uploads/magic-emperor/540/01-full.webp 1200w"
                                 class="wp-manga-chapter-img" />
                        </div>
                        <div class="page-break">
                            <img id="image-2"
                                 data-lazy-src="https://olympustaff.com/uploads/magic-emperor/540/02.jpg"
                                 class="wp-manga-chapter-img" />
                        </div>
                        <div class="wp-reactions-container">
                            <img src="https://olympustaff.com/wp-content/plugins/wp-reactions/assets/emoji-laugh.png" class="reaction-icon" />
                        </div>
                    </div>
                    <div class="comments-area">
                        <img src="https://s.w.org/images/core/emoji/14.0.0/72x72/1f602.png" class="emoji" />
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val extractionResult = LiveMangaSiteScraper.parseChapterPagesWithDiagnostics(
            html = magicEmperorHtml,
            chapterUrl = chapterUrl
        )
        val extractedPages = extractionResult.pageUrls
        val diagnostics = extractionResult.diagnostics

        // Only the 2 real pages should be accepted; emojis and reaction images must be rejected
        assertEquals(2, extractedPages.size)
        // Highest resolution from data-src / srcset must be preserved in order
        assertEquals("https://olympustaff.com/uploads/magic-emperor/540/01.webp", extractedPages[0])
        assertEquals("https://olympustaff.com/uploads/magic-emperor/540/02.jpg", extractedPages[1])
        assertFalse(extractedPages.any { it.contains("s.w.org") || it.contains("emoji") || it.contains("reaction") })

        // Verify diagnostics recorded the rejected emoji candidate and its reason
        assertTrue(diagnostics.any { !it.accepted && it.resolvedUrl.contains("1f600.png") })

        // Case 2: Page where ONLY emojis/reactions exist (e.g., broken/empty reader container)
        // Must return empty list so the app shows a clear error instead of displaying the emoji face!
        val emojiOnlyHtml = """
            <html>
            <body>
                <div class="entry-content">
                    <img id="image-emoji" src="https://s.w.org/images/core/emoji/15.0.3/72x72/1f62d.png" class="emoji" />
                    <img src="https://olympustaff.com/assets/reactions/big-face-emoji.png" width="96" height="96" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val emojiOnlyPages = LiveMangaSiteScraper.parseChapterPagesHtml(
            html = emojiOnlyHtml,
            chapterUrl = chapterUrl
        )
        assertTrue("Emoji-only HTML must produce 0 valid chapter pages", emojiOnlyPages.isEmpty())
    }

    @Test
    fun verifyChapterListSeparatesTitleNumberAndDateWithout201OrDateCorruption() {
        val baseWork = MangaWork(
            id = "work_test_29",
            title = "إمبراطور السحر",
            siteName = "Team-X",
            siteUrl = "https://olympustaff.com",
            mangaUrl = "https://olympustaff.com/series/magic-emperor"
        )

        // Build 29 chapters: 1..19, 20, 20.1 (which has embedded date 03/10/2026 & slash token), and 21..28 -> total 29 chapters
        val chapterItemsHtml = buildString {
            for (ch in 28 downTo 21) {
                appendLine(
                    """
                    <li data-num="$ch">
                        <a href="https://olympustaff.com/series/magic-emperor/chapter-$ch">
                            <span class="chapternum">الفصل $ch - بداية المعركة</span>
                            <span class="chapterdate">03/10/2026</span>
                        </a>
                    </li>
                    """.trimIndent()
                )
            }
            // The problematic decimal chapter 20.1 where date was previously concatenated into "201/10/2026"
            appendLine(
                """
                <li data-num="20.1">
                    <a href="https://olympustaff.com/series/magic-emperor/chapter-20-1">
                        <span class="chapternum">الفصل 20.1 03/10/2026</span>
                        <span class="chapterdate">03/10/2026</span>
                    </a>
                </li>
                """.trimIndent()
            )
            // Also include a duplicate link to verify deduplication
            appendLine(
                """
                <li data-num="20.1">
                    <a href="https://olympustaff.com/series/magic-emperor/chapter-20-1/">
                        <span class="chapternum">201/10/2026</span>
                    </a>
                </li>
                """.trimIndent()
            )
            for (ch in 20 downTo 1) {
                appendLine(
                    """
                    <li data-num="$ch">
                        <a href="https://olympustaff.com/series/magic-emperor/chapter-$ch">
                            <span class="chapternum">الفصل $ch</span>
                            <span class="chapterdate">01/10/2026</span>
                        </a>
                    </li>
                    """.trimIndent()
                )
            }
        }

        val detailsHtml = """
            <html>
            <body>
                <div class="eplister">
                    <ul>
                        $chapterItemsHtml
                    </ul>
                </div>
            </body>
            </html>
        """.trimIndent()

        val parsedWork = LiveMangaSiteScraper.parseMangaDetailsHtml(
            html = detailsHtml,
            baseWork = baseWork,
            pageUrl = baseWork.mangaUrl
        )

        // 1. Exact chapter count must be 29 (1..20 + 20.1 + 21..28)
        assertEquals(29, parsedWork.chapters.size)

        // 2. No chapter may be misparsed as 201 or 2026
        assertFalse(
            "No chapter should have number 201 from 20.1 or date 03/10/2026",
            parsedWork.chapters.any { it.number == 201 || it.number == 2026 }
        )

        // 3. Chapter 20.1 must preserve its decimal value and label, and be positioned between 20 and 21
        val ch20Index = parsedWork.chapters.indexOfFirst { it.displayNumber == "20" }
        val ch20Point1Index = parsedWork.chapters.indexOfFirst { it.displayNumber == "20.1" }
        val ch21Index = parsedWork.chapters.indexOfFirst { it.displayNumber == "21" }
        assertTrue(ch20Index >= 0)
        assertTrue(ch20Point1Index > ch20Index)
        assertTrue(ch21Index > ch20Point1Index)

        val ch20Point1 = parsedWork.chapters[ch20Point1Index]
        assertEquals(20.1, ch20Point1.chapterNumberValue, 0.001)
        assertEquals("20.1", ch20Point1.displayNumber)

        // 4. Release dates like 03/10/2026 must be in dateText and NOT inside chapter.title
        parsedWork.chapters.forEach { ch ->
            assertFalse(
                "Chapter title '${ch.title}' must not contain release date 03/10/2026 or 01/10/2026",
                ch.title.contains("03/10/2026") || ch.title.contains("01/10/2026")
            )
        }
        assertEquals("03/10/2026", ch20Point1.dateText)
    }

    @Test
    fun verifyImageByteAndDimensionValidationRejectsTinyEmojisAndHtmlErrorPages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storageManager = LocalStorageManager(context)

        // 1. Valid chapter page JPEG bytes (generated at full page resolution) -> accepted
        val validPageBytes = storageManager.generateValidJpegBytes("Magic Emperor", "Chapter 540", 1)
        assertTrue(storageManager.isValidChapterPageImageBytes(validPageBytes, "image/jpeg"))

        // 2. HTML error page masquerading as image URL -> rejected
        val htmlErrorBytes = "<html><body>403 Forbidden Cloudflare</body></html>".toByteArray()
        assertFalse(storageManager.isValidChapterPageImageBytes(htmlErrorBytes, "text/html"))
        assertFalse(storageManager.isValidChapterPageImageBytes(htmlErrorBytes, "image/jpeg"))

        // 3. Tiny 72x72 PNG (like a WordPress emoji) -> rejected by dimension check (<= 150x150)
        val tiny72x72PngHeader = ByteArray(256).apply {
            // PNG signature: 89 50 4E 47 0D 0A 1A 0A
            this[0] = 0x89.toByte()
            this[1] = 0x50
            this[2] = 0x4E
            this[3] = 0x47
            this[4] = 0x0D
            this[5] = 0x0A
            this[6] = 0x1A
            this[7] = 0x0A
            // IHDR width = 72 (bytes 16..19), height = 72 (bytes 20..23)
            this[16] = 0
            this[17] = 0
            this[18] = 0
            this[19] = 72
            this[20] = 0
            this[21] = 0
            this[22] = 0
            this[23] = 72
        }
        val dims = storageManager.decodeImageDimensions(tiny72x72PngHeader)
        assertNotNull(dims)
        assertEquals(72, dims?.first)
        assertEquals(72, dims?.second)
        assertFalse(
            "72x72 emoji PNG must be rejected as a chapter page",
            storageManager.isValidChapterPageImageBytes(tiny72x72PngHeader, "image/png")
        )
    }

    @Test
    fun verifyLatestUpdatesExtractionSortingMissingDateFallbackAndOfflineCache() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.appDao()
        val settingsDataStore = com.example.data.preferences.UserSettingsDataStore(context)
        val repository = com.example.data.repository.AppRepository(context, dao, settingsDataStore)

        val site = SiteEntity(
            id = 7L,
            name = "Azora",
            url = "https://azoramoon.com",
            domain = "azoramoon.com",
            iconKey = "azora"
        )

        val latestUpdatesHtml = """
            <html>
            <body>
                <div class="page-item-detail manga">
                    <div class="item-thumb">
                        <a href="https://azoramoon.com/series/magic-emperor" title="إمبراطور السحر">
                            <img data-src="https://azoramoon.com/uploads/covers/magic-emperor.jpg" alt="إمبراطور السحر" />
                        </a>
                    </div>
                    <div class="item-summary">
                        <div class="post-title">
                            <h3><a href="https://azoramoon.com/series/magic-emperor">إمبراطور السحر</a></h3>
                        </div>
                        <div class="list-chapter">
                            <div class="chapter-item">
                                <span class="chapter">
                                    <a href="https://azoramoon.com/series/magic-emperor/chapter-540">الفصل 540 - المواجهة الكبرى</a>
                                </span>
                                <span class="post-on">منذ 10 دقائق</span>
                            </div>
                            <div class="chapter-item">
                                <span class="chapter">
                                    <a href="https://azoramoon.com/series/magic-emperor/chapter-539-5">الفصل 539.5</a>
                                </span>
                                <span class="post-on">منذ ساعتين</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="page-item-detail manga">
                    <div class="item-thumb">
                        <a href="https://azoramoon.com/series/solo-leveling" title="ارتقاء المستوى المنفرد">
                            <img src="https://azoramoon.com/uploads/covers/solo-leveling.jpg" />
                        </a>
                    </div>
                    <div class="item-summary">
                        <div class="post-title">
                            <h3><a href="https://azoramoon.com/series/solo-leveling">ارتقاء المستوى المنفرد</a></h3>
                        </div>
                        <div class="list-chapter">
                            <div class="chapter-item">
                                <span class="chapter">
                                    <a href="https://azoramoon.com/series/solo-leveling/chapter-200">الفصل 200</a>
                                </span>
                            </div>
                            <!-- Duplicate chapter link to test deduplication -->
                            <div class="chapter-item">
                                <span class="chapter">
                                    <a href="https://azoramoon.com/series/solo-leveling/chapter-200/">الفصل 200</a>
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
                <a class="next page-numbers" href="https://azoramoon.com/page/2/">التالي</a>
            </body>
            </html>
        """.trimIndent()

        val extractedUpdates = LiveMangaSiteScraper.parseLatestUpdatesHtml(
            html = latestUpdatesHtml,
            site = site,
            pageUrl = site.url
        )

        // 3 distinct chapter updates (540, 539.5, and 200; duplicate 200 is deduplicated)
        assertEquals(3, extractedUpdates.size)

        // Sorted by newest timestamp first: "منذ 10 دقائق" (540) before "منذ ساعتين" (539.5), then undated item (200)
        val firstUpdate = extractedUpdates[0]
        assertEquals("إمبراطور السحر", firstUpdate.workTitle)
        assertEquals("https://azoramoon.com/uploads/covers/magic-emperor.jpg", firstUpdate.coverImageUrl)
        assertEquals("540", firstUpdate.displayChapterNumber)
        assertEquals("منذ 10 دقائق", firstUpdate.publishDateText)
        assertEquals("Azora", firstUpdate.sourceName)
        assertTrue((firstUpdate.publishedAtEpochMillis ?: 0L) > 0L)

        val secondUpdate = extractedUpdates[1]
        assertEquals("539.5", secondUpdate.displayChapterNumber)
        assertEquals("منذ ساعتين", secondUpdate.publishDateText)
        assertTrue(firstUpdate.publishedAtEpochMillis!! > secondUpdate.publishedAtEpochMillis!!)

        // Third update has no publish date in HTML -> must not fabricate a date and must return "التاريخ غير متاح"
        val thirdUpdate = extractedUpdates[2]
        assertEquals("ارتقاء المستوى المنفرد", thirdUpdate.workTitle)
        assertEquals("200", thirdUpdate.displayChapterNumber)
        assertEquals("", thirdUpdate.publishDateText)
        assertNull(thirdUpdate.publishedAtEpochMillis)
        assertEquals("التاريخ غير متاح", thirdUpdate.displayDateOrFallback)

        // Cache in Room and verify offline retrieval preserves order and fields
        repository.cacheLatestUpdatesDirectly(site.id, extractedUpdates)
        val cachedOffline = repository.getCachedLatestUpdates(site.id)
        assertEquals(3, cachedOffline.size)
        assertEquals("540", cachedOffline[0].displayChapterNumber)
        assertEquals("539.5", cachedOffline[1].displayChapterNumber)
        assertEquals("التاريخ غير متاح", cachedOffline[2].displayDateOrFallback)

        db.close()
    }

    @Test
    fun verifyMangatekImgSrcExtractionAndNonImageUrlFiltering() {
        val mangatekChapterHtml = """
            <html>
            <head>
                <script>
                    var chapter_Data = ["https://mangatek.com/manga/logging-10000-years/chapter-1", "https://mangatek.com/manga/logging-10000-years/chapter-3"];
                </script>
            </head>
            <body>
                <div class="reading-content">
                    <div class="page-break no-gaps">
                        <img id="image-0" src="
                            https://mangatek.com/wp-content/uploads/WP-manga/data/manga_65/ch2/001.webp
                        " class="wp-manga-chapter-img" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-1" src="data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=" data-src="https://mangatek.com/wp-content/uploads/WP-manga/data/manga_65/ch2/002.jpg" class="wp-manga-chapter-img lazyload" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-2" data-lazy-src="https://mangatek.com/wp-content/uploads/WP-manga/data/manga_65/ch2/003.png" src="https://mangatek.com/assets/lazy-placeholder.gif" class="wp-manga-chapter-img" />
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val pages = LiveMangaSiteScraper.parseChapterPagesHtml(
            html = mangatekChapterHtml,
            chapterUrl = "https://mangatek.com/manga/logging-10000-years/chapter-2/"
        )

        assertEquals(3, pages.size)
        assertEquals("https://mangatek.com/wp-content/uploads/WP-manga/data/manga_65/ch2/001.webp", pages[0])
        assertEquals("https://mangatek.com/wp-content/uploads/WP-manga/data/manga_65/ch2/002.jpg", pages[1])
        assertEquals("https://mangatek.com/wp-content/uploads/WP-manga/data/manga_65/ch2/003.png", pages[2])
    }
}

