package com.idlerpg.game.application

import com.idlerpg.game.domain.model.GameState

/**
 * Stable read-only application boundary for presentation/debug consumers.
 *
 * GameState is immutable by transition convention. The provider exposes the canonical
 * value for reading but does not expose any mutation path that bypasses GameCommand.
 */
class GameSnapshotProvider(
    private val sessionProvider: () -> GameSession
) {
    constructor(session: GameSession) : this({ session })

    fun snapshot(): GameState =
        sessionProvider().state()
}
