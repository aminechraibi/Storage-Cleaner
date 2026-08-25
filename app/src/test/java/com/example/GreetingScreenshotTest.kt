package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.DeviceStorageStats
import com.example.ui.components.StorageGaugeCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun storage_gauge_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        StorageGaugeCard(
          stats = DeviceStorageStats(
            totalBytes = 64L * 1024 * 1024 * 1024,
            usedBytes = 48L * 1024 * 1024 * 1024,
            freeBytes = 16L * 1024 * 1024 * 1024,
            safeCleanableBytes = 2L * 1024 * 1024 * 1024
          ),
          onQuickScanClick = {},
          onDeepScanClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
