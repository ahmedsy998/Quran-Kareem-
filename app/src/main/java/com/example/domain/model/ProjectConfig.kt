package com.example.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import com.example.R
import com.squareup.moshi.JsonClass

enum class TextAlignmentMode(val arabicLabel: String) {
    RIGHT("يمين"),
    CENTER("وسط"),
    LEFT("يسار")
}

enum class SingleLineOverflowMode(val arabicLabel: String) {
    AUTO_SHRINK("تصغير تلقائي للسطر"),
    MARQUEE("تمرير أفقي سلس")
}

enum class BackgroundType {
    BUNDLED_IMAGE,
    BUNDLED_VIDEO,
    USER_IMAGE,
    USER_VIDEO
}

enum class AspectRatioPreset(
    val arabicLabel: String,
    val ratioTag: String,
    val platformHint: String,
    val widthRatio: Float,
    val heightRatio: Float
) {
    RATIO_9_16("٩:١٦ عمودي", "9:16", "ريلز / تيك توك / شورتس", 9f, 16f),
    RATIO_16_9("١٦:٩ أفقي", "16:9", "يوتيوب", 16f, 9f),
    RATIO_1_1("١:١ مربع", "1:1", "منشورات", 1f, 1f),
    RATIO_4_5("٤:٥ عمودي", "4:5", "إنستغرام", 4f, 5f);

    fun outputDimensions(resolution: ExportResolution): Pair<Int, Int> {
        val shortEdge = if (resolution == ExportResolution.RES_1080P) 1080 else 720
        return when (this) {
            RATIO_9_16 -> shortEdge to (shortEdge * 16 / 9)
            RATIO_16_9 -> (shortEdge * 16 / 9) to shortEdge
            RATIO_1_1 -> shortEdge to shortEdge
            RATIO_4_5 -> shortEdge to (shortEdge * 5 / 4)
        }
    }
}

enum class ExportResolution(val arabicLabel: String, val badge: String) {
    RES_720P("جودة عالية 720p", "720p"),
    RES_1080P("جودة فائقة 1080p", "1080p")
}

enum class AppThemeStyle(
    val arabicName: String,
    val subtitle: String,
    val primaryHex: Long,
    val accentHex: Long,
    val surfaceHex: Long,
    val previewBorderHex: Long
) {
    EMERALD_GOLD("زمرد وذهب", "كلاسيكي روحاني", 0xFF0A5C46, 0xFFD4AF37, 0xFFF4F8F6, 0xFFD4AF37),
    MIDNIGHT_BLUE("أزرق منتصف الليل", "ليلي هادئ", 0xFF1A3A6E, 0xFF64B5F6, 0xFF0B1324, 0xFF64B5F6),
    ROYAL_PURPLE("بنفسجي ملكي", "فخامة داكنة", 0xFF5A2D82, 0xFFE6C27A, 0xFF150E21, 0xFFE6C27A),
    DESERT_SAND("رمال الصحراء", "دافئ مريح", 0xFF8A5A32, 0xFFC88B4B, 0xFFFAF5EE, 0xFFC88B4B),
    ROSE_MARBLE("رخام وردي", "ناعم أنيق", 0xFF8E3B59, 0xFFD4A373, 0xFFFCF4F6, 0xFFD4A373)
}

enum class FontOption(
    val id: String,
    val arabicName: String,
    val isQuranic: Boolean,
    @FontRes val fontResId: Int
) {
    KFGQPC_HAFS("kfgqpc_hafs", "خط حفص العثماني", true, R.font.kfgqpc_uthmanic_hafs),
    AMIRI_QURAN("amiri_quran", "أميري قرآن", true, R.font.amiri_quran),
    SCHEHERAZADE("scheherazade_new", "شهرزاد الجديد", true, R.font.scheherazade_new),
    CAIRO("cairo", "القاهرة (Cairo)", false, R.font.cairo),
    TAJAWAL("tajawal", "تجوال (Tajawal)", false, R.font.tajawal),
    ALMARAI("almarai", "المراعي (Almarai)", false, R.font.almarai),
    NOTO_KUFI("noto_kufi", "نوتو كوفي العربي", false, R.font.noto_kufi_arabic),
    IBM_PLEX("ibm_plex", "IBM بلكس العربي", false, R.font.ibm_plex_sans_arabic);

    companion object {
        fun fromId(id: String): FontOption = entries.firstOrNull { it.id == id } ?: AMIRI_QURAN
    }
}

data class BundledImageBg(
    val id: String,
    val arabicName: String,
    val category: String,
    @DrawableRes val drawableResId: Int
)

data class BundledVideoBg(
    val id: String,
    val arabicName: String,
    val category: String,
    val assetPath: String,
    @DrawableRes val thumbnailResId: Int
)

object BundledBackgrounds {
    val images = listOf(
        BundledImageBg("kaaba_night", "الكعبة المشرفة ليلاً", "إسلامي", R.drawable.bg_kaaba_night),
        BundledImageBg("mosque_arch", "قوس المسجد والشفق", "إسلامي", R.drawable.bg_mosque_arch),
        BundledImageBg("quran_rays", "المصحف وأشعة النور", "إسلامي", R.drawable.bg_quran_rays),
        BundledImageBg("emerald_sanctuary", "روضة زمردية", "إسلامي", R.drawable.bg_emerald_sanctuary),
        BundledImageBg("misty_mountains", "جبال وضباب الفجر", "طبيعة", R.drawable.bg_misty_mountains),
        BundledImageBg("ocean_twilight", "بحر ونجوم السحر", "طبيعة", R.drawable.bg_ocean_twilight),
        BundledImageBg("starry_sky", "سماء الليل والنجوم", "طبيعة", R.drawable.bg_starry_sky),
        BundledImageBg("desert_dusk", "غسق الصحراء", "طبيعة", R.drawable.bg_desert_dusk)
    )

