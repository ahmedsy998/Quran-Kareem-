package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.remote.NetworkModule
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.AspectRatioPreset
import com.example.domain.model.BundledBackgrounds
import com.example.domain.model.DefaultReciters
import com.example.domain.model.ExportResolution
import com.example.domain.model.FontOption
import com.example.domain.model.VideoProject
import com.example.util.ArabicTextRenderer
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuranVideoMakerTest {

    @Test
    fun `offline quran asset contains all 114 surahs and 6236 ayahs`() {
        val assetFile = listOf(
            File("src/main/assets/quran/quran_uthmani.json"),
            File("app/src/main/assets/quran/quran_uthmani.json")
        ).first { it.exists() }
        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
        val adapter = NetworkModule.moshi.adapter<List<Map<String, Any>>>(listType)
        val surahs = adapter.fromJson(assetFile.readText(Charsets.UTF_8))!!

        assertEquals(114, surahs.size)
        var totalAyahs = 0
        for (surah in surahs) {
            @Suppress("UNCHECKED_CAST")
            val ayahs = surah["ayahs"] as List<Map<String, Any>>
            totalAyahs += ayahs.size
        }
        assertEquals(6236, totalAyahs)

        @Suppress("UNCHECKED_CAST")
        val surah2Ayah1 = ((surahs[1]["ayahs"] as List<Map<String, Any>>)[0]["t"] as String)
        assertFalse(surah2Ayah1.startsWith("بِسْمِ"))
    }

    @Test
    fun `at least 24 famous reciters are configured with valid audio sources`() {
        assertTrue(DefaultReciters.list.size >= 24)
        DefaultReciters.list.forEach { reciter ->
            assertTrue(reciter.arabicName.isNotBlank())
            if (reciter.supportsExactPerAyah) {
                assertNotNull(reciter.perAyahUrl(1, 1, 1))
            } else {
                assertNotNull(reciter.fullSurahUrl(1))
            }
        }
    }

    @Test
    fun `tashkeel coloring assigns distinct colors to diacritics and base letters`() {
        val sample = "بِسْمِ ٱللَّهِ"
        val baseColor = Color.White
        val tashkeelColor = Color(0xFFFFD54F)

        val annotated = ArabicTextRenderer.buildTashkeelAnnotatedString(
            text = sample,
            baseColor = baseColor,
            tashkeelEnabled = true,
            tashkeelColor = tashkeelColor,
            baseFontSizeSp = 28f,
            tashkeelScale = 1.15f
        )
        assertTrue(annotated.spanStyles.any { it.item.color == tashkeelColor })
        assertTrue(annotated.spanStyles.any { it.item.color == baseColor })
        assertTrue(ArabicTextRenderer.isTashkeelChar('\u0650')) // Kasra
        assertTrue(ArabicTextRenderer.isTashkeelChar('\u0670')) // Superscript Alef
        assertTrue(ArabicTextRenderer.isTashkeelChar('\u06D6')) // Quranic mark
        assertFalse(ArabicTextRenderer.isTashkeelChar('ب'))
    }

    @Test
    fun `aspect ratios produce valid even dimensions for 720p and 1080p`() {
        val expected720 = mapOf(
            AspectRatioPreset.RATIO_9_16 to (720 to 1280),
            AspectRatioPreset.RATIO_16_9 to (1280 to 720),
            AspectRatioPreset.RATIO_1_1 to (720 to 720),
            AspectRatioPreset.RATIO_4_5 to (720 to 900)
        )
        expected720.forEach { (preset, dims) ->
            assertEquals(dims, preset.outputDimensions(ExportResolution.RES_720P))
        }
        assertEquals(1080 to 1920, AspectRatioPreset.RATIO_9_16.outputDimensions(ExportResolution.RES_1080P))
        assertEquals(1080 to 1350, AspectRatioPreset.RATIO_4_5.outputDimensions(ExportResolution.RES_1080P))
    }

    @Test
    fun `project state roundtrips cleanly through Moshi JSON and catalogs are complete`() {
        val adapter = NetworkModule.moshi.adapter(VideoProject::class.java)
        val project = VideoProject(
            id = "proj_test",
            name = "مقطع سورة الكهف",
            reciterId = "dosari",
            surahNumber = 18,
            startAyah = 1,
            endAyah = 10,
            tashkeelColoringEnabled = true,
            tashkeelColorArgb = 0xFF80CBC4,
            themeStyle = AppThemeStyle.MIDNIGHT_BLUE,
            aspectRatio = AspectRatioPreset.RATIO_4_5,
            watermarkText = "@quran_channel",
            exportResolution = ExportResolution.RES_1080P
        )
        val json = adapter.toJson(project)
        val restored = adapter.fromJson(json)
        assertEquals(project, restored)
        assertEquals(8, FontOption.entries.size)
        assertEquals(8, BundledBackgrounds.images.size)
        assertEquals(5, BundledBackgrounds.videos.size)
    }

    @Test
    fun `word tokenization keeps standalone waqf marks attached and builds real segment timings`() {
        val sampleWithWaqf = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ"
        val words = ArabicTextRenderer.tokenizeQuranWords(sampleWithWaqf)
        // Standalone ۛ stop signs merge into previous words instead of becoming separate spoken words (7 spoken words, not 9)
        assertEquals(7, words.size)
        assertTrue(words[3].contains("ۛ"))
        assertTrue(words[4].contains("ۛ"))

        val simpleAyah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val realSegments = listOf(
            listOf(0.0, 1.0, 60.0, 610.0),
            listOf(1.0, 2.0, 620.0, 1310.0),
            listOf(2.0, 3.0, 1320.0, 2450.0),
            listOf(3.0, 4.0, 2460.0, 5970.0)
        )
        val timings = ArabicTextRenderer.buildWordTimings(
            text = simpleAyah,
            ayahDurationMs = 6000L,
            rawSegments = realSegments
        )
        assertEquals(4, timings.size)
        assertEquals(0L, timings[0].startMs)
        assertEquals(620L, timings[1].startMs)
        assertEquals(1320L, timings[2].startMs)
        assertEquals(2460L, timings[3].startMs)

        val line = com.example.domain.model.SyncedAyahLine(
            index = 0,
            numberInSurah = 1,
            absoluteNumber = 1,
            text = simpleAyah,
            isBismillah = false,
            audioSourceUri = null,
            startMs = 0L,
            endMs = 6000L,
            wordTimings = timings
        )
        assertEquals(0, line.activeWordIndexAt(0L))
        assertEquals(1, line.activeWordIndexAt(700L))
        assertEquals(2, line.activeWordIndexAt(1500L))
        assertEquals(3, line.activeWordIndexAt(3000L))
    }

    @Test
    fun `proportional fallback and spoken words renderer omit unrecited words completely`() {
        val text = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ"
        val estimated = ArabicTextRenderer.buildWordTimings(
            text = text,
            ayahDurationMs = 4000L,
            rawSegments = null
        )
        assertEquals(4, estimated.size)
        assertEquals(0L, estimated.first().startMs)
        assertEquals(4000L, estimated.last().endMs)
        for (i in 1 until estimated.size) {
            assertTrue(estimated[i].startMs > estimated[i - 1].startMs)
        }

        val normalColor = Color.White
        val highlightColor = Color(0xFFD4AF37)
        val tashkeelColor = Color(0xFFFFD54F)

        val step1 = ArabicTextRenderer.buildSpokenWordsAnnotatedString(
            words = estimated,
            fallbackText = text,
            activeWordIndex = 1,
            ayahNumberInSurah = 2,
            isBismillah = false,
            normalColor = normalColor,
            highlightColor = highlightColor,
            tashkeelEnabled = true,
            tashkeelColor = tashkeelColor,
            baseFontSizeSp = 28f,
            tashkeelScale = 1.0f
        )
        // Only the first two words are present; remaining words and end badge are completely hidden
        assertTrue(step1.text.contains("ٱلْحَمْدُ"))
        assertTrue(step1.text.contains("لِلَّهِ"))
        assertFalse(step1.text.contains("رَبِّ"))
        assertFalse(step1.text.contains("ٱلْعَٰلَمِينَ"))
        assertFalse(step1.text.contains("﴿٢﴾"))
        assertTrue(step1.spanStyles.any { it.item.color == normalColor })
        assertTrue(step1.spanStyles.any { it.item.color == highlightColor })

        val stepFinal = ArabicTextRenderer.buildSpokenWordsAnnotatedString(
            words = estimated,
            fallbackText = text,
            activeWordIndex = 3,
            ayahNumberInSurah = 2,
            isBismillah = false,
            normalColor = normalColor,
            highlightColor = highlightColor,
            tashkeelEnabled = true,
            tashkeelColor = tashkeelColor,
            baseFontSizeSp = 28f,
            tashkeelScale = 1.0f
        )
        assertTrue(stepFinal.text.contains("ٱلْعَٰلَمِينَ"))
        assertTrue(stepFinal.text.contains("﴿٢﴾"))
    }
}
