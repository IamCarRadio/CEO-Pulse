package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// CEO Pulse enforces a clean, bright, modern executive light design
private val CeoPulseLightColorScheme = lightColorScheme(
  primary = TealPrimary,
  onPrimary = Color.White,
  primaryContainer = TealLightContainer,
  onPrimaryContainer = TealOnContainer,
  secondary = TealAccent,
  onSecondary = Color.White,
  secondaryContainer = TealLightContainer,
  onSecondaryContainer = TealOnContainer,
  tertiary = TealPrimaryDark,
  onTertiary = Color.White,
  background = BackgroundCanvas,
  onBackground = TextHeadline,
  surface = SurfaceCard,
  onSurface = TextHeadline,
  surfaceVariant = SurfaceSubtle,
  onSurfaceVariant = TextBody,
  outline = BorderSubtle,
  outlineVariant = BorderMedium,
  error = ErrorRed,
  onError = Color.White,
  errorContainer = ErrorRedBg,
  onErrorContainer = ErrorRed
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For CEO Pulse, we default to the clean executive light theme
  forceLightTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = CeoPulseLightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = BackgroundCanvas.toArgb()
        window.navigationBarColor = SurfaceCard.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
