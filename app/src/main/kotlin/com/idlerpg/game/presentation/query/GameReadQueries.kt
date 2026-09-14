package com.idlerpg.game.presentation.query

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.EnemyState

/** Read-only presentation facade over authoritative backend-derived values. */
interface GameReadQueries {
    fun attackPower(state: GameState): GameNumber
    fun armor(state: GameState): GameNumber
    fun maximumHealth(state: GameState): GameNumber = GameNumber.ZERO
    fun actionSpeed(state: GameState): Ratio = Ratio.ONE
    fun criticalChance(state: GameState): Ratio = Ratio.ZERO
    fun criticalMultiplier(state: GameState): Ratio = Ratio.ONE
    fun damageReduction(state: GameState): Ratio = Ratio.ZERO
    fun effectPower(state: GameState): Ratio = Ratio.ONE
    fun healingPower(state: GameState): Ratio = Ratio.ONE
    fun basicAttackInterval(state: GameState): GameDuration
    fun basicAttackDps(state: GameState): GameRate
    fun enemyMaximumHealth(state: GameState, enemy: EnemyState): GameNumber
    fun skillQueueRejection(state: GameState, skillId: ContentId): CommandRejectionReason?
    fun skillExecutionRejection(state: GameState, skillId: ContentId): CommandRejectionReason?
    fun upgradeLevel(state: GameState, upgradeId: ContentId): Long
    fun upgradeCurrentCost(state: GameState, upgradeId: ContentId): GameNumber
    fun upgradePurchaseCost(state: GameState, upgradeId: ContentId, quantity: Long): GameNumber? = null
    fun maximumAffordableUpgradeQuantity(state: GameState, upgradeId: ContentId): Long = 0L
    fun playerExperienceToNextLevel(state: GameState): GameNumber
    fun masteryLevel(state: GameState, affinityId: ContentId): Long
    fun masteryExperienceToNextLevel(state: GameState, affinityId: ContentId): GameNumber
    fun isFeatureUnlocked(state: GameState, featureId: ContentId): Boolean
    fun chronicleEligible(state: GameState): Boolean
    fun doctrineCapacity(state: GameState): Int
    fun doctrineConditionMaxDepth(): Int
    fun resonanceChargeCap(): GameNumber
    fun skillLoadoutCapacity(): Int
    fun effectiveInventoryCapacity(state: GameState): Long
    fun inventoryOverflowCapacity(): Long
    fun nextInventoryExpansionCost(state: GameState): GameNumber
    fun inventoryProgressionBlocked(state: GameState): Boolean
}
