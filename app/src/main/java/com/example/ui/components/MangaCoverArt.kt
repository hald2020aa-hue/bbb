package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.DiagnosticCategory
import com.example.data.DiagnosticLogger
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaWork
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Shared Coil ImageLoader with bounded concurrency and automatic User-Agent, Referer, and Cookie headers
 * so loading 20-60 chapter images in a LazyColumn never overwhelms the host or fails due to missing cookies.
 */
object MangaCoilImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: buildImageLoader(context.applicationContext).also { instance = it }
        }
    }

    private fun buildImageLoader(appContext: Context): ImageLoader {
        val dispatcher = Dispatcher().apply {
            maxRequests = 24
            maxRequestsPerHost = 10
        }
        val okHttp = LiveMangaSiteScraper.httpClient.newBuilder()
            .dispatcher(dispatcher)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val reqUrl = original.url.toString()
                val referer = original.header("Referer").orEmpty()
                val cleanUa = LiveMangaSiteScraper.activeUserAgent
                    .replace("; wv", "")
                    .ifBlank { LiveMangaSiteScraper.DEFAULT_USER_AGENT }
                val builder = original.newBuilder()
                if (original.header("User-Agent").isNullOrBlank()) {
                    builder.header("User-Agent", cleanUa)
                }
                if (original.header("Accept").isNullOrBlank()) {
                    builder.header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                }
                if (original.header("Cookie").isNullOrBlank()) {
                    val cookies = LiveMangaSiteScraper.getCombinedCookies(
                        targetUrl = reqUrl,
                        refererUrl = referer
                    )
                    if (cookies.isNotBlank()) {
                        builder.header("Cookie", cookies)
                    }
                }
                var response = chain.proceed(builder.build())
                // If CDN rejects hotlink Referer with 403/400, retry immediately without Referer
                if (!response.isSuccessful && (response.code == 403 || response.code == 400) && referer.isNotBlank()) {
                    response.close()
                    val retryReq = builder.removeHeader("Referer").build()
                    response = chain.proceed(retryReq)
                }
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (!response.isSuccessful) {
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.READER,
                        message = "فشل جلب الصورة عبر الشبكة (HTTP ${response.code})",
                        url = reqUrl,
                        httpCode = response.code
                    )
                } else if (contentType.contains("text/html")) {
                    // Extract the actual image URL from <img> tags (src / data-src) instead of loading full HTML
                    val html = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
                    response.close()
                    val unwrappedImgUrl = LiveMangaSiteScraper.extractImgTagSrcUrls(html, reqUrl)
                        .firstOrNull { it.startsWith("http") && !it.equals(reqUrl, ignoreCase = true) }
                    if (!unwrappedImgUrl.isNullOrBlank()) {
                        val imgReqBuilder = original.newBuilder()
                            .url(unwrappedImgUrl)
                            .header("User-Agent", cleanUa)
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .header("Referer", reqUrl)
                        val imgCookies = LiveMangaSiteScraper.getCombinedCookies(
                            targetUrl = unwrappedImgUrl,
                            refererUrl = reqUrl
                        )
                        if (imgCookies.isNotBlank()) {
                            imgReqBuilder.header("Cookie", imgCookies)
                        }
                        response = chain.proceed(imgReqBuilder.build())
                    } else {
                        DiagnosticLogger.logError(
                            category = DiagnosticCategory.READER,
                            message = "استجابة غير صالحة للصورة (نوع المحتوى: $contentType)",
                            url = reqUrl,
                            httpCode = response.code
                        )
                        throw java.io.IOException("استجابة الخادم ليست صورة صالحة ($contentType)")
                    }
                } else if (contentType.contains("application/json")) {
                    response.close()
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.READER,
                        message = "استجابة غير صالحة للصورة (نوع المحتوى: $contentType)",
                        url = reqUrl,
                        httpCode = response.code
                    )
                    throw java.io.IOException("استجابة الخادم ليست صورة صالحة ($contentType)")
                }
                response
            }
            .build()

        return ImageLoader.Builder(appContext)
            .okHttpClient(okHttp)
            .memoryCache {
                coil.memory.MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(java.io.File(appContext.cacheDir, "manga_pages_disk_cache"))
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }
}

