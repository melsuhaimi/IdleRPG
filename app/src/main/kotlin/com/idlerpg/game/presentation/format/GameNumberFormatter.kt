package com.idlerpg.game.presentation.format

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import java.math.RoundingMode

/** Exact-string idle-RPG number formatting. No Double conversion feeds presentation. */
object GameNumberFormatter {
    private val suffixes = listOf(
        "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"
    )

    fun full(value: GameNumber): String = value.toPlainString()

    /**
     * Compact a fractional rate without exposing the six-decimal storage precision to players.
     * Rates use the same suffix vocabulary as magnitudes. Values below one retain three
     * decimals; values smaller than 0.001 use a visible floor marker instead of becoming zero.
     */
    fun compact(value: GameRate): String {
        val raw = value.toBigDecimal()
        if (raw.signum() == 0) return "0"

        val integerDigits = (raw.precision() - raw.scale()).coerceAtLeast(0)
        if (integerDigits <= 3) {
            if (raw < BigDecimal.ONE) {
                val display = raw
                    .setScale(3, RoundingMode.DOWN)
                    .stripTrailingZeros()
                    .toPlainString()
                return if (display == "0") "<0.001" else display
            }
            return raw
                .setScale(minOf(2, raw.scale()), RoundingMode.DOWN)
                .stripTrailingZeros()
                .toPlainString()
        }

        val group = (integerDigits - 1) / 3
        val scaled = raw.movePointLeft(group * 3)
        val display = scaled
            .setScale(2, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
        val suffix = suffixes.getOrNull(group) ?: "e${group * 3}"
        return display + suffix
    }

    fun compact(value: GameNumber): String {
        val raw = value.toPlainString()
        if (raw.length <= 3) {
            return raw
        }

        val group = (raw.length - 1) / 3
        val leadingDigits = raw.length - (group * 3)
        val whole = raw.substring(0, leadingDigits)
        val fractionalSource = raw.substring(leadingDigits)
        val fractional = fractionalSource
            .take(2)
            .padEnd(2, '0')
            .trimEnd('0')
        val suffix = suffixes.getOrNull(group) ?: "e${group * 3}"

        return if (fractional.isEmpty()) {
            "$whole$suffix"
        } else {
            "$whole.$fractional$suffix"
        }
    }
}
