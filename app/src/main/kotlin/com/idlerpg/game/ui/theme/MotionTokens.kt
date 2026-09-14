package com.idlerpg.game.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/** Presentation-only motion policy. Gameplay timing never reads these values. */
object MotionTokens {
    const val MICRO_MILLIS: Int = 160
    const val CARD_MILLIS: Int = 260
    const val SIGNATURE_MILLIS: Int = 450
}

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun GameMotionScope(
    reducedMotion: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        content = content
    )
}
