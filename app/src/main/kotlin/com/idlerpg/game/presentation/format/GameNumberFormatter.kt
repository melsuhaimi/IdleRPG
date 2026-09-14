package com.idlerpg.game.presentation.format

import com.idlerpg.game.core.number.GameNumber

/** Exact-string idle-RPG number formatting. No Double conversion feeds presentation. */
object GameNumberFormatter {
    private val suffixes = listOf(
        "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"
    )

    fun full(value: GameNumber): String = value.toPlainString()

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
