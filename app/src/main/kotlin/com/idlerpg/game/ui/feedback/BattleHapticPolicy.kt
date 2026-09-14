package com.idlerpg.game.ui.feedback

import com.idlerpg.game.presentation.model.BattleFeedbackKind

/** Presentation-only classification. It cannot affect combat, timing, rewards, or RNG. */
object BattleHapticPolicy {
    enum class Cue {
        REWARD,
        SIGNATURE,
        WARNING
    }

    fun cueFor(kind: BattleFeedbackKind): Cue? = when (kind) {
        BattleFeedbackKind.LOOT -> Cue.REWARD
        BattleFeedbackKind.CONVERGENCE,
        BattleFeedbackKind.LEVEL_UP -> Cue.SIGNATURE
        BattleFeedbackKind.PLAYER_DEFEATED,
        BattleFeedbackKind.BOSS_PHASE -> Cue.WARNING
        else -> null
    }
}

/** Remembers the last observed event so recomposition, toggles, and navigation cannot replay it. */
class BattleHapticSequenceGate(initialSequence: Long?) {
    private var lastHandledSequence: Long? = initialSequence

    fun consume(sequence: Long?): Boolean {
        if (sequence == null || sequence == lastHandledSequence) return false
        lastHandledSequence = sequence
        return true
    }
}
