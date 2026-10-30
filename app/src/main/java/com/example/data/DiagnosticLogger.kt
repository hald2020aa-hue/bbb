package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosticCategory(val labelAr: String) {
    READER("القارئ والصفحات"),
    SCRAPER("جلب وتحليل المصدر"),
    DOWNLOAD("التنزيل الخلفي"),
    STORAGE("التخزين المحلي"),
    COVER("الأغلفة والصور"),
    NETWORK("الشبكة والترويسات")
}

enum class DiagnosticLevel(val labelAr: String) {
    INFO("معلومات"),
    WARN("تحذير"),
    ERROR("خطأ")
}

data class DiagnosticLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = formatDiagnosticTimestamp(timestamp),
    val level: DiagnosticLevel,
    val category: DiagnosticCategory,
    val sourceId: String = "",
    val sourceName: String = "",
    val workId: String = "",
    val chapterId: String = "",
    val pageNumber: Int? = null,
    val url: String = "",
    val httpCode: Int? = null,
    val message: String,
    val details: String = ""
)

private fun formatDiagnosticTimestamp(ts: Long): String {
    return try {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(ts))
    } catch (_: Exception) {
        ts.toString()
    }
}

/**
 * مسجل تشخيصي للمطور والمستخدم لتتبع تحميل صفحات الفصول، حالة HTTP، التنزيل، وحفظ الملفات
 * مع تنقية الروابط من أي رموز جلسات أو بيانات حساسة.
 */
object DiagnosticLogger {
    private const val TAG = "MangaDiagnostics"
    private const val MAX_ENTRIES = 250

    private val _logs = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val logs: StateFlow<List<DiagnosticLogEntry>> = _logs.asStateFlow()

    private val sensitiveParamRegex = Regex(
        """(?i)([?&](?:token|access_token|auth|session|sid|password|pwd|key|secret|signature|sig)=)[^&#\s]+"""
    )

    fun sanitizeUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return ""
        return rawUrl.replace(sensitiveParamRegex, "$1***").take(260)
    }

    fun logInfo(
        category: DiagnosticCategory,
        message: String,
        sourceId: String = "",
        sourceName: String = "",
        workId: String = "",
        chapterId: String = "",
        pageNumber: Int? = null,
        url: String = "",
        httpCode: Int? = null,
        details: String = ""
    ) {
        record(
            level = DiagnosticLevel.INFO,
            category = category,
            sourceId = sourceId,
            sourceName = sourceName,
            workId = workId,
            chapterId = chapterId,
            pageNumber = pageNumber,
            url = url,
            httpCode = httpCode,
            message = message,
            details = details
        )
    }

    fun logWarn(
        category: DiagnosticCategory,
        message: String,
        sourceId: String = "",
        sourceName: String = "",
        workId: String = "",
        chapterId: String = "",
        pageNumber: Int? = null,
        url: String = "",
        httpCode: Int? = null,
        details: String = ""
    ) {
        record(
            level = DiagnosticLevel.WARN,
            category = category,
            sourceId = sourceId,
            sourceName = sourceName,
            workId = workId,
            chapterId = chapterId,
            pageNumber = pageNumber,
            url = url,
            httpCode = httpCode,
            message = message,
            details = details
        )
    }

    fun logError(
        category: DiagnosticCategory,
        message: String,
        sourceId: String = "",
        sourceName: String = "",
        workId: String = "",
        chapterId: String = "",
        pageNumber: Int? = null,
        url: String = "",
        httpCode: Int? = null,
        details: String = ""
    ) {
        record(
            level = DiagnosticLevel.ERROR,
            category = category,
            sourceId = sourceId,
            sourceName = sourceName,
            workId = workId,
            chapterId = chapterId,
            pageNumber = pageNumber,
            url = url,
            httpCode = httpCode,
            message = message,
            details = details
        )
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun record(
        level: DiagnosticLevel,
        category: DiagnosticCategory,
        sourceId: String,
        sourceName: String,
        workId: String,
        chapterId: String,
        pageNumber: Int?,
        url: String,
        httpCode: Int?,
        message: String,
        details: String
    ) {
        val cleanUrl = sanitizeUrl(url)
        val entry = DiagnosticLogEntry(
            level = level,
            category = category,
            sourceId = sourceId,
            sourceName = sourceName,
            workId = workId,
            chapterId = chapterId,
            pageNumber = pageNumber,
            url = cleanUrl,
            httpCode = httpCode,
            message = message,
            details = details.take(400)
        )
        synchronized(this) {
            val updated = ArrayList<DiagnosticLogEntry>(_logs.value.size + 1)
            updated.add(entry)
            updated.addAll(_logs.value.take(MAX_ENTRIES - 1))
            _logs.value = updated
        }
        val logLine = buildString {
            append("[${category.name}] ")
            if (sourceId.isNotBlank()) append("src=$sourceId ")
            if (workId.isNotBlank()) append("work=$workId ")
            if (chapterId.isNotBlank()) append("ch=$chapterId ")
            if (pageNumber != null) append("page=$pageNumber ")
            if (httpCode != null) append("http=$httpCode ")
            append(message)
            if (cleanUrl.isNotBlank()) append(" | url=$cleanUrl")
            if (details.isNotBlank()) append(" | $details")
        }
        try {
            when (level) {
                DiagnosticLevel.INFO -> Log.d(TAG, logLine)
                DiagnosticLevel.WARN -> Log.w(TAG, logLine)
                DiagnosticLevel.ERROR -> Log.e(TAG, logLine)
            }
        } catch (_: Throwable) {
            // Safe fallback in local JVM unit tests where android.util.Log might not be mocked
        }
    }
}
