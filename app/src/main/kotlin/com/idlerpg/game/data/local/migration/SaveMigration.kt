package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveVersion

/**
 * One explicit sequential logical-save migration.
 *
 * Migrations operate on persistence DTO fields rather than directly mutating domain
 * GameState. This keeps old save compatibility independent from ordinary domain refactors.
 */
interface SaveMigration {
    val fromVersion: SaveVersion
    val toVersion: SaveVersion

    fun migrate(data: SaveData): SaveData
}
