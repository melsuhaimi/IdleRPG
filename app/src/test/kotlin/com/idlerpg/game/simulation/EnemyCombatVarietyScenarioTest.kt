package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.CombatEndReason
import com.idlerpg.game.domain.event.CombatEnded
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EncounterFailed
import com.idlerpg.game.domain.event.EncounterStarted
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.PlayerDefeated
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus

/**
 * Training Hollow enemy-combat and encounter-variety contract.
 *
 * These scenarios are intentionally dependency-free so they can run inside the existing
 * deterministic integration harness without adding a test library or Android dependency.
 */
object EnemyCombatVarietyScenarioTest {
    fun run() {
        enemyAttackDeadlineIsExact()
        armorAndPenetrationAreAuthoritative()
        encounterOrderIsLockedAndCycles()
        defeatGrantsNothingAndRetryRecovers()
        legacyV2ActiveCombatReconstructsEnemyDeadline()
        deterministicReplayIncludesEnemyActions()
    }

    private fun enemyAttackDeadlineIsExact() {
        val runtime = SimulationTestSupport.runtime(seed = 901L)
        SimulationTestSupport.startTraining(runtime)
        val playerId = runtime.state().run.combat.playerCombatant?.instanceId
            ?: error("Expected player combatant")
        val enemyId = runtime.state().run.combat.enemies.single().instanceId

        val before = runtime.advance(GameDuration.ofMillis(2_199L))
        check(before.events.none { envelope ->
            val event = envelope.event as? DamageDealt ?: return@none false
            event.sourceInstanceId == enemyId && event.targetInstanceId == playerId
        })
        check(runtime.state().run.player.currentHealth == GameNumber.of(100L))

        val atDeadline = runtime.advance(GameDuration.ofMillis(1L))
        val incoming = atDeadline.events
            .map { it.event }
            .filterIsInstance<DamageDealt>()
            .single { it.sourceInstanceId == enemyId && it.targetInstanceId == playerId }
        check(incoming.amount == GameNumber.of(6L))
        check(runtime.state().run.player.currentHealth == GameNumber.of(94L))
    }

    private fun armorAndPenetrationAreAuthoritative() {
        val runtime = SimulationTestSupport.runtime(seed = 902L)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(SelectRegion(DefaultGameContent.TRAINING_HOLLOW_REGION_ID))
        )

