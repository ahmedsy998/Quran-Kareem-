package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeStyle

val LocalPreviewAccentColor = staticCompositionLocalOf { Color(0xFFD4AF37) }

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun QuranVideoMakerTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.EMERALD_GOLD,
    content: @Composable () -> Unit
) {
    val colorScheme = AppPalettes.colorSchemeFor(themeStyle)
    val previewAccent = Color(themeStyle.previewBorderHex)

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalPreviewAccentColor provides previewAccent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
