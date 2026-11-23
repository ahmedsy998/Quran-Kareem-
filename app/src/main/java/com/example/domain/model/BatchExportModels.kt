package com.example.domain.model

enum class BatchJobStatus(val arabicLabel: String) {
    QUEUED("في الانتظار"),
    DOWNLOADING("جاري تحميل الصوت"),
    RENDERING("جاري التصيير"),
    COMPLETED("مكتمل"),
    FAILED("فشل"),
    CANCELLED("ملغي")
}

data class BatchJobItem(
    val id: String,
    val surahNumber: Int,
    val surahName: String,
    val startAyah: Int,
    val endAyah: Int,
    val reciterName: String,
    val status: BatchJobStatus = BatchJobStatus.QUEUED,
    val progressPercent: Int = 0,
    val outputFilePath: String? = null,
    val savedToGallery: Boolean = false,
    val errorMessage: String? = null
)
