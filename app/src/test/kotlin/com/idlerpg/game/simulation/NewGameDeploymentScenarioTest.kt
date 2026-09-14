package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.DeployStartingEncounter
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.EncounterStarted
import com.idlerpg.game.domain.event.RegionSelected
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.presentation.intent.BattleUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand

/** Production new games enter visible deterministic combat; empty legacy saves can recover once. */
object NewGameDeploymentScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val first = factory.newPlayableGame(11_001L)
        val second = factory.newPlayableGame(11_001L)

        check(first.state() == second.state())
        check(first.recentEvents() == second.recentEvents())
        assertDeployed(first.state())
        check(first.recentEvents().map { it.event }.any { it is RegionSelected })
        check(first.recentEvents().map { it.event }.any { it is EncounterStarted })

        val rawState = factory.newGame(11_002L).state()
        check(rawState.run.world.activeRegionId == null)
        check(rawState.run.world.currentEncounter == null)
        val runtime = GameRuntime(factory.loadedGame(rawState), factory)
        val recovery = runtime.dispatch(
            DeployStartingEncounter(CommandCorrelationId(77L))
        )
        SimulationTestSupport.checkAccepted(recovery)
        assertDeployed(runtime.state())

        val beforeDuplicate = runtime.state()
        val duplicate = runtime.dispatch(DeployStartingEncounter())
        check((duplicate.commandResult as CommandResult.Rejected).reason.code ==
            CommandRejectionCode.INVALID_STATE)
        check(duplicate.state == beforeDuplicate)

        val mapped = BattleUiIntent.DeployStartingEncounter.toGameCommand(
            CommandCorrelationId(78L)
        )
        check(mapped is DeployStartingEncounter)
        check(mapped.correlationId == CommandCorrelationId(78L))
    }

    private fun assertDeployed(state: com.idlerpg.game.domain.model.GameState) {
        check(state.run.world.activeRegionId == DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
        val encounter = state.run.world.currentEncounter
            ?: error("Production new game must have an encounter")
        check(encounter.definitionId == DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID)
        check(encounter.status == EncounterStatus.ACTIVE)
        check(state.run.combat.status == CombatStatus.ACTIVE)
        check(state.run.combat.enemies.count {
            it.combatant.currentHealth > GameNumber.ZERO
        } in 1..3)
    }
}
