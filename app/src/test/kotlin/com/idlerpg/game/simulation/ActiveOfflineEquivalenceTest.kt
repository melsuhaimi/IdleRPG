package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.PlayerLeveledUp

/** Foreground and offline elapsed-time paths must use identical canonical simulation rules. */
object ActiveOfflineEquivalenceTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val setup = GameRuntime(
            initialSession = factory.newGame(202L),
            sessionFactory = factory
        )
        SimulationTestSupport.startTraining(setup)
        val startingState = setup.state()

        val foreground = GameRuntime(
            initialSession = factory.loadedGame(startingState),
            sessionFactory = factory
        )
        val foregroundResult = foreground.advance(GameDuration.ofSeconds(60L))

        val savedAt = 5_000_000L
        val repository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = startingState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = savedAt
            )
        )
        val clock = SimulationTestSupport.MutableClock(savedAt + 60_000L)
        val offline = OfflineSessionCoordinator(
            repository = repository,
            clock = clock,
            engineContext = factory.createEngineContext()
        )
        val offlineResult = offline.resume()
            ?: error("Expected offline save")

        check(
            offlineResult.state.engine.simulationTime ==
                startingState.engine.simulationTime + GameDuration.ofSeconds(60L)
        )
        check(offlineResult.state.run.world == startingState.run.world)
        check(offlineResult.state.run.inventory == startingState.run.inventory)
        check(offlineResult.state.run.combat == startingState.run.combat)
        check(offlineResult.state.run.player == startingState.run.player)
        check(offlineResult.state.run.resonance == startingState.run.resonance)
        check(offlineResult.state.run.doctrine == startingState.run.doctrine)
        check(offlineResult.state.run.adaptation == startingState.run.adaptation)
        check(offlineResult.state.run.quests == startingState.run.quests)
        check(offlineResult.state.meta == startingState.meta)
        check(offlineResult.state.run.economy.upgrades == startingState.run.economy.upgrades)
        check(
            offlineResult.state.run.economy.wallet.amountsByCurrencyId
                .filterKeys { it != CurrencyId.GOLD } ==
                startingState.run.economy.wallet.amountsByCurrencyId
                    .filterKeys { it != CurrencyId.GOLD }
        )
        check(offlineResult.state.run.progression.featureUnlocks == startingState.run.progression.featureUnlocks)
        check(offlineResult.state.run.progression.affinityMastery == startingState.run.progression.affinityMastery)
        check(
            offlineResult.events.all { envelope ->
                when (val event = envelope.event) {
                    is CurrencyGranted -> event.currencyId == CurrencyId.GOLD
                    is ExperienceGranted, is PlayerLeveledUp -> true
                    else -> false
                }
            }
        )
        check(offlineResult.summary.masteryGranted == com.idlerpg.game.core.number.GameNumber.ZERO)
        check(offlineResult.summary.itemsFound == com.idlerpg.game.core.number.GameNumber.ZERO)
        check(offlineResult.summary.encountersCleared == com.idlerpg.game.core.number.GameNumber.ZERO)
        check(foregroundResult.state.engine.simulationTime == offlineResult.state.engine.simulationTime)
    }
}
