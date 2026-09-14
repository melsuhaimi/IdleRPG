package com.idlerpg.game.simulation

import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveCodec
import com.idlerpg.game.data.local.SaveEnvelope

/** Mature canonical state must round-trip without changing deterministic continuation. */
object SaveRoundTripTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 111L)
        SimulationTestSupport.startTraining(runtime)
        runtime.advance(GameDuration.ofSeconds(37L))
        val before = runtime.state()

        val envelope = SaveEnvelope.create(
            gameState = before,
            contentVersion = SimulationTestSupport.CONTENT_VERSION,
            writtenAtEpochMs = 1_000_000L
        )
        val codec = SaveCodec()
        val bytes = codec.encode(envelope)
        val decoded = codec.decode(bytes)
        val after = decoded.gameState()

        check(before == after)
        check(codec.encode(envelope).contentEquals(codec.encode(envelope)))
        check(before.engine.randomState == after.engine.randomState)
    }
}
