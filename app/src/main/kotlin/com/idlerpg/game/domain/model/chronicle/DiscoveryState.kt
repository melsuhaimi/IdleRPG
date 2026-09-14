package com.idlerpg.game.domain.model.chronicle

import com.idlerpg.game.core.id.ContentId

/** Persistent knowledge discovered across Chronicles. */
data class DiscoveryState(
    val discoveredConvergenceIds: Set<ContentId> = emptySet(),
    val discoveredMutationIds: Set<ContentId> = emptySet(),
    val discoveredEnemyKnowledgeIds: Set<ContentId> = emptySet(),
    val unlockedHiddenContentIds: Set<ContentId> = emptySet()
)
