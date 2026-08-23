package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CleanCategory
import com.example.model.CleanupResult
import com.example.model.DeviceStorageStats
import com.example.model.FileType
import com.example.model.StorageItem
import com.example.ui.theme.AmberReview
import com.example.ui.theme.CleanGreenTrack
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RoseSensitive
import com.example.util.StorageUtils
import java.io.File

@Composable
fun StorageGaugeCard(
    stats: DeviceStorageStats,
    onQuickScanClick: () -> Unit,
    onDeepScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = stats.usedPercentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "StorageGauge"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_gauge_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Minimalist Gauge
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .testTag("storage_gauge_circular_ring"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    // Background track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedPercent * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${(stats.usedPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = onContainerColor
                    )
                    Text(
                        text = "USED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp
                        ),
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "${StorageUtils.formatBytes(stats.usedBytes)} of ${StorageUtils.formatBytes(stats.totalBytes)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = onContainerColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${StorageUtils.formatBytes(stats.freeBytes)} Free • Safe cleanable: ${StorageUtils.formatBytes(stats.safeCleanableBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onQuickScanClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_scan_button"),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Quick Scan",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Quick Scan", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDeepScanClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("deep_scan_button"),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Deep Scan",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun QuickCleanCard(
    safeBytes: Long,
    onCleanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (safeBytes <= 0L) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_clean_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Clean Safe",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Safe Junk Cleanable",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = StorageUtils.formatBytes(safeBytes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                onClick = onCleanClick,
                modifier = Modifier.testTag("clean_safe_action_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Clean Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategorySummaryRow(
    safeBytes: Long,
    reviewBytes: Long,
    sensitiveBytes: Long,
    onCategoryClick: (CleanCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CategoryPill(
            title = "Safe Junk",
            bytes = safeBytes,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { onCategoryClick(CleanCategory.SAFE_JUNK) },
            testTag = "pill_safe_junk"
        )
        CategoryPill(
            title = "Review",
            bytes = reviewBytes,
            color = AmberReview,
            modifier = Modifier.weight(1f),
            onClick = { onCategoryClick(CleanCategory.REVIEW) },
            testTag = "pill_review"
        )
        CategoryPill(
            title = "Sensitive",
            bytes = sensitiveBytes,
            color = RoseSensitive,
            modifier = Modifier.weight(1f),
            onClick = { onCategoryClick(CleanCategory.SENSITIVE) },
            testTag = "pill_sensitive"
        )
    }
}

@Composable
fun CategoryPill(
    title: String,
    bytes: Long,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = StorageUtils.formatBytes(bytes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StorageFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StorageItemRow(
    item: StorageItem,
    isSelected: Boolean,
    onToggleSelect: (StorageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleSelect(item) }
            .testTag("item_row_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon or thumbnail
            ItemIconOrThumbnail(item = item)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = StorageUtils.formatBytes(item.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = StorageUtils.formatDate(item.lastModified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect(item) },
                modifier = Modifier.testTag("checkbox_${item.id}")
            )
        }
    }
}

@Composable
fun ItemIconOrThumbnail(item: StorageItem) {
    val file = File(item.path)
    val isImage = item.type == FileType.MEDIA_IMAGE || item.type == FileType.SIMILAR_PHOTO || item.type == FileType.SCREENSHOT
    if (isImage && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        val (icon, color) = when (item.type) {
            FileType.APK -> Icons.Default.Android to EmeraldSafe
            FileType.MEDIA_VIDEO -> Icons.Default.Movie to MaterialTheme.colorScheme.primary
            FileType.MEDIA_AUDIO -> Icons.Default.AudioFile to MaterialTheme.colorScheme.primary
            FileType.DOCUMENT -> Icons.Default.Description to AmberReview
            FileType.ARCHIVE -> Icons.Default.Archive to AmberReview
            FileType.EMPTY_FOLDER, FileType.RESIDUAL -> Icons.Default.Folder to EmeraldSafe
            else -> Icons.Default.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemCount: Int,
    totalBytes: Long,
    isRecycleBinEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isRecycleBinEnabled) MaterialTheme.colorScheme.primary else RoseSensitive
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isRecycleBinEnabled) "Move to Recycle Bin?" else "Permanently Delete?")
            }
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to clean $itemCount item(s)?"
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Estimated space to free: ${StorageUtils.formatBytes(totalBytes)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!isRecycleBinEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This action is permanent and cannot be undone.",
                        color = RoseSensitive,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecycleBinEnabled) MaterialTheme.colorScheme.primary else RoseSensitive,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(text = if (isRecycleBinEnabled) "Move to Trash" else "Delete Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(text = "Cancel", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun CleanupResultDialog(
    result: CleanupResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(text = "Cleanup Complete! ✨", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Successfully freed ${StorageUtils.formatBytes(result.freedBytes)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldSafe
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Deleted ${result.itemsDeleted} item(s)")
                if (result.failedCount > 0) {
                    Text(
                        text = "${result.failedCount} protected or excluded item(s) skipped",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Free space: ${StorageUtils.formatBytes(result.beforeFreeBytes)} → ${StorageUtils.formatBytes(result.afterFreeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("cleanup_result_done_button")
            ) {
                Text(text = "Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