        val initial = runtime.state()
        val progress = initial.run.world.regionProgressById[
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ] ?: error("Missing Training Hollow progress")
        val prepared = initial.copy(
            run = initial.run.copy(
                player = initial.run.player.copy(
                    baseStats = initial.run.player.baseStats.copy(
                        armor = GameNumber.of(5L)
                    )
                ),
                world = initial.run.world.copy(
                    currentEncounter = EncounterState(
                        definitionId = DefaultGameContent.RIFTFANG_ENCOUNTER_ID,
                        encounterIndex = 2L,
                        encounterSeed = 902L,
                        status = EncounterStatus.CLEARED,
                        rewardEligible = false
                    ),
                    regionProgressById = initial.run.world.regionProgressById +
                        (DefaultGameContent.TRAINING_HOLLOW_REGION_ID to progress.copy(
                            highestClearedEncounterTier = 2L,
                            normalClears = GameNumber.of(2L)
                        )),
                    // Encounter IDs are the authoritative unlock state.
                    clearedEncounterIds = initial.run.world.clearedEncounterIds + setOf(
                        DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID,
                        DefaultGameContent.RIFTFANG_ENCOUNTER_ID
                    )
                )
            )
        )
        runtime.replaceLoadedState(prepared)
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(StartEncounter(DefaultGameContent.CINDER_WISP_ENCOUNTER_ID))
        )

        val playerId = runtime.state().run.combat.playerCombatant?.instanceId
            ?: error("Expected player combatant")
        val enemyId = runtime.state().run.combat.enemies.single().instanceId
        val result = runtime.advance(GameDuration.ofMillis(1_800L))
        val incoming = result.events
            .map { it.event }
            .filterIsInstance<DamageDealt>()
            .single { it.sourceInstanceId == enemyId && it.targetInstanceId == playerId }

        // Tier two scales Cinder Bolt to 10 damage; 2 penetration leaves 3 armor.
        // Diminishing mitigation with armorScale 100 floors 1000 / 103 to 9.
        check(incoming.amount == GameNumber.of(9L))
        check(runtime.state().run.player.currentHealth == GameNumber.of(91L))
    }

    private fun encounterOrderIsLockedAndCycles() {
        val lockRuntime = SimulationTestSupport.runtime(seed = 903L)
        SimulationTestSupport.checkAccepted(
            lockRuntime.dispatch(SelectRegion(DefaultGameContent.TRAINING_HOLLOW_REGION_ID))
        )
        val skipped = lockRuntime.dispatch(StartEncounter(DefaultGameContent.RIFTFANG_ENCOUNTER_ID))
        val rejection = skipped.commandResult as? CommandResult.Rejected
            ?: error("Expected encounter-skip rejection")
        check(rejection.reason.code == CommandRejectionCode.LOCKED)

        val runtime = SimulationTestSupport.runtime(seed = 904L)
        val boosted = runtime.state().copy(
            run = runtime.state().run.copy(
                player = runtime.state().run.player.copy(
                    baseStats = runtime.state().run.player.baseStats.copy(
                        attackPower = GameNumber.of(1_000L)
                    )
                )
            )
        )
        runtime.replaceLoadedState(boosted)
        SimulationTestSupport.startTraining(runtime)

        // Six seconds clears the two-wave elite at current tier scaling.
        val result = runtime.advance(GameDuration.ofSeconds(6L))
        val automaticStarts = result.events
            .map { it.event }
            .filterIsInstance<EncounterStarted>()
            .map { it.encounterDefinitionId }
        val expectedStarts = listOf(
            DefaultGameContent.RIFTFANG_ENCOUNTER_ID,
            DefaultGameContent.CINDER_WISP_ENCOUNTER_ID,
            DefaultGameContent.HOLLOW_BULWARK_ENCOUNTER_ID,
            DefaultGameContent.ARCANE_SEER_ENCOUNTER_ID,
            com.idlerpg.game.data.content.TrainingHollowStrategyContent.FROSTBOUND_MITE_ENCOUNTER_ID
        )
        check(automaticStarts == expectedStarts) {
            "Expected automatic starts $expectedStarts, got $automaticStarts"
        }
        val current = runtime.state().run.world.currentEncounter
            ?: error("Expected sixth encounter")
        check(current.encounterIndex == 6L)
        check(current.definitionId == com.idlerpg.game.data.content.TrainingHollowStrategyContent.FROSTBOUND_MITE_ENCOUNTER_ID)
        check(runtime.state().run.combat.enemies.any {
            it.definitionId == com.idlerpg.game.data.content.TrainingHollowStrategyContent.FROSTBOUND_MITE_ID
        })

        val registry = SimulationTestSupport.factory().contentRegistry
        check(registry.encounter(DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID).type.name == "NORMAL")
        check(registry.encounter(DefaultGameContent.RIFTFANG_ENCOUNTER_ID).type.name == "NORMAL")
        check(registry.encounter(DefaultGameContent.CINDER_WISP_ENCOUNTER_ID).type.name == "NORMAL")
        check(registry.encounter(DefaultGameContent.HOLLOW_BULWARK_ENCOUNTER_ID).type.name == "ELITE")
        check(registry.encounter(DefaultGameContent.ARCANE_SEER_ENCOUNTER_ID).type.name == "ANOMALY")
    }

    private fun defeatGrantsNothingAndRetryRecovers() {
        val runtime = SimulationTestSupport.runtime(seed = 905L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        val playerCombatant = started.run.combat.playerCombatant
            ?: error("Expected player combatant")
        runtime.replaceLoadedState(
            started.copy(
                run = started.run.copy(
                    player = started.run.player.copy(currentHealth = GameNumber.ONE),
                    combat = started.run.combat.copy(
                        playerCombatant = playerCombatant.copy(currentHealth = GameNumber.ONE)
                    )
                )
            )
        )

        val defeat = runtime.advance(GameDuration.ofMillis(2_200L))
        check(defeat.events.any { it.event is PlayerDefeated })
        check(defeat.events.any {
            (it.event as? CombatEnded)?.reason == CombatEndReason.DEFEAT
        })
        check(defeat.events.any { it.event is EncounterFailed })
        check(defeat.events.none { envelope ->
            envelope.event is EnemyKilled ||
                envelope.event is EncounterCleared ||
                envelope.event is CurrencyGranted ||
                envelope.event is ExperienceGranted ||
                envelope.event is ItemAdded
        })
        check(defeat.events.any {
            (it.event as? EncounterStarted)?.encounterDefinitionId ==
                DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID
        })
        check(runtime.state().run.player.currentHealth == GameNumber.of(100L))
        check(runtime.state().run.combat.playerCombatant?.currentHealth == GameNumber.of(100L))
        check(runtime.state().run.combat.status == CombatStatus.ACTIVE)
        check(runtime.state().run.world.currentEncounter?.status == EncounterStatus.ACTIVE)
    }

    private fun legacyV2ActiveCombatReconstructsEnemyDeadline() {
        val runtime = SimulationTestSupport.runtime(seed = 906L)
        SimulationTestSupport.startTraining(runtime)
        val legacyLike = runtime.state().copy(
            run = runtime.state().run.copy(
                combat = runtime.state().run.combat.copy(nextEnemyDecisionAt = emptyMap())
            )
        )
        val roundTripped = SaveData.fromGameState(legacyLike).toGameState()
        check(roundTripped.run.combat.nextEnemyDecisionAt.isEmpty())

        val factory = SimulationTestSupport.factory()
        val loaded = GameRuntime(
            initialSession = factory.loadedGame(roundTripped),
            sessionFactory = factory
        )
        val playerId = loaded.state().run.combat.playerCombatant?.instanceId
            ?: error("Expected player combatant")
        val enemyId = loaded.state().run.combat.enemies.single().instanceId
        val result = loaded.advance(GameDuration.ofMillis(2_200L))
        val incoming = result.events
            .map { it.event }
            .filterIsInstance<DamageDealt>()
            .single { it.sourceInstanceId == enemyId && it.targetInstanceId == playerId }
        check(incoming.amount == GameNumber.of(6L))
        check(loaded.state().run.combat.nextEnemyDecisionAt[enemyId] != null)
    }

    private fun deterministicReplayIncludesEnemyActions() {
        fun scenario(): Pair<com.idlerpg.game.domain.model.GameState, List<com.idlerpg.game.domain.event.GameEventEnvelope>> {
            val runtime = SimulationTestSupport.runtime(seed = 907L)
            SimulationTestSupport.startTraining(runtime)
            val result = runtime.advance(GameDuration.ofSeconds(60L))
            return result.state to result.events
        }

        val first = scenario()
        val second = scenario()
        check(first == second)
        check(first.second.any { it.event is PlayerDefeated || it.event is DamageDealt })
    }
}
