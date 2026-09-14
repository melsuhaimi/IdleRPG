package com.idlerpg.game.ui.feedback

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.idlerpg.game.presentation.model.BattleFeedbackUiState

/**
 * A thin Android adapter for already-produced battle feedback.
 *
 * Sequence de-duplication keeps recomposition and preference changes from replaying an old cue.
 * Haptics acknowledge canonical results but never participate in their calculation.
 */
@Composable
fun BattleHapticEffect(
    feedback: BattleFeedbackUiState?,
    enabled: Boolean
) {
    val view = LocalView.current
    val sequenceGate = remember {
        BattleHapticSequenceGate(feedback?.sequenceNumber)
    }

    LaunchedEffect(feedback?.sequenceNumber, feedback?.kind, enabled) {
        if (!sequenceGate.consume(feedback?.sequenceNumber)) return@LaunchedEffect

        val cue = feedback?.let { BattleHapticPolicy.cueFor(it.kind) }
        if (!enabled || cue == null) return@LaunchedEffect
        val constant = when (cue) {
            BattleHapticPolicy.Cue.REWARD -> HapticFeedbackConstants.VIRTUAL_KEY
            BattleHapticPolicy.Cue.SIGNATURE -> HapticFeedbackConstants.LONG_PRESS
            BattleHapticPolicy.Cue.WARNING -> HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(constant)
    }
}
