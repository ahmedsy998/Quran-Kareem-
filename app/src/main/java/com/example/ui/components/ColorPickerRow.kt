package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

val CuratedCalligraphyColors: List<Long> = listOf(
    0xFFFFFFFF, // أبيض ناصع
    0xFFFFF8E1, // عاجي دافئ
    0xFFFFD54F, // ذهبي فاتح
    0xFFD4AF37, // ذهب ملكي
    0xFF80CBC4, // فيروزي ناعم
    0xFF64B5F6, // أزرق سماوي
    0xFFA5D6A7, // زمردي ناعم
    0xFFF48FB1, // وردي لؤلؤي
    0xFFFFAB91, // مرجاني دافئ
    0xFFCE93D8, // لافندر
    0xFF1A1A1A, // فحمي داكن
    0xFF0A5C46  // أخضر زمردي
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    label: String,
    selectedColorArgb: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = { showCustomDialog = true },
                modifier = Modifier.testTag("custom_color_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = "لون مخصص",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text("لون مخصص", style = MaterialTheme.typography.labelMedium)
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CuratedCalligraphyColors.forEach { argb ->
                val color = Color(argb)
                val isSelected = (selectedColorArgb and 0x00FFFFFFL) == (argb and 0x00FFFFFFL)
                val checkTint = if (color.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(argb) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "محدد",
                            tint = checkTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        val initial = Color(selectedColorArgb)
        var red by remember { mutableFloatStateOf(initial.red) }
        var green by remember { mutableFloatStateOf(initial.green) }
        var blue by remember { mutableFloatStateOf(initial.blue) }
        val previewColor = Color(red, green, blue, 1f)

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("اختيار لون مخصص", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(previewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    Text("أحمر (${(red * 255).toInt()})", style = MaterialTheme.typography.labelMedium)
                    Slider(value = red, onValueChange = { red = it })
                    Text("أخضر (${(green * 255).toInt()})", style = MaterialTheme.typography.labelMedium)
                    Slider(value = green, onValueChange = { green = it })
                    Text("أزرق (${(blue * 255).toInt()})", style = MaterialTheme.typography.labelMedium)
                    Slider(value = blue, onValueChange = { blue = it })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val argb = (previewColor.toArgb().toLong() and 0xFFFFFFFFL) or 0xFF000000L
                        onColorSelected(argb)
                        showCustomDialog = false
                    }
                ) {
                    Text("اعتماد اللون")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
