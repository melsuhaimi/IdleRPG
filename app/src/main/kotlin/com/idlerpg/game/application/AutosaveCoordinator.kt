package com.idlerpg.game.application

import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.model.GameState

/**
 * Explicit application-layer save coordinator.
 *
 * Foundation 18 intentionally does not start a background timer/coroutine. Callers choose
 * lifecycle/periodic save triggers; this class only translates one canonical state into a
 * repository save using injected wall-clock time.
 */
class AutosaveCoordinator(
    private val repository: GameRepository,
    private val clock: GameClock,
    private val contentVersion: String
) {
    init {
        require(contentVersion.isNotBlank()) {
            "AutosaveCoordinator.contentVersion cannot be blank"
        }
    }

    fun save(state: GameState): SaveEnvelope {
        val nowEpochMs = clock.nowEpochMs()
        require(nowEpochMs >= 0L) {
            "GameClock returned negative epoch milliseconds: $nowEpochMs"
        }

        val envelope = SaveEnvelope.create(
            gameState = state,
            contentVersion = contentVersion,
            writtenAtEpochMs = nowEpochMs,
            schemaVersion = SaveVersion.CURRENT
        )
        repository.save(envelope)
        return envelope
    }

    fun save(session: GameSession): SaveEnvelope =
        save(session.state())
}
