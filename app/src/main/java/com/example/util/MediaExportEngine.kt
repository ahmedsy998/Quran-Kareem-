package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.remote.AudioModePayload
import com.example.data.remote.PreparedAudioPackage
import com.example.domain.model.BackgroundType
import com.example.domain.model.BundledBackgrounds
import com.example.domain.model.ExportResolution
import com.example.domain.model.FontOption
import com.example.domain.model.SingleLineOverflowMode
import com.example.domain.model.SyncedAyahLine
import com.example.domain.model.TextAlignmentMode
import com.example.domain.model.TextLayerStyle
import com.example.domain.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

class MediaExportEngine(private val context: Context) {

    private val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
    private val tempWorkDir = File(context.cacheDir, "export_work").apply { mkdirs() }

    data class ExportHandle(
        val cancel: () -> Unit
    )

    @OptIn(UnstableApi::class)
    suspend fun exportVideo(
        project: VideoProject,
        audioPackage: PreparedAudioPackage,
        resolution: ExportResolution,
        outputFileName: String,
        onProgress: (Int) -> Unit,
        onRegisterCancel: (ExportHandle) -> Unit
    ): File {
        val (outWidth, outHeight) = project.aspectRatio.outputDimensions(resolution)
        val safeWidth = outWidth - (outWidth % 2)
        val safeHeight = outHeight - (outHeight % 2)
        val totalDurationMs = audioPackage.totalDurationMs.coerceAtLeast(1500L)

        // Prepare visual background file on IO thread
        val preparedBg = withContext(Dispatchers.IO) {
            prepareBackgroundAsset(
                project = project,
                width = safeWidth,
                height = safeHeight
            )
        }

        val outputFile = File(exportsDir, "$outputFileName.mp4")
        if (outputFile.exists()) outputFile.delete()

        val mainHandler = Handler(Looper.getMainLooper())

        return suspendCancellableCoroutine { continuation ->
            mainHandler.post {
                try {
                    val isVideoBackground = preparedBg.isVideo
                    val overlay = QuranTextBitmapOverlay(
                        context = context,
                        width = safeWidth,
                        height = safeHeight,
                        project = project,
                        surahArabicName = audioPackage.surah.arabicName,
                        reciterArabicName = audioPackage.reciter.arabicName,
                        lines = audioPackage.lines,
                        drawDimScrim = isVideoBackground
                    )

                    val videoEffects = mutableListOf<Effect>(
                        Presentation.createForWidthAndHeight(
                            safeWidth,
                            safeHeight,
                            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                        ),
                        OverlayEffect(listOf(overlay))
                    )

                    val videoSequence: EditedMediaItemSequence = if (isVideoBackground) {
                        val bgMediaItem = MediaItem.fromUri(Uri.fromFile(preparedBg.file))
                        val bgEditedItem = EditedMediaItem.Builder(bgMediaItem)
                            .setRemoveAudio(true)
                            .setEffects(Effects(emptyList(), videoEffects))
                            .build()

                        if (project.videoLoopEnabled) {
                            EditedMediaItemSequence.Builder(listOf(bgEditedItem))
                                .setIsLooping(true)
                                .build()
                        } else {
                            EditedMediaItemSequence.Builder(listOf(bgEditedItem)).build()
                        }
                    } else {
                        val bgMediaItem = MediaItem.Builder()
                            .setUri(Uri.fromFile(preparedBg.file))
                            .setImageDurationMs(totalDurationMs)
                            .build()
                        val bgEditedItem = EditedMediaItem.Builder(bgMediaItem)
                            .setFrameRate(30)
                            .setEffects(Effects(emptyList(), videoEffects))
                            .build()
                        EditedMediaItemSequence.Builder(listOf(bgEditedItem)).build()
                    }

                    val audioSequence: EditedMediaItemSequence = when (val payload = audioPackage.payload) {
                        is AudioModePayload.PerAyahFiles -> {
                            val items = payload.files.map { f ->
                                EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(f)))
                                    .setRemoveVideo(true)
                                    .build()
                            }
                            EditedMediaItemSequence.Builder(items).build()
                        }
                        is AudioModePayload.FullSurahFile -> {
                            val clipped = MediaItem.Builder()
                                .setUri(Uri.fromFile(payload.file))
                                .setClippingConfiguration(
                                    MediaItem.ClippingConfiguration.Builder()
                                        .setStartPositionMs(payload.clipStartMs)
                                        .setEndPositionMs(payload.clipEndMs)
                                        .build()
                                )
                                .build()
                            val edited = EditedMediaItem.Builder(clipped)
                                .setRemoveVideo(true)
                                .build()
                            EditedMediaItemSequence.Builder(listOf(edited)).build()
                        }
                    }

                    val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()

                    var progressRunnable: Runnable? = null
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                progressRunnable?.let { mainHandler.removeCallbacks(it) }
                                onProgress(100)
                                overlay.release()
                                if (continuation.isActive) {
                                    continuation.resume(outputFile)
                                }
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                progressRunnable?.let { mainHandler.removeCallbacks(it) }
                                overlay.release()
                                if (continuation.isActive) {
                                    continuation.resumeWithException(exportException)
                                }
                            }
                        })
                        .build()

                    onRegisterCancel(
                        ExportHandle {
                            mainHandler.post {
                                progressRunnable?.let { mainHandler.removeCallbacks(it) }
                                runCatching { transformer.cancel() }
                                overlay.release()
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        mainHandler.post {
                            progressRunnable?.let { mainHandler.removeCallbacks(it) }
                            runCatching { transformer.cancel() }
                            overlay.release()
                        }
                    }

                    transformer.start(composition, outputFile.absolutePath)

                    val progressHolder = ProgressHolder()
                    progressRunnable = object : Runnable {
                        override fun run() {
                            if (!continuation.isActive) return
                            val state = transformer.getProgress(progressHolder)
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress(progressHolder.progress.coerceIn(0, 99))
                            }
                            if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                mainHandler.postDelayed(this, 250L)
                            }
                        }
                    }
                    mainHandler.postDelayed(progressRunnable, 250L)
                } catch (t: Throwable) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(t)
                    }
                }
            }
        }
    }

    private data class PreparedBackground(
        val file: File,
        val isVideo: Boolean
    )

    private fun prepareBackgroundAsset(
        project: VideoProject,
        width: Int,
        height: Int
    ): PreparedBackground {
        return when (project.backgroundType) {
            BackgroundType.BUNDLED_VIDEO -> {
                val vid = BundledBackgrounds.findVideo(project.bundledBackgroundId)
                val dest = File(tempWorkDir, "${vid.id}.mp4")
                if (!dest.exists() || dest.length() == 0L) {
                    context.assets.open(vid.assetPath).use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                PreparedBackground(dest, isVideo = true)
            }
            BackgroundType.USER_VIDEO -> {
                val path = project.userMediaLocalPath
                val srcFile = if (path != null) File(path) else null
                if (srcFile != null && srcFile.exists() && srcFile.length() > 0L) {
                    PreparedBackground(srcFile, isVideo = true)
                } else {
                    val fallback = BundledBackgrounds.videos.first()
                    val dest = File(tempWorkDir, "${fallback.id}.mp4")
                    if (!dest.exists()) {
                        context.assets.open(fallback.assetPath).use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    PreparedBackground(dest, isVideo = true)
                }
            }
            BackgroundType.BUNDLED_IMAGE, BackgroundType.USER_IMAGE -> {
                val rawBitmap = loadSourceBitmap(project)
                val rendered = renderCroppedBlurredDimmedBitmap(
                    source = rawBitmap,
                    targetWidth = width,
                    targetHeight = height,
                    blurRadius = project.backgroundBlurRadius,
                    dimAmount = project.backgroundDim
                )
                if (rawBitmap != rendered) rawBitmap.recycle()
                val outPng = File(tempWorkDir, "bg_frame_${width}x${height}.png")
                FileOutputStream(outPng).use { fos ->
                    rendered.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                rendered.recycle()
                PreparedBackground(outPng, isVideo = false)
            }
        }
    }

    private fun loadSourceBitmap(project: VideoProject): Bitmap {
        if (project.backgroundType == BackgroundType.USER_IMAGE && !project.userMediaLocalPath.isNullOrBlank()) {
            val file = File(project.userMediaLocalPath)
            if (file.exists()) {
                val decoded = BitmapFactory.decodeFile(file.absolutePath)
                if (decoded != null) return decoded
            }
        }
        val bundled = BundledBackgrounds.findImage(project.bundledBackgroundId)
        return BitmapFactory.decodeResource(context.resources, bundled.drawableResId)
    }

    private fun renderCroppedBlurredDimmedBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        blurRadius: Float,
        dimAmount: Float
    ): Bitmap {
        val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val srcW = source.width.toFloat().coerceAtLeast(1f)
        val srcH = source.height.toFloat().coerceAtLeast(1f)
        val scale = max(targetWidth / srcW, targetHeight / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val dx = (targetWidth - scaledW) / 2f
        val dy = (targetHeight - scaledH) / 2f

        val workingSource = if (blurRadius > 0.5f) {
            fastSoftBlur(source, blurRadius.roundToInt().coerceIn(1, 24))
        } else {
            source
        }

        val matrix = Matrix().apply {
            postScale(
                targetWidth.toFloat() / workingSource.width * (scaledW / targetWidth),
                targetHeight.toFloat() / workingSource.height * (scaledH / targetHeight)
            )
            postTranslate(dx, dy)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(workingSource, matrix, paint)
        if (workingSource !== source) workingSource.recycle()

        val dimAlpha = (dimAmount.coerceIn(0f, 0.9f) * 255).toInt()
        if (dimAlpha > 0) {
            canvas.drawColor(Color.argb(dimAlpha, 0, 0, 0))
        }
        return out
    }

    private fun fastSoftBlur(src: Bitmap, radius: Int): Bitmap {
        val factor = (1f + radius / 2.2f).coerceIn(1.5f, 14f)
        val smallW = (src.width / factor).roundToInt().coerceAtLeast(24)
        val smallH = (src.height / factor).roundToInt().coerceAtLeast(24)
        val down = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val up = Bitmap.createScaledBitmap(down, src.width, src.height, true)
        down.recycle()
        return up
    }

    fun saveVideoToGallery(sourceFile: File, displayName: String): Uri? {
        val safeName = displayName.replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "").ifBlank { "quran_video" }
        val fileName = "${safeName}_${System.currentTimeMillis()}.mp4"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/QuranVideoMaker")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "QuranVideoMaker"
            ).apply { mkdirs() }
            val dest = File(dir, fileName)
            sourceFile.copyTo(dest, overwrite = true)
            Uri.fromFile(dest)
        }
    }

    fun shareVideo(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "مشاركة فيديو التلاوة").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    @Suppress("unused")
    fun probeDuration(file: File): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { mmr.release() }
        }
    }
}

