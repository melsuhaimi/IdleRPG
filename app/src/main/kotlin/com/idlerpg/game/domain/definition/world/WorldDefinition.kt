package com.idlerpg.game.domain.definition.world

import com.idlerpg.game.core.id.ContentId

/**
 * Root authored world graph.
 *
 * Foundation 7 keeps the graph deliberately small: it identifies every region and which
 * regions are unlocked at the beginning of a fresh run. Later foundations may add richer
 * unlock requirements without changing runtime identity.
 */
data class WorldDefinition(
    val id: ContentId,
    val regionIds: List<ContentId>,
    val startingRegionIds: Set<ContentId>
) {
    init {
        require(regionIds.isNotEmpty()) {
            "WorldDefinition.regionIds cannot be empty for $id"
        }
        require(regionIds.size == regionIds.toSet().size) {
            "WorldDefinition.regionIds cannot contain duplicates for $id"
        }
        require(startingRegionIds.isNotEmpty()) {
            "WorldDefinition.startingRegionIds cannot be empty for $id"
        }
        require(startingRegionIds.all { it in regionIds }) {
            "WorldDefinition.startingRegionIds must be contained in regionIds for $id"
        }
    }
}
