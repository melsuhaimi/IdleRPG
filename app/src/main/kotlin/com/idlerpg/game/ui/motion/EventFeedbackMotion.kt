package com.idlerpg.game.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.idlerpg.game.ui.theme.LocalReducedMotion
import com.idlerpg.game.ui.theme.MotionTokens
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Bounded, event-keyed acknowledgement motion.
 *
 * The animation only reacts to an already-produced backend event sequence number. It never
 * predicts damage, rewards, cooldowns, or any other gameplay result. Reduced-motion mode
 * snaps immediately to the settled visual state.
 */
fun Modifier.eventFeedbackPulse(sequenceNumber: Long?): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(sequenceNumber, reducedMotion) {
        scale.snapTo(1f)
        alpha.snapTo(1f)
        if (!reducedMotion && sequenceNumber != null) {
            scale.snapTo(0.97f)
            alpha.snapTo(0.82f)
            coroutineScope {
                launch {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(MotionTokens.MICRO_MILLIS)
                    )
                }
                launch {
                    alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(MotionTokens.MICRO_MILLIS)
                    )
                }
            }
        }
    }

    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        this.alpha = alpha.value
    }
}
