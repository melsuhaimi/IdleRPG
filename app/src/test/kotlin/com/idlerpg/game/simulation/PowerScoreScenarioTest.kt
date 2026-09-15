package com.idlerpg.game.simulation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.model.rebirth.RebirthState
import com.idlerpg.game.domain.model.rebirth.RebirthStat
import com.idlerpg.game.domain.system.stats.PowerScoreSystem

/** Power Score is deterministic, additive, and presentation-only. */
object PowerScoreScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val initial = factory.newGame(707L).state()
        val base = PowerScoreSystem.calculate(initial, factory.contentRegistry)

        check(base.total == base.offense + base.defense + base.gear + base.skills + base.rebirth)
        check(base.expectedBasicAttackDamage > com.idlerpg.game.core.number.GameNumber.ZERO)
        check(base.effectiveHealth > com.idlerpg.game.core.number.GameNumber.ZERO)

        val allocated = initial.copy(
            meta = initial.meta.copy(
                rebirth = RebirthState(
                    normalPointsEarned = 2L,
                    normalAllocations = mapOf(RebirthStat.ATTACK_POWER to 2L)
                )
            )
        )
        val stronger = PowerScoreSystem.calculate(allocated, factory.contentRegistry)
        check(stronger.rebirth == com.idlerpg.game.core.number.GameNumber.of(200L))
        check(stronger.total > base.total)
        check(
            com.idlerpg.game.domain.system.stats.DerivedStatSystem.attackPower(
                allocated,
                factory.contentRegistry
            ) > com.idlerpg.game.domain.system.stats.DerivedStatSystem.attackPower(
                initial,
                factory.contentRegistry
            )
        )

        // The calculator must not create or mutate game progression state.
        check(initial.run.progression.skillProgression.rankBySkillId.isEmpty())
        check(DefaultGameContent.BASIC_ATTACK_ID.toString().isNotBlank())
    }
}
