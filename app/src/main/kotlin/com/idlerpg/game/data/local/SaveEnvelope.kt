package com.idlerpg.game.data.local

import com.idlerpg.game.domain.model.GameState

/**
 * Persistence envelope around the logical SaveData payload.
 *
 * [writtenAtEpochMs] is wall-clock metadata. It is intentionally outside GameState and is
 * used later by OfflineSessionCoordinator to determine elapsed real-world time.
 */
data class SaveEnvelope(
    val schemaVersion: SaveVersion,
    val contentVersion: String,
    val writtenAtEpochMs: Long,
    val data: SaveData,
    val integrity: SaveIntegrityMetadata? = null
) {
    init {
        require(contentVersion.isNotBlank()) {
            "SaveEnvelope.contentVersion cannot be blank"
        }
        require(writtenAtEpochMs >= 0L) {
            "SaveEnvelope.writtenAtEpochMs cannot be negative: $writtenAtEpochMs"
        }
    }

    /** Reconstruct and validate the canonical domain state represented by this envelope. */
    fun gameState(): GameState =
        data.toGameState()

    companion object {
        fun create(
            gameState: GameState,
            contentVersion: String,
            writtenAtEpochMs: Long,
            schemaVersion: SaveVersion = SaveVersion.CURRENT
        ): SaveEnvelope =
            SaveEnvelope(
                schemaVersion = schemaVersion,
                contentVersion = contentVersion,
                writtenAtEpochMs = writtenAtEpochMs,
                data = SaveData.fromGameState(gameState),
                integrity = null
            )
    }
}
