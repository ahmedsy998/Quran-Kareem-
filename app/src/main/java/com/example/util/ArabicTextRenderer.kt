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
            var runStart = -1
            var runIsTashkeel = false
            for (i in text.indices) {
                val isMark = isTashkeelChar(text[i])
                if (runStart == -1) {
                    runStart = i
                    runIsTashkeel = isMark
                } else if (isMark != runIsTashkeel) {
                    applyStyleRun(
                        start = runStart,
                        end = i,
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
                    start = runStart,
                    end = text.length,
                    isTashkeel = runIsTashkeel,
                    baseColor = baseColor,
                    tashkeelColor = tashkeelColor,
                    tashkeelFontSize = tashkeelFontSize,
                    tashkeelScale = tashkeelScale
                )
            }
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
        if (!tashkeelEnabled || text.isEmpty()) {
            builder.setSpan(
                ForegroundColorSpan(baseColorArgb),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return builder
        }
        var runStart = -1
        var runIsTashkeel = false
        for (i in text.indices) {
            val isMark = isTashkeelChar(text[i])
            if (runStart == -1) {
                runStart = i
                runIsTashkeel = isMark
            } else if (isMark != runIsTashkeel) {
                applyAndroidSpanRun(
                    builder = builder,
                    start = runStart,
                    end = i,
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
                start = runStart,
                end = text.length,
                isTashkeel = runIsTashkeel,
                baseColorArgb = baseColorArgb,
                tashkeelColorArgb = tashkeelColorArgb,
                tashkeelScale = tashkeelScale
            )
        }
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
