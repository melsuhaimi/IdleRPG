package com.idlerpg.game.presentation.query

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.system.chronicle.ChronicleSystem
import com.idlerpg.game.domain.system.economy.UpgradeSystem
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import com.idlerpg.game.domain.system.inventory.InventoryCapacitySystem
import com.idlerpg.game.domain.system.progression.AffinityMasterySystem
import com.idlerpg.game.domain.system.progression.FeatureUnlockSystem
import com.idlerpg.game.domain.system.progression.PlayerProgressionSystem
import com.idlerpg.game.domain.system.skill.SkillValidationSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem

/** Delegates every supported derived value to the existing backend owner. */
class DefaultGameReadQueries(
    private val contentRegistry: ContentRegistry,
    private val balanceConfig: BalanceConfig,
    private val readEngineContext: EngineContext
) : GameReadQueries {
    override fun attackPower(state: GameState): GameNumber =
        DerivedStatSystem.attackPower(state, contentRegistry)

    override fun armor(state: GameState): GameNumber =
        DerivedStatSystem.armor(state, contentRegistry)

    override fun maximumHealth(state: GameState): GameNumber =
        DerivedStatSystem.maximumHealth(state, contentRegistry)

    override fun actionSpeed(state: GameState): Ratio =
        DerivedStatSystem.actionSpeed(state, contentRegistry)

    override fun criticalChance(state: GameState): Ratio =
        DerivedStatSystem.criticalChance(state, contentRegistry)

    override fun criticalMultiplier(state: GameState): Ratio =
        DerivedStatSystem.criticalMultiplier(state, contentRegistry)

    override fun damageReduction(state: GameState): Ratio =
        DerivedStatSystem.damageReduction(state, contentRegistry)

    override fun effectPower(state: GameState): Ratio =
        DerivedStatSystem.effectPower(state, contentRegistry)

    override fun healingPower(state: GameState): Ratio =
        DerivedStatSystem.healingPower(state, contentRegistry)

    override fun basicAttackInterval(state: GameState): GameDuration =
        DerivedStatSystem.basicAttackInterval(state, contentRegistry.basicAttack, contentRegistry)

    override fun basicAttackDps(state: GameState): GameRate =
        DerivedStatSystem.basicAttackDps(state, contentRegistry.basicAttack, contentRegistry)

    override fun enemyMaximumHealth(state: GameState, enemy: EnemyState): GameNumber {
        val definition = contentRegistry.enemy(enemy.definitionId)
        val region = state.run.world.activeRegionId?.let(contentRegistry::regionOrNull)
        return EnemyScalingSystem.scaledHealth(
            enemyDefinition = definition,
            regionDefinition = region,
            scalingTier = enemy.scalingTier
        )
    }

    override fun skillQueueRejection(
        state: GameState,
        skillId: ContentId
    ): CommandRejectionReason? =
        SkillValidationSystem.queueRejectionReason(state, skillId, contentRegistry)

    override fun skillExecutionRejection(
        state: GameState,
        skillId: ContentId
    ): CommandRejectionReason? =
        SkillValidationSystem.rejectionReason(state, skillId, contentRegistry)

    override fun upgradeLevel(state: GameState, upgradeId: ContentId): Long =
        UpgradeSystem.currentLevel(state, upgradeId)

    override fun upgradeCurrentCost(state: GameState, upgradeId: ContentId): GameNumber =
        UpgradeSystem.currentCost(state, upgradeId, readEngineContext)

    override fun upgradePurchaseCost(
        state: GameState,
        upgradeId: ContentId,
        quantity: Long
    ): GameNumber? = UpgradeSystem.purchaseCost(state, upgradeId, quantity, readEngineContext)

    override fun maximumAffordableUpgradeQuantity(state: GameState, upgradeId: ContentId): Long =
        UpgradeSystem.maximumAffordableQuantity(state, upgradeId, readEngineContext)

    override fun playerExperienceToNextLevel(state: GameState): GameNumber =
        PlayerProgressionSystem.experienceToNextLevel(state, contentRegistry)

    override fun masteryLevel(state: GameState, affinityId: ContentId): Long =
        AffinityMasterySystem.levelFor(state, affinityId, contentRegistry)

    override fun masteryExperienceToNextLevel(
        state: GameState,
        affinityId: ContentId
    ): GameNumber =
        AffinityMasterySystem.experienceToNextLevel(state, affinityId, contentRegistry)

    override fun isFeatureUnlocked(state: GameState, featureId: ContentId): Boolean {
        val definition = contentRegistry.featureUnlockOrNull(featureId) ?: return false
        return FeatureUnlockSystem.isUnlocked(state, definition)
    }

    override fun chronicleEligible(state: GameState): Boolean =
        ChronicleSystem.isEligible(state, contentRegistry.defaultChronicleDefinition())

    override fun doctrineCapacity(state: GameState): Int =
        balanceConfig.baseDoctrineRuleCapacity +
            if (DefaultGameContent.DOCTRINE_MEMORY_FEATURE_ID in
                state.meta.persistentFeatureUnlocks.unlockedFeatureIds
            ) 1 else 0

    override fun doctrineConditionMaxDepth(): Int = balanceConfig.doctrineConditionMaxDepth

    override fun resonanceChargeCap(): GameNumber = balanceConfig.resonanceChargeCapPerAffinity

    override fun skillLoadoutCapacity(): Int = balanceConfig.skillLoadoutCapacity

    override fun effectiveInventoryCapacity(state: GameState): Long =
        InventoryCapacitySystem.effectiveSlotCapacity(state.run.inventory)

    override fun inventoryOverflowCapacity(): Long = balanceConfig.inventoryOverflowCapacity

    override fun nextInventoryExpansionCost(state: GameState): GameNumber =
        InventoryCapacitySystem.nextExpansionCost(state.run.inventory, balanceConfig)

    override fun inventoryProgressionBlocked(state: GameState): Boolean =
        InventoryCapacitySystem.isProgressionBlocked(state.run.inventory, balanceConfig)
}
