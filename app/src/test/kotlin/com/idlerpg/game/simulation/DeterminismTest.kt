package com.idlerpg.game.simulation

import com.idlerpg.game.core.time.GameDuration

/** Same state/seed/commands/duration must produce exactly the same state and event stream. */
object DeterminismTest {
    fun run() {
        val first = SimulationTestSupport.runtime(seed = 2024L)
        val second = SimulationTestSupport.runtime(seed = 2024L)
        SimulationTestSupport.startTraining(first)
        SimulationTestSupport.startTraining(second)

        val firstResult = first.advance(GameDuration.ofSeconds(60L))
        val secondResult = second.advance(GameDuration.ofSeconds(60L))

        check(firstResult.state == secondResult.state)
        check(firstResult.events == secondResult.events)
        check(
            firstResult.state.engine.randomState ==
                secondResult.state.engine.randomState
        )
    }
}
