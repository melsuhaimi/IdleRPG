package com.idlerpg.game.core.config

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration

/**
 * Cross-system balance configuration through Foundation 16.
 *
 * Resonance, Doctrine, Adaptation, and offline progression need bounded deterministic
 * policy. These defaults are baseline/test tuning, not final production balance.
 */
data class BalanceConfig(
    val revision: Int = DEFAULT_REVISION,
    val resonanceChargeCapPerAffinity: GameNumber = DEFAULT_RESONANCE_CHARGE_CAP,
    val resonanceSequenceBufferSize: Int = DEFAULT_RESONANCE_SEQUENCE_BUFFER_SIZE,
    val baseDoctrineRuleCapacity: Int = DEFAULT_DOCTRINE_RULE_CAPACITY,
    val doctrineConditionMaxDepth: Int = DEFAULT_DOCTRINE_CONDITION_MAX_DEPTH,
    val skillLoadoutCapacity: Int = DEFAULT_SKILL_LOADOUT_CAPACITY,
    val adaptationExposureGainRatio: Ratio = DEFAULT_ADAPTATION_EXPOSURE_GAIN_RATIO,
    val adaptationUnusedAffinityDecayPerEncounter: GameNumber =
        DEFAULT_ADAPTATION_UNUSED_AFFINITY_DECAY,
    val maximumOfflineDuration: GameDuration = DEFAULT_MAXIMUM_OFFLINE_DURATION,
    val baseInventoryCapacity: Long = DEFAULT_BASE_INVENTORY_CAPACITY,
    val inventoryCapacityPerExpansion: Long = DEFAULT_INVENTORY_CAPACITY_PER_EXPANSION,
    val maximumInventoryCapacity: Long = DEFAULT_MAXIMUM_INVENTORY_CAPACITY,
    val baseInventoryExpansionGoldCost: GameNumber = DEFAULT_BASE_INVENTORY_EXPANSION_GOLD_COST,
    val inventoryExpansionGoldCostStep: GameNumber = DEFAULT_INVENTORY_EXPANSION_GOLD_COST_STEP,
    val inventoryOverflowCapacity: Long = DEFAULT_INVENTORY_OVERFLOW_CAPACITY
) {
    init {
        require(revision > 0) { "BalanceConfig revision must be positive: $revision" }
        require(resonanceChargeCapPerAffinity > GameNumber.ZERO) {
            "resonanceChargeCapPerAffinity must be > 0"
        }
        require(resonanceSequenceBufferSize > 0) {
            "resonanceSequenceBufferSize must be positive: $resonanceSequenceBufferSize"
        }
        require(baseDoctrineRuleCapacity > 0) {
            "baseDoctrineRuleCapacity must be positive: $baseDoctrineRuleCapacity"
        }
        require(doctrineConditionMaxDepth > 0) {
            "doctrineConditionMaxDepth must be positive: $doctrineConditionMaxDepth"
        }
        require(skillLoadoutCapacity > 0) {
            "skillLoadoutCapacity must be positive: $skillLoadoutCapacity"
        }
        require(adaptationExposureGainRatio > Ratio.ZERO) {
            "adaptationExposureGainRatio must be > 0"
        }
        require(maximumOfflineDuration > GameDuration.ZERO) {
            "maximumOfflineDuration must be > 0"
        }
        require(baseInventoryCapacity > 0L) {
            "baseInventoryCapacity must be positive: $baseInventoryCapacity"
        }
        require(inventoryCapacityPerExpansion > 0L) {
            "inventoryCapacityPerExpansion must be positive: $inventoryCapacityPerExpansion"
        }
        require(maximumInventoryCapacity >= baseInventoryCapacity) {
            "maximumInventoryCapacity must be >= baseInventoryCapacity"
        }
        require(baseInventoryExpansionGoldCost > GameNumber.ZERO) {
            "baseInventoryExpansionGoldCost must be > 0"
        }
        require(inventoryExpansionGoldCostStep >= GameNumber.ZERO) {
            "inventoryExpansionGoldCostStep cannot be negative"
        }
        require(inventoryOverflowCapacity > 0L) {
            "inventoryOverflowCapacity must be positive: $inventoryOverflowCapacity"
        }
    }

    companion object {
        const val DEFAULT_REVISION: Int = 1
        val DEFAULT_RESONANCE_CHARGE_CAP: GameNumber = GameNumber.of(100L)
        const val DEFAULT_RESONANCE_SEQUENCE_BUFFER_SIZE: Int = 8
        const val DEFAULT_DOCTRINE_RULE_CAPACITY: Int = 8
        const val DEFAULT_DOCTRINE_CONDITION_MAX_DEPTH: Int = 4
        const val DEFAULT_SKILL_LOADOUT_CAPACITY: Int = 4

        /**
         * Foundation 10 baseline/test tuning:
         * - 100% of effective affinity contribution becomes pressure.
         * - an unused affinity loses 20 pressure per completed encounter.
         */
        val DEFAULT_ADAPTATION_EXPOSURE_GAIN_RATIO: Ratio = Ratio.ONE
        val DEFAULT_ADAPTATION_UNUSED_AFFINITY_DECAY: GameNumber =
            GameNumber.of(20L)

        /**
         * Foundation 16 baseline offline cap.
         *
         * The architecture requires a bounded maximum offline duration but does not
         * prescribe its final balance value. Twenty-four hours is the initial exact-
         * simulation verification cap and may be tuned later through this one config.
         */
        val DEFAULT_MAXIMUM_OFFLINE_DURATION: GameDuration =
            GameDuration.ofHours(24L)

        /**
         * FBE-00 persistence defaults for the inventory-capacity state contract.
         * Enforcement/overflow behavior is activated later by FBE-04; these values are
         * centralized now so new games, Chronicle resets, and V1 -> V2 migration share
         * one source of truth.
         */
        const val DEFAULT_BASE_INVENTORY_CAPACITY: Long = 60L
        const val DEFAULT_INVENTORY_CAPACITY_PER_EXPANSION: Long = 20L
        const val DEFAULT_MAXIMUM_INVENTORY_CAPACITY: Long = 300L
        val DEFAULT_BASE_INVENTORY_EXPANSION_GOLD_COST: GameNumber =
            GameNumber.of(100L)
        val DEFAULT_INVENTORY_EXPANSION_GOLD_COST_STEP: GameNumber =
            GameNumber.of(100L)
        const val DEFAULT_INVENTORY_OVERFLOW_CAPACITY: Long = 20L
    }
}
