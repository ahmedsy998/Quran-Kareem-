package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.FontOption
import com.example.domain.model.TextAlignmentMode
import com.example.domain.model.TextLayerStyle
import kotlin.math.roundToInt

@Composable
fun TextLayerControls(
    style: TextLayerStyle,
    onStyleChange: (TextLayerStyle) -> Unit,
    showEnabledToggle: Boolean = false,
    enabledToggleLabel: String = "تفعيل العنصر",
    showFontPicker: Boolean = true,
    showBackgroundPillToggle: Boolean = false,
    minSizeSp: Float = 10f,
    maxSizeSp: Float = 44f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showEnabledToggle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(enabledToggleLabel, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = style.enabled,
                    onCheckedChange = { onStyleChange(style.copy(enabled = it)) }
                )
            }
        }

        if (!style.enabled && showEnabledToggle) return@Column

        if (showFontPicker) {
            Text("الخط", style = MaterialTheme.typography.bodyMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FontOption.entries) { font ->
                    FilterChip(
                        selected = style.fontId == font.id,
                        onClick = { onStyleChange(style.copy(fontId = font.id)) },
                        label = { Text(font.arabicName, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }

        // Size Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("حجم الخط", style = MaterialTheme.typography.bodyMedium)
                Text("${style.fontSizeSp.roundToInt()} sp", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = style.fontSizeSp.coerceIn(minSizeSp, maxSizeSp),
                onValueChange = { onStyleChange(style.copy(fontSizeSp = it)) },
                valueRange = minSizeSp..maxSizeSp
            )
        }

        // Color Picker
        ColorPickerRow(
            label = "لون النص",
            selectedColorArgb = style.colorArgb,
            onColorSelected = { onStyleChange(style.copy(colorArgb = it)) }
        )

        // Vertical Position Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الموضع العمودي", style = MaterialTheme.typography.bodyMedium)
                Text("${(style.verticalPosition * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = style.verticalPosition.coerceIn(0.05f, 0.95f),
                onValueChange = { onStyleChange(style.copy(verticalPosition = it)) },
                valueRange = 0.05f..0.95f
            )
        }

        // Alignment Control
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("المحاذاة", style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TextAlignmentMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = style.alignment == mode,
                        onClick = { onStyleChange(style.copy(alignment = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = idx,
                            count = TextAlignmentMode.entries.size
                        ),
                        icon = {
                            val icon = when (mode) {
                                TextAlignmentMode.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                                TextAlignmentMode.CENTER -> Icons.Filled.FormatAlignCenter
                                TextAlignmentMode.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                            }
                            Icon(icon, contentDescription = mode.arabicLabel)
                        }
                    ) {
                        Text(mode.arabicLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Opacity Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الشفافية", style = MaterialTheme.typography.bodyMedium)
                Text("${(style.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = style.opacity.coerceIn(0.1f, 1f),
                onValueChange = { onStyleChange(style.copy(opacity = it)) },
                valueRange = 0.1f..1f
            )
        }

        // Shadow Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ظل / حدود للوضوح", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = style.shadowEnabled,
                onCheckedChange = { onStyleChange(style.copy(shadowEnabled = it)) }
            )
        }

        // Background Pill Toggle
        if (showBackgroundPillToggle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("شريط خلفية أنيق خلف النص", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = style.backgroundPillEnabled,
                    onCheckedChange = { onStyleChange(style.copy(backgroundPillEnabled = it)) }
                )
            }
        }
    }
}
