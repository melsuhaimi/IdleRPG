package com.idlerpg.game.domain.definition.world

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio

/**
 * Static authored region content through Foundation 10.
 *
 * Adaptation references are data-driven. The region selects its pressure threshold curve,
 * mutation pool, reward compensation profile, and maximum simultaneous mutations.
 */
data class RegionDefinition(
    val id: ContentId,
    val displayName: String,
    val encounterIds: List<ContentId>,
    val bossIds: List<ContentId> = emptyList(),
    val enemyHealthGrowthPerTier: GameNumber = GameNumber.ZERO,
    val rewardMultiplier: Ratio = Ratio.ONE,
    val adaptationThresholdDefinitionId: ContentId? = null,
    val adaptationMutationIds: List<ContentId> = emptyList(),
    val adaptationRewardDefinitionId: ContentId? = null,
    val maxAdaptationMutationsPerEnemy: Int = 0
) {
    init {
        require(displayName.isNotBlank()) {
            "RegionDefinition.displayName cannot be blank for $id"
        }
        require(encounterIds.isNotEmpty()) {
            "RegionDefinition.encounterIds cannot be empty for $id"
        }
        require(encounterIds.size == encounterIds.toSet().size) {
            "RegionDefinition.encounterIds cannot contain duplicates for $id"
        }
        require(bossIds.size == bossIds.toSet().size) {
            "RegionDefinition.bossIds cannot contain duplicates for $id"
        }
        require(adaptationMutationIds.size == adaptationMutationIds.toSet().size) {
            "RegionDefinition.adaptationMutationIds cannot contain duplicates for $id"
        }
        require(maxAdaptationMutationsPerEnemy >= 0) {
            "maxAdaptationMutationsPerEnemy cannot be negative for $id"
        }
        if (adaptationMutationIds.isNotEmpty()) {
            require(adaptationThresholdDefinitionId != null) {
                "Region $id with Adaptation mutations requires a threshold definition"
            }
            require(maxAdaptationMutationsPerEnemy > 0) {
                "Region $id with Adaptation mutations requires at least one mutation slot"
            }
        }
    }
}