@Composable
fun MangaPlaceholderCoverBox(
    title: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222630))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "غلاف المانجا",
                tint = Color(0xFF5D657E),
                modifier = Modifier.size(34.dp)
            )
            if (title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    color = Color(0xFF9EA6BE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MangaCoverArtwork(
    work: MangaWork,
    modifier: Modifier = Modifier,
    showArabicTitleBanner: Boolean = false
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { MangaCoilImageLoader.get(context) }

    val resolvedLocalCover = remember(work.localCoverPath, work.sourceId, work.id) {
        val direct = work.localCoverPath.trim()
        if (direct.isNotBlank() && File(direct).exists() && File(direct).length() >= 64L) {
            direct
        } else {
            // Check standard local cover location on disk as fallback
            val extDir = try { context.getExternalFilesDir("saved") } catch (_: Throwable) { null }
            val baseDir = extDir ?: File(context.filesDir, "saved")
            val safeWork = work.id.replace(Regex("[^a-zA-Z0-9_\\-\u0600-\u06FF]"), "_").take(80).ifBlank { "item" }
            val candidate = File(baseDir, "manga_offline/src_${work.sourceId}/$safeWork/cover.jpg")
            if (candidate.exists() && candidate.length() >= 64L) candidate.absolutePath else ""
        }
    }

    val hasLocalFile = resolvedLocalCover.isNotBlank()
    val coverUrl = work.effectiveCoverUrl?.trim().orEmpty()

    if (!hasLocalFile && coverUrl.isBlank()) {
        MangaPlaceholderCoverBox(title = work.title, modifier = modifier)
        return
    }

    val imageRequest = remember(hasLocalFile, resolvedLocalCover, coverUrl, work.siteUrl, work.mangaUrl) {
        if (hasLocalFile) {
            ImageRequest.Builder(context)
                .data(File(resolvedLocalCover))
                .crossfade(true)
                .build()
        } else {
            val effectiveReferer = work.mangaUrl.ifBlank { work.siteUrl }
            val builder = ImageRequest.Builder(context)
                .data(coverUrl)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .addHeader("User-Agent", LiveMangaSiteScraper.activeUserAgent)

            if (effectiveReferer.isNotBlank()) {
                builder.addHeader("Referer", effectiveReferer)
            }
            val cookie = LiveMangaSiteScraper.getCombinedCookies(
                targetUrl = coverUrl,
                refererUrl = effectiveReferer,
                siteUrl = work.siteUrl
            )
            if (cookie.isNotBlank()) {
                builder.addHeader("Cookie", cookie)
            }
            builder.build()
        }
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        imageLoader = imageLoader,
        contentDescription = work.title,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        loading = {
            MangaPlaceholderCoverBox(title = work.title, modifier = Modifier.fillMaxSize())
        },
        error = {
            MangaPlaceholderCoverBox(title = work.title, modifier = Modifier.fillMaxSize())
        }
    )
}

@Composable
fun ChapterPageIllustration(
    work: MangaWork,
    chapterNumber: Int,
    pageIndex: Int,
    totalPages: Int,
    pageImageUrl: String? = null,
    localFilePath: String? = null,
    refererUrl: String? = null,
    autoCropBorders: Boolean = false,
    colorFilterMode: String = "طبيعي (افتراضي)",
    splitLongStrip: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { MangaCoilImageLoader.get(context) }
    var retryNonce by remember(pageImageUrl, localFilePath) { mutableIntStateOf(0) }
    var lastErrorReason by remember(pageImageUrl, localFilePath) { mutableStateOf("") }

    val activeColorFilter: ColorFilter? = remember(colorFilterMode) {
        when (colorFilterMode) {
            "دافئ لحماية العين (Sepia)" -> {
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 12f,
                            0.349f, 0.686f, 0.168f, 0f, 8f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            "عكس الألوان الذكي (ليلي)" -> {
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            -0.299f, -0.587f, -0.114f, 0f, 248f,
                            -0.299f, -0.587f, -0.114f, 0f, 248f,
                            -0.299f, -0.587f, -0.114f, 0f, 252f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            "تدرج رمادي مريح (Grayscale)" -> {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(0f) }
                )
            }
            else -> null
        }
    }

    val cleanLocalPath = remember(localFilePath, pageImageUrl) {
        when {
            !localFilePath.isNullOrBlank() -> localFilePath.trim()
            pageImageUrl?.startsWith("file://") == true -> pageImageUrl.removePrefix("file://").trim()
            pageImageUrl?.startsWith("/") == true -> pageImageUrl.trim()
            else -> ""
        }
    }
    val hasValidLocalFile = remember(cleanLocalPath, retryNonce) {
        cleanLocalPath.isNotBlank() && File(cleanLocalPath).exists() && File(cleanLocalPath).length() >= 64L
    }
    val cleanUrl = remember(pageImageUrl) {
        val raw = pageImageUrl?.trim().orEmpty()
        if (raw.startsWith("http://") || raw.startsWith("https://")) raw else ""
    }
    val urlRejectionReason = remember(cleanUrl) {
        if (cleanUrl.isNotBlank()) {
            LiveMangaSiteScraper.getCandidateRejectionReason(cleanUrl, isInsideReaderContainer = true)
        } else null
    }

    if (!hasValidLocalFile && (cleanUrl.isBlank() || urlRejectionReason != null)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF0E1118))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "صفحة غير متوفرة",
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = if (urlRejectionReason != null) {
                        "تم استبعاد عنصر غير صالح من الصفحة ${pageIndex + 1}"
                    } else {
                        "الصفحة ${pageIndex + 1} من $totalPages غير محفوظة محليًا"
                    },
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = urlRejectionReason ?: "يرجى الاتصال بالإنترنت واستكمال تنزيل الفصل لعرض هذه الصفحة",
                    color = TextSecondaryGrey,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val imageRequest = remember(hasValidLocalFile, cleanLocalPath, cleanUrl, refererUrl, retryNonce, autoCropBorders) {
        val cropTransformations = if (autoCropBorders) {
            listOf(
                object : coil.transform.Transformation {
                    override val cacheKey: String = "auto_crop_borders_v1"
                    override suspend fun transform(input: Bitmap, size: coil.size.Size): Bitmap {
                        return com.example.data.storage.LocalStorageManager.cropUniformBorders(input)
                    }
                }
            )
        } else {
            emptyList()
        }
        if (hasValidLocalFile) {
            // Read directly from local disk without any network request (100% offline capable in Airplane mode)
            ImageRequest.Builder(context)
                .data(File(cleanLocalPath))
                .crossfade(false)
                .transformations(cropTransformations)
                .setParameter("retry_nonce", retryNonce)
                .listener(
                    onError = { _, result ->
                        lastErrorReason = result.throwable.localizedMessage ?: "ملف الصورة المحلي تالف"
                        DiagnosticLogger.logError(
                            category = DiagnosticCategory.STORAGE,
                            message = "فشل فك ترميز ملف الصفحة المحلية ${pageIndex + 1}: $lastErrorReason",
                            workId = work.id,
                            chapterId = "ch_$chapterNumber",
                            pageNumber = pageIndex + 1,
                            url = cleanLocalPath
                        )
                    }
                )
                .build()
        } else {
            val effectiveReferer = refererUrl?.ifBlank { work.mangaUrl.ifBlank { work.siteUrl } }
                ?: work.mangaUrl.ifBlank { work.siteUrl }
            val builder = ImageRequest.Builder(context)
                .data(cleanUrl)
                .crossfade(false)
                .transformations(cropTransformations)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(
                    if (retryNonce > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED
                )
                .addHeader("User-Agent", LiveMangaSiteScraper.activeUserAgent)
                .setParameter("retry_nonce", retryNonce)
                .listener(
                    onError = { _, result ->
                        val msg = result.throwable.localizedMessage ?: "خطأ في الاتصال أو الصورة غير مدعومة"
                        lastErrorReason = msg
                        DiagnosticLogger.logError(
                            category = DiagnosticCategory.READER,
                            message = "فشل عرض الصفحة ${pageIndex + 1}: $msg",
                            sourceId = work.sourceId.toString(),
                            sourceName = work.siteName,
                            workId = work.id,
                            chapterId = "ch_$chapterNumber",
                            pageNumber = pageIndex + 1,
                            url = cleanUrl
                        )
                    }
                )

            if (effectiveReferer.isNotBlank()) {
                builder.addHeader("Referer", effectiveReferer)
            }
            val cookie = LiveMangaSiteScraper.getCombinedCookies(
                targetUrl = cleanUrl,
                refererUrl = effectiveReferer,
                siteUrl = work.siteUrl
            )
            if (cookie.isNotBlank()) {
                builder.addHeader("Cookie", cookie)
            }
            builder.build()
        }
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        imageLoader = imageLoader,
        contentDescription = "صفحة ${pageIndex + 1} من $totalPages",
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .testTag("reader_page_image_${pageIndex + 1}"),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(Color(0xFF0B0D13)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = PeriwinklePrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "جاري تحميل الصفحة ${pageIndex + 1} من $totalPages",
                        color = TextSecondaryGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Black)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "خطأ في التحميل",
                        color = TextSecondaryGrey,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { retryNonce++ },
                        modifier = Modifier.testTag("retry_page_button_${pageIndex + 1}")
                    ) {
                        Text(
                            text = "حاول مجددا",
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        )
                    }
                }
            }
        },
        success = { state ->
            val intrinsicSize = state.painter.intrinsicSize
            val isTinyEmojiOrIcon = intrinsicSize.width > 0f &&
                intrinsicSize.height > 0f &&
                intrinsicSize.width <= 150f &&
                intrinsicSize.height <= 150f
            if (isTinyEmojiOrIcon) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFF131620))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "صورة مستبعدة",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "تم رفض صورة غير صالحة كصفحة فصل (${intrinsicSize.width.toInt()}×${intrinsicSize.height.toInt()})",
                            color = TextPrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "الصورة المستلمة ذات أبعاد صغيرة جدًا (إيموجي أو أيقونة واجهة) وليست صفحة مانجا أصلية.",
                            color = TextSecondaryGrey,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val drawable = state.result.drawable
                val rawBitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                val slices = remember(rawBitmap, splitLongStrip) {
                    if (splitLongStrip && rawBitmap != null && rawBitmap.height > 2600 && rawBitmap.height > rawBitmap.width * 2.6f) {
                        com.example.data.storage.LocalStorageManager.splitTallBitmapIfNeeded(rawBitmap, 2400)
                    } else {
                        null
                    }
                }
                val cropModifier = if (autoCropBorders) {
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            scaleX = 1.07f,
                            scaleY = 1.03f,
                            clip = true
                        )
                } else {
                    Modifier.fillMaxWidth()
                }
                if (slices != null && slices.size > 1) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        slices.forEachIndexed { sliceIdx, sliceBmp ->
                            androidx.compose.foundation.Image(
                                bitmap = remember(sliceBmp) { sliceBmp.asImageBitmap() },
                                contentDescription = "صفحة ${pageIndex + 1} - جزء ${sliceIdx + 1}",
                                contentScale = if (autoCropBorders) ContentScale.Crop else ContentScale.FillWidth,
                                colorFilter = activeColorFilter,
                                modifier = cropModifier
                            )
                        }
                    }
                } else {
                    SubcomposeAsyncImageContent(
                        modifier = cropModifier,
                        contentScale = if (autoCropBorders) ContentScale.Crop else ContentScale.FillWidth,
                        colorFilter = activeColorFilter
                    )
                }
            }
        }
    )
}

