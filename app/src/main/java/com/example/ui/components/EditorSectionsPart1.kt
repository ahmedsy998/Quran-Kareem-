package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.BackgroundType
import com.example.domain.model.BundledBackgrounds
import com.example.domain.model.FontOption
import com.example.domain.model.Reciter
import com.example.domain.model.SingleLineOverflowMode
import com.example.domain.model.Surah
import com.example.domain.model.SyncedAyahLine
import com.example.domain.model.VideoProject
import com.example.ui.viewmodel.AudioLoadState
import com.example.util.ArabicTextRenderer
import com.example.util.ArabicTextRenderer.toArabicDigits
import java.io.File
import kotlin.math.roundToInt

// Section 1: Reciter (القارئ)
@Composable
fun ReciterSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    reciters: List<Reciter>,
    selectedReciter: Reciter,
    onSelectReciter: (Reciter) -> Unit,
    onToggleFavorite: (Reciter) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }

    val filtered = remember(reciters, searchQuery, favoritesOnly) {
        reciters
            .filter { r ->
                (!favoritesOnly || r.isFavorite) &&
                    (searchQuery.isBlank() || r.arabicName.contains(searchQuery.trim(), ignoreCase = true))
            }
            .sortedByDescending { it.isFavorite }
    }

    EditorSectionCard(
        sectionIndex = 1,
        title = "القارئ",
        summary = selectedReciter.arabicName,
        icon = Icons.Filled.Mic,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث عن قارئ...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reciter_search_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !favoritesOnly,
                onClick = { favoritesOnly = false },
                label = { Text("الكل (${toArabicDigits(reciters.size)})") }
            )
            FilterChip(
                selected = favoritesOnly,
                onClick = { favoritesOnly = true },
                label = { Text("المفضلة (${toArabicDigits(reciters.count { it.isFavorite })})") },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.id }) { reciter ->
                val isSelected = reciter.id == selectedReciter.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectReciter(reciter) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reciter.arabicName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = reciter.styleLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "محدد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = { onToggleFavorite(reciter) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (reciter.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "تفضيل القارئ",
                                tint = if (reciter.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Section 2: Surahs (السور + نطاق الآيات)
@Composable
fun SurahSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    surahs: List<Surah>,
    project: VideoProject,
    currentSurah: Surah?,
    onSelectSurah: (Surah) -> Unit,
    onRangeChange: (Int, Int) -> Unit,
    onBismillahToggle: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSurahs = remember(surahs, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) surahs
        else surahs.filter {
            it.arabicName.contains(q) || it.number.toString() == q || it.englishName.contains(q, ignoreCase = true)
        }
    }

    val summaryText = currentSurah?.let {
        "${it.arabicName} • الآيات ${toArabicDigits(project.startAyah)} - ${toArabicDigits(project.endAyah)}"
    } ?: "اختر السورة"

    EditorSectionCard(
        sectionIndex = 2,
        title = "السور ونطاق الآيات",
        summary = summaryText,
        icon = Icons.Filled.MenuBook,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        if (currentSurah != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "نطاق الآيات (${currentSurah.arabicName} - ${toArabicDigits(currentSurah.ayahCount)} آية)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AyahStepper(
                            label = "من الآية",
                            value = project.startAyah,
                            min = 1,
                            max = project.endAyah,
                            onValueChange = { onRangeChange(it, project.endAyah) },
                            modifier = Modifier.weight(1f)
                        )
                        AyahStepper(
                            label = "إلى الآية",
                            value = project.endAyah,
                            min = project.startAyah,
                            max = currentSurah.ayahCount,
                            onValueChange = { onRangeChange(project.startAyah, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { onRangeChange(1, currentSurah.ayahCount) },
                            label = { Text("كامل السورة") }
                        )
                        AssistChip(
                            onClick = { onRangeChange(1, currentSurah.ayahCount.coerceAtMost(5)) },
                            label = { Text("أول ٥ آيات") }
                        )
                    }

                    if (currentSurah.number != 1 && currentSurah.number != 9 && project.startAyah == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عرض البسملة في بداية السورة", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = project.showBismillahIntro,
                                onCheckedChange = onBismillahToggle
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث باسم السورة أو رقمها...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث السور") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("surah_search_input")
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                val isSelected = surah.number == project.surahNumber
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectSurah(surah) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toArabicDigits(surah.number),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(surah.arabicName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${surah.revelationTypeArabic} • ${toArabicDigits(surah.ayahCount)} آية",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "محددة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            OutlinedButton(
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp)
            ) { Text("-") }
            Text(
                text = toArabicDigits(value),
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp)
            ) { Text("+") }
        }
    }
}

