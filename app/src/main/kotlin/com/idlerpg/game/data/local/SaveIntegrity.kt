package com.idlerpg.game.data.local

import java.security.MessageDigest

/**
 * Integrity metadata for one encoded save.
 *
 * This detects accidental corruption/incomplete writes. It is explicitly not anti-cheat
 * security because a local single-player client can recompute an unkeyed digest.
 */
data class SaveIntegrityMetadata(
    val algorithm: String,
    val digestHex: String
) {
    init {
        require(algorithm == SaveIntegrity.ALGORITHM) {
            "Unsupported save integrity algorithm: $algorithm"
        }
        require(digestHex.length == SaveIntegrity.DIGEST_BYTES * 2) {
            "Invalid SHA-256 hex length: ${digestHex.length}"
        }
        require(digestHex.all { it in '0'..'9' || it in 'a'..'f' }) {
            "Save integrity digest must be lowercase hexadecimal"
        }
    }
}

object SaveIntegrity {
    const val ALGORITHM: String = "SHA-256"
    const val DIGEST_BYTES: Int = 32

    fun digest(payload: ByteArray): ByteArray =
        MessageDigest.getInstance(ALGORITHM).digest(payload)

    fun digestHex(payload: ByteArray): String =
        toHex(digest(payload))

    fun metadata(payload: ByteArray): SaveIntegrityMetadata =
        SaveIntegrityMetadata(
            algorithm = ALGORITHM,
            digestHex = digestHex(payload)
        )

    fun verify(payload: ByteArray, expectedDigest: ByteArray): Boolean {
        if (expectedDigest.size != DIGEST_BYTES) {
            return false
        }
        return MessageDigest.isEqual(
            digest(payload),
            expectedDigest
        )
    }

    fun verify(payload: ByteArray, metadata: SaveIntegrityMetadata): Boolean {
        if (metadata.algorithm != ALGORITHM) {
            return false
        }
        val expected = fromHex(metadata.digestHex) ?: return false
        return verify(payload, expected)
    }

    fun toHex(bytes: ByteArray): String =
        buildString(bytes.size * 2) {
            for (byte in bytes) {
                val value = byte.toInt() and 0xFF
                append(HEX[value ushr 4])
                append(HEX[value and 0x0F])
            }
        }

    fun fromHex(value: String): ByteArray? {
        if (value.length % 2 != 0) {
            return null
        }
        val result = ByteArray(value.length / 2)
        var index = 0
        while (index < value.length) {
            val high = hexDigit(value[index])
            val low = hexDigit(value[index + 1])
            if (high < 0 || low < 0) {
                return null
            }
            result[index / 2] = ((high shl 4) or low).toByte()
            index += 2
        }
        return result
    }

    private fun hexDigit(char: Char): Int =
        when (char) {
            in '0'..'9' -> char - '0'
            in 'a'..'f' -> char - 'a' + 10
            in 'A'..'F' -> char - 'A' + 10
            else -> -1
        }

    private const val HEX: String = "0123456789abcdef"
}
