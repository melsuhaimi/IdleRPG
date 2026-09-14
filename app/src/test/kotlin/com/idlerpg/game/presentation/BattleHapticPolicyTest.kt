package com.idlerpg.game.presentation

import com.idlerpg.game.presentation.model.BattleFeedbackKind
import com.idlerpg.game.ui.feedback.BattleHapticPolicy
import com.idlerpg.game.ui.feedback.BattleHapticSequenceGate

/** Locks restrained major-event feedback without requiring an Android haptics device. */
object BattleHapticPolicyTest {
    fun run() {
        val expected = mapOf(
            BattleFeedbackKind.LOOT to BattleHapticPolicy.Cue.REWARD,
            BattleFeedbackKind.CONVERGENCE to BattleHapticPolicy.Cue.SIGNATURE,
            BattleFeedbackKind.LEVEL_UP to BattleHapticPolicy.Cue.SIGNATURE,
            BattleFeedbackKind.PLAYER_DEFEATED to BattleHapticPolicy.Cue.WARNING,
            BattleFeedbackKind.BOSS_PHASE to BattleHapticPolicy.Cue.WARNING
        )

        BattleFeedbackKind.values().forEach { kind ->
            check(BattleHapticPolicy.cueFor(kind) == expected[kind]) {
                "Unexpected haptic policy for $kind"
            }
        }
        check(expected.keys.none {
            it == BattleFeedbackKind.DAMAGE ||
                it == BattleFeedbackKind.HEALING ||
                it == BattleFeedbackKind.SKILL_USED
        })

        val entryGate = BattleHapticSequenceGate(initialSequence = 42L)
        check(!entryGate.consume(42L))
        check(entryGate.consume(43L))
        check(!entryGate.consume(43L))
        check(entryGate.consume(44L))

        val initiallyEmptyGate = BattleHapticSequenceGate(initialSequence = null)
        check(!initiallyEmptyGate.consume(null))
        check(initiallyEmptyGate.consume(1L))
        check(!initiallyEmptyGate.consume(1L))
        check(initiallyEmptyGate.consume(2L))

        val disabledEventGate = BattleHapticSequenceGate(initialSequence = 7L)
        check(disabledEventGate.consume(8L))
        check(!disabledEventGate.consume(8L))
    }
}
