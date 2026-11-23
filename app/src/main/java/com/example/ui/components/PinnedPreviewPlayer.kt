package com.example.ui.components

import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.domain.model.BackgroundType
import com.example.domain.model.BundledBackgrounds
import com.example.domain.model.FontOption
import com.example.domain.model.SingleLineOverflowMode
import com.example.domain.model.SyncedAyahLine
import com.example.domain.model.TextAlignmentMode
import com.example.domain.model.TextLayerStyle
import com.example.domain.model.VideoProject
import com.example.ui.theme.LocalPreviewAccentColor
import com.example.util.ArabicTextRenderer
import com.example.util.ArabicTextRenderer.toArabicDigits
import com.example.ui.viewmodel.AudioLoadState
import com.example.ui.viewmodel.PlaybackUiState
import java.io.File

@Composable
fun PinnedPreviewPlayer(
    project: VideoProject,
    surahName: String,
    reciterName: String,
    lines: List<SyncedAyahLine>,
    playbackState: PlaybackUiState,
    audioLoadState: AudioLoadState,
    showSafeGuides: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onJumpAyah: (Int) -> Unit,
    onRetryAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFullscreen by remember { mutableStateOf(false) }
    val accentBorder = LocalPreviewAccentColor.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0A0F14),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Video Stage (Aspect Ratio Constrained)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 165.dp, max = 235.dp),
                contentAlignment = Alignment.Center
            ) {
                VideoStageCanvas(
                    project = project,
                    surahName = surahName,
                    reciterName = reciterName,
                    lines = lines,
                    currentLineIndex = playbackState.currentLineIndex,
                    currentOffsetInAyahMs = playbackState.currentOffsetInAyahMs,
                    showSafeGuides = showSafeGuides,
                    accentBorder = accentBorder,
                    maxHeight = 235.dp,
                    referenceShortEdgeDp = 200f
                )

                IconButton(
                    onClick = { showFullscreen = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.50f), CircleShape)
                        .testTag("fullscreen_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "معاينة ملء الشاشة",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Audio download status bar if loading or error
            when (audioLoadState) {
                is AudioLoadState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { audioLoadState.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f),
                            color = accentBorder
                        )
                        Text(
                            text = audioLoadState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                is AudioLoadState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = audioLoadState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF8A80),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onRetryAudio, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "إعادة التحميل",
                                tint = accentBorder,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                else -> Unit
            }

            // Real Video Player Controls (SeekBar, Play/Pause, Ayah Counter, Time)
            PlayerControlBar(
                lines = lines,
                playbackState = playbackState,
                audioLoadState = audioLoadState,
                accentColor = accentBorder,
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onJumpAyah = onJumpAyah
            )
        }
    }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاينة كاملة - $surahName",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        IconButton(onClick = { showFullscreen = false }) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = "إغلاق المعاينة الكاملة",
                                tint = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoStageCanvas(
                            project = project,
                            surahName = surahName,
                            reciterName = reciterName,
                            lines = lines,
                            currentLineIndex = playbackState.currentLineIndex,
                            currentOffsetInAyahMs = playbackState.currentOffsetInAyahMs,
                            showSafeGuides = showSafeGuides,
                            accentBorder = accentBorder,
                            maxHeight = 560.dp,
                            referenceShortEdgeDp = 320f
                        )
                    }

                    PlayerControlBar(
                        lines = lines,
                        playbackState = playbackState,
                        audioLoadState = audioLoadState,
                        accentColor = accentBorder,
                        onTogglePlayPause = onTogglePlayPause,
                        onSeekTo = onSeekTo,
                        onJumpAyah = onJumpAyah
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerControlBar(
    lines: List<SyncedAyahLine>,
    playbackState: PlaybackUiState,
    audioLoadState: AudioLoadState,
    accentColor: Color,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onJumpAyah: (Int) -> Unit
) {
    val totalMs = playbackState.totalDurationMs.coerceAtLeast(1L)
    val currentMs = playbackState.currentPositionMs.coerceIn(0L, totalMs)
    val currentLine = lines.getOrNull(playbackState.currentLineIndex)
    val ayahCounterText = when {
        lines.isEmpty() -> "لا توجد آيات"
        currentLine?.isBismillah == true -> "البسملة (${toArabicDigits(playbackState.currentLineIndex + 1)}/${toArabicDigits(lines.size)})"
        currentLine != null -> "الآية ${toArabicDigits(currentLine.numberInSurah)} • (${toArabicDigits(playbackState.currentLineIndex + 1)}/${toArabicDigits(lines.size)})"
        else -> ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledIconButton(
                onClick = onTogglePlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("preview_play_pause_button")
            ) {
                if (audioLoadState is AudioLoadState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.Black
                    )
                } else {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "إيقاف مؤقت" else "تشغيل"
                    )
                }
            }

            IconButton(
                onClick = { onJumpAyah((playbackState.currentLineIndex - 1).coerceAtLeast(0)) },
                enabled = playbackState.currentLineIndex > 0,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "الآية السابقة",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { onJumpAyah((playbackState.currentLineIndex + 1).coerceAtMost((lines.size - 1).coerceAtLeast(0))) },
                enabled = playbackState.currentLineIndex < lines.lastIndex,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "الآية التالية",
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = currentMs.toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..totalMs.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("preview_seek_bar")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ayahCounterText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Text(
                        text = "${ArabicTextRenderer.formatDurationMs(currentMs)} / ${ArabicTextRenderer.formatDurationMs(totalMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoStageCanvas(
    project: VideoProject,
    surahName: String,
    reciterName: String,
    lines: List<SyncedAyahLine>,
    currentLineIndex: Int,
    currentOffsetInAyahMs: Long,
    showSafeGuides: Boolean,
    accentBorder: Color,
    maxHeight: Dp,
    referenceShortEdgeDp: Float
) {
    val ratio = project.aspectRatio.widthRatio / project.aspectRatio.heightRatio
    val currentLine = lines.getOrNull(currentLineIndex)
    val prevLine = lines.getOrNull(currentLineIndex - 1)
    val nextLine = lines.getOrNull(currentLineIndex + 1)

    BoxWithConstraints(
        modifier = Modifier
            .heightIn(max = maxHeight)
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, accentBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .background(Color.Black)
    ) {
        val shortEdgeDp = minOf(maxWidth.value, maxHeight.value)
        val previewScale = (shortEdgeDp / 360f).coerceIn(0.35f, 1.35f)
        val safeSideMargin = maxWidth * 0.08f

        // 1. Background Media (Image or Muted Video)
        BackgroundMediaLayer(
            project = project,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (project.backgroundBlurRadius > 0.5f) {
                        Modifier.blur((project.backgroundBlurRadius * previewScale).dp)
                    } else Modifier
                )
        )

        // 2. Dim Overlay for Text Readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = project.backgroundDim.coerceIn(0f, 0.9f)))
        )

        // 3. Optional Safe Margins Guide (8% horizontal safe margins)
        if (showSafeGuides) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = safeSideMargin, vertical = (maxHeight * 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            )
        }

        // 4. Surah Title (Top)
        if (project.surahTitleStyle.enabled) {
            OverlayLabelElement(
                text = surahName,
                style = project.surahTitleStyle,
                containerHeight = maxHeight,
                safeSideMargin = safeSideMargin,
                scale = previewScale
            )
        }

        // 5. Optional Dimmed Previous/Next Ayahs + Synchronized Spoken-Words Active Ayah
        if (currentLine != null && project.ayahStyle.enabled) {
            val ayahCenterFraction = project.ayahStyle.verticalPosition.coerceIn(0.12f, 0.88f)
            val adjGap = (project.ayahStyle.fontSizeSp * 1.55f * previewScale).dp
            val activeWordIndex = currentLine.activeWordIndexAt(currentOffsetInAyahMs)

            if (project.showDimmedAdjacentAyahs && prevLine != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = safeSideMargin)
                        .align(Alignment.TopCenter)
                        .padding(top = (maxHeight * ayahCenterFraction - adjGap).coerceAtLeast(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SingleLineAyahText(
                        line = prevLine,
                        project = project,
                        previewScale = previewScale,
                        opacityMultiplier = 0.35f,
                        progressiveWordsOnly = false,
                        activeWordIndex = 0
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = safeSideMargin)
                    .align(Alignment.TopCenter)
                    .padding(top = (maxHeight * ayahCenterFraction - (18 * previewScale).dp).coerceAtLeast(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                SingleLineAyahText(
                    line = currentLine,
                    project = project,
                    previewScale = previewScale,
                    opacityMultiplier = 1f,
                    progressiveWordsOnly = true,
                    activeWordIndex = activeWordIndex
                )
            }

            if (project.showDimmedAdjacentAyahs && nextLine != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = safeSideMargin)
                        .align(Alignment.TopCenter)
                        .padding(top = (maxHeight * ayahCenterFraction + adjGap * 0.7f).coerceAtMost(maxHeight - 24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SingleLineAyahText(
                        line = nextLine,
                        project = project,
                        previewScale = previewScale,
                        opacityMultiplier = 0.35f,
                        progressiveWordsOnly = false,
                        activeWordIndex = 0
                    )
                }
            }
        }

        // 6. Reciter Name (Bottom)
        if (project.reciterTitleStyle.enabled) {
            OverlayLabelElement(
                text = "القارئ: $reciterName",
                style = project.reciterTitleStyle,
                containerHeight = maxHeight,
                safeSideMargin = safeSideMargin,
                scale = previewScale
            )
        }

        // 7. Optional Watermark (only if enabled & non-empty)
        if (project.watermarkStyle.enabled && project.watermarkText.isNotBlank()) {
            OverlayLabelElement(
                text = project.watermarkText.trim(),
                style = project.watermarkStyle,
                containerHeight = maxHeight,
                safeSideMargin = safeSideMargin,
                scale = previewScale
            )
        }
    }
}

@Composable
private fun SingleLineAyahText(
    line: SyncedAyahLine,
    project: VideoProject,
    previewScale: Float,
    opacityMultiplier: Float,
    progressiveWordsOnly: Boolean,
    activeWordIndex: Int
) {
    val style = project.ayahStyle
    val fontOption = FontOption.fromId(style.fontId)
    val fontFamily = remember(fontOption) { FontFamily(Font(fontOption.fontResId)) }
    val maxFontSp = (style.fontSizeSp * previewScale).coerceAtLeast(8f)

    val effectiveOpacity = (style.opacity * opacityMultiplier).coerceIn(0f, 1f)
    val baseColor = Color(style.colorArgb).copy(alpha = effectiveOpacity)
    val rawAccentHex = if (project.themeStyle.accentHex == style.colorArgb) {
        0xFFFFD54FL
    } else {
        project.themeStyle.accentHex
    }
    val highlightColor = Color(rawAccentHex).copy(alpha = effectiveOpacity)
    val tashkeelColor = Color(project.tashkeelColorArgb).copy(alpha = effectiveOpacity)

    val textAlign = when (style.alignment) {
        TextAlignmentMode.RIGHT -> TextAlign.Start
        TextAlignmentMode.LEFT -> TextAlign.End
        TextAlignmentMode.CENTER -> TextAlign.Center
    }

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(24f)

        val (fittedFontSp, annotated) = remember(
            line.absoluteNumber,
            line.text,
            line.wordTimings,
            progressiveWordsOnly,
            activeWordIndex,
            maxFontSp,
            availableWidthPx,
            fontOption.id,
            baseColor,
            highlightColor,
            project.tashkeelColoringEnabled,
            tashkeelColor,
            project.tashkeelScale
        ) {
            fun buildForSize(fontSp: Float) = if (progressiveWordsOnly) {
                ArabicTextRenderer.buildSpokenWordsAnnotatedString(
                    words = line.wordTimings,
                    fallbackText = line.text,
                    activeWordIndex = activeWordIndex,
                    ayahNumberInSurah = line.numberInSurah,
                    isBismillah = line.isBismillah,
                    normalColor = baseColor,
                    highlightColor = highlightColor,
                    tashkeelEnabled = project.tashkeelColoringEnabled,
                    tashkeelColor = tashkeelColor,
                    baseFontSizeSp = fontSp,
                    tashkeelScale = project.tashkeelScale
                )
            } else {
                val fullText = if (line.isBismillah) line.text else "${line.text} ﴿${toArabicDigits(line.numberInSurah)}﴾"
                ArabicTextRenderer.buildTashkeelAnnotatedString(
                    text = fullText,
                    baseColor = baseColor,
                    tashkeelEnabled = project.tashkeelColoringEnabled,
                    tashkeelColor = tashkeelColor,
                    baseFontSizeSp = fontSp,
                    tashkeelScale = project.tashkeelScale
                )
            }

            var candidateSp = maxFontSp
            var candidateAnnotated = buildForSize(candidateSp)
            val safeTargetWidthPx = availableWidthPx * 0.96f

            var guard = 0
            while (candidateSp > 4.5f && guard < 30) {
                val measured = textMeasurer.measure(
                    text = candidateAnnotated,
                    style = TextStyle(fontFamily = fontFamily, fontSize = candidateSp.sp),
                    maxLines = 1,
                    softWrap = false,
                    constraints = Constraints()
                )
                if (measured.size.width <= safeTargetWidthPx) {
                    break
                }
                candidateSp = (candidateSp * 0.91f).coerceAtLeast(4f)
                candidateAnnotated = buildForSize(candidateSp)
                guard++
            }
            candidateSp to candidateAnnotated
        }

        Text(
            text = annotated,
            fontFamily = fontFamily,
            fontSize = fittedFontSp.sp,
            lineHeight = (fittedFontSp * project.lineSpacingMultiplier).sp,
            color = baseColor,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(
                shadow = if (style.shadowEnabled) {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.78f * effectiveOpacity),
                        offset = Offset(0f, 2f * previewScale),
                        blurRadius = 6f * previewScale
                    )
                } else null
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OverlayLabelElement(
    text: String,
    style: TextLayerStyle,
    containerHeight: Dp,
    safeSideMargin: Dp,
    scale: Float
) {
    val fontOption = FontOption.fromId(style.fontId)
    val fontFamily = remember(fontOption) { FontFamily(Font(fontOption.fontResId)) }
    val alignment = when (style.alignment) {
        TextAlignmentMode.RIGHT -> Alignment.TopStart
        TextAlignmentMode.LEFT -> Alignment.TopEnd
        TextAlignmentMode.CENTER -> Alignment.TopCenter
    }
    val yOffset = (containerHeight * style.verticalPosition.coerceIn(0.03f, 0.93f) - (10 * scale).dp)
        .coerceAtLeast(4.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = safeSideMargin)
            .padding(top = yOffset),
        contentAlignment = alignment
    ) {
        Text(
            text = text,
            fontFamily = fontFamily,
            fontSize = (style.fontSizeSp * scale).coerceAtLeast(8f).sp,
            color = Color(style.colorArgb).copy(alpha = style.opacity.coerceIn(0.1f, 1f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                shadow = if (style.shadowEnabled) {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.7f * style.opacity),
                        offset = Offset(0f, 1.5f * scale),
                        blurRadius = 4f * scale
                    )
                } else null
            ),
            modifier = Modifier
                .alpha(style.opacity.coerceIn(0.1f, 1f))
                .then(
                    if (style.backgroundPillEnabled) {
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(style.backgroundPillColorArgb))
                            .padding(horizontal = (10 * scale).dp, vertical = (3 * scale).dp)
                    } else Modifier
                )
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun BackgroundMediaLayer(
    project: VideoProject,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when (project.backgroundType) {
        BackgroundType.BUNDLED_IMAGE -> {
            val bg = BundledBackgrounds.findImage(project.bundledBackgroundId)
            Image(
                painter = painterResource(id = bg.drawableResId),
                contentDescription = bg.arabicName,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        BackgroundType.USER_IMAGE -> {
            val file = project.userMediaLocalPath?.let { File(it) }
            if (file != null && file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = "خلفية مخصصة",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            } else {
                val fallback = BundledBackgrounds.images.first()
                Image(
                    painter = painterResource(id = fallback.drawableResId),
                    contentDescription = fallback.arabicName,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }
        }
        BackgroundType.BUNDLED_VIDEO, BackgroundType.USER_VIDEO -> {
            val videoUri = remember(project.backgroundType, project.bundledBackgroundId, project.userMediaLocalPath) {
                if (project.backgroundType == BackgroundType.USER_VIDEO && !project.userMediaLocalPath.isNullOrBlank()) {
                    Uri.fromFile(File(project.userMediaLocalPath))
                } else {
                    val vid = BundledBackgrounds.findVideo(project.bundledBackgroundId)
                    Uri.parse("asset:///${vid.assetPath}")
                }
            }

            val bgExoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    volume = 0f // Background video is always muted; recitation is sole audio
                }
            }

            DisposableEffect(Unit) {
                onDispose { bgExoPlayer.release() }
            }

            LaunchedEffect(videoUri, project.videoLoopEnabled) {
                bgExoPlayer.repeatMode = if (project.videoLoopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                bgExoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
                bgExoPlayer.prepare()
                bgExoPlayer.playWhenReady = true
            }

            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).also { textureView ->
                        bgExoPlayer.setVideoTextureView(textureView)
                    }
                },
                modifier = modifier
            )
        }
    }
}
