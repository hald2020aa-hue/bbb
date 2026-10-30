package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.webkit.CookieManager
import android.webkit.WebStorage
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Manages actual image files and offline chapter storage in app-private directories.
 * Keeps offline chapters strictly separate from WebView caches so clearing browser data
 * never deletes downloaded chapters, and offline reading works in Airplane Mode.
 */
class LocalStorageManager(private val context: Context) {

    fun getRootSavedDir(): File {
        val extDir = try {
            context.getExternalFilesDir("saved")
        } catch (_: Throwable) {
            null
        }
        val baseDir = extDir ?: File(context.filesDir, "saved")
        if (!baseDir.exists()) baseDir.mkdirs()
        return baseDir
    }

    fun getOfflineRootDir(): File {
        val offlineRoot = File(getRootSavedDir(), "manga_offline")
        if (!offlineRoot.exists()) offlineRoot.mkdirs()
        return offlineRoot
    }

    fun getWorkDir(sourceId: Long, workId: String): File {
        val safeWork = sanitizeSegment(workId)
        val dir = File(getOfflineRootDir(), "src_${sourceId}/$safeWork")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getChapterDir(sourceId: Long, workId: String, chapterKey: String): File {
        val safeChapter = sanitizeSegment(chapterKey)
        val dir = File(getWorkDir(sourceId, workId), safeChapter)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getChapterDirectory(workId: String, chapterKey: String): File =
        getChapterDir(0L, workId, chapterKey)

    fun hasValidLocalCover(path: String): Boolean {
        if (path.isBlank()) return false
        val f = File(path)
        return isValidImageFile(f)
    }

    fun getExistingValidCoverFile(sourceId: Long, workId: String): File? {
        val workDir = getWorkDir(sourceId, workId)
        val candidates = listOf("cover.jpg", "cover.webp", "cover.png", "cover.jpeg")
        for (name in candidates) {
            val f = File(workDir, name)
            if (isValidImageFile(f)) return f
        }
        return null
    }

    /**
     * Checks if a valid downloaded image already exists for a given page index (0-based)
     * so retrying an incomplete chapter reuses already-saved valid pages without re-downloading.
     */
    fun getExistingValidPageFile(
        sourceId: Long,
        workId: String,
        chapterKey: String,
        pageIndex: Int
    ): File? {
        val chapterDir = getChapterDir(sourceId, workId, chapterKey)
        val prefix = String.format("page_%03d.", pageIndex + 1)
        val matches = chapterDir.listFiles { file ->
            file.isFile && file.name.startsWith(prefix)
        } ?: return null
        return matches.firstOrNull { isValidImageFile(it) }
    }

    fun saveCoverImage(sourceId: Long, workId: String, bytes: ByteArray, allowSyntheticFallback: Boolean = false): File {
        val workDir = getWorkDir(sourceId, workId)
        val coverFile = File(workDir, "cover.jpg")
        if (bytes.isNotEmpty() && isValidImageBytes(bytes)) {
            val tmpFile = File(workDir, "cover.jpg.tmp")
            tmpFile.writeBytes(bytes)
            if (tmpFile.exists() && tmpFile.length() >= 64L) {
                if (coverFile.exists()) coverFile.delete()
                tmpFile.renameTo(coverFile)
            }
        } else if (allowSyntheticFallback && (!coverFile.exists() || coverFile.length() == 0L)) {
            coverFile.writeBytes(generateValidJpegBytes(title = workId, subtitle = "Cover", pageNumber = 0))
        }
        return coverFile
    }

    fun saveCoverImage(workId: String, bytes: ByteArray): String =
        saveCoverImage(0L, workId, bytes, allowSyntheticFallback = false).absolutePath

    fun saveChapterPageImage(
        sourceId: Long,
        workId: String,
        chapterKey: String,
        pageIndex: Int,
        bytes: ByteArray,
        extension: String = "jpg"
    ): File {
        val chapterDir = getChapterDir(sourceId, workId, chapterKey)
        val safeExt = extension.lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") } ?: "jpg"
        val fileName = String.format("page_%03d.%s", pageIndex + 1, safeExt)
        val pageFile = File(chapterDir, fileName)
        val tmpFile = File(chapterDir, "$fileName.tmp")
        tmpFile.writeBytes(bytes)
        if (pageFile.exists()) pageFile.delete()
        if (!tmpFile.renameTo(pageFile)) {
            pageFile.writeBytes(bytes)
            tmpFile.delete()
        }
        return pageFile
    }

    fun saveChapterPageImage(
        workId: String,
        chapterKey: String,
        pageIndex: Int,
        bytes: ByteArray
    ): String = saveChapterPageImage(0L, workId, chapterKey, pageIndex, bytes).absolutePath

    /**
     * Generates a valid JPEG image byte array for offline test/verification or fallback
     * when a test environment provides synthetic image streams.
     */
    fun generateValidJpegBytes(title: String, subtitle: String, pageNumber: Int): ByteArray {
        return try {
            val width = 720
            val height = 1080
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(18, 20, 28))

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(66, 76, 112)
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(155, 168, 238)
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(title.take(32), width / 2f, height * 0.42f, titlePaint)
            canvas.drawText(subtitle.take(36), width / 2f, height * 0.50f, subPaint)
            if (pageNumber > 0) {
                canvas.drawText("Page $pageNumber", width / 2f, height * 0.58f, subPaint)
            }

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            val result = out.toByteArray()
            if (result.size > 128) result else createMinimalJpegHeaderBytes(pageNumber)
        } catch (_: Throwable) {
            createMinimalJpegHeaderBytes(pageNumber)
        }
    }

    private fun createMinimalJpegHeaderBytes(seed: Int): ByteArray {
        val header = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x48
        )
        val body = ByteArray(1024) { idx -> ((idx + seed) and 0xFF).toByte() }
        val footer = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
        return header + body + footer
    }

