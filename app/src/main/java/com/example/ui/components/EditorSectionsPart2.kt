package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.AspectRatioPreset
import com.example.domain.model.ExportResolution
import com.example.domain.model.VideoProject
import com.example.ui.viewmodel.ExportUiState
import com.example.util.ArabicTextRenderer.toArabicDigits
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Section 7: Save Progress (حفظ التقدم)
@Composable
fun SaveProgressSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    activeProject: VideoProject,
    savedProjects: List<VideoProject>,
    lastSavedTimestamp: Long?,
    onSaveAsNamed: (String) -> Unit,
    onOpenProject: (VideoProject) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var newName by remember(activeProject.id) { mutableStateOf(activeProject.name) }
    val timeFormatted = remember(lastSavedTimestamp) {
        lastSavedTimestamp?.let {
            SimpleDateFormat("HH:mm:ss", Locale("ar")).format(Date(it))
        } ?: "الآن"
    }

    EditorSectionCard(
        sectionIndex = 7,
        title = "حفظ التقدم والمشاريع",
        summary = "حفظ تلقائي • آخر حفظ $timeFormatted",
        icon = Icons.Filled.BookmarkAdded,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "يتم حفظ كافة التعديلات تلقائياً لحظياً. عند إغلاق التطبيق أو إعادة تشغيل الهاتف ستعود لنفس النقطة تماماً.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("اسم النسخة / المشروع") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onSaveAsNamed(newName) },
                modifier = Modifier.testTag("save_named_project_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("حفظ")
            }
        }

        if (savedProjects.isNotEmpty()) {
            Text("مشاريعي المحفوظة (${toArabicDigits(savedProjects.size)})", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                savedProjects.take(6).forEach { item ->
                    val isCurrent = item.id == activeProject.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${item.aspectRatio.ratioTag} • آيات ${toArabicDigits(item.startAyah)}-${toArabicDigits(item.endAyah)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isCurrent) {
                                IconButton(onClick = { onOpenProject(item) }) {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = "فتح المشروع")
                                }
                            }
                            if (savedProjects.size > 1) {
                                IconButton(onClick = { onDeleteProject(item.id) }) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "حذف المشروع",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Section 8: UI Styles / Themes (أنماط الواجهة)
@Composable
fun ThemesSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    currentTheme: AppThemeStyle,
    onSelectTheme: (AppThemeStyle) -> Unit
) {
    EditorSectionCard(
        sectionIndex = 8,
        title = "أنماط الواجهة",
        summary = currentTheme.arabicName,
        icon = Icons.Filled.ColorLens,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AppThemeStyle.entries) { style ->
                val isSelected = style == currentTheme
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(style.surfaceHex),
                    border = BorderStroke(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color(style.accentHex) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .width(138.dp)
                        .clickable { onSelectTheme(style) }
                        .testTag("theme_card_${style.name}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(style.primaryHex))
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(style.accentHex))
                            )
                            Spacer(Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "مفعل",
                                    tint = Color(style.accentHex),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        val labelColor = if (style == AppThemeStyle.MIDNIGHT_BLUE || style == AppThemeStyle.ROYAL_PURPLE) {
                            Color.White
                        } else Color(0xFF1A1A1A)
                        Text(style.arabicName, style = MaterialTheme.typography.titleSmall, color = labelColor)
                        Text(style.subtitle, style = MaterialTheme.typography.labelSmall, color = labelColor.copy(alpha = 0.72f))
                    }
                }
            }
        }
    }
}

