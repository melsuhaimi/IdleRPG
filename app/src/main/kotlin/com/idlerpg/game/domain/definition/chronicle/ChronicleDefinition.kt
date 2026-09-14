package com.idlerpg.game.domain.definition.chronicle

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Authored reset contract for a Chronicle collapse.
 *
 * Foundation 17 intentionally begins with one bounded eligibility metric: the total
 * number of normal encounters cleared during the current RunState. The definition owns
 * the required amount, Echo reward, reset-rule version, and persistent milestone ID.
 */
data class ChronicleDefinition(
    val id: ContentId,
    val resetRuleVersion: Int,
    val requiredTotalNormalClears: GameNumber,
    val echoReward: GameNumber,
    val milestoneId: ContentId? = null,
    val requiredBossId: ContentId? = null,
    val echoPerDeepestStage: GameNumber = GameNumber.ZERO,
    val echoPerEliteClear: GameNumber = GameNumber.ZERO
) {
    init {
        require(resetRuleVersion > 0) {
            "resetRuleVersion must be positive for $id"
        }
        require(requiredTotalNormalClears > GameNumber.ZERO) {
            "requiredTotalNormalClears must be > 0 for $id"
        }
        require(echoReward > GameNumber.ZERO) {
            "echoReward must be > 0 for $id"
        }
        require(echoPerDeepestStage >= GameNumber.ZERO && echoPerEliteClear >= GameNumber.ZERO)
    }
}
