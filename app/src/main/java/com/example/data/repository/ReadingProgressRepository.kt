package com.example.data.repository

import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.local.AppDao
import com.example.data.local.ReadingProgressEntity
import com.example.data.storage.LocalStorageManager
import kotlinx.coroutines.flow.Flow

/**
 * Dedicated repository for persisting and restoring reading progress per work and per chapter
 * in Room, enabling exact page/scroll restoration even after closing the app or offline.
 */
class ReadingProgressRepository(private val dao: AppDao) {

    val allReadingProgress: Flow<List<ReadingProgressEntity>> = dao.getAllReadingProgress()

    fun getProgressForWorkFlow(workId: String): Flow<List<ReadingProgressEntity>> {
        return dao.getReadingProgressForWorkFlow(workId)
    }

    suspend fun getProgressForWorkOnce(workId: String): List<ReadingProgressEntity> {
        return dao.getReadingProgressForWorkOnce(workId)
    }

    suspend fun getChapterProgress(chapterKey: String): ReadingProgressEntity? {
        return dao.getReadingProgressByKey(chapterKey)
    }

    suspend fun getChapterProgress(workId: String, chapterNumber: Int): ReadingProgressEntity? {
        val list = dao.getReadingProgressForWorkOnce(workId)
        return list.find { it.chapterNumber == chapterNumber }
    }

    suspend fun getLatestWorkProgress(workId: String): ReadingProgressEntity? {
        return dao.getReadingProgressForWorkOnce(workId).maxByOrNull { it.lastReadAt }
    }

    suspend fun getLatestProgressOnce(): ReadingProgressEntity? {
        return dao.getLatestReadingProgressOnce()
    }

    suspend fun saveProgress(
        workId: String,
        workTitle: String = "",
        chapterId: String = "",
        chapterNumber: Int,
        chapterTitle: String,
        pageIndex: Int = 0,
        lastPageIndex: Int = pageIndex,
        totalPages: Int = 1,
        scrollOffset: Int = 0,
        sourceId: Long = 0L,
        chapterKey: String = ""
    ) {
        val effectiveKey = chapterKey.ifBlank { "${workId}_ch_${chapterNumber}" }
        val safeTotal = totalPages.coerceAtLeast(1)
        val safePage = lastPageIndex.coerceIn(0, (safeTotal - 1).coerceAtLeast(0))
        val isCompleted = safePage >= safeTotal - 1

        dao.upsertReadingProgress(
            ReadingProgressEntity(
                chapterKey = effectiveKey,
                workId = workId,
                workTitle = workTitle,
                sourceId = sourceId,
                chapterId = chapterId.ifBlank { "ch_$chapterNumber" },
                chapterNumber = chapterNumber,
                chapterTitle = chapterTitle,
                lastPageIndex = safePage,
                scrollOffset = scrollOffset.coerceAtLeast(0),
                totalPages = safeTotal,
                isCompleted = isCompleted,
                lastReadAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveChapterProgress(
        work: MangaWork,
        chapter: MangaChapter,
        pageIndex: Int,
        scrollOffset: Int = 0,
        totalPages: Int = 1
    ) {
        val chapterKey = chapter.chapterKey.ifBlank {
            LocalStorageManager.buildStableChapterKey(
                sourceId = work.sourceId,
                workId = work.id,
                chapterNumber = chapter.number,
                chapterId = chapter.id
            )
        }
        saveProgress(
            workId = work.id,
            workTitle = work.title,
            chapterId = chapter.id,
            chapterNumber = chapter.number,
            chapterTitle = chapter.title,
            lastPageIndex = pageIndex,
            totalPages = totalPages,
            scrollOffset = scrollOffset,
            sourceId = work.sourceId,
            chapterKey = chapterKey
        )
    }

    fun formatResumeText(progress: ReadingProgressEntity): String {
        val rel = formatRelativeTimeArabic(progress.lastReadAt)
        return "$rel • الصفحة ${progress.lastPageIndex + 1}"
    }

    fun formatRelativeProgressText(progress: ReadingProgressEntity): String =
        formatResumeText(progress)

    companion object {
        fun formatRelativeTimeArabic(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
            if (timestampMs <= 0L) return "الآن"
            val diffSec = ((nowMs - timestampMs) / 1000L).coerceAtLeast(1L)
            return when {
                diffSec < 60L -> "قبل $diffSec ثوان"
                diffSec < 120L -> "قبل دقيقة واحدة"
                diffSec < 3600L -> "قبل ${diffSec / 60L} دقائق"
                diffSec < 7200L -> "قبل ساعة واحدة"
                diffSec < 86400L -> "قبل ${diffSec / 3600L} ساعات"
                else -> "قبل ${diffSec / 86400L} يوم"
            }
        }
    }
}