/**
 * Dynamic Cover Palette extracted/derived for each Manga/Manhwa work to give every title
 * a distinct, luxury visual atmosphere in MangaDetailsScreen.
 */
data class DynamicCoverPalette(
    val dominantBackdropTop: Color,
    val dominantBackdropMid: Color,
    val dominantBackdropBottom: Color,
    val accentPrimary: Color,
    val accentOnPrimary: Color,
    val badgeBorder: Color,
    val badgeSurface: Color
)

@Composable
fun rememberDynamicCoverPalette(work: MangaWork): DynamicCoverPalette {
    val palettes = remember {
        listOf(
            // Royal Indigo & Periwinkle
            DynamicCoverPalette(
                dominantBackdropTop = Color(0xD9121933),
                dominantBackdropMid = Color(0xCC0E1324),
                dominantBackdropBottom = Color(0xF2090C14),
                accentPrimary = Color(0xFFA0AEFF),
                accentOnPrimary = Color(0xFF101735),
                badgeBorder = Color(0xFF3E4B75),
                badgeSurface = Color(0xD9151B2B)
            ),
            // Emerald Jade & Cyber Teal
            DynamicCoverPalette(
                dominantBackdropTop = Color(0xD90E2926),
                dominantBackdropMid = Color(0xCC0B1D1C),
                dominantBackdropBottom = Color(0xF2081112),
                accentPrimary = Color(0xFF64FFDA),
                accentOnPrimary = Color(0xFF062922),
                badgeBorder = Color(0xFF2B5B54),
                badgeSurface = Color(0xD9112221)
            ),
            // Crimson Ember & Gold
            DynamicCoverPalette(
                dominantBackdropTop = Color(0xD92E141C),
                dominantBackdropMid = Color(0xCC1F1016),
                dominantBackdropBottom = Color(0xF20D090C),
                accentPrimary = Color(0xFFFF8A80),
                accentOnPrimary = Color(0xFF310B09),
                badgeBorder = Color(0xFF63323D),
                badgeSurface = Color(0xD9221318)
            ),
            // Mystic Violet & Orchid
            DynamicCoverPalette(
                dominantBackdropTop = Color(0xD9241536),
                dominantBackdropMid = Color(0xCC181026),
                dominantBackdropBottom = Color(0xF20B0812),
                accentPrimary = Color(0xFFCE93D8),
                accentOnPrimary = Color(0xFF2A0E33),
                badgeBorder = Color(0xFF533769),
                badgeSurface = Color(0xD91C1326)
            ),
            // Imperial Gold & Bronze
            DynamicCoverPalette(
                dominantBackdropTop = Color(0xD92B2111),
                dominantBackdropMid = Color(0xCC1E170D),
                dominantBackdropBottom = Color(0xF20C0A07),
                accentPrimary = Color(0xFFFFD54F),
                accentOnPrimary = Color(0xFF2B2000),
                badgeBorder = Color(0xFF634F2B),
                badgeSurface = Color(0xD9211B11)
            ),
            // Ocean Azure & Cyan
            DynamicCoverPalette(
                dominantBackdropTop = Color(0xD90E2238),
                dominantBackdropMid = Color(0xCC0B1726),
                dominantBackdropBottom = Color(0xF2080D14),
                accentPrimary = Color(0xFF80D8FF),
                accentOnPrimary = Color(0xFF052436),
                badgeBorder = Color(0xFF2D5073),
                badgeSurface = Color(0xD9121E2B)
            )
        )
    }
    val keyString = "${work.id}_${work.title}_${work.coverImageUrl}_${work.genres.joinToString()}"
    val index = (keyString.hashCode() and Int.MAX_VALUE) % palettes.size
    return palettes[index]
}

