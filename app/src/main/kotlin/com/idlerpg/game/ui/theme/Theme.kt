package com.idlerpg.game.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ObsidianColorScheme = darkColorScheme(
    primary = ArcaneViolet,
    onPrimary = ObsidianBackground,
    secondary = ResonanceTeal,
    onSecondary = ObsidianBackground,
    tertiary = ResourceGold,
    onTertiary = ObsidianBackground,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface1,
    surfaceTint = Color.Transparent,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurface2,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    error = ErrorRose,
    onError = ObsidianBackground,
    errorContainer = Color(0xFF4C1D28),
    onErrorContainer = Color(0xFFFFD9DF)
)

private val GameShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Production game theme. Dynamic system color is deliberately disabled because affinity,
 * rarity, resource, warning, and error colors are stable game presentation semantics.
 */
@Composable
fun IdleRPGTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(
                activity.window,
                view
            ).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            activity.window.statusBarColor = ObsidianBackground.toArgb()
            activity.window.navigationBarColor = ObsidianBackground.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        shapes = GameShapes,
        content = content
    )
}
