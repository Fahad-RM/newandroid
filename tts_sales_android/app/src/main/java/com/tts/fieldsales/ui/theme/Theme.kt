package com.tts.fieldsales.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BrownGoldColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = TextOnGold,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldLight,
    secondary = GoldDim,
    onSecondary = TextOnGold,
    secondaryContainer = BrownMedium,
    onSecondaryContainer = TextPrimary,
    tertiary = GoldBright,
    onTertiary = TextOnGold,
    tertiaryContainer = BrownLight,
    onTertiaryContainer = TextPrimary,
    background = BrownDarkest,
    onBackground = TextPrimary,
    surface = BrownDark,
    onSurface = TextPrimary,
    surfaceVariant = BrownCard,
    onSurfaceVariant = TextSecondary,
    error = StatusRed,
    onError = TextPrimary,
    outline = GoldDim,
    outlineVariant = DividerColor,
    scrim = OverlayDark,
    inverseSurface = TextPrimary,
    inverseOnSurface = BrownDarkest,
    inversePrimary = GoldDark,
)

@Composable
fun TTSFieldSalesTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BrownDarkest.toArgb()
            window.navigationBarColor = BrownDarkest.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = BrownGoldColorScheme,
        typography = AppTypography,
        content = content
    )
}
