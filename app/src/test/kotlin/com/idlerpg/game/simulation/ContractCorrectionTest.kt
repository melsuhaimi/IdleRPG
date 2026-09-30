package com.idlerpg.game.simulation

import com.idlerpg.game.application.AutosaveCoordinator
import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.GameSessionFactory
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.random.GameRandom
import com.idlerpg.game.core.random.RandomState
import com.idlerpg.game.core.random.WeightedValue
import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.command.AllocateRebirthPoints
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthState
import com.idlerpg.game.domain.model.rebirth.RebirthStat
import com.idlerpg.game.ui.app.foregroundPumpDecision
import org.junit.Test

class ContractCorrectionTest {
    @Test fun armorOnlyMitigatesPhysicalIncomingDamage() {
        val factory = GameSessionFactory.default()
        val initial = factory.newPlayableGame().state()
        val state = initial.copy(run = initial.run.copy(player = initial.run.player.copy(
            baseStats = initial.run.player.baseStats.copy(armor = GameNumber.of(100))
        )))
        val source = state.run.combat.enemies.first().instanceId
        fun damage(kind: com.idlerpg.game.domain.definition.DamageKind) =
            com.idlerpg.game.domain.system.combat.DamageSystem.dealToPlayer(
                state, source, GameNumber.of(80), GameNumber.ZERO, kind.id,
                factory.contentRegistry, FixedRollRandom(9_999L)
            ).event.amount
        check(damage(com.idlerpg.game.domain.definition.DamageKind.PHYSICAL) == GameNumber.of(40))
        check(damage(com.idlerpg.game.domain.definition.DamageKind.ELEMENTAL) == GameNumber.of(80))
    }
    @Test fun defenseUsesCappedGuardAfterPhysicalArmor() {
        val factory = GameSessionFactory.default()
        val initial = factory.newPlayableGame().state()
        val state = initial.copy(run = initial.run.copy(player = initial.run.player.copy(
            baseStats = initial.run.player.baseStats.copy(
                armor = GameNumber.of(100),
                defense = GameNumber.of(1_000)
            )
        )))
        val source = state.run.combat.enemies.first().instanceId
        fun damage(kind: com.idlerpg.game.domain.definition.DamageKind, roll: Long) =
            com.idlerpg.game.domain.system.combat.DamageSystem.dealToPlayer(
                state, source, GameNumber.of(80), GameNumber.ZERO, kind.id,
                factory.contentRegistry, FixedRollRandom(roll)
            ).event

        val guardedPhysical = damage(com.idlerpg.game.domain.definition.DamageKind.PHYSICAL, 0L)
        val unguardedPhysical = damage(com.idlerpg.game.domain.definition.DamageKind.PHYSICAL, 5_000L)
        val guardedElemental = damage(com.idlerpg.game.domain.definition.DamageKind.ELEMENTAL, 0L)
        val unguardedElemental = damage(com.idlerpg.game.domain.definition.DamageKind.ELEMENTAL, 5_000L)
        check(guardedPhysical.amount == GameNumber.of(20L) && guardedPhysical.guarded)
        check(unguardedPhysical.amount == GameNumber.of(40L) && !unguardedPhysical.guarded)
        check(guardedElemental.amount == GameNumber.of(40L) && guardedElemental.guarded)
        check(unguardedElemental.amount == GameNumber.of(80L) && !unguardedElemental.guarded)

        val defenseSystem = com.idlerpg.game.domain.system.combat.DefenseSystem
        check(defenseSystem.guardChance(GameNumber.ZERO) == Ratio.ZERO)
        check(defenseSystem.guardChance(GameNumber.of(-1L)) == Ratio.ZERO)
        check(defenseSystem.guardChance(GameNumber.of(1_000L)) == Ratio.ofUnits(5_000L))
        check(defenseSystem.guardChance(GameNumber.of(3_000L)) == Ratio.ofUnits(7_500L))
        val level100 = state.copy(run = state.run.copy(progression = state.run.progression.copy(
            playerLevel = state.run.progression.playerLevel.copy(level = 100L)
        )))
        check(com.idlerpg.game.domain.system.stats.DerivedStatSystem.defense(
            level100, factory.contentRegistry
        ) == GameNumber.of(1_099L))
    }
    @Test fun levelCurve() = IncrementalProgressionContractTest.run()
    @Test fun enhancementAndRefinement() = GearEnhancementScenarioTest.run()
    @Test fun skillProgression() = SkillProgressionScenarioTest.run()
    @Test fun powerScore() = PowerScoreScenarioTest.run()
    @Test fun loot() = LootDeterminismTest.run()
    @Test fun migration() = SaveV10FailstackMigrationTest.run()
    @Test fun gearProjection() = com.idlerpg.game.presentation.GearProjectionTest.run()
    @Test fun progressProjection() = com.idlerpg.game.presentation.ProgressProjectionTest.main(emptyArray())
    @Test fun physicalEffectiveHealthUsesArmorRatio() {
        val factory = GameSessionFactory.default()
        val state = factory.newGame().state()
        val armored = state.copy(run = state.run.copy(player = state.run.player.copy(
            baseStats = state.run.player.baseStats.copy(maxHealth = GameNumber.of(200), armor = GameNumber.of(100))
        )))
        check(com.idlerpg.game.domain.system.stats.PowerScoreSystem.calculate(armored, factory.contentRegistry).effectiveHealth == GameNumber.of(400))
    }
    @Test fun v11SaveGainsZeroDefenseRating() {
        val factory = GameSessionFactory.default()
        val state = factory.newGame(12L).state()
        val defensePath = "run.player.baseStats.defense"
        val v11Data = com.idlerpg.game.data.local.SaveData(
            com.idlerpg.game.data.local.SaveData.fromGameState(state).fields - defensePath
        )
        val migrated = com.idlerpg.game.data.local.migration.SaveMigrationRegistry().migrate(
            com.idlerpg.game.data.local.SaveEnvelope(
                schemaVersion = com.idlerpg.game.data.local.SaveVersion.V11,
                contentVersion = "test-content",
                writtenAtEpochMs = 1L,
                data = v11Data
            )
        )
        check(migrated.schemaVersion == com.idlerpg.game.data.local.SaveVersion.V12)
        check(migrated.data.fields[defensePath] == "0")
        check(migrated.gameState().run.player.baseStats.defense == GameNumber.ZERO)
    }
    @Test fun rarityRaisesRollFloorWithinAuthoredBounds() {
        GameSessionFactory.default().contentRegistry.allAffixes().forEach { affix ->
            val floors = com.idlerpg.game.domain.definition.Rarity.values().map {
                com.idlerpg.game.domain.system.loot.GearRollQuality.minimum(affix, it)
            }
            check(floors == floors.sorted())
            check(floors.first() == affix.minimumRollValue)
            check(floors.all { it in affix.minimumRollValue..affix.maximumRollValue })
        }
    }

