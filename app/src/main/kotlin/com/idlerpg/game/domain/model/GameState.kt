package com.idlerpg.game.domain.model

import com.idlerpg.game.core.random.RandomState

/**
 * Canonical full-game domain root.
 *
 * Foundation 2 establishes state ownership only. No combat, economy, world, Resonance,
 * Doctrine, Adaptation, persistence, or frontend behavior is implemented here.
 *
 * The root is deliberately partitioned into:
 * - [engine]: deterministic simulation mechanics that survive save/load,
 * - [run]: Chronicle-resettable gameplay state,
 * - [meta]: Chronicle-persistent knowledge and lifetime state.
 */
data class GameState(
    val engine: EngineState = EngineState(),
    val run: RunState = RunState(),
    val meta: MetaState = MetaState()
) {
    companion object {
        /**
         * Structural deterministic new-game state.
         *
         * The default seed of 0 is a Foundation 2 implementation seed, not a gameplay
         * balance rule. A later GameSessionFactory may supply a different controlled seed.
         */
        fun newGame(randomSeed: Long = EngineState.DEFAULT_RANDOM_SEED): GameState =
            GameState(
                engine = EngineState(
                    randomState = RandomState(randomSeed)
                ),
                run = RunState(),
                meta = MetaState()
            )
    }
}
