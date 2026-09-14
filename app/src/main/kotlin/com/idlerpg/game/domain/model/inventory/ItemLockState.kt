package com.idlerpg.game.domain.model.inventory

import com.idlerpg.game.core.id.InstanceId

/** Run-level protection state for destructive item actions such as salvage. */
data class ItemLockState(
    val lockedItemInstanceIds: Set<InstanceId> = emptySet()
) {
    fun isLocked(itemInstanceId: InstanceId): Boolean =
        itemInstanceId in lockedItemInstanceIds
}
