package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.world.EncounterStatus

/** Explicit region -> encounter -> combat -> clear -> next encounter progression. */
object WorldProgressionScenarioTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 17L)
        SimulationTestSupport.startTraining(runtime)

        val started = runtime.state().run.world.currentEncounter
            ?: error("Encounter should be active")
        check(started.encounterIndex == 1L)
        check(started.status == EncounterStatus.ACTIVE)

        runtime.advance(GameDuration.ofSeconds(10L))

        check(SimulationTestSupport.normalClears(runtime.state()) == GameNumber.ONE)
        val next = runtime.state().run.world.currentEncounter
            ?: error("Next encounter should be active")
        check(next.encounterIndex == 2L)
        check(next.status == EncounterStatus.ACTIVE)
        check(runtime.state().run.combat.status == CombatStatus.ACTIVE)
        check(next.definitionId == com.idlerpg.game.data.content.DefaultGameContent.RIFTFANG_ENCOUNTER_ID)
        check(runtime.state().run.combat.enemies.single().definitionId == com.idlerpg.game.data.content.DefaultGameContent.RIFTFANG_ID)
        check(SimulationTestSupport.primaryEnemyHealth(runtime.state()) == SimulationTestSupport.riftfangTierOneHealth())
    }
}
