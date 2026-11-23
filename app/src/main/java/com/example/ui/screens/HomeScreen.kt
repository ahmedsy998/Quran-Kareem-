package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.Reciter
import com.example.domain.model.Surah
import com.example.domain.model.VideoProject
import com.example.util.ArabicTextRenderer.toArabicDigits

private data class QuickSectionTile(
    val sectionIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun HomeScreen(
    hasLastProject: Boolean,
    lastProject: VideoProject,
    lastSurah: Surah?,
    lastReciter: Reciter,
    currentTheme: AppThemeStyle,
    savedProjectsCount: Int,
    batchCount: Int,
    onContinueLastProject: () -> Unit,
    onCreateNewProject: () -> Unit,
    onOpenSectionInEditor: (Int) -> Unit,
    onOpenMyProjects: () -> Unit,
    onOpenBatchQueue: () -> Unit,
    onCycleTheme: () -> Unit
) {
    val quickTiles = listOf(
        QuickSectionTile(1, "القارئ", "٢٧+ قارئ مع البحث والمفضلة", Icons.Filled.Mic),
        QuickSectionTile(2, "السور والآيات", "١١٤ سورة • اختيار نطاق الآيات", Icons.Filled.MenuBook),
        QuickSectionTile(3, "الخطوط", "٨ خطوط قرآنية وعربية", Icons.Filled.TextFields),
        QuickSectionTile(4, "تشكيل الحروف", "لون وحجم مستقل للحركات", Icons.Filled.FormatColorText),
        QuickSectionTile(5, "خلفية الفيديو", "صور وفيديوهات مدمجة أو من الهاتف", Icons.Filled.PhotoLibrary),
        QuickSectionTile(8, "أنماط الواجهة", currentTheme.arabicName, Icons.Filled.ColorLens),
        QuickSectionTile(9, "مقاس الفيديو", "٩:١٦ • ١٦:٩ • ١:١ • ٤:٥", Icons.Filled.AspectRatio),
        QuickSectionTile(12, "حفظ الفيديو", "تصدير MP4 بدقة 720p / 1080p", Icons.Filled.MovieCreation)
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_launcher_quran),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.home_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onCycleTheme,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_cycle_theme_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ColorLens,
                                    contentDescription = "تبديل النمط",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Continue Last Project Card
                if (hasLastProject) {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("continue_last_project_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_continue_project),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = lastProject.aspectRatio.ratioTag,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = lastSurah?.arabicName ?: lastProject.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${lastReciter.arabicName} • الآيات ${toArabicDigits(lastProject.startAyah)} إلى ${toArabicDigits(lastProject.endAyah)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Button(
                                    onClick = onContinueLastProject,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("continue_last_project_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.home_continue_button))
                                }
                            }
                        }
                    }
                }

                // Primary Actions Row: New Video + My Projects + Batch Queue
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onCreateNewProject,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("new_video_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_new_video),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onOpenMyProjects,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_my_projects_button")
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("مشاريعي (${toArabicDigits(savedProjectsCount)})", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onOpenBatchQueue,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_batch_queue_button")
                            ) {
                                Icon(Icons.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (batchCount > 0) "دفعة (${toArabicDigits(batchCount)})" else "دفعة سور",
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Grid of Main Sections
                item {
                    Text(
                        text = stringResource(R.string.home_sections_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val rows = quickTiles.chunked(2)
                items(rows.size) { rowIdx ->
                    val pair = rows[rowIdx]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { tile ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenSectionInEditor(tile.sectionIndex) }
                                    .testTag("home_tile_${tile.sectionIndex}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = tile.icon,
                                            contentDescription = tile.title,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = tile.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tile.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
