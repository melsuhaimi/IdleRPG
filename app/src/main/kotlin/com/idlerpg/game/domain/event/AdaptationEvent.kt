package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber

/** Completed World Adaptation facts. */
sealed interface AdaptationEvent : GameEvent

data class AdaptationExposureRecorded(
    val regionId: ContentId,
    val affinityId: ContentId,
    val amount: GameNumber
) : AdaptationEvent

data class AdaptationTierChanged(
    val regionId: ContentId,
    val affinityId: ContentId,
    val previousTier: Int,
    val newTier: Int
) : AdaptationEvent {
    init {
        require(previousTier >= 0) { "previousTier cannot be negative: $previousTier" }
        require(newTier >= 0) { "newTier cannot be negative: $newTier" }
        require(previousTier != newTier) {
            "AdaptationTierChanged requires an actual tier change"
        }
    }
}

data class MutationRolled(
    val enemyInstanceId: InstanceId,
    val mutationId: ContentId
) : AdaptationEvent

data class AdaptedEnemySpawned(
    val enemyInstanceId: InstanceId,
    val enemyDefinitionId: ContentId,
    val mutationIds: List<ContentId>
) : AdaptationEvent {
    init {
        require(mutationIds.isNotEmpty()) {
            "AdaptedEnemySpawned requires at least one mutationId"
        }
        require(mutationIds.size == mutationIds.toSet().size) {
            "AdaptedEnemySpawned.mutationIds cannot contain duplicates"
        }
    }
}
