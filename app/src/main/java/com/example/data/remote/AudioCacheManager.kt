package com.example.data.remote

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.local.QuranAssetsLoader
import com.example.domain.model.AudioSourceType
import com.example.domain.model.Reciter
import com.example.domain.model.Surah
import com.example.domain.model.SyncedAyahLine
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

sealed interface AudioModePayload {
    data class PerAyahFiles(
        val files: List<File>
    ) : AudioModePayload

    data class FullSurahFile(
        val file: File,
        val clipStartMs: Long,
        val clipEndMs: Long
    ) : AudioModePayload
}

data class PreparedAudioPackage(
    val reciter: Reciter,
    val surah: Surah,
    val lines: List<SyncedAyahLine>,
    val totalDurationMs: Long,
    val payload: AudioModePayload,
    val isEstimatedTiming: Boolean = false
)

class AudioCacheManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val mp3QuranService: Mp3QuranService
) {
    private val audioRootDir = File(context.filesDir, "audio_cache").apply { mkdirs() }
    private val timingRootDir = File(context.filesDir, "timing_cache").apply { mkdirs() }

    suspend fun prepareAudio(
        reciter: Reciter,
        surah: Surah,
        startAyah: Int,
        endAyah: Int,
        showBismillahIntro: Boolean,
        onProgress: (Float, String) -> Unit
    ): PreparedAudioPackage = withContext(Dispatchers.IO) {
        val safeStart = startAyah.coerceIn(1, surah.ayahCount)
        val safeEnd = endAyah.coerceIn(safeStart, surah.ayahCount)

        if (reciter.supportsExactPerAyah) {
            preparePerAyah(reciter, surah, safeStart, safeEnd, showBismillahIntro, onProgress)
        } else {
            prepareFullSurahWithTiming(reciter, surah, safeStart, safeEnd, showBismillahIntro, onProgress)
        }
    }

    private suspend fun preparePerAyah(
        reciter: Reciter,
        surah: Surah,
        startAyah: Int,
        endAyah: Int,
        showBismillahIntro: Boolean,
        onProgress: (Float, String) -> Unit
    ): PreparedAudioPackage {
        val reciterDir = File(audioRootDir, reciter.id).apply { mkdirs() }
        val includeBismillah = showBismillahIntro && startAyah == 1 && surah.number != 1 && surah.number != 9

        data class PendingAyah(
            val numberInSurah: Int,
            val absoluteNumber: Int,
            val text: String,
            val isBismillah: Boolean,
            val url: String,
            val fallbackUrl: String?,
            val cacheFile: File
        )

        val pending = mutableListOf<PendingAyah>()
        if (includeBismillah) {
            val bismUrl = reciter.perAyahUrl(1, 1, 1)!!
            val fallback = if (reciter.sourceType == AudioSourceType.ISLAMIC_NETWORK_128) {
                "https://cdn.islamic.network/quran/audio/64/${reciter.editionOrFolder}/1.mp3"
            } else null
            pending.add(
                PendingAyah(
                    numberInSurah = 0,
                    absoluteNumber = 1,
                    text = QuranAssetsLoader.BISMILLAH_TEXT,
                    isBismillah = true,
                    url = bismUrl,
                    fallbackUrl = fallback,
                    cacheFile = File(reciterDir, "1_1_bism.mp3")
                )
            )
        }

        val selectedAyahs = surah.ayahs.subList(startAyah - 1, endAyah)
        for (ayah in selectedAyahs) {
            val url = reciter.perAyahUrl(surah.number, ayah.numberInSurah, ayah.absoluteNumber)!!
            val fallback = if (reciter.sourceType == AudioSourceType.ISLAMIC_NETWORK_128) {
                "https://cdn.islamic.network/quran/audio/64/${reciter.editionOrFolder}/${ayah.absoluteNumber}.mp3"
            } else null
            pending.add(
                PendingAyah(
                    numberInSurah = ayah.numberInSurah,
                    absoluteNumber = ayah.absoluteNumber,
                    text = ayah.text,
                    isBismillah = false,
                    url = url,
                    fallbackUrl = fallback,
                    cacheFile = File(reciterDir, "${surah.number}_${ayah.numberInSurah}.mp3")
                )
            )
        }

        val total = pending.size
        val lines = ArrayList<SyncedAyahLine>(total)
        val files = ArrayList<File>(total)
        var cursorMs = 0L

        pending.forEachIndexed { idx, item ->
            val progress = idx.toFloat() / total.coerceAtLeast(1)
            val label = if (item.isBismillah) "جاري تحميل البسملة..." else "جاري تحميل الآية ${item.numberInSurah} من $endAyah..."
            onProgress(progress, label)

            if (!item.cacheFile.exists() || item.cacheFile.length() < 512L) {
                downloadFileWithFallback(item.url, item.fallbackUrl, item.cacheFile)
            }

            val durMs = measureDurationMs(item.cacheFile).coerceAtLeast(800L)
            val startMs = cursorMs
            val endMs = cursorMs + durMs
            cursorMs = endMs

            files.add(item.cacheFile)
            lines.add(
                SyncedAyahLine(
                    index = idx,
                    numberInSurah = item.numberInSurah,
                    absoluteNumber = item.absoluteNumber,
                    text = item.text,
                    isBismillah = item.isBismillah,
                    audioSourceUri = Uri.fromFile(item.cacheFile).toString(),
                    startMs = startMs,
                    endMs = endMs
                )
            )
        }

        onProgress(1f, "جاهز للتشغيل")
        return PreparedAudioPackage(
            reciter = reciter,
            surah = surah,
            lines = lines,
            totalDurationMs = cursorMs.coerceAtLeast(1000L),
            payload = AudioModePayload.PerAyahFiles(files),
            isEstimatedTiming = false
        )
    }

    private suspend fun prepareFullSurahWithTiming(
        reciter: Reciter,
        surah: Surah,
        startAyah: Int,
        endAyah: Int,
        showBismillahIntro: Boolean,
        onProgress: (Float, String) -> Unit
    ): PreparedAudioPackage {
        val reciterDir = File(audioRootDir, reciter.id).apply { mkdirs() }
        val surahFile = File(reciterDir, "surah_${surah.number}.mp3")
        val url = reciter.fullSurahUrl(surah.number)
            ?: throw IOException("رابط التلاوة غير متوفر لهذا القارئ")

        if (!surahFile.exists() || surahFile.length() < 2048L) {
            onProgress(0.15f, "جاري تحميل سورة ${surah.arabicName}...")
            downloadFileWithProgress(url, surahFile) { fraction ->
                onProgress(0.15f + fraction * 0.70f, "جاري تحميل التلاوة ${(fraction * 100).toInt()}%")
            }
        }

        onProgress(0.90f, "جاري مطابقة توقيت الآيات...")
        val fullDurationMs = measureDurationMs(surahFile).coerceAtLeast(3000L)
        val timings = loadOrFetchTimings(reciter.mp3QuranReadId, surah.number)

        val selectedAyahs = surah.ayahs.subList(startAyah - 1, endAyah)
        val includeBismillah = showBismillahIntro && startAyah == 1 && surah.number != 1 && surah.number != 9

        val rawLines = mutableListOf<SyncedAyahLine>()
        var isEstimated = false

        if (timings.isNotEmpty()) {
            val timingMap = timings.associateBy { it.ayah }
            var idx = 0
            if (includeBismillah) {
                val bismTiming = timingMap[0]
                if (bismTiming != null && bismTiming.endTimeMs > bismTiming.startTimeMs) {
                    rawLines.add(
                        SyncedAyahLine(
                            index = idx++,
                            numberInSurah = 0,
                            absoluteNumber = 1,
                            text = QuranAssetsLoader.BISMILLAH_TEXT,
                            isBismillah = true,
                            audioSourceUri = Uri.fromFile(surahFile).toString(),
                            startMs = bismTiming.startTimeMs,
                            endMs = bismTiming.endTimeMs
                        )
                    )
                }
            }
            for (ayah in selectedAyahs) {
                val t = timingMap[ayah.numberInSurah]
                if (t != null && t.endTimeMs > t.startTimeMs) {
                    rawLines.add(
                        SyncedAyahLine(
                            index = idx++,
                            numberInSurah = ayah.numberInSurah,
                            absoluteNumber = ayah.absoluteNumber,
                            text = ayah.text,
                            isBismillah = false,
                            audioSourceUri = Uri.fromFile(surahFile).toString(),
                            startMs = t.startTimeMs,
                            endMs = t.endTimeMs
                        )
                    )
                }
            }
        }

        if (rawLines.isEmpty()) {
            // Graceful weighted proportional estimation across full surah duration
            isEstimated = true
            val allAyahs = surah.ayahs
            val totalWeight = allAyahs.sumOf { it.text.length.coerceAtLeast(10) }.toDouble()
            var cursor = 0L
            val allComputed = allAyahs.map { ayah ->
                val slice = ((ayah.text.length.coerceAtLeast(10) / totalWeight) * fullDurationMs).toLong()
                    .coerceAtLeast(800L)
                val s = cursor
                val e = (cursor + slice).coerceAtMost(fullDurationMs)
                cursor = e
                Triple(ayah, s, e)
            }
            val filtered = allComputed.filter { it.first.numberInSurah in startAyah..endAyah }
            filtered.forEachIndexed { i, (ayah, s, e) ->
                rawLines.add(
                    SyncedAyahLine(
                        index = i,
                        numberInSurah = ayah.numberInSurah,
                        absoluteNumber = ayah.absoluteNumber,
                        text = ayah.text,
                        isBismillah = false,
                        audioSourceUri = Uri.fromFile(surahFile).toString(),
                        startMs = s,
                        endMs = e
                    )
                )
            }
        }

        val clipStartMs = rawLines.first().startMs
        val clipEndMs = rawLines.last().endMs.coerceAtMost(fullDurationMs)
        val normalizedLines = rawLines.mapIndexed { index, line ->
            line.copy(
                index = index,
                startMs = (line.startMs - clipStartMs).coerceAtLeast(0L),
                endMs = (line.endMs - clipStartMs).coerceAtLeast(1L)
            )
        }
        val totalDur = (clipEndMs - clipStartMs).coerceAtLeast(1000L)

        onProgress(1f, "جاهز للتشغيل")
        return PreparedAudioPackage(
            reciter = reciter,
            surah = surah,
            lines = normalizedLines,
            totalDurationMs = totalDur,
            payload = AudioModePayload.FullSurahFile(
                file = surahFile,
                clipStartMs = clipStartMs,
                clipEndMs = clipEndMs
            ),
            isEstimatedTiming = isEstimated
        )
    }

    private suspend fun loadOrFetchTimings(readId: Int?, surahNumber: Int): List<AyahTimingDto> {
        if (readId == null) return emptyList()
        val cacheFile = File(timingRootDir, "${readId}_$surahNumber.json")
        val type = Types.newParameterizedType(List::class.java, AyahTimingDto::class.java)
        val adapter = NetworkModule.moshi.adapter<List<AyahTimingDto>>(type)

        if (cacheFile.exists() && cacheFile.length() > 10L) {
            runCatching {
                adapter.fromJson(cacheFile.readText())
            }.getOrNull()?.let { if (it.isNotEmpty()) return it }
        }

        return runCatching {
            val remote = mp3QuranService.getAyahTiming(surah = surahNumber, readId = readId)
            if (remote.isNotEmpty()) {
                cacheFile.writeText(adapter.toJson(remote))
            }
            remote
        }.getOrDefault(emptyList())
    }

    private fun downloadFileWithFallback(primaryUrl: String, fallbackUrl: String?, destination: File) {
        try {
            downloadFileWithProgress(primaryUrl, destination, null)
        } catch (primaryError: Exception) {
            if (fallbackUrl != null) {
                downloadFileWithProgress(fallbackUrl, destination, null)
            } else {
                throw primaryError
            }
        }
    }

    private fun downloadFileWithProgress(
        url: String,
        destination: File,
        onFraction: ((Float) -> Unit)?
    ) {
        val tempFile = File(destination.parentFile, "${destination.name}.part")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "QuranVideoMaker/1.0")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("فشل تحميل الملف الصوتي (${response.code})")
            }
            val body = response.body ?: throw IOException("استجابة صوتية فارغة")
            val totalBytes = body.contentLength()
            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (onFraction != null && totalBytes > 0L) {
                            onFraction((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
        }
        if (destination.exists()) destination.delete()
        if (!tempFile.renameTo(destination)) {
            tempFile.copyTo(destination, overwrite = true)
            tempFile.delete()
        }
    }

    fun isSurahCached(reciter: Reciter, surahNumber: Int, startAyah: Int, endAyah: Int): Boolean {
        val reciterDir = File(audioRootDir, reciter.id)
        if (!reciterDir.exists()) return false
        return if (reciter.supportsExactPerAyah) {
            (startAyah..endAyah).all { a ->
                val f = File(reciterDir, "${surahNumber}_$a.mp3")
                f.exists() && f.length() > 512L
            }
        } else {
            val f = File(reciterDir, "surah_$surahNumber.mp3")
            f.exists() && f.length() > 2048L
        }
    }

    private fun measureDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 2500L
        } catch (_: Exception) {
            2500L
        } finally {
            runCatching { retriever.release() }
        }
    }
}
