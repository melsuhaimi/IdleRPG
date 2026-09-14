package com.idlerpg.game.domain.model.doctrine

/**
 * Canonical run-level Doctrine automation state.
 *
 * Rules are evaluated strictly in list order. Rule capacity usage is derived from the
 * list size rather than duplicated as a second source of truth. Structural legality is
 * enforced by DoctrineValidator before command transitions are committed.
 */
data class DoctrineState(
    val enabled: Boolean = true,
    val rules: List<DoctrineRule> = emptyList()
) {
    val ruleCount: Int
        get() = rules.size
}
