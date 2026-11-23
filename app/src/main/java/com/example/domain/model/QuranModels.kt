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

data class SyncedAyahLine(
    val index: Int,
    val numberInSurah: Int,
    val absoluteNumber: Int,
    val text: String,
    val isBismillah: Boolean,
    val audioSourceUri: String?,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1L)
}
