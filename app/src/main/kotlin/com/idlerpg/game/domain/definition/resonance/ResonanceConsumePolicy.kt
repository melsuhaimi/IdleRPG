package com.idlerpg.game.domain.definition.resonance

/**
 * Determines how an eligible Convergence consumes stored Resonance charge.
 *
 * Foundation 8 intentionally starts with two deterministic policies. Additional policies
 * require an explicit architecture/balance decision rather than ad-hoc system branches.
 */
enum class ResonanceConsumePolicy {
    NONE,
    REQUIRED_CHARGE
}
