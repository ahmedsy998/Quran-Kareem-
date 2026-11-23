package com.example.domain.model

data class Ayah(
    val absoluteNumber: Int,
    val numberInSurah: Int,
    val text: String,
    val page: Int,
    val juz: Int
)

data class Surah(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val revelationType: String,
    val ayahs: List<Ayah>
) {
    val ayahCount: Int get() = ayahs.size
    val revelationTypeArabic: String
        get() = if (revelationType.equals("Meccan", ignoreCase = true)) "مكية" else "مدنية"
}

data class WordTiming(
    val index: Int,
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class SyncedAyahLine(
    val index: Int,
    val numberInSurah: Int,
    val absoluteNumber: Int,
    val text: String,
    val isBismillah: Boolean,
    val audioSourceUri: String?,
    val startMs: Long,
    val endMs: Long,
    val wordTimings: List<WordTiming> = emptyList()
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1L)

    fun activeWordIndexAt(offsetInAyahMs: Long): Int {
        if (wordTimings.isEmpty()) return 0
        val clamped = offsetInAyahMs.coerceAtLeast(0L)
        var active = 0
        for (i in 1 until wordTimings.size) {
            if (clamped >= wordTimings[i].startMs) {
                active = i
            } else {
                break
            }
        }
        return active
    }
}