    val videos = listOf(
        BundledVideoBg("vid_kaaba_glow", "توهج الحرم المكي", "إسلامي", "backgrounds/videos/video_kaaba_glow.mp4", R.drawable.bg_kaaba_night),
        BundledVideoBg("vid_golden_rays", "أنوار المصحف الذهبية", "إسلامي", "backgrounds/videos/video_golden_rays.mp4", R.drawable.bg_quran_rays),
        BundledVideoBg("vid_mosque_rain", "أجواء المسجد المسائية", "إسلامي", "backgrounds/videos/video_mosque_rain.mp4", R.drawable.bg_mosque_arch),
        BundledVideoBg("vid_ocean_waves", "أمواج البحر الهادئة", "طبيعة", "backgrounds/videos/video_ocean_waves.mp4", R.drawable.bg_ocean_twilight),
        BundledVideoBg("vid_misty_forest", "ضباب الغابة والجبال", "طبيعة", "backgrounds/videos/video_misty_forest.mp4", R.drawable.bg_misty_mountains)
    )

    fun findImage(id: String): BundledImageBg = images.firstOrNull { it.id == id } ?: images.first()
    fun findVideo(id: String): BundledVideoBg = videos.firstOrNull { it.id == id } ?: videos.first()
}

@JsonClass(generateAdapter = true)
data class TextLayerStyle(
    val enabled: Boolean = true,
    val fontId: String = FontOption.AMIRI_QURAN.id,
    val fontSizeSp: Float = 28f,
    val colorArgb: Long = 0xFFFFFFFF,
    val verticalPosition: Float = 0.50f, // 0f top .. 1f bottom
    val alignment: TextAlignmentMode = TextAlignmentMode.CENTER,
    val opacity: Float = 1.0f,
    val shadowEnabled: Boolean = true,
    val backgroundPillEnabled: Boolean = false,
    val backgroundPillColorArgb: Long = 0x80000000
)

@JsonClass(generateAdapter = true)
data class VideoProject(
    val id: String = "default_project",
    val name: String = "تلاوة سورة الفاتحة",
    val updatedAt: Long = System.currentTimeMillis(),
    // 1. Reciter
    val reciterId: String = "alafasy",
    // 2. Surah & Ayah Range
    val surahNumber: Int = 1,
    val startAyah: Int = 1,
    val endAyah: Int = 7,
    val showBismillahIntro: Boolean = true,
    // 3. Ayah Font & Text Style
    val ayahStyle: TextLayerStyle = TextLayerStyle(
        enabled = true,
        fontId = FontOption.KFGQPC_HAFS.id,
        fontSizeSp = 30f,
        colorArgb = 0xFFFFFFFF,
        verticalPosition = 0.50f,
        alignment = TextAlignmentMode.CENTER,
        opacity = 1.0f,
        shadowEnabled = true,
        backgroundPillEnabled = false
    ),
    val lineSpacingMultiplier: Float = 1.35f,
    val overflowMode: SingleLineOverflowMode = SingleLineOverflowMode.AUTO_SHRINK,
    val showDimmedAdjacentAyahs: Boolean = false,
    // 4. Tashkeel Coloring
    val tashkeelColoringEnabled: Boolean = true,
    val tashkeelColorArgb: Long = 0xFFFFD54F,
    val tashkeelScale: Float = 1.0f,
    // 5. Video Background
    val backgroundType: BackgroundType = BackgroundType.BUNDLED_IMAGE,
    val bundledBackgroundId: String = "kaaba_night",
    val userMediaLocalPath: String? = null,
    val backgroundDim: Float = 0.45f, // 0f bright .. 0.85f dark
    val backgroundBlurRadius: Float = 0f, // 0f .. 24f
    val videoLoopEnabled: Boolean = true,
    // 8. Theme Style (stored per project + global preference)
    val themeStyle: AppThemeStyle = AppThemeStyle.EMERALD_GOLD,
    // 9. Aspect Ratio
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    // 10. Auto Titles (Surah top & Reciter bottom)
    val surahTitleStyle: TextLayerStyle = TextLayerStyle(
        enabled = true,
        fontId = FontOption.CAIRO.id,
        fontSizeSp = 18f,
        colorArgb = 0xFFFFE082,
        verticalPosition = 0.08f,
        alignment = TextAlignmentMode.CENTER,
        opacity = 0.95f,
        shadowEnabled = true,
        backgroundPillEnabled = true,
        backgroundPillColorArgb = 0x70000000
    ),
    val reciterTitleStyle: TextLayerStyle = TextLayerStyle(
        enabled = true,
        fontId = FontOption.TAJAWAL.id,
        fontSizeSp = 15f,
        colorArgb = 0xFFE0E0E0,
        verticalPosition = 0.84f,
        alignment = TextAlignmentMode.CENTER,
        opacity = 0.92f,
        shadowEnabled = true,
        backgroundPillEnabled = true,
        backgroundPillColorArgb = 0x66000000
    ),
    // 11. User Info / Watermark
    val watermarkText: String = "",
    val watermarkStyle: TextLayerStyle = TextLayerStyle(
        enabled = false,
        fontId = FontOption.CAIRO.id,
        fontSizeSp = 13f,
        colorArgb = 0xCCFFFFFF,
        verticalPosition = 0.92f,
        alignment = TextAlignmentMode.CENTER,
        opacity = 0.80f,
        shadowEnabled = true,
        backgroundPillEnabled = false
    ),
    // 12. Export Resolution
    val exportResolution: ExportResolution = ExportResolution.RES_720P
)
