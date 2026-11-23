package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.BatchJobItem
import com.example.domain.model.BatchJobStatus
import com.example.domain.model.ExportResolution
import com.example.domain.model.Surah
import com.example.util.ArabicTextRenderer.toArabicDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchQueueScreen(
    surahs: List<Surah>,
    queue: List<BatchJobItem>,
    isRunning: Boolean,
    defaultResolution: ExportResolution,
    onNavigateBack: () -> Unit,
    onAddSurah: (Surah) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onStartBatch: (ExportResolution) -> Unit,
    onCancelBatch: () -> Unit,
    onSaveItemToGallery: (BatchJobItem) -> Unit,
    onShareItem: (BatchJobItem) -> Unit
) {
    var selectedResolution by remember { mutableStateOf(defaultResolution) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("معالجة دفعة سور (تصدير متزامن)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "أضف عدة سور للطابور ليتم تصييرها جميعاً بالتصميم والإعدادات الحالية (القارئ والخط والتشكيل والخلفية والمقاس).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ExportResolution.entries.forEachIndexed { idx, res ->
                    SegmentedButton(
                        selected = selectedResolution == res,
                        onClick = { selectedResolution = res },
                        enabled = !isRunning,
                        shape = SegmentedButtonDefaults.itemShape(idx, ExportResolution.entries.size)
                    ) {
                        Text(res.arabicLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    enabled = !isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_add_surahs_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة سور للطابور")
                }

                if (queue.any { it.status == BatchJobStatus.COMPLETED || it.status == BatchJobStatus.CANCELLED }) {
                    OutlinedButton(onClick = onClearCompleted, enabled = !isRunning) {
                        Text("مسح المكتمل")
                    }
                }
            }

            if (queue.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("الطابور فارغ — أضف سوراً للبدء", style = MaterialTheme.typography.titleSmall)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(queue, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.surahName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "الآيات ${toArabicDigits(item.startAyah)}-${toArabicDigits(item.endAyah)} • ${item.reciterName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = item.status.arabicLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when (item.status) {
                                            BatchJobStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                            BatchJobStatus.FAILED -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                    if (!isRunning && item.status != BatchJobStatus.RENDERING) {
                                        IconButton(onClick = { onRemoveItem(item.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "إزالة")
                                        }
                                    }
                                }

                                if (item.status == BatchJobStatus.DOWNLOADING || item.status == BatchJobStatus.RENDERING) {
                                    LinearProgressIndicator(
                                        progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (item.status == BatchJobStatus.COMPLETED) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onSaveItemToGallery(item) },
                                            enabled = !item.savedToGallery,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (item.savedToGallery) "تم الحفظ" else "حفظ")
                                        }
                                        OutlinedButton(
                                            onClick = { onShareItem(item) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("مشاركة")
                                        }
                                    }
                                }

                                if (item.status == BatchJobStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                                    Text(
                                        text = item.errorMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isRunning) {
                OutlinedButton(
                    onClick = onCancelBatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("batch_cancel_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("إيقاف التصيير المتزامن")
                }
            } else {
                Button(
                    onClick = { onStartBatch(selectedResolution) },
                    enabled = queue.any { it.status == BatchJobStatus.QUEUED || it.status == BatchJobStatus.FAILED },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("batch_start_button")
                ) {
                    Icon(Icons.Filled.MovieCreation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("بدء التصيير المتزامن للطابور")
                }
            }
        }
    }

    if (showAddDialog) {
        var search by remember { mutableStateOf("") }
        val selectedIds = remember { mutableStateListOf<Int>() }
        val filtered = remember(surahs, search) {
            val q = search.trim()
            if (q.isEmpty()) surahs else surahs.filter { it.arabicName.contains(q) || it.number.toString() == q }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة سور إلى الطابور", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("ابحث عن سورة...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(filtered, key = { it.number }) { surah ->
                            val checked = surah.number in selectedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedIds.remove(surah.number) else selectedIds.add(surah.number)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (checked) selectedIds.remove(surah.number) else selectedIds.add(surah.number)
                                    }
                                )
                                Text(
                                    text = "${toArabicDigits(surah.number)}. ${surah.arabicName} (${toArabicDigits(surah.ayahCount)} آية)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedIds.forEach { num ->
                            surahs.firstOrNull { it.number == num }?.let(onAddSurah)
                        }
                        showAddDialog = false
                    },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة (${toArabicDigits(selectedIds.size)})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