/**
 * Smooth Animated Shimmer Brush for skeleton loading cards.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1150,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF171A24),
            Color(0xFF272C3D),
            Color(0xFF171A24)
        ),
        start = Offset(translateAnim - 280f, translateAnim - 280f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun ShimmerCatalogGrid(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("shimmer_catalog_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rows = (itemCount + 2) / 3
        for (rowIdx in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (colIdx in 0 until 3) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerUpdatesList(
    itemCount: Int = 4,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("shimmer_updates_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(itemCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141720))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(62.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.48f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

/**
 * Floating "Continue Reading Hero Dock" (شريط متابعة القراءة السريعة العائم)
 * Shows the last read work, chapter number, percentage progress bar, and a 1-tap "أكمل من الصفحة X" button.
 */
@Composable
fun ContinueReadingHeroDock(
    workTitle: String,
    chapterNumber: Int,
    pageNumber: Int,
    totalPages: Int,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveProgress = workTitle.isNotBlank() && chapterNumber > 0
    val safePage = pageNumber.coerceAtLeast(1)
    val safeTotal = totalPages.coerceAtLeast(safePage).coerceAtLeast(1)
    val progressFraction = if (hasActiveProgress) {
        (safePage.toFloat() / safeTotal.toFloat()).coerceIn(0.08f, 1f)
    } else {
        0f
    }
    val progressPercent = (progressFraction * 100f).toInt()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF21D2336),
        border = androidx.compose.foundation.BorderStroke(1.dp, PeriwinklePrimary.copy(alpha = 0.45f)),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .clickable { onContinueClick() }
            .testTag("continue_reading_button")
    ) {
        if (hasActiveProgress) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = PeriwinklePrimary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.width(155.dp)) {
                    Text(
                        text = workTitle,
                        color = TextPrimaryWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "الفصل $chapterNumber • $progressPercent%",
                        color = TextSecondaryGrey,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progressFraction },
                        color = PeriwinklePrimary,
                        trackColor = Color(0xFF32394F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PeriwinklePrimary)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "أكمل ص $safePage",
                        color = PeriwinkleOnPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = Color(0xFFD5DEFF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1E2B))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "متابعة",
                        color = TextPrimaryWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Hidden 1x1 WebView DOM Bridge that loads a live manga site URL with full JS & CookieManager enabled,
 * triggers scroll events for lazy-loaded images, executes the provided extraction script once the DOM settles,
 * and passes the JSON result back to the screen.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HiddenSiteWebViewBridge(
    targetUrl: String,
    jsExtractorScript: String,
    reloadTrigger: Int = 0,
    onResultJson: (String) -> Unit
) {
    if (targetUrl.isBlank() || !targetUrl.startsWith("http")) return

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(targetUrl, reloadTrigger) {
        onDispose {
            try {
                webViewRef?.stopLoading()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .size(1.dp)
            .alpha(0.01f)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(1, 1)
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.loadsImagesAutomatically = false
                    settings.blockNetworkImage = true
                    LiveMangaSiteScraper.activeUserAgent = (settings.userAgentString ?: LiveMangaSiteScraper.DEFAULT_USER_AGENT)
                        .replace("; wv", "")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()
                            // Trigger scroll events so lazy-load scripts populate data-src / src attributes
                            view?.evaluateJavascript(
                                "(function(){ try { window.scrollTo(0, 1200); window.scrollTo(0, document.body.scrollHeight); window.scrollTo(0, 0); } catch(e){} })();",
                                null
                            )
                            view?.postDelayed({
                                try {
                                    view.evaluateJavascript(jsExtractorScript) { rawJson ->
                                        if (!rawJson.isNullOrBlank() && rawJson != "null") {
                                            onResultJson(rawJson)
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            }, 450L)
                        }
                    }
                    webViewRef = this
                    loadUrl(targetUrl)
                }
            },
            update = { view ->
                if (view.url != targetUrl) {
                    view.loadUrl(targetUrl)
                }
            }
        )
    }
}

