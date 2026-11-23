package com.example.util

import android.graphics.Color as AndroidColor
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.domain.model.WordTiming

object ArabicTextRenderer {

    /**
     * Exact Unicode ranges requested for Arabic harakat / Quranic tashkeel & annotation marks:
     * U+064B–U+0652 (Fathatan..Sukun)
     * U+0670 (Superscript Alef)
     * U+06D6–U+06ED (Quranic small high/low marks & stop signs)
     * Also covers U+0653–U+065F (Maddah/Hamza above/below) for visual consistency.
     */
    fun isTashkeelChar(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x064B..0x0652) ||
            (code in 0x0653..0x065F) ||
            code == 0x0670 ||
            (code in 0x06D6..0x06ED)
    }

    private fun isStandaloneQuranMarkToken(token: String): Boolean {
        if (token.isEmpty()) return false
        return token.all { ch ->
            val code = ch.code
            (code in 0x06D6..0x06ED) || code == 0x06DE || code == 0x06E9
        }
    }

    fun tokenizeQuranWords(text: String): List<String> {
        val rawTokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (rawTokens.isEmpty()) return emptyList()
        val result = ArrayList<String>(rawTokens.size)
        for (token in rawTokens) {
            if (isStandaloneQuranMarkToken(token) && result.isNotEmpty()) {
                result[result.lastIndex] = "${result.last()} $token"
            } else {
                result.add(token)
            }
        }
        return result
    }

    fun buildWordTimings(
        text: String,
        ayahDurationMs: Long,
        rawSegments: List<List<Double>>? = null
    ): List<WordTiming> {
        val words = tokenizeQuranWords(text)
        if (words.isEmpty()) return emptyList()
        val safeDuration = ayahDurationMs.coerceAtLeast(words.size.toLong() * 80L)

        // Try real Quran.com API v4 segments if available
        if (!rawSegments.isNullOrEmpty()) {
            data class RawSeg(val wordIdx: Int, val startMs: Long, val endMs: Long)
            val parsed = rawSegments.mapNotNull { seg ->
                if (seg.size < 4) return@mapNotNull null
                val wordIdx = seg[0].toInt()
                val startMs = seg[2].toLong().coerceAtLeast(0L)
                val endMs = seg[3].toLong().coerceAtLeast(startMs + 40L)
                if (wordIdx < 0) null else RawSeg(wordIdx, startMs, endMs)
            }

            if (parsed.isNotEmpty()) {
                val maxSegIdx = parsed.maxOf { it.wordIdx }
                val segMaxEnd = parsed.maxOf { it.endMs }.coerceAtLeast(1L)
                val timeScale = if (segMaxEnd > (safeDuration * 1.12f).toLong()) {
                    safeDuration.toDouble() / segMaxEnd.toDouble()
                } else {
                    1.0
                }

                if (maxSegIdx + 1 == words.size) {
                    val grouped = parsed.groupBy { it.wordIdx }
                    val result = ArrayList<WordTiming>(words.size)
                    var prevEnd = 0L
                    for (i in words.indices) {
                        val segsForWord = grouped[i]
                        val rawStart = segsForWord?.minOfOrNull { it.startMs }?.let { (it * timeScale).toLong() }
                        val rawEnd = segsForWord?.maxOfOrNull { it.endMs }?.let { (it * timeScale).toLong() }

                        val start = if (i == 0) {
                            0L
                        } else {
                            (rawStart ?: prevEnd).coerceIn(prevEnd, safeDuration - (words.size - i))
                        }
                        val defaultEnd = if (i == words.lastIndex) safeDuration else (start + 120L).coerceAtMost(safeDuration)
                        val end = (rawEnd ?: defaultEnd).coerceIn(start + 1L, safeDuration)
                        result.add(WordTiming(index = i, text = words[i], startMs = start, endMs = end))
                        prevEnd = start.coerceAtLeast(prevEnd)
                    }
                    return result
                } else {
                    // Token count slight mismatch (e.g. Rasm Uthmani vs Imlaei): fit words proportionally across segment span
                    val spanStart = (parsed.minOf { it.startMs } * timeScale).toLong().coerceIn(0L, safeDuration / 3L)
                    val spanEnd = (parsed.maxOf { it.endMs } * timeScale).toLong().coerceIn(spanStart + words.size * 50L, safeDuration)
                    return buildProportionalTimings(
                        words = words,
                        spanStartMs = spanStart,
                        spanEndMs = spanEnd,
                        totalDurationMs = safeDuration
                    )
                }
            }
        }

        // Fallback: split ayah duration proportionally to each word's character length (including tashkeel)
        return buildProportionalTimings(
            words = words,
            spanStartMs = 0L,
            spanEndMs = safeDuration,
            totalDurationMs = safeDuration
        )
    }

    private fun buildProportionalTimings(
        words: List<String>,
        spanStartMs: Long,
        spanEndMs: Long,
        totalDurationMs: Long
    ): List<WordTiming> {
        if (words.isEmpty()) return emptyList()
        val spanDur = (spanEndMs - spanStartMs).coerceAtLeast(words.size * 50L)
        val totalChars = words.sumOf { it.length.coerceAtLeast(1) }.toDouble().coerceAtLeast(1.0)
        val result = ArrayList<WordTiming>(words.size)
        var cursor = spanStartMs
        for (i in words.indices) {
            val start = if (i == 0) 0L else cursor
            val end = if (i == words.lastIndex) {
                totalDurationMs.coerceAtLeast(start + 1L)
            } else {
                val slice = ((words[i].length.coerceAtLeast(1) / totalChars) * spanDur).toLong().coerceAtLeast(50L)
                (cursor + slice).coerceIn(start + 1L, totalDurationMs - (words.size - 1 - i))
            }
            result.add(WordTiming(index = i, text = words[i], startMs = start, endMs = end))
            cursor = end
        }
        return result
    }

    fun buildSpokenWordsAnnotatedString(
        words: List<WordTiming>,
        fallbackText: String,
        activeWordIndex: Int,
        ayahNumberInSurah: Int,
        isBismillah: Boolean,
        normalColor: Color,
        highlightColor: Color,
        tashkeelEnabled: Boolean,
        tashkeelColor: Color,
        baseFontSizeSp: Float,
        tashkeelScale: Float
    ): AnnotatedString {
        val wordStrings = if (words.isNotEmpty()) {
            words.map { it.text }
        } else {
            tokenizeQuranWords(fallbackText)
        }
        if (wordStrings.isEmpty()) return AnnotatedString("")

        val safeActive = activeWordIndex.coerceIn(0, wordStrings.lastIndex)
        val visibleWords = wordStrings.subList(0, safeActive + 1)
        val isLastWord = safeActive == wordStrings.lastIndex
        val ayahBadge = if (!isBismillah && isLastWord) " ﴿${toArabicDigits(ayahNumberInSurah)}﴾" else ""

        val tashkeelFontSize: TextUnit = (baseFontSizeSp * tashkeelScale.coerceIn(0.8f, 1.4f)).sp
        return buildAnnotatedString {
            visibleWords.forEachIndexed { idx, word ->
                if (idx > 0) append(" ")
                val wordColor = if (idx == safeActive) highlightColor else normalColor
                val startIdx = length
                append(word)
                applyColoredRunsCompose(
                    source = word,
                    offsetInBuilder = startIdx,
                    baseColor = wordColor,
                    tashkeelEnabled = tashkeelEnabled,
                    tashkeelColor = tashkeelColor,
                    tashkeelFontSize = tashkeelFontSize,
                    tashkeelScale = tashkeelScale
                )
            }
            if (ayahBadge.isNotEmpty()) {
                val badgeStart = length
                append(ayahBadge)
                addStyle(
                    style = SpanStyle(color = highlightColor),
                    start = badgeStart,
                    end = length
                )
            }
        }
    }

    fun buildSpokenWordsSpannable(
        words: List<WordTiming>,
        fallbackText: String,
        activeWordIndex: Int,
        ayahNumberInSurah: Int,
        isBismillah: Boolean,
        normalColorArgb: Int,
        highlightColorArgb: Int,
        tashkeelEnabled: Boolean,
        tashkeelColorArgb: Int,
        tashkeelScale: Float
    ): SpannableStringBuilder {
        val wordStrings = if (words.isNotEmpty()) {
            words.map { it.text }
        } else {
            tokenizeQuranWords(fallbackText)
        }
        val builder = SpannableStringBuilder()
        if (wordStrings.isEmpty()) return builder

        val safeActive = activeWordIndex.coerceIn(0, wordStrings.lastIndex)
        val visibleWords = wordStrings.subList(0, safeActive + 1)
        val isLastWord = safeActive == wordStrings.lastIndex
        val ayahBadge = if (!isBismillah && isLastWord) " ﴿${toArabicDigits(ayahNumberInSurah)}﴾" else ""

        visibleWords.forEachIndexed { idx, word ->
            if (idx > 0) builder.append(" ")
            val wordColor = if (idx == safeActive) highlightColorArgb else normalColorArgb
            val startIdx = builder.length
            builder.append(word)
            applyColoredRunsAndroid(
                builder = builder,
                source = word,
                offsetInBuilder = startIdx,
                baseColorArgb = wordColor,
                tashkeelEnabled = tashkeelEnabled,
                tashkeelColorArgb = tashkeelColorArgb,
                tashkeelScale = tashkeelScale
            )
        }
        if (ayahBadge.isNotEmpty()) {
            val badgeStart = builder.length
            builder.append(ayahBadge)
            builder.setSpan(
                ForegroundColorSpan(highlightColorArgb),
                badgeStart,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }

    private fun AnnotatedString.Builder.applyColoredRunsCompose(
        source: String,
        offsetInBuilder: Int,
        baseColor: Color,
        tashkeelEnabled: Boolean,
        tashkeelColor: Color,
        tashkeelFontSize: TextUnit,
        tashkeelScale: Float
    ) {
        if (!tashkeelEnabled || source.isEmpty()) {
            addStyle(SpanStyle(color = baseColor), offsetInBuilder, offsetInBuilder + source.length)
            return
        }
        var runStart = -1
        var runIsTashkeel = false
        for (i in source.indices) {
            val isMark = isTashkeelChar(source[i])
            if (runStart == -1) {
                runStart = i
                runIsTashkeel = isMark
            } else if (isMark != runIsTashkeel) {
                applyStyleRun(
                    start = offsetInBuilder + runStart,
                    end = offsetInBuilder + i,
                    isTashkeel = runIsTashkeel,
                    baseColor = baseColor,
                    tashkeelColor = tashkeelColor,
                    tashkeelFontSize = tashkeelFontSize,
                    tashkeelScale = tashkeelScale
                )
                runStart = i
                runIsTashkeel = isMark
            }
        }
        if (runStart != -1) {
            applyStyleRun(
                start = offsetInBuilder + runStart,
                end = offsetInBuilder + source.length,
                isTashkeel = runIsTashkeel,
                baseColor = baseColor,
                tashkeelColor = tashkeelColor,
                tashkeelFontSize = tashkeelFontSize,
                tashkeelScale = tashkeelScale
            )
        }
    }

    private fun applyColoredRunsAndroid(
        builder: SpannableStringBuilder,
        source: String,
        offsetInBuilder: Int,
        baseColorArgb: Int,
        tashkeelEnabled: Boolean,
        tashkeelColorArgb: Int,
        tashkeelScale: Float
    ) {
        if (!tashkeelEnabled || source.isEmpty()) {
            builder.setSpan(
                ForegroundColorSpan(baseColorArgb),
                offsetInBuilder,
                offsetInBuilder + source.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return
        }
        var runStart = -1
        var runIsTashkeel = false
        for (i in source.indices) {
            val isMark = isTashkeelChar(source[i])
            if (runStart == -1) {
                runStart = i
                runIsTashkeel = isMark
            } else if (isMark != runIsTashkeel) {
                applyAndroidSpanRun(
                    builder = builder,
                    start = offsetInBuilder + runStart,
                    end = offsetInBuilder + i,
                    isTashkeel = runIsTashkeel,
                    baseColorArgb = baseColorArgb,
                    tashkeelColorArgb = tashkeelColorArgb,
                    tashkeelScale = tashkeelScale
                )
                runStart = i
                runIsTashkeel = isMark
            }
        }
        if (runStart != -1) {
            applyAndroidSpanRun(
                builder = builder,
                start = offsetInBuilder + runStart,
                end = offsetInBuilder + source.length,
                isTashkeel = runIsTashkeel,
                baseColorArgb = baseColorArgb,
                tashkeelColorArgb = tashkeelColorArgb,
                tashkeelScale = tashkeelScale
            )
        }
    }

    fun buildTashkeelAnnotatedString(
        text: String,
        baseColor: Color,
        tashkeelEnabled: Boolean,
        tashkeelColor: Color,
        baseFontSizeSp: Float,
        tashkeelScale: Float
    ): AnnotatedString {
        if (!tashkeelEnabled || text.isEmpty()) {
            return AnnotatedString(text)
        }
        val tashkeelFontSize: TextUnit = (baseFontSizeSp * tashkeelScale.coerceIn(0.8f, 1.4f)).sp
        return buildAnnotatedString {
            append(text)
            applyColoredRunsCompose(
                source = text,
                offsetInBuilder = 0,
                baseColor = baseColor,
                tashkeelEnabled = true,
                tashkeelColor = tashkeelColor,
                tashkeelFontSize = tashkeelFontSize,
                tashkeelScale = tashkeelScale
            )
        }
    }

    private fun AnnotatedString.Builder.applyStyleRun(
        start: Int,
        end: Int,
        isTashkeel: Boolean,
        baseColor: Color,
        tashkeelColor: Color,
        tashkeelFontSize: TextUnit,
        tashkeelScale: Float
    ) {
        if (isTashkeel) {
            addStyle(
                style = SpanStyle(
                    color = tashkeelColor,
                    fontSize = if (tashkeelScale != 1f) tashkeelFontSize else TextUnit.Unspecified
                ),
                start = start,
                end = end
            )
        } else {
            addStyle(
                style = SpanStyle(color = baseColor),
                start = start,
                end = end
            )
        }
    }

    fun buildTashkeelSpannable(
        text: String,
        baseColorArgb: Int,
        tashkeelEnabled: Boolean,
        tashkeelColorArgb: Int,
        tashkeelScale: Float
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        if (text.isEmpty()) return builder
        applyColoredRunsAndroid(
            builder = builder,
            source = text,
            offsetInBuilder = 0,
            baseColorArgb = baseColorArgb,
            tashkeelEnabled = tashkeelEnabled,
            tashkeelColorArgb = tashkeelColorArgb,
            tashkeelScale = tashkeelScale
        )
        return builder
    }

    private fun applyAndroidSpanRun(
        builder: SpannableStringBuilder,
        start: Int,
        end: Int,
        isTashkeel: Boolean,
        baseColorArgb: Int,
        tashkeelColorArgb: Int,
        tashkeelScale: Float
    ) {
        if (isTashkeel) {
            builder.setSpan(
                ForegroundColorSpan(tashkeelColorArgb),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (tashkeelScale != 1f) {
                builder.setSpan(
                    RelativeSizeSpan(tashkeelScale.coerceIn(0.8f, 1.4f)),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            builder.setSpan(
                ForegroundColorSpan(baseColorArgb),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun applyOpacityToArgb(colorArgb: Long, opacity: Float): Int {
        val baseInt = colorArgb.toInt()
        val origAlpha = AndroidColor.alpha(baseInt)
        val newAlpha = (origAlpha * opacity.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return AndroidColor.argb(
            newAlpha,
            AndroidColor.red(baseInt),
            AndroidColor.green(baseInt),
            AndroidColor.blue(baseInt)
        )
    }

    fun formatDurationMs(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return String.format("%02d:%02d", mins, secs)
    }

    fun toArabicDigits(input: Any): String {
        val str = input.toString()
        val sb = StringBuilder(str.length)
        for (ch in str) {
            when (ch) {
                in '0'..'9' -> sb.append(('٠'.code + (ch - '0')).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
