package com.idlerpg.game.presentation

import com.idlerpg.game.ui.theme.MotionTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** FUI-11 palette contrast + bounded-motion contract regression. */
object PresentationAccessibilityContractTest {
    @JvmStatic
    fun main(args: Array<String>) {
        check(contrastRatio(0xFFF4F5F8.toInt(), 0xFF090A0F.toInt()) >= 7.0)
        check(contrastRatio(0xFFAEB4C2.toInt(), 0xFF11131A.toInt()) >= 4.5)
        check(contrastRatio(0xFFFF6B7A.toInt(), 0xFF090A0F.toInt()) >= 4.5)

        val affinityColors = intArrayOf(
            0xFFFF6470.toInt(),
            0xFF49D6E9.toInt(),
            0xFFFF8A45.toInt(),
            0xFF8CBFFF.toInt(),
            0xFFB277FF.toInt(),
            0xFF67D889.toInt(),
            0xFF7F78D9.toInt(),
            0xFFD7C86E.toInt()
        )
        affinityColors.forEach { color ->
            check(contrastRatio(color, 0xFF090A0F.toInt()) >= 4.5) {
                "Affinity color must remain readable on Obsidian background: ${color.toUInt().toString(16)}"
            }
        }

        check(MotionTokens.MICRO_MILLIS in 120..180)
        check(MotionTokens.CARD_MILLIS in 220..320)
        check(MotionTokens.SIGNATURE_MILLIS in 350..550)

        println("FUI11_ACCESSIBILITY_CONTRACT_PASS")
    }

    private fun contrastRatio(foreground: Int, background: Int): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        val lighter = max(a, b)
        val darker = min(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(argb: Int): Double {
        fun channel(value: Int): Double {
            val normalized = value / 255.0
            return if (normalized <= 0.04045) {
                normalized / 12.92
            } else {
                ((normalized + 0.055) / 1.055).pow(2.4)
            }
        }
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
