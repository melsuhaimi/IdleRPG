package com.idlerpg.game.simulation

import com.idlerpg.game.application.AutosaveCoordinator
import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.GameSessionFactory
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.command.AllocateRebirthPoints
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthState
import com.idlerpg.game.domain.model.rebirth.RebirthStat
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
                state, source, GameNumber.of(80), GameNumber.ZERO, kind.id, factory.contentRegistry
            ).event.amount
        check(damage(com.idlerpg.game.domain.definition.DamageKind.PHYSICAL) == GameNumber.of(40))
        check(damage(com.idlerpg.game.domain.definition.DamageKind.ELEMENTAL) == GameNumber.of(80))
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
}
