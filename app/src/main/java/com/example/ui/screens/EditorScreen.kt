package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.ExportResolution
import com.example.domain.model.Reciter
import com.example.domain.model.Surah
import com.example.domain.model.SyncedAyahLine
import com.example.domain.model.VideoProject
import com.example.ui.components.AutoTitlesSectionCard
import com.example.ui.components.BackgroundSectionCard
import com.example.ui.components.ExportVideoSectionCard
import com.example.ui.components.FontsSectionCard
import com.example.ui.components.LivePreviewSectionCard
import com.example.ui.components.PinnedPreviewPlayer
import com.example.ui.components.ReciterSectionCard
import com.example.ui.components.SaveProgressSectionCard
import com.example.ui.components.SurahSectionCard
import com.example.ui.components.TashkeelSectionCard
import com.example.ui.components.ThemesSectionCard
import com.example.ui.components.VideoSizeSectionCard
import com.example.ui.components.WatermarkSectionCard
import com.example.ui.viewmodel.AudioLoadState
import com.example.ui.viewmodel.ExportUiState
import com.example.ui.viewmodel.PlaybackUiState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialSection: Int,
    project: VideoProject,
    surahs: List<Surah>,
    currentSurah: Surah?,
    reciters: List<Reciter>,
    selectedReciter: Reciter,
    savedProjects: List<VideoProject>,
    lastSavedTimestamp: Long?,
    currentTheme: AppThemeStyle,
    lines: List<SyncedAyahLine>,
    playbackState: PlaybackUiState,
    audioLoadState: AudioLoadState,
    exportState: ExportUiState,
    batchCount: Int,
    isCurrentSurahCached: Boolean,
    statusBanner: String?,
    onConsumeStatusBanner: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenBatchQueue: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onJumpAyah: (Int) -> Unit,
    onRetryAudio: () -> Unit,
    onChangePlaybackSpeed: (Float) -> Unit,
    onSelectReciter: (Reciter) -> Unit,
    onToggleFavoriteReciter: (Reciter) -> Unit,
    onSelectSurah: (Surah) -> Unit,
    onSelectAyahRange: (Int, Int) -> Unit,
    onToggleBismillah: (Boolean) -> Unit,
    onUpdateProject: ((VideoProject) -> VideoProject) -> Unit,
    onImportUserMedia: (Uri, Boolean) -> Unit,
    onSaveAsNamedProject: (String) -> Unit,
    onOpenSavedProject: (VideoProject) -> Unit,
    onDeleteSavedProject: (String) -> Unit,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onSelectResolution: (ExportResolution) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onSaveToGallery: (File) -> Unit,
    onShareVideo: (File) -> Unit
) {
    var expandedSection by remember { mutableIntStateOf(initialSection.coerceIn(1, 12)) }
    var showSafeGuides by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialSection) {
        if (initialSection in 1..12) {
            expandedSection = initialSection
            listState.animateScrollToItem((initialSection - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(statusBanner) {
        if (!statusBanner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusBanner)
            onConsumeStatusBanner()
        }
    }

    fun toggleSection(index: Int) {
        expandedSection = if (expandedSection == index) 0 else index
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSurah?.arabicName ?: "محرر الفيديو",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedReciter.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenBatchQueue,
                        modifier = Modifier.testTag("editor_batch_button")
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = "معالجة دفعة سور")
                    }
                    IconButton(
                        onClick = {
                            expandedSection = 12
                            coroutineScope.launch { listState.animateScrollToItem(11) }
                        },
                        modifier = Modifier.testTag("editor_jump_export_button")
                    ) {
                        Icon(Icons.Filled.MovieCreation, contentDescription = "تصدير الفيديو")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FIXED PINNED TOP PREVIEW (Never scrolls away)
            PinnedPreviewPlayer(
                project = project,
                surahName = currentSurah?.arabicName ?: "سورة الفاتحة",
                reciterName = selectedReciter.arabicName,
                lines = lines,
                playbackState = playbackState,
                audioLoadState = audioLoadState,
                showSafeGuides = showSafeGuides,
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onJumpAyah = onJumpAyah,
                onRetryAudio = onRetryAudio
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // SCROLLABLE COLLAPSIBLE EDITOR SECTIONS (1..12 in exact order)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1) RECITER
                item(key = "sec_1") {
                    ReciterSectionCard(
                        expanded = expandedSection == 1,
                        onToggleExpand = { toggleSection(1) },
                        reciters = reciters,
                        selectedReciter = selectedReciter,
                        onSelectReciter = onSelectReciter,
                        onToggleFavorite = onToggleFavoriteReciter
                    )
                }

                // 2) SURAHS
                item(key = "sec_2") {
                    SurahSectionCard(
                        expanded = expandedSection == 2,
                        onToggleExpand = { toggleSection(2) },
                        surahs = surahs,
                        project = project,
                        currentSurah = currentSurah,
                        onSelectSurah = onSelectSurah,
                        onRangeChange = onSelectAyahRange,
                        onBismillahToggle = onToggleBismillah
                    )
                }

                // 3) FONTS
                item(key = "sec_3") {
                    FontsSectionCard(
                        expanded = expandedSection == 3,
                        onToggleExpand = { toggleSection(3) },
                        project = project,
                        onUpdateProject = onUpdateProject
                    )
                }

                // 4) TASHKEEL COLORING
                item(key = "sec_4") {
                    TashkeelSectionCard(
                        expanded = expandedSection == 4,
                        onToggleExpand = { toggleSection(4) },
                        project = project,
                        onUpdateProject = onUpdateProject
                    )
                }

                // 5) VIDEO BACKGROUND
                item(key = "sec_5") {
                    BackgroundSectionCard(
                        expanded = expandedSection == 5,
                        onToggleExpand = { toggleSection(5) },
                        project = project,
                        onUpdateProject = onUpdateProject,
                        onImportUserMedia = onImportUserMedia
                    )
                }

                // 6) LIVE PREVIEW
                item(key = "sec_6") {
                    LivePreviewSectionCard(
                        expanded = expandedSection == 6,
                        onToggleExpand = { toggleSection(6) },
                        lines = lines,
                        currentLineIndex = playbackState.currentLineIndex,
                        playbackSpeed = playbackState.playbackSpeed,
                        showSafeGuides = showSafeGuides,
                        audioLoadState = audioLoadState,
                        isCachedOffline = isCurrentSurahCached,
                        onToggleSafeGuides = { showSafeGuides = it },
                        onChangeSpeed = onChangePlaybackSpeed,
                        onJumpToLine = onJumpAyah
                    )
                }

                // 7) SAVE PROGRESS
                item(key = "sec_7") {
                    SaveProgressSectionCard(
                        expanded = expandedSection == 7,
                        onToggleExpand = { toggleSection(7) },
                        activeProject = project,
                        savedProjects = savedProjects,
                        lastSavedTimestamp = lastSavedTimestamp,
                        onSaveAsNamed = onSaveAsNamedProject,
                        onOpenProject = onOpenSavedProject,
                        onDeleteProject = onDeleteSavedProject
                    )
                }

                // 8) UI STYLES / THEMES
                item(key = "sec_8") {
                    ThemesSectionCard(
                        expanded = expandedSection == 8,
                        onToggleExpand = { toggleSection(8) },
                        currentTheme = currentTheme,
                        onSelectTheme = onSelectTheme
                    )
                }

                // 9) VIDEO SIZE / ASPECT RATIO
                item(key = "sec_9") {
                    VideoSizeSectionCard(
                        expanded = expandedSection == 9,
                        onToggleExpand = { toggleSection(9) },
                        selectedRatio = project.aspectRatio,
                        onSelectRatio = { ratio -> onUpdateProject { it.copy(aspectRatio = ratio) } }
                    )
                }

                // 10) AUTO TITLES (Surah top & Reciter bottom)
                item(key = "sec_10") {
                    AutoTitlesSectionCard(
                        expanded = expandedSection == 10,
                        onToggleExpand = { toggleSection(10) },
                        project = project,
                        onUpdateProject = onUpdateProject
                    )
                }

                // 11) USER INFO / WATERMARK
                item(key = "sec_11") {
                    WatermarkSectionCard(
                        expanded = expandedSection == 11,
                        onToggleExpand = { toggleSection(11) },
                        project = project,
                        onUpdateProject = onUpdateProject
                    )
                }

                // 12) SAVE VIDEO / EXPORT
                item(key = "sec_12") {
                    ExportVideoSectionCard(
                        expanded = expandedSection == 12,
                        onToggleExpand = { toggleSection(12) },
                        project = project,
                        exportState = exportState,
                        batchCount = batchCount,
                        onSelectResolution = onSelectResolution,
                        onStartExport = onStartExport,
                        onCancelExport = onCancelExport,
                        onSaveToGallery = onSaveToGallery,
                        onShareVideo = onShareVideo,
                        onOpenBatchQueue = onOpenBatchQueue
                    )
                }
            }
        }
    }
}
