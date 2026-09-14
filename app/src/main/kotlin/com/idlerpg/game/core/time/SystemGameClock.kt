package com.idlerpg.game.core.time

/** Production wall-clock implementation backed by the device/JVM system clock. */
class SystemGameClock : GameClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