    @Test fun legacyCannotBecomeASecondCombatTree() {
        val factory = GameSessionFactory.default()
        val session = factory.newGame()
        val state = session.state().copy(meta = session.state().meta.copy(
            rebirth = RebirthState(completedRebirths = 1, normalPointsEarned = 100, legacyPointsEarned = 20)
        ))
        session.replaceLoadedState(state)
        val result = session.applyCommand(AllocateRebirthPoints(RebirthPointPool.LEGACY, RebirthStat.ATTACK_POWER, 1))
        check(result.commandResult is CommandResult.Rejected)
        check(session.state() == state)
        check(session.applyCommand(AllocateRebirthPoints(RebirthPointPool.LEGACY, RebirthStat.LEGENDARY_FIND, 1)).commandResult == CommandResult.Accepted)
    }

    @Test fun oldCombatLegacyInvestmentsAreRefundedWithoutDeletingPoints() {
        val initial = GameSessionFactory.default().newGame().state()
        val old = initial.copy(meta = initial.meta.copy(rebirth = RebirthState(
            completedRebirths = 1, normalPointsEarned = 100, legacyPointsEarned = 20,
            normalAllocations = mapOf(RebirthStat.ATTACK_POWER to 5L, RebirthStat.LEGENDARY_FIND to 3L),
            legacyAllocations = mapOf(RebirthStat.ATTACK_POWER to 10L, RebirthStat.LEGENDARY_FIND to 2L)
        )))
        val data = com.idlerpg.game.data.local.SaveData.fromGameState(old)
        val migrated = com.idlerpg.game.data.local.migration.V10ToV11SaveMigration().migrate(data).toGameState()
        check(migrated.run == old.run)
        check(migrated.engine == old.engine)
        check(migrated.meta.rebirth.normalPointsEarned == 100L)
        check(migrated.meta.rebirth.legacyPointsEarned == 20L)
        check(migrated.meta.rebirth.unspentPoints(RebirthPointPool.NORMAL) == 95L)
        check(migrated.meta.rebirth.unspentPoints(RebirthPointPool.LEGACY) == 18L)
    }

    @Test fun failedRandomizedPurchaseRetriesTheSameRoll() {
        val factory = GameSessionFactory.default()
        val initial = factory.newGame(9101L).state()
        val id = com.idlerpg.game.core.id.InstanceId(91001L)
        val item = com.idlerpg.game.domain.model.inventory.ItemInstance(
            instanceId = id,
            definitionId = com.idlerpg.game.data.content.DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            rarity = com.idlerpg.game.domain.definition.Rarity.RARE,
            mainStat = com.idlerpg.game.domain.model.inventory.RolledAffix(com.idlerpg.game.core.id.ContentId("affix.brutal"), 3L)
        )
        val before = initial.copy(run = initial.run.copy(
            inventory = initial.run.inventory.copy(itemsById = mapOf(id to item)),
            economy = initial.run.economy.copy(wallet = com.idlerpg.game.domain.model.economy.CurrencyWallet(mapOf(
                com.idlerpg.game.domain.definition.CurrencyId.ENHANCEMENT_MATERIAL to GameNumber.of(100),
                com.idlerpg.game.domain.definition.CurrencyId.REFINEMENT_MATERIAL to GameNumber.of(10)
            )))
        ))
        val commands = listOf(
            com.idlerpg.game.domain.command.EnhanceItem(id),
            com.idlerpg.game.domain.command.RefineItem(id, item.mainStat!!.affixId)
        )
        commands.forEach { command ->
            var rejectWrite = true
            var durable: SaveEnvelope? = null
            val repository = object : GameRepository {
                override fun load() = durable
                override fun exists() = durable != null
                override fun delete() { durable = null }
                override fun save(envelope: SaveEnvelope) {
                    check(!rejectWrite) { "Disk unavailable" }
                    durable = envelope
                }
            }
            val runtime = GameRuntime(factory.loadedGame(before), factory,
                AutosaveCoordinator(repository, GameClock { 1000L }, "contract-test"))
            check(runCatching { runtime.dispatch(command) }.isFailure)
            check(runtime.state() == before)
            check(durable == null)
            rejectWrite = false
            val retry = runtime.dispatch(command)
            val reference = factory.loadedGame(before).applyCommand(command)
            check(retry.state == reference.state)
            check(retry.events == reference.events)
            check(durable!!.gameState() == retry.state)
        }
    }

