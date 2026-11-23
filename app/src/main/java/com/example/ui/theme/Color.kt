package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AppThemeStyle

object AppPalettes {

    private val EmeraldGoldScheme = lightColorScheme(
        primary = Color(0xFF0A5C46),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD2EFE3),
        onPrimaryContainer = Color(0xFF02291E),
        secondary = Color(0xFFB6891B),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFBEBC4),
        onSecondaryContainer = Color(0xFF332300),
        tertiary = Color(0xFF1F6B5C),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFF5F8F6),
        onBackground = Color(0xFF10201B),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF10201B),
        surfaceVariant = Color(0xFFE4ECE8),
        onSurfaceVariant = Color(0xFF3E524B),
        outline = Color(0xFF70877F),
        outlineVariant = Color(0xFFC5D5CE)
    )

    private val MidnightBlueScheme = darkColorScheme(
        primary = Color(0xFF72B8FF),
        onPrimary = Color(0xFF002D54),
        primaryContainer = Color(0xFF173D6E),
        onPrimaryContainer = Color(0xFFD6E8FF),
        secondary = Color(0xFFE5C16C),
        onSecondary = Color(0xFF3A2B00),
        secondaryContainer = Color(0xFF473712),
        onSecondaryContainer = Color(0xFFFCE8B2),
        tertiary = Color(0xFF66D1C1),
        onTertiary = Color(0xFF003731),
        background = Color(0xFF0B1324),
        onBackground = Color(0xFFE9EFFA),
        surface = Color(0xFF121C33),
        onSurface = Color(0xFFE9EFFA),
        surfaceVariant = Color(0xFF1B2947),
        onSurfaceVariant = Color(0xFFB5C4DF),
        outline = Color(0xFF5D739B),
        outlineVariant = Color(0xFF28395C)
    )

    private val RoyalPurpleScheme = darkColorScheme(
        primary = Color(0xFFCFA7FF),
        onPrimary = Color(0xFF350E5E),
        primaryContainer = Color(0xFF4F247D),
        onPrimaryContainer = Color(0xFFF1E3FF),
        secondary = Color(0xFFE9C46A),
        onSecondary = Color(0xFF3A2900),
        secondaryContainer = Color(0xFF4D3913),
        onSecondaryContainer = Color(0xFFFFEAB5),
        tertiary = Color(0xFFF09EB5),
        onTertiary = Color(0xFF4A1527),
        background = Color(0xFF140D20),
        onBackground = Color(0xFFF2ECF9),
        surface = Color(0xFF1E1430),
        onSurface = Color(0xFFF2ECF9),
        surfaceVariant = Color(0xFF2B1E42),
        onSurfaceVariant = Color(0xFFCABCE0),
        outline = Color(0xFF7B6896),
        outlineVariant = Color(0xFF3B2B57)
    )

    private val DesertSandScheme = lightColorScheme(
        primary = Color(0xFF8A5428),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF5DEC9),
        onPrimaryContainer = Color(0xFF331B06),
        secondary = Color(0xFFB87D3B),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFBE8D0),
        onSecondaryContainer = Color(0xFF3A2105),
        tertiary = Color(0xFF5A6D47),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFFAF5EE),
        onBackground = Color(0xFF261C12),
        surface = Color(0xFFFFFCF8),
        onSurface = Color(0xFF261C12),
        surfaceVariant = Color(0xFFEFE4D6),
        onSurfaceVariant = Color(0xFF5A4939),
        outline = Color(0xFF8E7A66),
        outlineVariant = Color(0xFFDCCCB9)
    )

    private val RoseMarbleScheme = lightColorScheme(
        primary = Color(0xFF8C3654),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFBDCE6),
        onPrimaryContainer = Color(0xFF3A0A1B),
        secondary = Color(0xFFC28E5C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF7E4D0),
        onSecondaryContainer = Color(0xFF382209),
        tertiary = Color(0xFF6B5373),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFFCF5F7),
        onBackground = Color(0xFF26141B),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF26141B),
        surfaceVariant = Color(0xFFF2E2E7),
        onSurfaceVariant = Color(0xFF5C424B),
        outline = Color(0xFF8E6E78),
        outlineVariant = Color(0xFFE1C8D0)
    )

    fun colorSchemeFor(style: AppThemeStyle): ColorScheme = when (style) {
        AppThemeStyle.EMERALD_GOLD -> EmeraldGoldScheme
        AppThemeStyle.MIDNIGHT_BLUE -> MidnightBlueScheme
        AppThemeStyle.ROYAL_PURPLE -> RoyalPurpleScheme
        AppThemeStyle.DESERT_SAND -> DesertSandScheme
        AppThemeStyle.ROSE_MARBLE -> RoseMarbleScheme
    }

    fun isDark(style: AppThemeStyle): Boolean =
        style == AppThemeStyle.MIDNIGHT_BLUE || style == AppThemeStyle.ROYAL_PURPLE
}