// Section 9: Video Size / Aspect Ratio (مقاس الفيديو)
@Composable
fun VideoSizeSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    selectedRatio: AspectRatioPreset,
    onSelectRatio: (AspectRatioPreset) -> Unit
) {
    EditorSectionCard(
        sectionIndex = 9,
        title = "مقاس الفيديو",
        summary = "${selectedRatio.arabicLabel} (${selectedRatio.platformHint})",
        icon = Icons.Filled.AspectRatio,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectRatioPreset.entries.forEach { preset ->
                val isSelected = preset == selectedRatio
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectRatio(preset) }
                        .testTag("aspect_ratio_${preset.name}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.height(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(preset.widthRatio / preset.heightRatio)
                                    .height(32.dp)
                                    .border(
                                        1.8.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                        Text(preset.ratioTag, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = preset.platformHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Section 10: Auto Titles (العناوين التلقائية: السورة أعلى والقارئ أسفل)
@Composable
fun AutoTitlesSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    project: VideoProject,
    onUpdateProject: ((VideoProject) -> VideoProject) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    EditorSectionCard(
        sectionIndex = 10,
        title = "العناوين التلقائية (السورة والقارئ)",
        summary = "عنوان السورة أعلى • اسم القارئ أسفل",
        icon = Icons.Filled.Subtitles,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("اسم السورة (أعلى)") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("اسم القارئ (أسفل)") }
            )
        }

        if (selectedTab == 0) {
            TextLayerControls(
                style = project.surahTitleStyle,
                onStyleChange = { updated -> onUpdateProject { it.copy(surahTitleStyle = updated) } },
                showEnabledToggle = true,
                enabledToggleLabel = "إظهار اسم السورة تلقائياً",
                showFontPicker = true,
                showBackgroundPillToggle = true,
                minSizeSp = 12f,
                maxSizeSp = 32f
            )
        } else {
            TextLayerControls(
                style = project.reciterTitleStyle,
                onStyleChange = { updated -> onUpdateProject { it.copy(reciterTitleStyle = updated) } },
                showEnabledToggle = true,
                enabledToggleLabel = "إظهار اسم القارئ تلقائياً",
                showFontPicker = true,
                showBackgroundPillToggle = true,
                minSizeSp = 10f,
                maxSizeSp = 28f
            )
        }
    }
}

// Section 11: User Info / Watermark (معلومات المستخدم)
@Composable
fun WatermarkSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    project: VideoProject,
    onUpdateProject: ((VideoProject) -> VideoProject) -> Unit
) {
    EditorSectionCard(
        sectionIndex = 11,
        title = "معلومات المستخدم (العلامة المائية)",
        summary = if (project.watermarkStyle.enabled && project.watermarkText.isNotBlank()) project.watermarkText else "اختياري (مغلق)",
        icon = Icons.Filled.Badge,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("عرض العلامة المائية في الفيديو", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = project.watermarkStyle.enabled,
                onCheckedChange = { enabled ->
                    onUpdateProject { it.copy(watermarkStyle = it.watermarkStyle.copy(enabled = enabled)) }
                }
            )
        }

        OutlinedTextField(
            value = project.watermarkText,
            onValueChange = { txt -> onUpdateProject { it.copy(watermarkText = txt) } },
            label = { Text("اسم الصفحة أو الحساب (مثال: @quran_clips)") },
            placeholder = { Text("اتركه فارغاً لعدم العرض") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("watermark_text_input")
        )

        if (project.watermarkStyle.enabled) {
            TextLayerControls(
                style = project.watermarkStyle,
                onStyleChange = { updated -> onUpdateProject { it.copy(watermarkStyle = updated) } },
                showEnabledToggle = false,
                showFontPicker = true,
                showBackgroundPillToggle = true,
                minSizeSp = 10f,
                maxSizeSp = 24f
            )
        }
    }
}

// Section 12: Save / Export Video (حفظ الفيديو)
@Composable
fun ExportVideoSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    project: VideoProject,
    exportState: ExportUiState,
    batchCount: Int,
    onSelectResolution: (ExportResolution) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onSaveToGallery: (File) -> Unit,
    onShareVideo: (File) -> Unit,
    onOpenBatchQueue: () -> Unit
) {
    EditorSectionCard(
        sectionIndex = 12,
        title = "حفظ الفيديو والتصدير (MP4)",
        summary = "${project.exportResolution.arabicLabel} • ${project.aspectRatio.ratioTag}",
        icon = Icons.Filled.MovieCreation,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("دقة التصدير", style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ExportResolution.entries.forEachIndexed { idx, res ->
                    SegmentedButton(
                        selected = project.exportResolution == res,
                        onClick = { onSelectResolution(res) },
                        shape = SegmentedButtonDefaults.itemShape(idx, ExportResolution.entries.size),
                        modifier = Modifier.testTag("resolution_${res.name}")
                    ) {
                        val dims = project.aspectRatio.outputDimensions(res)
                        Text("${res.badge} (${dims.first}×${dims.second})", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        when (exportState) {
            is ExportUiState.Idle -> {
                Button(
                    onClick = onStartExport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_video_button")
                ) {
                    Icon(Icons.Filled.MovieCreation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تصدير الفيديو الآن (MP4)", style = MaterialTheme.typography.titleSmall)
                }
            }
            is ExportUiState.Exporting -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(exportState.stageLabel, style = MaterialTheme.typography.bodyMedium)
                        Text("${toArabicDigits(exportState.progressPercent)}%", style = MaterialTheme.typography.titleSmall)
                    }
                    LinearProgressIndicator(
                        progress = { (exportState.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onCancelExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_export_button")
                    ) {
                        Text("إلغاء التصدير")
                    }
                }
            }
            is ExportUiState.Completed -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("تم تصدير الفيديو بنجاح!", style = MaterialTheme.typography.titleSmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSaveToGallery(exportState.file) },
                                enabled = !exportState.savedToGallery,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_to_gallery_button")
                            ) {
                                Icon(Icons.Filled.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (exportState.savedToGallery) "تم الحفظ" else "حفظ في المعرض")
                            }
                            OutlinedButton(
                                onClick = { onShareVideo(exportState.file) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_video_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مشاركة")
                            }
                        }
                        OutlinedButton(
                            onClick = onStartExport,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إعادة التصدير بإعدادات جديدة")
                        }
                    }
                }
            }
            is ExportUiState.Failed -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = exportState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onStartExport, modifier = Modifier.fillMaxWidth()) {
                        Text("إعادة المحاولة")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenBatchQueue,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_batch_queue_button")
        ) {
            Icon(Icons.Filled.QueueMusic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (batchCount > 0) "طابور تصدير عدة سور (${toArabicDigits(batchCount)})"
                else "معالجة دفعة سور متعددة (Batch)"
            )
        }
    }
}