    @Test fun failedCheckpointDoesNotPublishAllocation() {
        val factory = GameSessionFactory.default()
        val session = factory.newGame()
        val before = session.state().copy(meta = session.state().meta.copy(
            rebirth = RebirthState(completedRebirths = 1, normalPointsEarned = 100)
        ))
        session.replaceLoadedState(before)
        val repository = object : GameRepository {
            override fun load(): SaveEnvelope? = null
            override fun exists(): Boolean = false
            override fun delete() = Unit
            override fun save(envelope: SaveEnvelope) { error("Disk unavailable") }
        }
        val runtime = GameRuntime(session, factory, AutosaveCoordinator(repository, object : GameClock {
            override fun nowEpochMs(): Long = 1000L
        }, "contract-test"))
        val result = runCatching { runtime.dispatch(AllocateRebirthPoints(RebirthPointPool.NORMAL, RebirthStat.ATTACK_POWER, 1)) }
        check(result.isFailure)
        check(runtime.state() == before)
        check(runtime.recentEvents().isEmpty())
    }

    @Test fun zeroElapsedResumeDoesNotExecuteAReadyEncounter() {
        val factory = GameSessionFactory.default()
        val startingState = factory.newPlayableGame(77L).state()
        val savedAt = 5_000L
        val repository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = startingState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = savedAt
            )
        )
        val resumed = OfflineSessionCoordinator(
            repository = repository,
            clock = SimulationTestSupport.MutableClock(savedAt),
            engineContext = factory.createEngineContext()
        ).resume() ?: error("Expected saved game")

        check(resumed.state == startingState)
        check(resumed.events.isEmpty())
        check(resumed.summary.simulatedElapsed.millis == 0L)
    }

    @Test fun simulationFailureForcesFreshForegroundBaselineBeforeRecoveryTick() {
        val failed = foregroundPumpDecision(
            nowMillis = 1_000L,
            lastPumpAtMillis = 750L,
            runtimeReady = false,
            lifecycleBarrier = false,
            awaitingReadyBaseline = false
        )
        check(failed.awaitingReadyBaseline)
        check(!failed.shouldSubmitElapsed)

        val recovered = foregroundPumpDecision(
            nowMillis = 1_250L,
            lastPumpAtMillis = failed.lastPumpAtMillis,
            runtimeReady = true,
            lifecycleBarrier = false,
            awaitingReadyBaseline = failed.awaitingReadyBaseline
        )
        check(!recovered.shouldSubmitElapsed)
        check(!recovered.awaitingReadyBaseline)

        val nextTick = foregroundPumpDecision(
            nowMillis = 1_500L,
            lastPumpAtMillis = recovered.lastPumpAtMillis,
            runtimeReady = true,
            lifecycleBarrier = false,
            awaitingReadyBaseline = recovered.awaitingReadyBaseline
        )
        check(nextTick.shouldSubmitElapsed)
    }

    @Test fun recoveryActionBaselineBlocksTickWhenFailureWasNotObservedByPump() {
        val recoveryAction = foregroundPumpDecision(
            nowMillis = 1_100L,
            lastPumpAtMillis = 750L,
            runtimeReady = true,
            lifecycleBarrier = false,
            awaitingReadyBaseline = true
        )
        check(!recoveryAction.shouldSubmitElapsed)
        check(!recoveryAction.awaitingReadyBaseline)
        check(recoveryAction.lastPumpAtMillis == 1_100L)
    }

    private class FixedRollRandom(private val roll: Long) : GameRandom {
        override fun nextInt(bound: Int): Int = nextLong(bound.toLong()).toInt()
        override fun nextLong(bound: Long): Long {
            require(roll in 0 until bound)
            return roll
        }
        override fun nextUnitDouble(): Double = error("Unused test RNG method")
        override fun <T> chooseWeighted(options: List<WeightedValue<T>>): T =
            error("Unused test RNG method")
        override fun snapshot(): RandomState = RandomState(0L)
    }
}
