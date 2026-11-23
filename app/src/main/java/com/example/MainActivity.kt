package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.DefaultReciters
import com.example.ui.screens.BatchQueueScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyProjectsScreen
import com.example.ui.theme.QuranVideoMakerTheme
import com.example.ui.viewmodel.MainViewModel

object AppRoutes {
    const val HOME = "home"
    const val EDITOR = "editor/{section}"
    const val MY_PROJECTS = "my_projects"
    const val BATCH_QUEUE = "batch_queue"

    fun editor(section: Int = 1): String = "editor/$section"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuranVideoMakerApp()
        }
    }
}

@Composable
fun QuranVideoMakerApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val themeStyle by viewModel.appThemeStyle.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val hasLastProject by viewModel.hasLastSavedProject.collectAsStateWithLifecycle()
    val lastSavedTimestamp by viewModel.lastSavedTimestamp.collectAsStateWithLifecycle()
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val reciters by viewModel.reciters.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val lines by viewModel.previewLines.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val audioLoadState by viewModel.audioLoadState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val batchQueue by viewModel.batchQueue.collectAsStateWithLifecycle()
    val isBatchRunning by viewModel.isBatchRunning.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val currentSurah = surahs.firstOrNull { it.number == activeProject.surahNumber } ?: surahs.firstOrNull()
    val currentReciter = reciters.firstOrNull { it.id == activeProject.reciterId } ?: DefaultReciters.list.first()

    QuranVideoMakerTheme(themeStyle = themeStyle) {
        NavHost(
            navController = navController,
            startDestination = AppRoutes.HOME
        ) {
            composable(AppRoutes.HOME) {
                HomeScreen(
                    hasLastProject = hasLastProject,
                    lastProject = activeProject,
                    lastSurah = currentSurah,
                    lastReciter = currentReciter,
                    currentTheme = themeStyle,
                    savedProjectsCount = savedProjects.size,
                    batchCount = batchQueue.size,
                    onContinueLastProject = {
                        navController.navigate(AppRoutes.editor(1))
                    },
                    onCreateNewProject = {
                        viewModel.createNewProject()
                        navController.navigate(AppRoutes.editor(2))
                    },
                    onOpenSectionInEditor = { sectionIndex ->
                        navController.navigate(AppRoutes.editor(sectionIndex))
                    },
                    onOpenMyProjects = {
                        navController.navigate(AppRoutes.MY_PROJECTS)
                    },
                    onOpenBatchQueue = {
                        navController.navigate(AppRoutes.BATCH_QUEUE)
                    },
                    onCycleTheme = {
                        val all = AppThemeStyle.entries
                        val next = all[(all.indexOf(themeStyle) + 1) % all.size]
                        viewModel.applyThemeStyle(next)
                    }
                )
            }

            composable(
                route = AppRoutes.EDITOR,
                arguments = listOf(navArgument("section") { type = NavType.IntType; defaultValue = 1 })
            ) { backStackEntry ->
                val initialSection = backStackEntry.arguments?.getInt("section") ?: 1
                EditorScreen(
                    initialSection = initialSection,
                    project = activeProject,
                    surahs = surahs,
                    currentSurah = currentSurah,
                    reciters = reciters,
                    selectedReciter = currentReciter,
                    savedProjects = savedProjects,
                    lastSavedTimestamp = lastSavedTimestamp,
                    currentTheme = themeStyle,
                    lines = lines,
                    playbackState = playbackState,
                    audioLoadState = audioLoadState,
                    exportState = exportState,
                    batchCount = batchQueue.size,
                    isCurrentSurahCached = viewModel.isCurrentSurahCached(),
                    statusBanner = statusBanner,
                    onConsumeStatusBanner = viewModel::consumeStatusBanner,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenBatchQueue = { navController.navigate(AppRoutes.BATCH_QUEUE) },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekToGlobalPosition,
                    onJumpAyah = viewModel::jumpToAyahLine,
                    onRetryAudio = { viewModel.loadAudioIfNeeded(force = true) },
                    onChangePlaybackSpeed = viewModel::setPlaybackSpeed,
                    onSelectReciter = viewModel::selectReciter,
                    onToggleFavoriteReciter = viewModel::toggleFavoriteReciter,
                    onSelectSurah = viewModel::selectSurah,
                    onSelectAyahRange = viewModel::selectAyahRange,
                    onToggleBismillah = viewModel::toggleBismillahIntro,
                    onUpdateProject = { transform -> viewModel.updateProject(reloadAudioIfNeeded = false, transform = transform) },
                    onImportUserMedia = viewModel::importUserMedia,
                    onSaveAsNamedProject = viewModel::saveAsNamedProject,
                    onOpenSavedProject = viewModel::openSavedProject,
                    onDeleteSavedProject = viewModel::deleteSavedProject,
                    onSelectTheme = viewModel::applyThemeStyle,
                    onSelectResolution = { res ->
                        viewModel.updateProject { it.copy(exportResolution = res) }
                    },
                    onStartExport = viewModel::startVideoExport,
                    onCancelExport = viewModel::cancelVideoExport,
                    onSaveToGallery = viewModel::saveExportedVideoToGallery,
                    onShareVideo = viewModel::shareExportedVideo
                )
            }

            composable(AppRoutes.MY_PROJECTS) {
                MyProjectsScreen(
                    projects = savedProjects,
                    activeProjectId = activeProject.id,
                    surahs = surahs,
                    reciters = reciters,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateNewProject = {
                        viewModel.createNewProject()
                        navController.navigate(AppRoutes.editor(2))
                    },
                    onOpenProject = { proj ->
                        viewModel.openSavedProject(proj)
                        navController.navigate(AppRoutes.editor(1))
                    },
                    onDeleteProject = viewModel::deleteSavedProject
                )
            }

            composable(AppRoutes.BATCH_QUEUE) {
                BatchQueueScreen(
                    surahs = surahs,
                    queue = batchQueue,
                    isRunning = isBatchRunning,
                    defaultResolution = activeProject.exportResolution,
                    onNavigateBack = { navController.popBackStack() },
                    onAddSurah = { surah -> viewModel.addSurahToBatchQueue(surah) },
                    onRemoveItem = viewModel::removeBatchJob,
                    onClearCompleted = viewModel::clearCompletedBatchJobs,
                    onStartBatch = viewModel::startBatchProcessing,
                    onCancelBatch = viewModel::cancelBatchProcessing,
                    onSaveItemToGallery = viewModel::saveBatchItemToGallery,
                    onShareItem = viewModel::shareBatchItem
                )
            }
        }
    }
}