@OptIn(UnstableApi::class)
private class QuranTextBitmapOverlay(
    private val context: Context,
    private val width: Int,
    private val height: Int,
    private val project: VideoProject,
    private val surahArabicName: String,
    private val reciterArabicName: String,
    private val lines: List<SyncedAyahLine>,
    private val drawDimScrim: Boolean
) : BitmapOverlay() {

    private val overlayBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val canvas: Canvas = Canvas(overlayBitmap)
    private val scaleFactor: Float = minOf(width, height) / 360f
    private val typefaceCache = mutableMapOf<String, Typeface>()

    private var lastLineIndex: Int = -99
    private var lastWordIndex: Int = -99

    private fun getTypeface(fontId: String): Typeface {
        return typefaceCache.getOrPut(fontId) {
            val option = FontOption.fromId(fontId)
            ResourcesCompat.getFont(context, option.fontResId) ?: Typeface.DEFAULT
        }
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = (presentationTimeUs / 1000L).coerceAtLeast(0L)
        val activeIdx = findLineIndex(timeMs)
        val activeLine = lines.getOrNull(activeIdx)
        val offsetInAyahMs = if (activeLine != null) (timeMs - activeLine.startMs).coerceAtLeast(0L) else 0L
        val activeWordIdx = activeLine?.activeWordIndexAt(offsetInAyahMs) ?: 0

        if (activeIdx != lastLineIndex || activeWordIdx != lastWordIndex) {
            lastLineIndex = activeIdx
            lastWordIndex = activeWordIdx
            redrawFrame(activeIdx, activeWordIdx)
        }
        return overlayBitmap
    }

    private fun findLineIndex(timeMs: Long): Int {
        if (lines.isEmpty()) return -1
        for (i in lines.indices) {
            val line = lines[i]
            if (timeMs >= line.startMs && timeMs < line.endMs) {
                return i
            }
        }
        return if (timeMs >= lines.last().endMs) lines.lastIndex else 0
    }

    private fun redrawFrame(activeIdx: Int, activeWordIdx: Int) {
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        if (drawDimScrim) {
            val dimAlpha = (project.backgroundDim.coerceIn(0f, 0.9f) * 255).toInt()
            if (dimAlpha > 0) {
                canvas.drawColor(Color.argb(dimAlpha, 0, 0, 0))
            }
        }

        // 1. Surah Title (Top)
        if (project.surahTitleStyle.enabled) {
            drawStyledLabel(
                text = surahArabicName,
                style = project.surahTitleStyle
            )
        }

        // 2. Dimmed adjacent ayahs (optional, off by default)
        val currentLine = lines.getOrNull(activeIdx)
        if (project.showDimmedAdjacentAyahs && currentLine != null) {
            val prevLine = lines.getOrNull(activeIdx - 1)
            val nextLine = lines.getOrNull(activeIdx + 1)
            val adjacentOffset = (project.ayahStyle.fontSizeSp * 1.9f * scaleFactor)
            if (prevLine != null) {
                drawSingleLineAyah(
                    line = prevLine,
                    yCenter = project.ayahStyle.verticalPosition * height - adjacentOffset,
                    alphaMultiplier = 0.35f,
                    progressiveWordsOnly = false,
                    activeWordIdx = 0
                )
            }
            if (nextLine != null) {
                drawSingleLineAyah(
                    line = nextLine,
                    yCenter = project.ayahStyle.verticalPosition * height + adjacentOffset,
                    alphaMultiplier = 0.35f,
                    progressiveWordsOnly = false,
                    activeWordIdx = 0
                )
            }
        }

        // 3. Active spoken-words single-line Ayah
        if (currentLine != null && project.ayahStyle.enabled) {
            drawSingleLineAyah(
                line = currentLine,
                yCenter = project.ayahStyle.verticalPosition * height,
                alphaMultiplier = 1f,
                progressiveWordsOnly = true,
                activeWordIdx = activeWordIdx
            )
        }

        // 4. Reciter Name (Bottom)
        if (project.reciterTitleStyle.enabled) {
            drawStyledLabel(
                text = "القارئ: $reciterArabicName",
                style = project.reciterTitleStyle
            )
        }

        // 5. Optional Watermark
        if (project.watermarkStyle.enabled && project.watermarkText.isNotBlank()) {
            drawStyledLabel(
                text = project.watermarkText.trim(),
                style = project.watermarkStyle
            )
        }
    }

    private fun drawSingleLineAyah(
        line: SyncedAyahLine,
        yCenter: Float,
        alphaMultiplier: Float,
        progressiveWordsOnly: Boolean,
        activeWordIdx: Int
    ) {
        val style = project.ayahStyle
        // Safe margins: 8% padding on each side, max content width = 84% of frame width
        val horizontalMargin = width * 0.08f
        val maxContentWidth = (width - horizontalMargin * 2f).coerceAtLeast(60f)

        val effectiveOpacity = (style.opacity * alphaMultiplier).coerceIn(0f, 1f)
        val baseColor = ArabicTextRenderer.applyOpacityToArgb(style.colorArgb, effectiveOpacity)
        val rawAccentHex = if (project.themeStyle.accentHex == style.colorArgb) {
            0xFFFFD54FL
        } else {
            project.themeStyle.accentHex
        }
        val highlightColor = ArabicTextRenderer.applyOpacityToArgb(rawAccentHex, effectiveOpacity)
        val tashkeelColor = ArabicTextRenderer.applyOpacityToArgb(project.tashkeelColorArgb, effectiveOpacity)

        val spannable = if (progressiveWordsOnly) {
            ArabicTextRenderer.buildSpokenWordsSpannable(
                words = line.wordTimings,
                fallbackText = line.text,
                activeWordIndex = activeWordIdx,
                ayahNumberInSurah = line.numberInSurah,
                isBismillah = line.isBismillah,
                normalColorArgb = baseColor,
                highlightColorArgb = highlightColor,
                tashkeelEnabled = project.tashkeelColoringEnabled,
                tashkeelColorArgb = tashkeelColor,
                tashkeelScale = project.tashkeelScale
            )
        } else {
            val fullText = if (line.isBismillah) {
                line.text
            } else {
                "${line.text} ﴿${ArabicTextRenderer.toArabicDigits(line.numberInSurah)}﴾"
            }
            ArabicTextRenderer.buildTashkeelSpannable(
                text = fullText,
                baseColorArgb = baseColor,
                tashkeelEnabled = project.tashkeelColoringEnabled,
                tashkeelColorArgb = tashkeelColor,
                tashkeelScale = project.tashkeelScale
            )
        }

        val maxTextPx = (style.fontSizeSp * scaleFactor).coerceAtLeast(8f)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = getTypeface(style.fontId)
            textSize = maxTextPx
            color = baseColor
            if (style.shadowEnabled) {
                setShadowLayer(8f * scaleFactor, 0f, 3f * scaleFactor, Color.argb((180 * effectiveOpacity).toInt(), 0, 0, 0))
            }
        }

        // Mandatory auto-fit: shrink below user's max font size until single line fits inside maxContentWidth
        val safeTargetWidth = maxContentWidth * 0.96f
        val unconstrainedWidth = (width * 4).coerceAtLeast(4000)
        var guard = 0
        while (paint.textSize > 4.5f && guard < 35) {
            val testLayout = StaticLayout.Builder
                .obtain(spannable, 0, spannable.length, paint, unconstrainedWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(1)
                .setIncludePad(true)
                .build()
            val lineW = if (testLayout.lineCount > 0) testLayout.getLineWidth(0) else 0f
            val plainW = paint.measureText(spannable, 0, spannable.length)
            if (maxOf(lineW, plainW) <= safeTargetWidth) {
                break
            }
            paint.textSize = (paint.textSize * 0.91f).coerceAtLeast(4f)
            guard++
        }

        val layoutWidth = maxContentWidth.roundToInt()
        val alignment = when (style.alignment) {
            TextAlignmentMode.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            TextAlignmentMode.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignmentMode.CENTER -> Layout.Alignment.ALIGN_CENTER
        }

        val staticLayout = StaticLayout.Builder
            .obtain(spannable, 0, spannable.length, paint, layoutWidth)
            .setAlignment(alignment)
            .setMaxLines(1)
            .setIncludePad(true)
            .build()

        canvas.save()
        canvas.clipRect(horizontalMargin, 0f, width - horizontalMargin, height.toFloat())
        val topY = (yCenter - staticLayout.height / 2f).coerceIn(8f, (height - staticLayout.height - 8f).coerceAtLeast(8f))
        canvas.translate(horizontalMargin, topY)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawStyledLabel(text: String, style: TextLayerStyle) {
        val effectiveColor = ArabicTextRenderer.applyOpacityToArgb(style.colorArgb, style.opacity)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = getTypeface(style.fontId)
            textSize = (style.fontSizeSp * scaleFactor).coerceAtLeast(11f)
            color = effectiveColor
            if (style.shadowEnabled) {
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.argb((170 * style.opacity).toInt(), 0, 0, 0))
            }
        }

        val marginH = width * 0.08f
        val maxW = (width - marginH * 2f).roundToInt().coerceAtLeast(80)
        val textWidth = paint.measureText(text)
        if (textWidth > maxW) {
            paint.textSize = paint.textSize * (maxW / textWidth) * 0.96f
        }
        val finalTextWidth = paint.measureText(text).coerceAtMost(maxW.toFloat())
        val fm = paint.fontMetrics
        val textHeight = fm.descent - fm.ascent

        val yCenter = (style.verticalPosition.coerceIn(0.04f, 0.96f)) * height
        val xStart = when (style.alignment) {
            TextAlignmentMode.RIGHT -> width - marginH - finalTextWidth
            TextAlignmentMode.LEFT -> marginH
            TextAlignmentMode.CENTER -> (width - finalTextWidth) / 2f
        }

        if (style.backgroundPillEnabled) {
            val padH = 14f * scaleFactor
            val padV = 6f * scaleFactor
            val pillRect = RectF(
                xStart - padH,
                yCenter - textHeight / 2f - padV,
                xStart + finalTextWidth + padH,
                yCenter + textHeight / 2f + padV
            )
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ArabicTextRenderer.applyOpacityToArgb(style.backgroundPillColorArgb, style.opacity)
            }
            canvas.drawRoundRect(pillRect, 20f * scaleFactor, 20f * scaleFactor, pillPaint)
        }

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxW)
            .setAlignment(
                when (style.alignment) {
                    TextAlignmentMode.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                    TextAlignmentMode.LEFT -> Layout.Alignment.ALIGN_NORMAL
                    TextAlignmentMode.CENTER -> Layout.Alignment.ALIGN_CENTER
                }
            )
            .setMaxLines(1)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate((width - maxW) / 2f, yCenter - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    override fun release() {
        super.release()
        if (!overlayBitmap.isRecycled) {
            overlayBitmap.recycle()
        }
    }
}
