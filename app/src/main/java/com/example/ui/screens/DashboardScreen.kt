package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CleanCategory
import com.example.model.FileType
import com.example.ui.components.CategorySummaryRow
import com.example.ui.components.QuickCleanCard
import com.example.ui.components.StorageFeatureCard
import com.example.ui.components.StorageGaugeCard
import com.example.ui.components.StorageItemRow
import com.example.ui.theme.AmberReview
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseSensitive
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.StorageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.storageStats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DEVICE STORAGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Storage Cleaner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.SEARCH) },
                        modifier = Modifier.testTag("nav_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp)
        ) {
            // Minimalist Circular Storage Gauge Card
            item {
                StorageGaugeCard(
                    stats = stats,
                    onQuickScanClick = { viewModel.startScan(deepScan = false) },
                    onDeepScanClick = { viewModel.startScan(deepScan = true) }
                )
            }

            // Quick Clean Safe Junk Action
            item {
                QuickCleanCard(
                    safeBytes = stats.safeCleanableBytes,
                    onCleanClick = { viewModel.cleanSafeJunk() }
                )
            }

            // Category Summary Row (Safe Junk, Review, Sensitive)
            item {
                CategorySummaryRow(
                    safeBytes = stats.safeCleanableBytes,
                    reviewBytes = stats.reviewCleanableBytes,
                    sensitiveBytes = stats.sensitiveCleanableBytes,
                    onCategoryClick = { cat -> viewModel.loadCategory(cat) }
                )
            }

            // Cleaners Grid Section Header
            item {
                Text(
                    text = "Storage Analyzers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StorageFeatureCard(
                        title = "Large Files",
                        subtitle = "Find biggest space eaters",
                        icon = Icons.Default.Storage,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.loadLargeFiles() },
                        testTag = "card_large_files",
                        modifier = Modifier.weight(1f)
                    )
                    StorageFeatureCard(
                        title = "Duplicates",
                        subtitle = "Exact file copies",
                        icon = Icons.Default.ContentCopy,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.loadDuplicates() },
                        testTag = "card_duplicates",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StorageFeatureCard(
                        title = "Similar Photos",
                        subtitle = "Visual photo clusters",
                        icon = Icons.Default.PhotoLibrary,
                        iconColor = AmberReview,
                        onClick = { viewModel.loadSimilarPhotos() },
                        testTag = "card_similar_photos",
                        modifier = Modifier.weight(1f)
                    )
                    StorageFeatureCard(
                        title = "App Manager",
                        subtitle = "Sizes, caches, uninstall",
                        icon = Icons.Default.Apps,
                        iconColor = EmeraldSafe,
                        onClick = { viewModel.loadApps() },
                        testTag = "card_app_manager",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StorageFeatureCard(
                        title = "Residual & Empty",
                        subtitle = "Leftovers & blank folders",
                        icon = Icons.Default.Folder,
                        iconColor = EmeraldSafe,
                        onClick = { viewModel.loadResidualFolders() },
                        testTag = "card_residual",
                        modifier = Modifier.weight(1f)
                    )
                    StorageFeatureCard(
                        title = "Old Files",
                        subtitle = "Unused for > 60 days",
                        icon = Icons.Default.HourglassEmpty,
                        iconColor = AmberReview,
                        onClick = { viewModel.loadOldFiles() },
                        testTag = "card_old_files",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StorageFeatureCard(
                        title = "APKs & Zips",
                        subtitle = "Installer packages",
                        icon = Icons.Default.Android,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.loadType(FileType.APK) },
                        testTag = "card_apks",
                        modifier = Modifier.weight(1f)
                    )
                    StorageFeatureCard(
                        title = "Recycle Bin",
                        subtitle = "Restorable deleted files",
                        icon = Icons.Default.Delete,
                        iconColor = RoseSensitive,
                        onClick = { viewModel.navigateTo(Screen.RECYCLE_BIN) },
                        testTag = "card_recycle_bin",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StorageFeatureCard(
                        title = "Exclusions",
                        subtitle = "Protected files/folders",
                        icon = Icons.Default.Security,
                        iconColor = EmeraldSafe,
                        onClick = { viewModel.navigateTo(Screen.EXCLUSIONS) },
                        testTag = "card_exclusions",
                        modifier = Modifier.weight(1f)
                    )
                    StorageFeatureCard(
                        title = "Clean History",
                        subtitle = "Lifetime saved reports",
                        icon = Icons.Default.History,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.navigateTo(Screen.HISTORY) },
                        testTag = "card_history",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

