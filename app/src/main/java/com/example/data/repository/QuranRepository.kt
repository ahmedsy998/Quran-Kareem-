package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteReciterEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.ProjectEntity
import com.example.data.local.QuranAssetsLoader
import com.example.data.remote.AudioCacheManager
import com.example.data.remote.Mp3QuranService
import com.example.data.remote.NetworkModule
import com.example.data.remote.PreparedAudioPackage
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.AudioSourceType
import com.example.domain.model.DefaultReciters
import com.example.domain.model.ExportResolution
import com.example.domain.model.Reciter
import com.example.domain.model.Surah
import com.example.domain.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class QuranRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val projectDao = database.projectDao()
    private val preferencesRepository = PreferencesRepository(context)
    private val quranAssetsLoader = QuranAssetsLoader(context)
    private val mp3QuranService: Mp3QuranService = NetworkModule.mp3QuranService
    private val audioCacheManager = AudioCacheManager(
        context = context,
        okHttpClient = NetworkModule.okHttpClient,
        mp3QuranService = mp3QuranService
    )

    private val projectAdapter = NetworkModule.moshi.adapter(VideoProject::class.java)
    private val userMediaDir = File(context.filesDir, "user_media").apply { mkdirs() }

    val themeStyleFlow: Flow<AppThemeStyle> = preferencesRepository.themeStyleFlow
    val preferredResolutionFlow: Flow<ExportResolution> = preferencesRepository.preferredResolutionFlow
    val favoriteReciterIdsFlow: Flow<Set<String>> = projectDao.observeFavoriteReciterIds().map { it.toSet() }

    val savedProjectsFlow: Flow<List<VideoProject>> = projectDao.observeAllProjects().map { entities ->
        entities.mapNotNull { entity ->
            runCatching { projectAdapter.fromJson(entity.configJson) }.getOrNull()
        }
    }

    suspend fun getAllSurahs(): List<Surah> = quranAssetsLoader.getAllSurahs()

    suspend fun getSurah(number: Int): Surah = quranAssetsLoader.getSurah(number)

    suspend fun saveProject(project: VideoProject) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val json = projectAdapter.toJson(updated)
        projectDao.upsertProject(
            ProjectEntity(
                id = updated.id,
                name = updated.name,
                surahNumber = updated.surahNumber,
                reciterId = updated.reciterId,
                updatedAt = updated.updatedAt,
                configJson = json
            )
        )
        preferencesRepository.setActiveProjectId(updated.id)
    }

    suspend fun loadInitialOrLastProject(): VideoProject? {
        val activeId = preferencesRepository.getActiveProjectId()
        val entity = (if (activeId != null) projectDao.getProjectById(activeId) else null)
            ?: projectDao.getLatestProject()
            ?: return null
        return runCatching { projectAdapter.fromJson(entity.configJson) }.getOrNull()
    }

    suspend fun getProjectById(id: String): VideoProject? {
        val entity = projectDao.getProjectById(id) ?: return null
        return runCatching { projectAdapter.fromJson(entity.configJson) }.getOrNull()
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        preferencesRepository.setThemeStyle(style)
    }

    suspend fun setPreferredResolution(resolution: ExportResolution) {
        preferencesRepository.setPreferredResolution(resolution)
    }

    suspend fun toggleFavoriteReciter(reciterId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            projectDao.removeFavoriteReciter(reciterId)
        } else {
            projectDao.addFavoriteReciter(FavoriteReciterEntity(reciterId = reciterId))
        }
    }

    suspend fun fetchAdditionalMp3QuranReciters(): List<Reciter> = withContext(Dispatchers.IO) {
        val existingMp3Ids = DefaultReciters.list.mapNotNull { it.mp3QuranReadId }.toSet()
        runCatching {
            val response = mp3QuranService.getReciters(language = "ar")
            response.reciters
                .filter { it.id !in existingMp3Ids && it.moshaf.isNotEmpty() }
                .take(40)
                .map { dto ->
                    val moshaf = dto.moshaf.first()
                    Reciter(
                        id = "mp3q_${dto.id}",
                        arabicName = dto.name.trim(),
                        styleLabel = moshaf.name.ifBlank { "مصحف كامل" },
                        sourceType = AudioSourceType.MP3QURAN_FULL,
                        editionOrFolder = "mp3q_${dto.id}",
                        mp3QuranReadId = dto.id,
                        mp3QuranServerUrl = moshaf.server
                    )
                }
        }.getOrDefault(emptyList())
    }

    suspend fun prepareAudio(
        reciter: Reciter,
        surah: Surah,
        startAyah: Int,
        endAyah: Int,
        showBismillahIntro: Boolean,
        onProgress: (Float, String) -> Unit
    ): PreparedAudioPackage {
        return audioCacheManager.prepareAudio(
            reciter = reciter,
            surah = surah,
            startAyah = startAyah,
            endAyah = endAyah,
            showBismillahIntro = showBismillahIntro,
            onProgress = onProgress
        )
    }

    fun isSurahCached(reciter: Reciter, surahNumber: Int, startAyah: Int, endAyah: Int): Boolean {
        return audioCacheManager.isSurahCached(reciter, surahNumber, startAyah, endAyah)
    }

    /**
     * Copies a user-picked image or video URI into internal app storage so that
     * restarting the device or revoking temporary photo picker grants never breaks the project.
     */
    suspend fun importUserMediaToInternalStorage(uri: Uri, isVideo: Boolean): String =
        withContext(Dispatchers.IO) {
            val ext = if (isVideo) "mp4" else "jpg"
            val dest = File(userMediaDir, "user_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("تعذر قراءة الملف المختار")
            dest.absolutePath
        }
}
