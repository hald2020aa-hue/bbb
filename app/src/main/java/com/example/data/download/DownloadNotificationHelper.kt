package com.example.data.download

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

/**
 * مساعد إشعارات النظام الحقيقية لمتابعة تنزيل الفصول في شريط إشعارات Android.
 * يعرض اسم المانجا، رقم الفصل، حالة التنزيل، عدد الصفحات المكتملة من الإجمالي، وشريط التقدم الفعلي،
 * وعند الاكتمال يتيح فتح الفصل مباشرة عند الضغط على الإشعار.
 */
class DownloadNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "manga_chapter_downloads"
        const val CHANNEL_NAME = "تنزيلات فصول المانجا"
        const val CHANNEL_DESC = "إشعارات متابعة تقدم تنزيل فصول المانجا والمانهوا وحالة اكتمالها"
        const val EXTRA_OPEN_WORK_ID = "open_work_id"
        const val EXTRA_OPEN_CHAPTER_NUMBER = "open_chapter_number"
    }

    init {
        ensureChannelCreated()
    }

    fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = CHANNEL_DESC
                        setShowBadge(false)
                    }
                    manager.createNotificationChannel(channel)
                }
            } catch (_: Throwable) {
                // Safe guard for unit test environments
            }
        }
    }

    private fun canPostNotifications(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun notificationIdForTask(taskId: String): Int {
        return (taskId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)
    }

    private fun buildOpenChapterPendingIntent(
        taskId: String,
        workId: String,
        chapterNumber: Int
    ): PendingIntent? {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_WORK_ID, workId)
                putExtra(EXTRA_OPEN_CHAPTER_NUMBER, chapterNumber)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            PendingIntent.getActivity(context, notificationIdForTask(taskId), intent, flags)
        } catch (_: Throwable) {
            null
        }
    }

    fun showDownloadStarting(
        taskId: String,
        workId: String,
        workTitle: String,
        chapterNumber: Int
    ) {
        if (!canPostNotifications()) return
        try {
            ensureChannelCreated()
            val pendingIntent = buildOpenChapterPendingIntent(taskId, workId, chapterNumber)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("تنزيل $workTitle")
                .setContentText("جارٍ التحضير لتنزيل الفصل $chapterNumber...")
                .setSubText("بدء التنزيل")
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .apply {
                    if (pendingIntent != null) setContentIntent(pendingIntent)
                }
                .build()

            NotificationManagerCompat.from(context).notify(notificationIdForTask(taskId), notification)
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    fun showDownloadProgress(
        taskId: String,
        workId: String,
        workTitle: String,
        chapterNumber: Int,
        downloadedPages: Int,
        totalPages: Int
    ) {
        if (!canPostNotifications()) return
        try {
            ensureChannelCreated()
            val safeTotal = totalPages.coerceAtLeast(1)
            val safeDone = downloadedPages.coerceIn(0, safeTotal)
            val percent = ((safeDone * 100f) / safeTotal).toInt().coerceIn(0, 100)
            val pendingIntent = buildOpenChapterPendingIntent(taskId, workId, chapterNumber)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("تنزيل $workTitle")
                .setContentText("جارٍ تنزيل الفصل $chapterNumber — $safeDone من $safeTotal صفحة")
                .setSubText("$percent%")
                .setProgress(safeTotal, safeDone, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .apply {
                    if (pendingIntent != null) setContentIntent(pendingIntent)
                }
                .build()

            NotificationManagerCompat.from(context).notify(notificationIdForTask(taskId), notification)
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    fun showDownloadCompleted(
        taskId: String,
        workId: String,
        workTitle: String,
        chapterNumber: Int,
        totalPages: Int
    ) {
        if (!canPostNotifications()) return
        try {
            ensureChannelCreated()
            val pendingIntent = buildOpenChapterPendingIntent(taskId, workId, chapterNumber)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("اكتمل التنزيل — $workTitle")
                .setContentText("الفصل $chapterNumber جاهز للقراءة بدون إنترنت ($totalPages صفحة)")
                .setSubText("اكتمل التنزيل")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .apply {
                    if (pendingIntent != null) setContentIntent(pendingIntent)
                }
                .build()

            NotificationManagerCompat.from(context).notify(notificationIdForTask(taskId), notification)
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    fun showDownloadFailed(
        taskId: String,
        workId: String,
        workTitle: String,
        chapterNumber: Int,
        reason: String,
        isIncomplete: Boolean = false
    ) {
        if (!canPostNotifications()) return
        try {
            ensureChannelCreated()
            val pendingIntent = buildOpenChapterPendingIntent(taskId, workId, chapterNumber)
            val statusTitle = if (isIncomplete) {
                "تنزيل غير مكتمل — $workTitle (الفصل $chapterNumber)"
            } else {
                "فشل تنزيل الفصل $chapterNumber — $workTitle"
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(statusTitle)
                .setContentText(reason)
                .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .apply {
                    if (pendingIntent != null) setContentIntent(pendingIntent)
                }
                .build()

            NotificationManagerCompat.from(context).notify(notificationIdForTask(taskId), notification)
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    fun showNewChapterUpdateNotification(
        workTitle: String,
        chapterNumber: Int,
        siteName: String
    ) {
        if (!canPostNotifications()) return
        try {
            ensureChannelCreated()
            val taskId = "update_${workTitle}_$chapterNumber"
            val pendingIntent = buildOpenChapterPendingIntent(taskId, workTitle, chapterNumber)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("فصل جديد متاح — $workTitle")
                .setContentText("صدر الفصل $chapterNumber على $siteName")
                .setSubText("تحديثات المكتبة")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .apply {
                    if (pendingIntent != null) setContentIntent(pendingIntent)
                }
                .build()

            NotificationManagerCompat.from(context).notify(notificationIdForTask(taskId), notification)
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    fun cancelNotification(taskId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationIdForTask(taskId))
        } catch (_: Throwable) {
        }
    }
}
