package com.idlerpg.game.core.id

/**
 * Stable identifier for authored static content.
 *
 * Display/localized strings must never be used as persistence identity. Content IDs are
 * lowercase dot-separated tokens such as enemy.slime or convergence.forged_flame.
 */
data class ContentId(
    val value: String
) : Comparable<ContentId> {

    init {
        require(PATTERN.matches(value)) {
            "Invalid ContentId '$value'. Expected lowercase dot-separated identifier tokens."
        }
    }

    override fun compareTo(other: ContentId): Int = value.compareTo(other.value)

    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")

        fun parse(value: String): ContentId = ContentId(value)
    }
}
