package com.idlerpg.game.data.repository

import com.idlerpg.game.data.local.SaveEnvelope

/**
 * Application-facing persistence contract.
 *
 * The interface deliberately exposes no Android Context, File, stream, or filesystem API.
 * Local storage mechanics remain an implementation detail of data/local.
 */
interface GameRepository {

    /**
     * Returns the latest valid save, or null when no save exists.
     *
     * Corrupt/unsupported save data is an error rather than being silently treated as a
     * new game.
     */
    fun load(): SaveEnvelope?

    /** Persist one complete save envelope atomically or fail without partial success. */
    fun save(envelope: SaveEnvelope)

    /** True when a primary or recoverable backup save exists. */
    fun exists(): Boolean

    /** Development/reset operation that removes primary, backup, and candidate files. */
    fun delete()
}

/** Persistence/storage failure surfaced at the repository boundary. */
class GameRepositoryException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)
