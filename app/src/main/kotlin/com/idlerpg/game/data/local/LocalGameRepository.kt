package com.idlerpg.game.data.local

import com.idlerpg.game.data.local.migration.SaveMigrationRegistry
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.data.repository.GameRepositoryException
import java.io.File
import java.io.FileOutputStream

/**
 * Local single-slot save repository with verified candidate and backup rotation.
 *
 * The constructor accepts a plain [File] directory rather than Android Context. Future
 * application wiring may pass Context.filesDir at the Android boundary, while this data
 * implementation stays headlessly testable.
 *
 * Save flow:
 *
 * encode current schema
 * -> write candidate + fsync
 * -> decode/verify candidate
 * -> rotate primary to backup
 * -> rename candidate to primary
 * -> decode/verify promoted primary
 *
 * [File.renameTo] is used only for same-directory promotion/rotation. The backup is kept
 * until the new primary has been verified so a failed promotion can be rolled back.
 */
class LocalGameRepository(
    private val directory: File,
    private val codec: SaveCodec = SaveCodec(),
    private val migrationRegistry: SaveMigrationRegistry = SaveMigrationRegistry(),
    fileName: String = DEFAULT_FILE_NAME
) : GameRepository {

    private val primaryFile: File
    private val candidateFile: File
    private val backupFile: File

    init {
        require(fileName.isNotBlank()) {
            "Save file name cannot be blank"
        }
        require('/' !in fileName && '\\' !in fileName) {
            "Save file name must not contain path separators: $fileName"
        }

        primaryFile = File(directory, fileName)
        candidateFile = File(directory, "$fileName.candidate")
        backupFile = File(directory, "$fileName.backup")
    }

    override fun load(): SaveEnvelope? {
        if (!primaryFile.exists() && !backupFile.exists()) {
            return null
        }

        val primaryFailure = if (primaryFile.exists()) {
            try {
                return loadAndMigrate(primaryFile)
            } catch (error: Throwable) {
                error
            }
        } else {
            null
        }

        if (backupFile.exists()) {
            try {
                return loadAndMigrate(backupFile)
            } catch (backupFailure: Throwable) {
                val exception = GameRepositoryException(
                    "Primary and backup saves are both invalid",
                    backupFailure
                )
                if (primaryFailure != null) {
                    exception.addSuppressed(primaryFailure)
                }
                throw exception
            }
        }

        throw GameRepositoryException(
            "Primary save is invalid and no backup exists",
            primaryFailure
        )
    }

    override fun save(envelope: SaveEnvelope) {
        ensureDirectory()

        val currentEnvelope = try {
            migrationRegistry.migrate(
                envelope,
                SaveVersion.CURRENT
            )
        } catch (error: Throwable) {
            throw GameRepositoryException(
                "Cannot migrate envelope to current save schema",
                error
            )
        }

        // Reconstruct before touching disk so invalid persistence DTOs cannot replace a save.
        try {
            currentEnvelope.gameState()
        } catch (error: Throwable) {
            throw GameRepositoryException(
                "Refusing to persist SaveData that cannot reconstruct GameState",
                error
            )
        }

        val bytes = try {
            codec.encode(currentEnvelope)
        } catch (error: Throwable) {
            throw GameRepositoryException(
                "Failed to encode save candidate",
                error
            )
        }

        deleteIfExistsOrThrow(
            candidateFile,
            "stale save candidate"
        )
        writeAndSync(candidateFile, bytes)

        try {
            verifyCurrentSaveFile(candidateFile)
        } catch (error: Throwable) {
            candidateFile.delete()
            throw GameRepositoryException(
                "Save candidate verification failed before promotion",
                error
            )
        }

        var primaryRotated = false
        try {
            if (primaryFile.exists()) {
                val primaryIsValid = try {
                    loadAndMigrate(primaryFile)
                    true
                } catch (_: Throwable) {
                    false
                }

                if (primaryIsValid) {
                    deleteIfExistsOrThrow(
                        backupFile,
                        "previous save backup"
                    )
                    if (!primaryFile.renameTo(backupFile)) {
                        throw GameRepositoryException(
                            "Failed to rotate primary save to backup"
                        )
                    }
                    primaryRotated = true
                } else {
                    // Never overwrite a known-good backup with a corrupt primary.
                    deleteIfExistsOrThrow(
                        primaryFile,
                        "invalid primary save"
                    )
                }
            }

            if (!candidateFile.renameTo(primaryFile)) {
                if (backupFile.exists() && !primaryFile.exists()) {
                    backupFile.renameTo(primaryFile)
                }
                throw GameRepositoryException(
                    "Failed to promote verified save candidate"
                )
            }

            try {
                verifyCurrentSaveFile(primaryFile)
            } catch (verificationFailure: Throwable) {
                // Remove the bad promoted file and restore any prior valid backup.
                primaryFile.delete()
                if (backupFile.exists()) {
                    backupFile.renameTo(primaryFile)
                }
                throw GameRepositoryException(
                    "Promoted save failed final verification; rollback attempted",
                    verificationFailure
                )
            }
        } finally {
            if (candidateFile.exists()) {
                candidateFile.delete()
            }
        }
    }

    override fun exists(): Boolean =
        primaryFile.exists() || backupFile.exists()

    override fun delete() {
        val failures = mutableListOf<String>()

        for (file in listOf(candidateFile, primaryFile, backupFile)) {
            if (file.exists() && !file.delete()) {
                failures += file.absolutePath
            }
        }

        if (failures.isNotEmpty()) {
            throw GameRepositoryException(
                "Failed to delete save files: ${failures.joinToString()}"
            )
        }
    }

    private fun loadAndMigrate(file: File): SaveEnvelope {
        val decoded = decodeFile(file)
        val migrated = migrationRegistry.migrate(
            decoded,
            SaveVersion.CURRENT
        )

        try {
            migrated.gameState()
        } catch (error: Throwable) {
            throw GameRepositoryException(
                "Save reconstructs an invalid current GameState: ${file.name}",
                error
            )
        }

        return migrated
    }

    private fun verifyCurrentSaveFile(file: File) {
        val decoded = decodeFile(file)
        if (decoded.schemaVersion != SaveVersion.CURRENT) {
            throw GameRepositoryException(
                "Verified save candidate is not current schema: " +
                    decoded.schemaVersion.value
            )
        }
        decoded.gameState()
    }

    private fun decodeFile(file: File): SaveEnvelope {
        val length = file.length()
        if (length <= 0L) {
            throw GameRepositoryException(
                "Save file is empty: ${file.name}"
            )
        }
        if (length > SaveCodec.MAX_SAVE_BYTES.toLong()) {
            throw GameRepositoryException(
                "Save file exceeds maximum supported size: $length"
            )
        }

        return try {
            codec.decode(file.readBytes())
        } catch (error: Throwable) {
            throw GameRepositoryException(
                "Failed to decode save file: ${file.name}",
                error
            )
        }
    }

    private fun ensureDirectory() {
        if (directory.exists()) {
            if (!directory.isDirectory) {
                throw GameRepositoryException(
                    "Save directory path is not a directory: " +
                        directory.absolutePath
                )
            }
            return
        }

        if (!directory.mkdirs() && !directory.isDirectory) {
            throw GameRepositoryException(
                "Failed to create save directory: " +
                    directory.absolutePath
            )
        }
    }

    private fun writeAndSync(
        file: File,
        bytes: ByteArray
    ) {
        try {
            FileOutputStream(file).use { output ->
                output.write(bytes)
                output.flush()
                output.fd.sync()
            }
        } catch (error: Throwable) {
            file.delete()
            throw GameRepositoryException(
                "Failed to write and sync save candidate",
                error
            )
        }
    }

    private fun deleteIfExistsOrThrow(
        file: File,
        description: String
    ) {
        if (file.exists() && !file.delete()) {
            throw GameRepositoryException(
                "Failed to remove $description: ${file.absolutePath}"
            )
        }
    }

    companion object {
        const val DEFAULT_FILE_NAME: String = "idle_rpg.save"
    }
}
