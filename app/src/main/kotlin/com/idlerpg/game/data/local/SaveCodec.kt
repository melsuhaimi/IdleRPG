package com.idlerpg.game.data.local

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/** Structured save-decoding/encoding failure. */
class SaveCodecException(
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

/**
 * Dependency-free deterministic binary save codec.
 *
 * Foundation 15 deliberately does not add a serialization library. The logical payload is
 * an explicit [SaveData] field map. This codec only owns the stable binary envelope:
 *
 * magic
 * binary format version
 * logical save schema version
 * content version
 * wall-clock written timestamp
 * sorted UTF-8 SaveData fields
 * SHA-256 digest of every preceding byte
 *
 * Sorted fields make byte output deterministic for identical envelopes. The SHA-256 digest
 * is corruption detection, not anti-cheat protection.
 */
class SaveCodec {

    fun encode(envelope: SaveEnvelope): ByteArray {
        if (envelope.data.fields.size > MAX_FIELD_COUNT) {
            throw SaveCodecException(
                "SaveData contains too many fields: ${envelope.data.fields.size}"
            )
        }

        val payloadBuffer = ByteArrayOutputStream()
        DataOutputStream(payloadBuffer).use { output ->
            output.write(MAGIC)
            output.writeInt(BINARY_FORMAT_VERSION)
            output.writeInt(envelope.schemaVersion.value)
            writeString(output, envelope.contentVersion)
            output.writeLong(envelope.writtenAtEpochMs)

            val entries = envelope.data.fields.entries.sortedBy { it.key }
            output.writeInt(entries.size)
            for ((key, value) in entries) {
                writeString(output, key)
                writeString(output, value)
            }
        }

        val payload = payloadBuffer.toByteArray()
        val digest = SaveIntegrity.digest(payload)
        val result = ByteArray(payload.size + digest.size)
        payload.copyInto(result, destinationOffset = 0)
        digest.copyInto(result, destinationOffset = payload.size)

        if (result.size > MAX_SAVE_BYTES) {
            throw SaveCodecException(
                "Encoded save exceeds maximum size: ${result.size} > $MAX_SAVE_BYTES"
            )
        }
        return result
    }

    fun decode(bytes: ByteArray): SaveEnvelope {
        if (bytes.size < MINIMUM_SAVE_BYTES) {
            throw SaveCodecException("Save is too small to contain a valid envelope")
        }
        if (bytes.size > MAX_SAVE_BYTES) {
            throw SaveCodecException(
                "Save exceeds maximum supported size: ${bytes.size} > $MAX_SAVE_BYTES"
            )
        }

        val payloadSize = bytes.size - SaveIntegrity.DIGEST_BYTES
        if (payloadSize <= 0) {
            throw SaveCodecException("Save does not contain an integrity digest")
        }

        val payload = bytes.copyOfRange(0, payloadSize)
        val storedDigest = bytes.copyOfRange(payloadSize, bytes.size)
        if (!SaveIntegrity.verify(payload, storedDigest)) {
            throw SaveCodecException("Save integrity verification failed")
        }

        try {
            DataInputStream(ByteArrayInputStream(payload)).use { input ->
                val magic = ByteArray(MAGIC.size)
                input.readFully(magic)
                if (!magic.contentEquals(MAGIC)) {
                    throw SaveCodecException("Unknown save magic header")
                }

                val binaryVersion = input.readInt()
                if (binaryVersion != BINARY_FORMAT_VERSION) {
                    throw SaveCodecException(
                        "Unsupported binary save format version: $binaryVersion"
                    )
                }

                val schemaVersion = SaveVersion(input.readInt())
                val contentVersion = readString(input)
                val writtenAtEpochMs = input.readLong()
                if (writtenAtEpochMs < 0L) {
                    throw SaveCodecException(
                        "Save writtenAtEpochMs cannot be negative: $writtenAtEpochMs"
                    )
                }

                val fieldCount = input.readInt()
                if (fieldCount < 0 || fieldCount > MAX_FIELD_COUNT) {
                    throw SaveCodecException("Invalid SaveData field count: $fieldCount")
                }

                val fields = LinkedHashMap<String, String>(fieldCount)
                repeat(fieldCount) {
                    val key = readString(input)
                    val value = readString(input)
                    if (fields.put(key, value) != null) {
                        throw SaveCodecException("Duplicate SaveData field key: $key")
                    }
                }

                if (input.available() != 0) {
                    throw SaveCodecException(
                        "Unexpected trailing payload bytes: ${input.available()}"
                    )
                }

                return SaveEnvelope(
                    schemaVersion = schemaVersion,
                    contentVersion = contentVersion,
                    writtenAtEpochMs = writtenAtEpochMs,
                    data = SaveData(fields),
                    integrity = SaveIntegrityMetadata(
                        algorithm = SaveIntegrity.ALGORITHM,
                        digestHex = SaveIntegrity.toHex(storedDigest)
                    )
                )
            }
        } catch (error: SaveCodecException) {
            throw error
        } catch (error: Throwable) {
            throw SaveCodecException("Malformed save payload", error)
        }
    }

    private fun writeString(
        output: DataOutputStream,
        value: String
    ) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_STRING_BYTES) {
            throw SaveCodecException(
                "Save string exceeds maximum UTF-8 length: ${bytes.size}"
            )
        }
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val length = input.readInt()
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw SaveCodecException("Invalid save string byte length: $length")
        }
        if (length > input.available()) {
            throw SaveCodecException(
                "Truncated save string: need $length bytes, only ${input.available()} remain"
            )
        }

        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    companion object {
        const val BINARY_FORMAT_VERSION: Int = 1
        const val MAX_FIELD_COUNT: Int = 100_000
        const val MAX_STRING_BYTES: Int = 8 * 1024 * 1024
        const val MAX_SAVE_BYTES: Int = 64 * 1024 * 1024

        private val MAGIC: ByteArray =
            byteArrayOf(
                'I'.code.toByte(),
                'D'.code.toByte(),
                'L'.code.toByte(),
                'E'.code.toByte(),
                'R'.code.toByte(),
                'P'.code.toByte(),
                'G'.code.toByte(),
                '1'.code.toByte()
            )

        private val MINIMUM_SAVE_BYTES: Int =
            MAGIC.size +
                4 + // binary format version
                4 + // logical schema version
                4 + // empty content-version string length
                8 + // writtenAtEpochMs
                4 + // field count
                SaveIntegrity.DIGEST_BYTES
    }
}
