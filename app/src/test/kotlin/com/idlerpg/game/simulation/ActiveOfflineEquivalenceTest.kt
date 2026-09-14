package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveEnvelope

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

        check(foregroundResult.state == offlineResult.engineResult.state)
        check(foregroundResult.events == offlineResult.engineResult.events)
        check(
            foregroundResult.state.engine.randomState ==
                offlineResult.engineResult.state.engine.randomState
        )
    }
}