/**
 * Interactive Cloudflare Protection & Live Site Verification Modal ("الحماية").
 * Lets the user solve Cloudflare challenges or inspect the live site connection, automatically syncing cookies
 * and extracting the rendered DOM back into the native app UI.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareProtectionDialog(
    url: String,
    siteName: String,
    jsExtractorScript: String? = null,
    onExtractedDomJson: ((String) -> Unit)? = null,
    onDismissAndRefresh: () -> Unit
) {
    var isLoadingPage by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismissAndRefresh,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = AppDialogSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161922))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PeriwinklePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "حماية الموقع وتخطي Cloudflare",
                                color = TextPrimaryWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = siteName,
                                color = TextSecondaryGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = TextPrimaryWhite)
                        }
                        IconButton(onClick = onDismissAndRefresh) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextPrimaryWhite)
                        }
                    }
                }

                // Live WebView for solving Cloudflare / loading session cookies
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)

                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                LiveMangaSiteScraper.activeUserAgent =
                                    settings.userAgentString ?: LiveMangaSiteScraper.DEFAULT_USER_AGENT

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoadingPage = true
                                    }

                                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                        super.onPageFinished(view, finishedUrl)
                                        isLoadingPage = false
                                        CookieManager.getInstance().flush()
                                        if (jsExtractorScript != null && onExtractedDomJson != null) {
                                            view?.postDelayed({
                                                try {
                                                    view.evaluateJavascript(jsExtractorScript) { rawJson ->
                                                        if (!rawJson.isNullOrBlank() && rawJson != "null") {
                                                            onExtractedDomJson(rawJson)
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }, 700L)
                                        }
                                    }
                                }
                                webViewInstance = this
                                loadUrl(url)
                            }
                        }
                    )

                    if (isLoadingPage) {
                        CircularProgressIndicator(
                            color = PeriwinklePrimary,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .size(28.dp)
                        )
                    }
                }

                // Footer action button to return to the native app screen after verification
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161922))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            CookieManager.getInstance().flush()
                            val view = webViewInstance
                            if (view != null && jsExtractorScript != null && onExtractedDomJson != null) {
                                view.evaluateJavascript(jsExtractorScript) { rawJson ->
                                    if (!rawJson.isNullOrBlank() && rawJson != "null") {
                                        onExtractedDomJson(rawJson)
                                    }
                                    onDismissAndRefresh()
                                }
                            } else {
                                onDismissAndRefresh()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PeriwinklePrimary,
                            contentColor = PeriwinkleOnPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تم التحقق — عرض المانجا داخل التطبيق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
