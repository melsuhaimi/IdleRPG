package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion


/** Failure to reach the requested save schema through explicit sequential migrations. */
class SaveMigrationException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/**
 * Deterministic ordered migration registry.
 *
 * Direct version jumps are deliberately forbidden. Every migration must advance exactly
 * one schema version so unsupported gaps cannot be silently guessed.
 */
class SaveMigrationRegistry(
    migrations: List<SaveMigration> = emptyList()
) {
    private val migrationByFromVersion: Map<SaveVersion, SaveMigration>

    init {
        val effectiveMigrations = builtInMigrations() + migrations

        require(
            effectiveMigrations.all {
                it.toVersion.value == it.fromVersion.value + 1
            }
        ) {
            "Each SaveMigration must advance exactly one version"
        }

        val grouped = effectiveMigrations.groupBy { it.fromVersion }
        require(grouped.values.all { it.size == 1 }) {
            "Only one migration may originate from each SaveVersion"
        }

        migrationByFromVersion = effectiveMigrations
            .sortedBy { it.fromVersion.value }
            .associateBy { it.fromVersion }
    }

    fun migrate(
        envelope: SaveEnvelope,
        targetVersion: SaveVersion = SaveVersion.CURRENT
    ): SaveEnvelope {
        if (envelope.schemaVersion > targetVersion) {
            throw SaveMigrationException(
                "Save schema ${envelope.schemaVersion.value} is newer than supported " +
                    "schema ${targetVersion.value}"
            )
        }

        var current = envelope
        while (current.schemaVersion < targetVersion) {
            val migration = migrationByFromVersion[current.schemaVersion]
                ?: throw SaveMigrationException(
                    "No migration registered from save schema " +
                        current.schemaVersion.value
                )

            val migratedData = try {
                migration.migrate(current.data)
            } catch (cause: Throwable) {
                throw SaveMigrationException(
                    "Migration ${migration.fromVersion.value} -> " +
                        "${migration.toVersion.value} failed",
                    cause
                )
            }

            current = current.copy(
                schemaVersion = migration.toVersion,
                data = migratedData,
                integrity = null
            )
        }

        return current
    }

    companion object {
        private fun builtInMigrations(): List<SaveMigration> =
            listOf(
                V1ToV2SaveMigration(),
                V2ToV3SaveMigration(),
                V3ToV4SaveMigration(),
                V4ToV5SaveMigration(),
                V5ToV6SaveMigration(),
                V6ToV7SaveMigration(),
                V7ToV8SaveMigration(),
                V8ToV9SaveMigration()
            )
    }
}
