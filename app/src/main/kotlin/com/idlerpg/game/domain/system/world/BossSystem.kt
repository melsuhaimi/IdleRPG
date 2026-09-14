package com.idlerpg.game.domain.system.world

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.world.BossDefinition
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.event.BossDefeated
import com.idlerpg.game.domain.event.BossFirstClearRewardGranted
import com.idlerpg.game.domain.event.BossUnlocked
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.world.RegionProgressState

/**
 * Boss progression infrastructure introduced by Foundation 7.
 *
 * Boss milestones use the same encounter/combat machinery as normal stages. Each authored
 * boss definition owns its own first-clear identity and unlock threshold.
 */
object BossSystem {

    fun bossForEncounter(
        encounterDefinition: EncounterDefinition,
        contentRegistry: ContentRegistry
    ): BossDefinition? =
        encounterDefinition.bossId?.let(contentRegistry::boss)

    fun isUnlocked(
        progress: RegionProgressState,
        bossDefinition: BossDefinition
    ): Boolean =
        progress.normalClears >= GameNumber.of(bossDefinition.requiredNormalClears)

    fun newlyUnlockedEvents(
        regionId: com.idlerpg.game.core.id.ContentId,
        bossIds: List<com.idlerpg.game.core.id.ContentId>,
        before: RegionProgressState,
        after: RegionProgressState,
        contentRegistry: ContentRegistry
    ): List<GameEvent> =
        bossIds
            .sorted()
            .map(contentRegistry::boss)
            .filter { boss ->
                !isUnlocked(before, boss) &&
                    isUnlocked(after, boss) &&
                    boss.id !in after.clearedBossIds
            }
            .map { boss ->
                BossUnlocked(
                    bossId = boss.id,
                    regionId = regionId
                )
            }

    fun recordBossDefeat(
        progress: RegionProgressState,
        encounterDefinition: EncounterDefinition,
        contentRegistry: ContentRegistry
    ): Pair<RegionProgressState, List<GameEvent>> {
        val boss = bossForEncounter(
            encounterDefinition = encounterDefinition,
            contentRegistry = contentRegistry
        ) ?: return progress to emptyList()

        if (boss.id in progress.clearedBossIds) {
            return progress to emptyList()
        }

        return progress.copy(
            clearedBossIds = progress.clearedBossIds + boss.id
        ) to buildList {
            add(BossDefeated(
                bossId = boss.id,
                regionId = boss.regionId
            ))
            encounterDefinition.rewardLootTableId?.let { add(BossFirstClearRewardGranted(boss.id, it)) }
        }
    }
}