    /**
     * Validates that the downloaded payload is a real image (JPEG, PNG, WEBP, GIF, BMP, AVIF)
     * and NOT an HTML error page, Cloudflare challenge, JSON error, or empty/corrupt response.
     */
    fun isValidImageBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 64) return false

        // Reject HTML / XML / JSON error payloads masquerading as 200 OK
        val headAscii = String(bytes, 0, minOf(bytes.size, 160), Charsets.UTF_8).trimStart()
        if (headAscii.startsWith("<!DOCTYPE", ignoreCase = true) ||
            headAscii.startsWith("<html", ignoreCase = true) ||
            headAscii.startsWith("<head", ignoreCase = true) ||
            headAscii.startsWith("<script", ignoreCase = true) ||
            headAscii.startsWith("<?xml", ignoreCase = true) ||
            headAscii.startsWith("{\"error\"", ignoreCase = true) ||
            headAscii.contains("Attention Required! | Cloudflare", ignoreCase = true) ||
            headAscii.contains("Just a moment...", ignoreCase = true)
        ) {
            return false
        }

        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF

        // JPEG: FF D8 FF
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return true
        // PNG: 89 50 4E 47
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return true
        // RIFF / WEBP: 52 49 46 46
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) return true
        // GIF: 47 49 46 38
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return true
        // BMP: 42 4D
        if (b0 == 0x42 && b1 == 0x4D && bytes.size > 128) return true
        // AVIF / HEIF ftyp box at offset 4..7 ('ftyp')
        if (bytes.size >= 12) {
            val b4 = bytes[4].toInt() and 0xFF
            val b5 = bytes[5].toInt() and 0xFF
            val b6 = bytes[6].toInt() and 0xFF
            val b7 = bytes[7].toInt() and 0xFF
            if (b4 == 0x66 && b5 == 0x74 && b6 == 0x79 && b7 == 0x70) return true
        }

        // Secondary check via BitmapFactory bounds decoding
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            opts.outWidth > 0 && opts.outHeight > 0
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * يقرأ أبعاد الصورة الفعلية (العرض × الارتفاع) من ترويسة الملف (PNG / GIF / WEBP / JPEG) أو BitmapFactory
     * لاستبعاد الأيقونات الصغيرة والوجوه التعبيرية (Emojis مثل 72x72 أو 120x120) قبل قبولها كصفحة فصل.
     */
    fun decodeImageDimensions(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size < 24) return null
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF

        // PNG IHDR chunk: bytes 16..19 width (big-endian), bytes 20..23 height (big-endian)
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47 && bytes.size >= 24) {
            val w = ((bytes[16].toInt() and 0xFF) shl 24) or
                ((bytes[17].toInt() and 0xFF) shl 16) or
                ((bytes[18].toInt() and 0xFF) shl 8) or
                (bytes[19].toInt() and 0xFF)
            val h = ((bytes[20].toInt() and 0xFF) shl 24) or
                ((bytes[21].toInt() and 0xFF) shl 16) or
                ((bytes[22].toInt() and 0xFF) shl 8) or
                (bytes[23].toInt() and 0xFF)
            if (w > 0 && h > 0) return w to h
        }

        // GIF: bytes 6..7 width (little-endian), bytes 8..9 height (little-endian)
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38 && bytes.size >= 10) {
            val w = (bytes[6].toInt() and 0xFF) or ((bytes[7].toInt() and 0xFF) shl 8)
            val h = (bytes[8].toInt() and 0xFF) or ((bytes[9].toInt() and 0xFF) shl 8)
            if (w > 0 && h > 0) return w to h
        }

        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                opts.outWidth to opts.outHeight
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * يتحقق من أن البيانات المستلمة تمثل صفحة فصل صالحة وليست استجابة خطأ HTML أو صورة تعبيرية صغيرة.
     */
    fun isValidChapterPageImageBytes(bytes: ByteArray, contentType: String? = null): Boolean {
        if (!contentType.isNullOrBlank()) {
            val lowerCt = contentType.lowercase()
            if (lowerCt.contains("text/html") ||
                lowerCt.contains("application/json") ||
                lowerCt.contains("text/plain") ||
                lowerCt.contains("image/svg")
            ) {
                return false
            }
        }
        if (!isValidImageBytes(bytes)) return false
        val dims = decodeImageDimensions(bytes)
        if (dims != null) {
            val (w, h) = dims
            // Reject tiny emoji / reaction / icon dimensions (e.g. 72x72, 120x120)
            if (w in 1..150 && h in 1..150) {
                return false
            }
        }
        return true
    }

    fun isValidImageFile(file: File?): Boolean {
        if (file == null || !file.exists() || !file.isFile || file.length() < 64L) return false
        return try {
            val headerBytes = file.inputStream().use { input ->
                val buf = ByteArray(160)
                val read = input.read(buf)
                if (read <= 0) ByteArray(0) else buf.copyOf(read)
            }
            if (headerBytes.size < 16) return false
            // Check magic header bytes
            val b0 = headerBytes[0].toInt() and 0xFF
            val b1 = headerBytes[1].toInt() and 0xFF
            val b2 = headerBytes[2].toInt() and 0xFF
            val b3 = headerBytes[3].toInt() and 0xFF
            if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return true
            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return true
            if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) return true
            if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return true
            if (b0 == 0x42 && b1 == 0x4D) return true
            if (headerBytes.size >= 12) {
                val b4 = headerBytes[4].toInt() and 0xFF
                val b5 = headerBytes[5].toInt() and 0xFF
                val b6 = headerBytes[6].toInt() and 0xFF
                val b7 = headerBytes[7].toInt() and 0xFF
                if (b4 == 0x66 && b5 == 0x74 && b6 == 0x79 && b7 == 0x70) return true
            }
            val headAscii = String(headerBytes, Charsets.UTF_8).trimStart()
            if (headAscii.startsWith("<!DOCTYPE", ignoreCase = true) ||
                headAscii.startsWith("<html", ignoreCase = true)
            ) {
                return false
            }
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            opts.outWidth > 0 && opts.outHeight > 0
        } catch (_: Throwable) {
            file.length() >= 64L
        }
    }

    /**
     * Strictly verifies that all required pages of a chapter exist on disk and are valid image files.
     * If even a single page file is missing, 0 bytes, or an HTML error file, returns false so the
     * chapter is never marked COMPLETED erroneously.
     */
    fun verifyChapterFilesComplete(expectedPageCount: Int, localPaths: List<String>): Boolean {
        if (expectedPageCount <= 0) return false
        if (localPaths.size < expectedPageCount) return false
        for (path in localPaths) {
            if (path.isBlank()) return false
            val file = File(path)
            if (!isValidImageFile(file)) {
                return false
            }
        }
        return true
    }

    fun verifyChapterFilesComplete(
        workId: String,
        chapterKey: String,
        expectedPages: Int,
        pagePaths: List<String>
    ): Boolean = verifyChapterFilesComplete(expectedPages, pagePaths)

    fun encodeLocalPathsJson(paths: List<String>): String {
        val arr = JSONArray()
        paths.forEach { arr.put(it) }
        return arr.toString()
    }

    fun decodeLocalPathsJson(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        if (!json.trimStart().startsWith("[")) {
            return json.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        }
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val path = arr.optString(i)
                if (path.isNotBlank()) list.add(path)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns only the paths from `localPaths` that currently exist and are valid image files on disk.
     */
    fun filterExistingValidPagePaths(localPaths: List<String>): List<String> {
        return localPaths.filter { path ->
            path.isNotBlank() && isValidImageFile(File(path))
        }
    }

    /**
     * Deletes a single chapter's local image files without affecting any other chapter of the same work.
     */
    fun deleteChapterFiles(
        sourceId: Long,
        workId: String,
        chapterKey: String,
        localPaths: List<String> = emptyList()
    ): Boolean {
        return try {
            localPaths.forEach { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            val chapterDir = getChapterDir(sourceId, workId, chapterKey)
            if (chapterDir.exists()) {
                chapterDir.deleteRecursively()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deleteChapterFiles(workId: String, chapterKey: String): Boolean =
        deleteChapterFiles(0L, workId, chapterKey, emptyList())

    /**
     * Deletes all local files (cover + all chapters) for a work when removed from Storage.
     */
    fun deleteWorkFiles(sourceId: Long, workId: String): Boolean {
        return try {
            val workDir = getWorkDir(sourceId, workId)
            if (workDir.exists()) {
                workDir.deleteRecursively()
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun deleteWorkFiles(workId: String): Boolean = deleteWorkFiles(0L, workId)

    fun getWorkStorageBytes(sourceId: Long, workId: String): Long {
        val workDir = getWorkDir(sourceId, workId)
        return calculateFolderBytes(workDir)
    }

    fun getTotalOfflineStorageBytes(): Long {
        return calculateFolderBytes(getOfflineRootDir())
    }

    fun calculateDownloadedMangaBytes(): Long = getTotalOfflineStorageBytes()

    fun getFolderSizeBytes(dir: File?): Long = calculateFolderBytes(dir)

    private fun calculateFolderBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { f ->
            total += if (f.isDirectory) calculateFolderBytes(f) else f.length()
        }
        return total
    }

    /**
     * Clears WebView browsing cache & cookies without deleting any downloaded manga chapters.
     */
    fun clearWebViewBrowsingDataOnly(): Boolean {
        return try {
            context.cacheDir?.listFiles()?.forEach { file ->
                if (!file.name.contains("manga_offline")) {
                    file.deleteRecursively()
                }
            }
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            true
        } catch (_: Exception) {
            true
        }
    }

    companion object {
        fun sanitizeSegment(raw: String): String {
            return raw.replace(Regex("[^a-zA-Z0-9_\\-\u0600-\u06FF]"), "_")
                .take(80)
                .ifBlank { "item" }
        }

        fun buildStableChapterKey(sourceId: Long, workId: String, chapterNumber: Int, chapterId: String): String {
            val safeWork = sanitizeSegment(workId)
            val safeChId = sanitizeSegment(chapterId.ifBlank { "ch_$chapterNumber" })
            return "src_${sourceId}_${safeWork}_${chapterNumber}_$safeChId"
        }
    }
}
