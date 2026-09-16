package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.GameSessionFactory
import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.config.GameConfig
import com.idlerpg.game.core.config.SimulationConfig
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.stats.DerivedStatSystem
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.data.content.HollowWardenContent

internal object SimulationTestSupport {
    const val CONTENT_VERSION: String = "foundation-18-harness"

    fun factory(
        maximumActionsPerAdvance: Int =
            SimulationConfig.DEFAULT_MAXIMUM_ACTIONS_PER_ADVANCE,
        balanceConfig: BalanceConfig = BalanceConfig()
    ): GameSessionFactory =
        GameSessionFactory.default(
            GameConfig(
                contentVersion = CONTENT_VERSION,
                balance = balanceConfig,
                simulation = SimulationConfig(
                    maximumActionsPerAdvance = maximumActionsPerAdvance
                )
            )
        )

    fun runtime(
        seed: Long = 123456789L,
        maximumActionsPerAdvance: Int =
            SimulationConfig.DEFAULT_MAXIMUM_ACTIONS_PER_ADVANCE,
        balanceConfig: BalanceConfig = BalanceConfig()
    ): GameRuntime {
        val factory = factory(maximumActionsPerAdvance, balanceConfig)
        return GameRuntime(
            initialSession = factory.newGame(seed),
            sessionFactory = factory
        )
    }

    fun startTraining(runtime: GameRuntime) {
        checkAccepted(
            runtime.dispatch(
                SelectRegion(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
            )
        )
        checkAccepted(
            runtime.dispatch(
                StartEncounter(DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID)
            )
        )
    }

    fun makeChronicleEligible(runtime: GameRuntime) {
        val state = runtime.state()
        runtime.replaceLoadedState(state.copy(run = state.run.copy(world = state.run.world.copy(
            unlockedRegionIds = state.run.world.unlockedRegionIds + DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            regionProgressById = state.run.world.regionProgressById +
                (DefaultGameContent.TRAINING_HOLLOW_REGION_ID to RegionProgressState(
                    highestClearedEncounterTier = 30L,
                    normalClears = GameNumber.of(15L),
                    clearedBossIds = setOf(HollowWardenContent.BOSS_ID)))))))
    }

    fun checkAccepted(result: EngineResult) {
        check(result.commandResult == CommandResult.Accepted) {
            "Expected accepted command, got ${result.commandResult}"
        }
    }

    fun gold(state: GameState): GameNumber =
        state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD]
            ?: GameNumber.ZERO

    fun normalClears(state: GameState): GameNumber =
        state.run.world.regionProgressById[
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ]?.normalClears ?: GameNumber.ZERO

    fun primaryEnemyHealth(state: GameState): GameNumber =
        state.run.combat.enemies
            .minByOrNull { it.instanceId }
            ?.combatant
            ?.currentHealth
            ?: GameNumber.ZERO

    fun advanceInChunks(
        runtime: GameRuntime,
        total: GameDuration,
        chunk: GameDuration
    ): List<GameEventEnvelope> {
        require(chunk > GameDuration.ZERO)
        var remaining = total.millis
        val events = mutableListOf<GameEventEnvelope>()
        while (remaining > 0L) {
            val step = minOf(remaining, chunk.millis)
            events += runtime.advance(GameDuration.ofMillis(step)).events
            remaining -= step
        }
        return events
    }

    class InMemoryGameRepository(
        initial: SaveEnvelope? = null
    ) : GameRepository {
        var envelope: SaveEnvelope? = initial

        override fun load(): SaveEnvelope? = envelope

        override fun save(envelope: SaveEnvelope) {
            this.envelope = envelope
        }

        override fun exists(): Boolean = envelope != null

        override fun delete() {
            envelope = null
        }
    }

    class MutableClock(
        var epochMs: Long
    ) : GameClock {
        override fun nowEpochMs(): Long = epochMs
    }

    fun contentId(value: String): ContentId = ContentId(value)
}

/**
 * End-to-end preservation of the verified Phase 1/2 prototype inside the final backend.
 */
object PrototypeParityTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = GameRuntime(
            initialSession = factory.newGame(101L),
            sessionFactory = factory
        )
        SimulationTestSupport.startTraining(runtime)

        check(
            DerivedStatSystem.attackPower(
                runtime.state(),
                factory.contentRegistry
            ) == GameNumber.of(10L)
        )

        runtime.advance(GameDuration.ofSeconds(9L))
        check(SimulationTestSupport.primaryEnemyHealth(runtime.state()) == GameNumber.of(10L))
        check(SimulationTestSupport.gold(runtime.state()) == GameNumber.ZERO)

        runtime.advance(GameDuration.ofSeconds(1L))
        check(SimulationTestSupport.gold(runtime.state()) == GameNumber.of(10L))
        check(SimulationTestSupport.normalClears(runtime.state()) == GameNumber.ONE)
        val nextEnemy = runtime.state().run.combat.enemies.single()
        check(nextEnemy.definitionId == DefaultGameContent.RIFTFANG_ID)
        // Tier one applies Riftfang's role and regional health growth.
        check(SimulationTestSupport.primaryEnemyHealth(runtime.state()) == GameNumber.of(89L))
    }
}
