package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CleanupResultDialog
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.CleanResultsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DuplicatesScreen
import com.example.ui.screens.ExclusionsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.ResidualScreen
import com.example.ui.screens.ScanProgressScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimilarPhotosScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application as CleanerApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StorageCleanerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StorageCleanerApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val deleteConfirmState by viewModel.deleteConfirmDialog.collectAsStateWithLifecycle()
    val cleanupResult by viewModel.lastCleanupResult.collectAsStateWithLifecycle()
    val isRecycleBinEnabled by viewModel.recycleBinEnabled.collectAsStateWithLifecycle()

    // Request necessary runtime permissions gracefully
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted, refresh stats
        viewModel.refreshStorageStats()
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        permissionLauncher.launch(permissions)
    }

    // Screen navigation
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
            Screen.SCAN_PROGRESS -> ScanProgressScreen(viewModel = viewModel)
            Screen.CLEAN_RESULTS, Screen.LARGE_FILES, Screen.OLD_FILES -> CleanResultsScreen(viewModel = viewModel)
            Screen.DUPLICATES -> DuplicatesScreen(viewModel = viewModel)
            Screen.SIMILAR_PHOTOS -> SimilarPhotosScreen(viewModel = viewModel)
            Screen.APP_MANAGER -> AppManagerScreen(viewModel = viewModel)
            Screen.RESIDUAL_FOLDERS -> ResidualScreen(viewModel = viewModel)
            Screen.RECYCLE_BIN -> RecycleBinScreen(viewModel = viewModel)
            Screen.EXCLUSIONS -> ExclusionsScreen(viewModel = viewModel)
            Screen.SEARCH -> SearchScreen(viewModel = viewModel)
            Screen.HISTORY -> HistoryScreen(viewModel = viewModel)
            Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
        }
    }

    // Confirmation & Result Dialogs
    deleteConfirmState?.let { (count, bytes) ->
        DeleteConfirmationDialog(
            itemCount = count,
            totalBytes = bytes,
            isRecycleBinEnabled = isRecycleBinEnabled,
            onConfirm = { viewModel.confirmCleanup() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }

    cleanupResult?.let { result ->
        CleanupResultDialog(
            result = result,
            onDismiss = { viewModel.dismissCleanupResult() }
        )
    }
}