// Section 3: Fonts (الخطوط)
@Composable
fun FontsSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    project: VideoProject,
    onUpdateProject: ((VideoProject) -> VideoProject) -> Unit
) {
    val selectedFont = FontOption.fromId(project.ayahStyle.fontId)
    EditorSectionCard(
        sectionIndex = 3,
        title = "الخطوط وتنسيق الآية",
        summary = "${selectedFont.arabicName} • ${project.ayahStyle.fontSizeSp.roundToInt()} sp",
        icon = Icons.Filled.TextFields,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        // Live Sample Line
        val sampleFont = remember(selectedFont) { FontFamily(Font(selectedFont.fontResId)) }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF101820),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                fontFamily = sampleFont,
                fontSize = 24.sp,
                color = Color(project.ayahStyle.colorArgb),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Text("اختر الخط العربي أو القرآني", style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FontOption.entries.forEach { font ->
                val isSelected = font.id == project.ayahStyle.fontId
                val family = remember(font) { FontFamily(Font(font.fontResId)) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onUpdateProject { it.copy(ayahStyle = it.ayahStyle.copy(fontId = font.id)) }
                        },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = font.arabicName,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = if (font.isQuranic) "خط مصحفي مخصص للتشكيل" else "خط عربي عصري",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
                            fontFamily = family,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Overflow single-line behavior
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("الآيات الطويلة في سطر واحد", style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SingleLineOverflowMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = project.overflowMode == mode,
                        onClick = { onUpdateProject { it.copy(overflowMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(idx, SingleLineOverflowMode.entries.size)
                    ) {
                        Text(mode.arabicLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Line Spacing
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تباعد الأسطر", style = MaterialTheme.typography.bodyMedium)
                Text(String.format("%.2f", project.lineSpacingMultiplier), style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = project.lineSpacingMultiplier.coerceIn(1.0f, 2.0f),
                onValueChange = { v -> onUpdateProject { it.copy(lineSpacingMultiplier = v) } },
                valueRange = 1.0f..2.0f
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("إظهار الآية السابقة والتالية باهتة", style = MaterialTheme.typography.bodyMedium)
                Text("افتراضياً مغلق للتركيز على آية واحدة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = project.showDimmedAdjacentAyahs,
                onCheckedChange = { enabled -> onUpdateProject { it.copy(showDimmedAdjacentAyahs = enabled) } }
            )
        }

        // Per-element full settings for Ayah text
        TextLayerControls(
            style = project.ayahStyle,
            onStyleChange = { newStyle -> onUpdateProject { it.copy(ayahStyle = newStyle) } },
            showEnabledToggle = false,
            showFontPicker = false,
            showBackgroundPillToggle = false,
            minSizeSp = 14f,
            maxSizeSp = 46f
        )
    }
}

// Section 4: Tashkeel Coloring (تشكيل الحروف)
@Composable
fun TashkeelSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    project: VideoProject,
    onUpdateProject: ((VideoProject) -> VideoProject) -> Unit
) {
    EditorSectionCard(
        sectionIndex = 4,
        title = "تشكيل الحروف",
        summary = if (project.tashkeelColoringEnabled) "مفعل بلون مستقل" else "لون موحد مع النص",
        icon = Icons.Filled.FormatColorText,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        val font = remember(project.ayahStyle.fontId) {
            FontFamily(Font(FontOption.fromId(project.ayahStyle.fontId).fontResId))
        }
        val sampleText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val sampleAnnotated = remember(
            project.tashkeelColoringEnabled,
            project.tashkeelColorArgb,
            project.ayahStyle.colorArgb,
            project.tashkeelScale
        ) {
            ArabicTextRenderer.buildTashkeelAnnotatedString(
                text = sampleText,
                baseColor = Color(project.ayahStyle.colorArgb),
                tashkeelEnabled = project.tashkeelColoringEnabled,
                tashkeelColor = Color(project.tashkeelColorArgb),
                baseFontSizeSp = 24f,
                tashkeelScale = project.tashkeelScale
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0E151B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = sampleAnnotated,
                fontFamily = font,
                fontSize = 24.sp,
                color = Color(project.ayahStyle.colorArgb),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("تلوين الحركات والتشكيل بلون مختلف", style = MaterialTheme.typography.bodyLarge)
                Text("يميز الفتحة والضمة والكسرة والشدة وعلامات المصحف", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = project.tashkeelColoringEnabled,
                onCheckedChange = { enabled -> onUpdateProject { it.copy(tashkeelColoringEnabled = enabled) } }
            )
        }

        if (project.tashkeelColoringEnabled) {
            ColorPickerRow(
                label = "لون التشكيل والحركات",
                selectedColorArgb = project.tashkeelColorArgb,
                onColorSelected = { argb -> onUpdateProject { it.copy(tashkeelColorArgb = argb) } }
            )

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("حجم وإبراز التشكيل", style = MaterialTheme.typography.bodyMedium)
                    Text("${(project.tashkeelScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = project.tashkeelScale.coerceIn(0.85f, 1.35f),
                    onValueChange = { scale -> onUpdateProject { it.copy(tashkeelScale = scale) } },
                    valueRange = 0.85f..1.35f
                )
            }
        }
    }
}

// Section 5: Video Background (خلفية الفيديو)
@Composable
fun BackgroundSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    project: VideoProject,
    onUpdateProject: ((VideoProject) -> VideoProject) -> Unit,
    onImportUserMedia: (Uri, Boolean) -> Unit
) {
    var selectedTab by remember(project.backgroundType) {
        mutableIntStateOf(
            if (project.backgroundType == BackgroundType.BUNDLED_VIDEO || project.backgroundType == BackgroundType.USER_VIDEO) 1 else 0
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportUserMedia(uri, false)
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportUserMedia(uri, true)
    }

    val bgLabel = when (project.backgroundType) {
        BackgroundType.BUNDLED_IMAGE -> BundledBackgrounds.findImage(project.bundledBackgroundId).arabicName
        BackgroundType.BUNDLED_VIDEO -> BundledBackgrounds.findVideo(project.bundledBackgroundId).arabicName
        BackgroundType.USER_IMAGE -> "صورة من الهاتف"
        BackgroundType.USER_VIDEO -> "فيديو من الهاتف"
    }

    EditorSectionCard(
        sectionIndex = 5,
        title = "خلفية الفيديو",
        summary = bgLabel,
        icon = Icons.Filled.PhotoLibrary,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("الصور") },
                icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "الصور") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("الفيديوهات") },
                icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = "الفيديوهات") }
            )
        }

        if (selectedTab == 0) {
            OutlinedButton(
                onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_image_background_button")
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("رفع صورة من الهاتف")
            }

            if (project.backgroundType == BackgroundType.USER_IMAGE && !project.userMediaLocalPath.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = File(project.userMediaLocalPath),
                        contentDescription = "الصورة المختارة",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Text("صورة مخصصة من جهازك قيد الاستخدام", style = MaterialTheme.typography.bodyMedium)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(BundledBackgrounds.images, key = { it.id }) { img ->
                    val isSelected = project.backgroundType == BackgroundType.BUNDLED_IMAGE &&
                        project.bundledBackgroundId == img.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(98.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onUpdateProject {
                                    it.copy(
                                        backgroundType = BackgroundType.BUNDLED_IMAGE,
                                        bundledBackgroundId = img.id
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 14f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = img.drawableResId),
                                contentDescription = img.arabicName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "محدد",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.White, CircleShape)
                                        .size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = img.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = {
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_video_background_button")
            ) {
                Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("رفع فيديو من الهاتف")
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(BundledBackgrounds.videos, key = { it.id }) { vid ->
                    val isSelected = project.backgroundType == BackgroundType.BUNDLED_VIDEO &&
                        project.bundledBackgroundId == vid.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(102.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onUpdateProject {
                                    it.copy(
                                        backgroundType = BackgroundType.BUNDLED_VIDEO,
                                        bundledBackgroundId = vid.id
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 14f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = vid.thumbnailResId),
                                contentDescription = vid.arabicName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = vid.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تكرار فيديو الخلفية تلقائياً", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = project.videoLoopEnabled,
                    onCheckedChange = { loop -> onUpdateProject { it.copy(videoLoopEnabled = loop) } }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = "صوت الخلفية مكتوم تلقائياً لإبقاء التلاوة صافية",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dim / Brightness slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تعتيم الخلفية لوضوح النص", style = MaterialTheme.typography.bodyMedium)
                Text("${(project.backgroundDim * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = project.backgroundDim.coerceIn(0f, 0.85f),
                onValueChange = { d -> onUpdateProject { it.copy(backgroundDim = d) } },
                valueRange = 0f..0.85f
            )
        }

        // Blur slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ضبابية الخلفية (Blur)", style = MaterialTheme.typography.bodyMedium)
                Text("${project.backgroundBlurRadius.roundToInt()} px", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = project.backgroundBlurRadius.coerceIn(0f, 20f),
                onValueChange = { b -> onUpdateProject { it.copy(backgroundBlurRadius = b) } },
                valueRange = 0f..20f
            )
        }
    }
}

// Section 6: Live Preview Controls (شاشة البث الحي)
@Composable
fun LivePreviewSectionCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    lines: List<SyncedAyahLine>,
    currentLineIndex: Int,
    playbackSpeed: Float,
    showSafeGuides: Boolean,
    audioLoadState: AudioLoadState,
    isCachedOffline: Boolean,
    onToggleSafeGuides: (Boolean) -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onJumpToLine: (Int) -> Unit
) {
    EditorSectionCard(
        sectionIndex = 6,
        title = "شاشة البث الحي",
        summary = if (isCachedOffline) "متاح بدون إنترنت • سرعة ${playbackSpeed}x" else "مزامنة فورية آية بآية",
        icon = Icons.Filled.LiveTv,
        expanded = expanded,
        onToggleExpand = onToggleExpand
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = when {
                    audioLoadState is AudioLoadState.Ready && audioLoadState.isEstimatedTiming ->
                        "المعاينة نشطة (توقيت آيات تقديري لهذا القارئ)"
                    isCachedOffline -> "التلاوة مخزنة محلياً وتعمل الآن بدون إنترنت"
                    else -> "الشاشة المثبتة بالأعلى تعكس كل تعديل لحظياً"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إظهار خطوط الهامش الآمن", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = showSafeGuides, onCheckedChange = onToggleSafeGuides)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("سرعة التلاوة في المعاينة", style = MaterialTheme.typography.bodyMedium)
            val speeds = listOf(0.75f to "0.75x", 1.0f to "1.0x عادي", 1.25f to "1.25x")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                speeds.forEachIndexed { idx, (spd, label) ->
                    SegmentedButton(
                        selected = playbackSpeed == spd,
                        onClick = { onChangeSpeed(spd) },
                        shape = SegmentedButtonDefaults.itemShape(idx, speeds.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (lines.isNotEmpty()) {
            Text("الانتقال السريع إلى آية:", style = MaterialTheme.typography.bodyMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lines.size) { idx ->
                    val line = lines[idx]
                    val label = if (line.isBismillah) "البسملة" else "آية ${toArabicDigits(line.numberInSurah)}"
                    FilterChip(
                        selected = idx == currentLineIndex,
                        onClick = { onJumpToLine(idx) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
