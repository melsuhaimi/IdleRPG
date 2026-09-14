package com.idlerpg.game.core.time

/**
 * Wall-clock abstraction used only at application/data boundaries.
 *
 * Domain simulation code must use GameTime instead of reading the device clock.
 */
fun interface GameClock {
    fun nowEpochMs(): Long
}
