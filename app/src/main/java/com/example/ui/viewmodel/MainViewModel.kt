package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.QuranAssetsLoader
import com.example.data.remote.AudioModePayload
import com.example.data.remote.PreparedAudioPackage
import com.example.data.repository.QuranRepository
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.AspectRatioPreset
import com.example.domain.model.BackgroundType
import com.example.domain.model.BatchJobItem
import com.example.domain.model.BatchJobStatus
import com.example.domain.model.DefaultReciters
import com.example.domain.model.ExportResolution
import com.example.domain.model.Reciter
import com.example.domain.model.SingleLineOverflowMode
import com.example.domain.model.Surah
import com.example.domain.model.SyncedAyahLine
import com.example.domain.model.TextLayerStyle
import com.example.domain.model.VideoProject
import com.example.util.ArabicTextRenderer
import com.example.util.MediaExportEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface AudioLoadState {
    data object Idle : AudioLoadState
    data class Loading(val progress: Float, val message: String) : AudioLoadState
    data class Ready(val isEstimatedTiming: Boolean) : AudioLoadState
    data class Error(val message: String) : AudioLoadState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progressPercent: Int, val stageLabel: String) : ExportUiState
    data class Completed(val file: File, val savedToGallery: Boolean = false) : ExportUiState
    data class Failed(val message: String) : ExportUiState
}

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val currentLineIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val currentOffsetInAyahMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val exportEngine = MediaExportEngine(application)

    val audioPlayer: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    private val _extraReciters = MutableStateFlow<List<Reciter>>(emptyList())
    private val _favoriteIds = repository.favoriteReciterIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val reciters: StateFlow<List<Reciter>> = combine(
        _extraReciters,
        _favoriteIds
    ) { extra, favs ->
        (DefaultReciters.list + extra).map { r -> r.copy(isFavorite = r.id in favs) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DefaultReciters.list)

    val savedProjects: StateFlow<List<VideoProject>> = repository.savedProjectsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appThemeStyle: StateFlow<AppThemeStyle> = repository.themeStyleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeStyle.EMERALD_GOLD)

    private val _activeProject = MutableStateFlow(VideoProject())
    val activeProject: StateFlow<VideoProject> = _activeProject.asStateFlow()

    private val _hasLastSavedProject = MutableStateFlow(false)
    val hasLastSavedProject: StateFlow<Boolean> = _hasLastSavedProject.asStateFlow()

    private val _lastSavedTimestamp = MutableStateFlow<Long?>(null)
    val lastSavedTimestamp: StateFlow<Long?> = _lastSavedTimestamp.asStateFlow()

    private val _previewLines = MutableStateFlow<List<SyncedAyahLine>>(emptyList())
    val previewLines: StateFlow<List<SyncedAyahLine>> = _previewLines.asStateFlow()

    private val _audioLoadState = MutableStateFlow<AudioLoadState>(AudioLoadState.Idle)
    val audioLoadState: StateFlow<AudioLoadState> = _audioLoadState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _batchQueue = MutableStateFlow<List<BatchJobItem>>(emptyList())
    val batchQueue: StateFlow<List<BatchJobItem>> = _batchQueue.asStateFlow()

    private val _isBatchRunning = MutableStateFlow(false)
    val isBatchRunning: StateFlow<Boolean> = _isBatchRunning.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private var currentAudioPackage: PreparedAudioPackage? = null
    private var autoSaveJob: Job? = null
    private var audioPrepareJob: Job? = null
    private var tickerJob: Job? = null
    private var activeExportHandle: MediaExportEngine.ExportHandle? = null
    private var batchJob: Job? = null
    private val batchExportHandles = mutableListOf<MediaExportEngine.ExportHandle>()

    private var lastAudioSignature: String? = null

    init {
        setupPlayerListener()
        viewModelScope.launch {
            val loadedSurahs = repository.getAllSurahs()
            _surahs.value = loadedSurahs

            val restored = repository.loadInitialOrLastProject()
            if (restored != null) {
                _hasLastSavedProject.value = true
                _activeProject.value = restored
                _lastSavedTimestamp.value = restored.updatedAt
            } else {
                val initial = VideoProject()
                _activeProject.value = initial
                repository.saveProject(initial)
                _hasLastSavedProject.value = true
                _lastSavedTimestamp.value = initial.updatedAt
            }
            rebuildOfflinePreviewLines(_activeProject.value)
            loadAudioIfNeeded(_activeProject.value, force = true)
        }

        viewModelScope.launch {
            val extra = repository.fetchAdditionalMp3QuranReciters()
            if (extra.isNotEmpty()) {
                _extraReciters.value = extra
            }
        }
    }

    private fun setupPlayerListener() {
        audioPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startPositionTicker()
                } else {
                    tickerJob?.cancel()
                    syncPlaybackPositionNow()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncPlaybackPositionNow()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    audioPlayer.pause()
                    audioPlayer.seekTo(0, 0L)
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            currentLineIndex = 0,
                            currentPositionMs = 0L,
                            currentOffsetInAyahMs = 0L
                        )
                    }
                }
            }
        })
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && audioPlayer.isPlaying) {
                syncPlaybackPositionNow()
                delay(40L)
            }
        }
    }

    private fun syncPlaybackPositionNow() {
        val pkg = currentAudioPackage ?: return
        val lines = pkg.lines
        if (lines.isEmpty()) return

        when (pkg.payload) {
            is AudioModePayload.PerAyahFiles -> {
                val itemIdx = audioPlayer.currentMediaItemIndex.coerceIn(0, lines.lastIndex)
                val line = lines[itemIdx]
                val itemPos = audioPlayer.currentPosition.coerceAtLeast(0L)
                val globalPos = (line.startMs + itemPos).coerceIn(0L, pkg.totalDurationMs)
                _playbackState.update {
                    it.copy(
                        currentLineIndex = itemIdx,
                        currentPositionMs = globalPos,
                        currentOffsetInAyahMs = itemPos,
                        totalDurationMs = pkg.totalDurationMs
                    )
                }
            }
            is AudioModePayload.FullSurahFile -> {
                val pos = audioPlayer.currentPosition.coerceIn(0L, pkg.totalDurationMs)
                val idx = lines.indexOfFirst { pos >= it.startMs && pos < it.endMs }
                    .let { if (it >= 0) it else if (pos >= lines.last().endMs) lines.lastIndex else 0 }
                val line = lines[idx]
                val offsetInLine = (pos - line.startMs).coerceAtLeast(0L)
                _playbackState.update {
                    it.copy(
                        currentLineIndex = idx,
                        currentPositionMs = pos,
                        currentOffsetInAyahMs = offsetInLine,
                        totalDurationMs = pkg.totalDurationMs
                    )
                }
            }
        }
    }

    fun currentReciter(): Reciter {
        val id = _activeProject.value.reciterId
        return reciters.value.firstOrNull { it.id == id } ?: DefaultReciters.list.first()
    }

    fun currentSurah(): Surah? {
        val num = _activeProject.value.surahNumber
        return _surahs.value.firstOrNull { it.number == num } ?: _surahs.value.firstOrNull()
    }

    fun updateProject(
        reloadAudioIfNeeded: Boolean = false,
        transform: (VideoProject) -> VideoProject
    ) {
        val updated = transform(_activeProject.value).copy(updatedAt = System.currentTimeMillis())
        _activeProject.value = updated
        rebuildOfflinePreviewLines(updated)
        scheduleAutoSave(updated)
        if (reloadAudioIfNeeded) {
            loadAudioIfNeeded(updated)
        }
    }

    private fun scheduleAutoSave(project: VideoProject) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(250L)
            repository.saveProject(project)
            _hasLastSavedProject.value = true
            _lastSavedTimestamp.value = project.updatedAt
        }
    }

    fun createNewProject(projectName: String = "مشروع تلاوة جديد") {
        val currentTheme = appThemeStyle.value
        val fresh = VideoProject(
            id = UUID.randomUUID().toString(),
            name = projectName,
            themeStyle = currentTheme
        )
        _activeProject.value = fresh
        rebuildOfflinePreviewLines(fresh)
        viewModelScope.launch {
            repository.saveProject(fresh)
            _hasLastSavedProject.value = true
            _lastSavedTimestamp.value = fresh.updatedAt
        }
        loadAudioIfNeeded(fresh, force = true)
    }

    fun saveAsNamedProject(customName: String) {
        val trimmed = customName.trim().ifBlank { "مشروع ${currentSurah()?.arabicName ?: "جديد"}" }
        val cloned = _activeProject.value.copy(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            updatedAt = System.currentTimeMillis()
        )
        _activeProject.value = cloned
        viewModelScope.launch {
            repository.saveProject(cloned)
            _lastSavedTimestamp.value = cloned.updatedAt
            _statusBannerMessage.value = "تم حفظ المشروع: $trimmed"
        }
    }

    fun openSavedProject(project: VideoProject) {
        _activeProject.value = project
        rebuildOfflinePreviewLines(project)
        viewModelScope.launch {
            repository.saveProject(project)
            repository.setThemeStyle(project.themeStyle)
            _lastSavedTimestamp.value = project.updatedAt
        }
        loadAudioIfNeeded(project, force = true)
    }

    fun deleteSavedProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _statusBannerMessage.value = "تم حذف المشروع"
        }
    }

    fun applyThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch {
            repository.setThemeStyle(style)
        }
        updateProject { it.copy(themeStyle = style) }
    }

    fun toggleFavoriteReciter(reciter: Reciter) {
        viewModelScope.launch {
            repository.toggleFavoriteReciter(reciter.id, reciter.isFavorite)
        }
    }

    fun selectReciter(reciter: Reciter) {
        updateProject(reloadAudioIfNeeded = true) { it.copy(reciterId = reciter.id) }
    }

    fun selectSurah(surah: Surah) {
        val defaultEnd = surah.ayahCount.coerceAtMost(10)
        updateProject(reloadAudioIfNeeded = true) {
            it.copy(
                surahNumber = surah.number,
                startAyah = 1,
                endAyah = defaultEnd,
                name = "تلاوة ${surah.arabicName}"
            )
        }
    }

    fun selectAyahRange(startAyah: Int, endAyah: Int) {
        val surah = currentSurah() ?: return
        val s = startAyah.coerceIn(1, surah.ayahCount)
        val e = endAyah.coerceIn(s, surah.ayahCount)
        updateProject(reloadAudioIfNeeded = true) {
            it.copy(startAyah = s, endAyah = e)
        }
    }

    fun toggleBismillahIntro(enabled: Boolean) {
        updateProject(reloadAudioIfNeeded = true) {
            it.copy(showBismillahIntro = enabled)
        }
    }

    fun importUserMedia(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.importUserMediaToInternalStorage(uri, isVideo)
            }.onSuccess { localPath ->
                updateProject {
                    it.copy(
                        backgroundType = if (isVideo) BackgroundType.USER_VIDEO else BackgroundType.USER_IMAGE,
                        userMediaLocalPath = localPath
                    )
                }
                _statusBannerMessage.value = if (isVideo) "تم اعتماد خلفية الفيديو بنجاح" else "تم اعتماد الصورة الخلفية بنجاح"
            }.onFailure { err ->
                _statusBannerMessage.value = err.message ?: "تعذر استيراد الملف"
            }
        }
    }

    private fun rebuildOfflinePreviewLines(project: VideoProject) {
        val surah = _surahs.value.firstOrNull { it.number == project.surahNumber } ?: return
        val safeStart = project.startAyah.coerceIn(1, surah.ayahCount)
        val safeEnd = project.endAyah.coerceIn(safeStart, surah.ayahCount)
        val includeBism = project.showBismillahIntro && safeStart == 1 && surah.number != 1 && surah.number != 9

        val list = mutableListOf<SyncedAyahLine>()
        var idx = 0
        var cursor = 0L
        if (includeBism) {
            list.add(
                SyncedAyahLine(
                    index = idx++,
                    numberInSurah = 0,
                    absoluteNumber = 1,
                    text = QuranAssetsLoader.BISMILLAH_TEXT,
                    isBismillah = true,
                    audioSourceUri = null,
                    startMs = cursor,
                    endMs = cursor + 3000L,
                    wordTimings = ArabicTextRenderer.buildWordTimings(
                        text = QuranAssetsLoader.BISMILLAH_TEXT,
                        ayahDurationMs = 3000L
                    )
                )
            )
            cursor += 3000L
        }
        surah.ayahs.subList(safeStart - 1, safeEnd).forEach { ayah ->
            list.add(
                SyncedAyahLine(
                    index = idx++,
                    numberInSurah = ayah.numberInSurah,
                    absoluteNumber = ayah.absoluteNumber,
                    text = ayah.text,
                    isBismillah = false,
                    audioSourceUri = null,
                    startMs = cursor,
                    endMs = cursor + 4000L,
                    wordTimings = ArabicTextRenderer.buildWordTimings(
                        text = ayah.text,
                        ayahDurationMs = 4000L
                    )
                )
            )
            cursor += 4000L
        }
        if (currentAudioPackage == null || currentAudioPackage?.surah?.number != surah.number) {
            _previewLines.value = list
            _playbackState.update {
                it.copy(
                    currentLineIndex = it.currentLineIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0)),
                    totalDurationMs = cursor
                )
            }
        }
    }

    fun loadAudioIfNeeded(project: VideoProject = _activeProject.value, force: Boolean = false) {
        val signature = "${project.reciterId}|${project.surahNumber}|${project.startAyah}|${project.endAyah}|${project.showBismillahIntro}"
        if (!force && signature == lastAudioSignature && currentAudioPackage != null) return
        lastAudioSignature = signature

        audioPrepareJob?.cancel()
        audioPrepareJob = viewModelScope.launch {
            audioPlayer.pause()
            val surah = _surahs.value.firstOrNull { it.number == project.surahNumber }
                ?: repository.getSurah(project.surahNumber)
            val reciter = reciters.value.firstOrNull { it.id == project.reciterId }
                ?: DefaultReciters.list.first()

            _audioLoadState.value = AudioLoadState.Loading(0.05f, "جاري تجهيز التلاوة...")
            runCatching {
                repository.prepareAudio(
                    reciter = reciter,
                    surah = surah,
                    startAyah = project.startAyah,
                    endAyah = project.endAyah,
                    showBismillahIntro = project.showBismillahIntro
                ) { fraction, msg ->
                    _audioLoadState.value = AudioLoadState.Loading(fraction, msg)
                }
            }.onSuccess { pkg ->
                currentAudioPackage = pkg
                _previewLines.value = pkg.lines
                withContext(Dispatchers.Main) {
                    applyPackageToExoPlayer(pkg)
                }
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        currentLineIndex = 0,
                        currentPositionMs = 0L,
                        currentOffsetInAyahMs = 0L,
                        totalDurationMs = pkg.totalDurationMs
                    )
                }
                _audioLoadState.value = AudioLoadState.Ready(pkg.isEstimatedTiming)
            }.onFailure { err ->
                _audioLoadState.value = AudioLoadState.Error(
                    err.message ?: "تعذر تحميل التلاوة، تأكد من اتصال الإنترنت"
                )
            }
        }
    }

    private fun applyPackageToExoPlayer(pkg: PreparedAudioPackage) {
        audioPlayer.stop()
        audioPlayer.clearMediaItems()
        when (val payload = pkg.payload) {
            is AudioModePayload.PerAyahFiles -> {
                val items = payload.files.map { f -> MediaItem.fromUri(Uri.fromFile(f)) }
                audioPlayer.setMediaItems(items)
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
                audioPlayer.setMediaItem(clipped)
            }
        }
        audioPlayer.playbackParameters = PlaybackParameters(_playbackState.value.playbackSpeed)
        audioPlayer.prepare()
    }

    fun togglePlayPause() {
        if (currentAudioPackage == null) {
            loadAudioIfNeeded(force = true)
            return
        }
        if (audioPlayer.isPlaying) {
            audioPlayer.pause()
        } else {
            if (audioPlayer.playbackState == Player.STATE_ENDED || audioPlayer.playbackState == Player.STATE_IDLE) {
                audioPlayer.prepare()
                audioPlayer.seekTo(0, 0L)
            }
            audioPlayer.play()
        }
    }

    fun seekToGlobalPosition(targetMs: Long) {
        val pkg = currentAudioPackage ?: return
        val clamped = targetMs.coerceIn(0L, pkg.totalDurationMs)
        when (pkg.payload) {
            is AudioModePayload.PerAyahFiles -> {
                val idx = pkg.lines.indexOfFirst { clamped >= it.startMs && clamped < it.endMs }
                    .let { if (it >= 0) it else pkg.lines.lastIndex.coerceAtLeast(0) }
                val line = pkg.lines.getOrNull(idx) ?: return
                val offsetInLine = (clamped - line.startMs).coerceAtLeast(0L)
                audioPlayer.seekTo(idx, offsetInLine)
                _playbackState.update {
                    it.copy(
                        currentLineIndex = idx,
                        currentPositionMs = clamped,
                        currentOffsetInAyahMs = offsetInLine
                    )
                }
            }
            is AudioModePayload.FullSurahFile -> {
                audioPlayer.seekTo(clamped)
                syncPlaybackPositionNow()
            }
        }
    }

    fun jumpToAyahLine(index: Int) {
        val pkg = currentAudioPackage
        val lines = _previewLines.value
        val target = lines.getOrNull(index) ?: return
        if (pkg != null) {
            seekToGlobalPosition(target.startMs)
        } else {
            _playbackState.update {
                it.copy(
                    currentLineIndex = index,
                    currentPositionMs = target.startMs,
                    currentOffsetInAyahMs = 0L
                )
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safe = speed.coerceIn(0.75f, 1.5f)
        audioPlayer.playbackParameters = PlaybackParameters(safe)
        _playbackState.update { it.copy(playbackSpeed = safe) }
    }

    fun isCurrentSurahCached(): Boolean {
        val p = _activeProject.value
        return repository.isSurahCached(currentReciter(), p.surahNumber, p.startAyah, p.endAyah)
    }

    fun startVideoExport() {
        if (_exportState.value is ExportUiState.Exporting) return
        val project = _activeProject.value
        val resolution = project.exportResolution

        audioPlayer.pause()
        viewModelScope.launch {
            try {
                _exportState.value = ExportUiState.Exporting(2, "جاري تحضير الصوتيات...")
                val surah = _surahs.value.firstOrNull { it.number == project.surahNumber }
                    ?: repository.getSurah(project.surahNumber)
                val reciter = reciters.value.firstOrNull { it.id == project.reciterId }
                    ?: DefaultReciters.list.first()

                val pkg = currentAudioPackage?.takeIf {
                    it.reciter.id == reciter.id &&
                        it.surah.number == surah.number &&
                        it.lines.isNotEmpty()
                } ?: repository.prepareAudio(
                    reciter = reciter,
                    surah = surah,
                    startAyah = project.startAyah,
                    endAyah = project.endAyah,
                    showBismillahIntro = project.showBismillahIntro
                ) { fraction, msg ->
                    _exportState.value = ExportUiState.Exporting((fraction * 20).toInt(), msg)
                }

                _exportState.value = ExportUiState.Exporting(22, "جاري تصيير الفيديو بجودة ${resolution.badge}...")
                val fileName = "quran_s${surah.number}_${project.startAyah}_${project.endAyah}_${System.currentTimeMillis()}"
                val outputFile = exportEngine.exportVideo(
                    project = project,
                    audioPackage = pkg,
                    resolution = resolution,
                    outputFileName = fileName,
                    onProgress = { pct ->
                        val mapped = (22 + pct * 0.78f).toInt().coerceIn(22, 100)
                        _exportState.value = ExportUiState.Exporting(mapped, "جاري تصيير الفيديو ($pct%)...")
                    },
                    onRegisterCancel = { handle ->
                        activeExportHandle = handle
                    }
                )
                activeExportHandle = null
                _exportState.value = ExportUiState.Completed(outputFile, savedToGallery = false)
            } catch (t: Throwable) {
                activeExportHandle = null
                _exportState.value = ExportUiState.Failed(
                    t.message ?: "حدث خطأ أثناء تصدير الفيديو"
                )
            }
        }
    }

    fun cancelVideoExport() {
        activeExportHandle?.cancel()
        activeExportHandle = null
        _exportState.value = ExportUiState.Idle
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }

    fun saveExportedVideoToGallery(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val surahName = currentSurah()?.arabicName ?: "quran"
            val uri = exportEngine.saveVideoToGallery(file, surahName)
            withContext(Dispatchers.Main) {
                if (uri != null) {
                    val current = _exportState.value
                    if (current is ExportUiState.Completed) {
                        _exportState.value = current.copy(savedToGallery = true)
                    }
                    _statusBannerMessage.value = "تم الحفظ في المعرض (Movies/QuranVideoMaker)"
                } else {
                    _statusBannerMessage.value = "تعذر الحفظ في المعرض"
                }
            }
        }
    }

    fun shareExportedVideo(file: File) {
        exportEngine.shareVideo(file)
    }

    // ---- Batch Processing Queue ----
    fun addSurahToBatchQueue(surah: Surah, startAyah: Int = 1, endAyah: Int = surah.ayahCount) {
        val reciter = currentReciter()
        val item = BatchJobItem(
            id = UUID.randomUUID().toString(),
            surahNumber = surah.number,
            surahName = surah.arabicName,
            startAyah = startAyah.coerceIn(1, surah.ayahCount),
            endAyah = endAyah.coerceIn(startAyah, surah.ayahCount),
            reciterName = reciter.arabicName
        )
        _batchQueue.update { it + item }
    }

    fun removeBatchJob(jobId: String) {
        _batchQueue.update { list -> list.filterNot { it.id == jobId } }
    }

    fun clearCompletedBatchJobs() {
        _batchQueue.update { list ->
            list.filter { it.status != BatchJobStatus.COMPLETED && it.status != BatchJobStatus.CANCELLED }
        }
    }

    fun startBatchProcessing(resolution: ExportResolution) {
        if (_isBatchRunning.value) return
        val pendingIds = _batchQueue.value
            .filter { it.status == BatchJobStatus.QUEUED || it.status == BatchJobStatus.FAILED }
            .map { it.id }
        if (pendingIds.isEmpty()) return

        audioPlayer.pause()
        val baseProject = _activeProject.value
        val reciter = currentReciter()
        _isBatchRunning.value = true
        batchExportHandles.clear()

        // Bounded concurrency (2 simultaneous Media3 Transformer jobs for safe hardware codec usage)
        val semaphore = Semaphore(2)
        batchJob = viewModelScope.launch {
            try {
                val jobs = _batchQueue.value.filter { it.id in pendingIds }
                jobs.map { item ->
                    async {
                        semaphore.withPermit {
                            if (!isActive) return@withPermit
                            updateBatchItem(item.id) {
                                it.copy(status = BatchJobStatus.DOWNLOADING, progressPercent = 5, errorMessage = null)
                            }
                            runCatching {
                                val surah = _surahs.value.firstOrNull { it.number == item.surahNumber }
                                    ?: repository.getSurah(item.surahNumber)
                                val itemProject = baseProject.copy(
                                    surahNumber = surah.number,
                                    startAyah = item.startAyah,
                                    endAyah = item.endAyah,
                                    exportResolution = resolution
                                )
                                val pkg = repository.prepareAudio(
                                    reciter = reciter,
                                    surah = surah,
                                    startAyah = item.startAyah,
                                    endAyah = item.endAyah,
                                    showBismillahIntro = itemProject.showBismillahIntro
                                ) { fraction, _ ->
                                    updateBatchItem(item.id) {
                                        it.copy(progressPercent = (fraction * 25).toInt())
                                    }
                                }
                                updateBatchItem(item.id) {
                                    it.copy(status = BatchJobStatus.RENDERING, progressPercent = 25)
                                }
                                val outFile = exportEngine.exportVideo(
                                    project = itemProject,
                                    audioPackage = pkg,
                                    resolution = resolution,
                                    outputFileName = "batch_s${surah.number}_${System.currentTimeMillis()}",
                                    onProgress = { pct ->
                                        updateBatchItem(item.id) {
                                            it.copy(progressPercent = (25 + pct * 0.75f).toInt().coerceIn(25, 99))
                                        }
                                    },
                                    onRegisterCancel = { handle ->
                                        synchronized(batchExportHandles) { batchExportHandles.add(handle) }
                                    }
                                )
                                updateBatchItem(item.id) {
                                    it.copy(
                                        status = BatchJobStatus.COMPLETED,
                                        progressPercent = 100,
                                        outputFilePath = outFile.absolutePath
                                    )
                                }
                            }.onFailure { err ->
                                updateBatchItem(item.id) {
                                    it.copy(
                                        status = BatchJobStatus.FAILED,
                                        errorMessage = err.message ?: "فشل التصيير"
                                    )
                                }
                            }
                        }
                    }
                }.awaitAll()
            } finally {
                _isBatchRunning.value = false
            }
        }
    }

    fun cancelBatchProcessing() {
        batchJob?.cancel()
        synchronized(batchExportHandles) {
            batchExportHandles.forEach { it.cancel() }
            batchExportHandles.clear()
        }
        _batchQueue.update { list ->
            list.map {
                if (it.status == BatchJobStatus.DOWNLOADING || it.status == BatchJobStatus.RENDERING || it.status == BatchJobStatus.QUEUED) {
                    it.copy(status = BatchJobStatus.CANCELLED)
                } else it
            }
        }
        _isBatchRunning.value = false
    }

    fun saveBatchItemToGallery(item: BatchJobItem) {
        val path = item.outputFilePath ?: return
        val file = File(path)
        if (!file.exists()) return
        viewModelScope.launch(Dispatchers.IO) {
            val uri = exportEngine.saveVideoToGallery(file, item.surahName)
            withContext(Dispatchers.Main) {
                if (uri != null) {
                    updateBatchItem(item.id) { it.copy(savedToGallery = true) }
                    _statusBannerMessage.value = "تم حفظ ${item.surahName} في المعرض"
                }
            }
        }
    }

    fun shareBatchItem(item: BatchJobItem) {
        val path = item.outputFilePath ?: return
        val file = File(path)
        if (file.exists()) exportEngine.shareVideo(file)
    }

    private fun updateBatchItem(id: String, transform: (BatchJobItem) -> BatchJobItem) {
        _batchQueue.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun consumeStatusBanner() {
        _statusBannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        audioPlayer.release()
    }
}
