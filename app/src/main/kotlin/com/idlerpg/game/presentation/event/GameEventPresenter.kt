package com.idlerpg.game.presentation.event

import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.AdaptationTierChanged
import com.idlerpg.game.domain.event.CombatEnded
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.ConvergenceDiscovered
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.DoctrineActionRejected
import com.idlerpg.game.domain.event.DoctrineFallbackUsed
import com.idlerpg.game.domain.event.DoctrineRuleSelected
import com.idlerpg.game.domain.event.DoctrineUpdated
import com.idlerpg.game.domain.event.ResonanceConsumed
import com.idlerpg.game.domain.event.ResonanceGenerated
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.event.ItemDropped
import com.idlerpg.game.domain.event.GearEnhancementAttempted
import com.idlerpg.game.domain.event.GearRefined
import com.idlerpg.game.domain.event.ItemAutoSalvaged
import com.idlerpg.game.domain.event.InventoryCapacityExpanded
import com.idlerpg.game.domain.event.ItemEquipped
import com.idlerpg.game.domain.event.ItemLocked
import com.idlerpg.game.domain.event.ItemSalvaged
import com.idlerpg.game.domain.event.ItemUnequipped
import com.idlerpg.game.domain.event.ItemUnlocked
import com.idlerpg.game.domain.event.OverflowItemClaimed
import com.idlerpg.game.domain.event.OverflowItemSalvaged
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.event.InventoryProgressionBlocked
import com.idlerpg.game.domain.event.InventoryProgressionUnblocked
import com.idlerpg.game.domain.event.MutationRolled
import com.idlerpg.game.domain.event.PlayerLeveledUp
import com.idlerpg.game.domain.event.PlayerDefeated
import com.idlerpg.game.domain.event.MasteryIncreased
import com.idlerpg.game.domain.event.QuestCompleted
import com.idlerpg.game.domain.event.QuestRewardClaimed
import com.idlerpg.game.domain.event.AchievementCompleted
import com.idlerpg.game.domain.event.AchievementRewardClaimed
import com.idlerpg.game.domain.event.ChronicleCollapsed
import com.idlerpg.game.domain.event.ChroniclePreviewPrepared
import com.idlerpg.game.domain.event.DiscoveryUnlocked
import com.idlerpg.game.domain.event.EchoGranted
import com.idlerpg.game.domain.event.EchoOfferPurchased
import com.idlerpg.game.domain.event.NewChronicleStarted
import com.idlerpg.game.domain.event.RebirthAllocationsReset
import com.idlerpg.game.domain.event.RebirthPerformed
import com.idlerpg.game.domain.event.RebirthPointsAllocated
import com.idlerpg.game.domain.event.RegionSelected
import com.idlerpg.game.domain.event.EncounterStarted
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EncounterFailed
import com.idlerpg.game.domain.event.EncounterRetreated
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.BossPhaseChanged
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillCastQueueConsumed
import com.idlerpg.game.domain.event.SkillCastQueueDeferred
import com.idlerpg.game.domain.event.SkillCastQueueReplaced
import com.idlerpg.game.domain.event.SkillCastQueued
import com.idlerpg.game.domain.event.SkillEquipped
import com.idlerpg.game.domain.event.SkillLoadoutMoved
import com.idlerpg.game.domain.event.SkillEvolutionSelected
import com.idlerpg.game.domain.event.SkillUnequipped
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.event.StatusApplied
import com.idlerpg.game.domain.event.UpgradePurchased
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.BattleFeedbackKind
import com.idlerpg.game.presentation.model.BattleFeedbackUiState
import com.idlerpg.game.presentation.model.BattleKillRewardUiState
import com.idlerpg.game.presentation.model.BattleRewardItemUiState
import com.idlerpg.game.presentation.model.BattleRewardUiState
import com.idlerpg.game.presentation.model.ChroniclePreviewUiState
import com.idlerpg.game.presentation.model.DoctrineFeedbackKind
import com.idlerpg.game.presentation.model.DoctrineFeedbackUiState
import com.idlerpg.game.presentation.model.GearFeedbackKind
import com.idlerpg.game.presentation.model.GearFeedbackUiState
import com.idlerpg.game.presentation.model.ProgressFeedbackKind
import com.idlerpg.game.presentation.model.ProgressFeedbackUiState
import com.idlerpg.game.presentation.model.SkillLoadoutFeedbackKind
import com.idlerpg.game.presentation.model.SkillLoadoutFeedbackUiState
import com.idlerpg.game.presentation.model.WorldFeedbackKind
import com.idlerpg.game.presentation.model.WorldFeedbackUiState
import com.idlerpg.game.presentation.runtime.RuntimeTransition

/**
 * Converts ordered completed backend facts into one bounded Battle feedback item.
 * Durable truth still comes from the next GameState projection.
 */
class GameEventPresenter(
    private val presentationContentRegistry: PresentationContentRegistry =
        PresentationContentRegistry.default()
) {
    fun presentBattle(transition: RuntimeTransition): BattleFeedbackUiState? {
        val message = presentBattleMessage(transition) ?: return null
        val defeatedEnemyInstanceIds = transition.events
            .asSequence()
            .mapNotNull { (it.event as? EnemyKilled)?.enemyInstanceId }
            .distinct()
            .toList()
        val reward = projectBattleReward(transition)
        val impacts = transition.events.asReversed().asSequence().mapNotNull { envelope ->
            val damage = envelope.event as? DamageDealt ?: return@mapNotNull null
            com.idlerpg.game.presentation.model.BattleImpactUiState(
                envelope.sequenceNumber, damage.targetInstanceId,
                GameNumberFormatter.full(damage.amount), damage.critical
            )
        }.take(12).toList().asReversed()
        return message.copy(
            impacts = impacts,
            reward = reward,
            killReward = defeatedEnemyInstanceIds
                .takeIf { it.isNotEmpty() }
                ?.let { ids -> BattleKillRewardUiState(ids, reward) }
        )
    }

    /** Aggregates only reward events from this one canonical transition. */
    private fun projectBattleReward(transition: RuntimeTransition): BattleRewardUiState? {
        var gold = com.idlerpg.game.core.number.GameNumber.ZERO
        var experience = com.idlerpg.game.core.number.GameNumber.ZERO
        val items = linkedMapOf<com.idlerpg.game.core.id.InstanceId, BattleRewardItemUiState>()

        transition.events.forEach { envelope ->
            when (val event = envelope.event) {
                is CurrencyGranted -> if (event.currencyId == CurrencyId.GOLD) {
                    gold += event.amount
                }
                is ExperienceGranted -> experience += event.amount
                is ItemDropped -> {
                    items[event.itemInstanceId] = BattleRewardItemUiState(
                        itemInstanceId = event.itemInstanceId,
                        itemDefinitionId = event.itemDefinitionId,
                        titleStringKey = presentationContentRegistry
                            .entryOrNull(event.itemDefinitionId)?.titleStringKey,
                        rarityTitleStringKey = presentationContentRegistry
                            .entryOrNull(event.rarity.id)?.titleStringKey,
                        sentToOverflow = false,
                        autoSalvaged = false
                    )
                }
                is ItemSentToOverflow -> {
                    val prior = items[event.itemInstanceId]
                    if (prior != null) {
                        items[event.itemInstanceId] = prior.copy(sentToOverflow = true)
                    }
                }
                is ItemAutoSalvaged -> {
                    val prior = items[event.itemInstanceId]
                    if (prior != null) {
                        items[event.itemInstanceId] = prior.copy(autoSalvaged = true)
                    }
                }
                else -> Unit
            }
        }

        val reward = BattleRewardUiState(
            goldDisplay = gold.takeIf { it > com.idlerpg.game.core.number.GameNumber.ZERO }
                ?.let(GameNumberFormatter::full),
            experienceDisplay = experience
                .takeIf { it > com.idlerpg.game.core.number.GameNumber.ZERO }
                ?.let(GameNumberFormatter::full),
            items = items.values.toList()
        )
        return reward.takeUnless(BattleRewardUiState::isEmpty)
    }

    private fun presentBattleMessage(transition: RuntimeTransition): BattleFeedbackUiState? {
        val rejection = transition.commandResult as? CommandResult.Rejected
        if (rejection != null) {
            return BattleFeedbackUiState(
                sequenceNumber = transition.events.lastOrNull()?.sequenceNumber,
                kind = BattleFeedbackKind.COMMAND_REJECTED,
                contentId = rejection.reason.subjectContentId,
                rejectionCode = rejection.reason.code
            )
        }

        // Boss phases outrank action acknowledgements, but a later terminal event must win.
        for (envelope in transition.events.asReversed()) {
            when (val event = envelope.event) {
                is PlayerDefeated -> return BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.PLAYER_DEFEATED
                )
                is CombatEnded -> return BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.COMBAT_ENDED
                )
                is BossPhaseChanged -> return BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.BOSS_PHASE,
                    contentId = event.bossId,
                    bossPhase = event.phase,
                    bossTotalPhases = event.totalPhases
                )
                else -> Unit
            }
        }

        // Manual queue acknowledgement has presentation priority over later damage/loot
        // facts in the same atomic transition. Durable state still comes from GameState.
        for (envelope in transition.events.asReversed()) {
            val queueFeedback = when (val event = envelope.event) {
                is SkillCastQueued -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.SKILL_QUEUED,
                    contentId = event.skillId
                )
                is SkillCastQueueReplaced -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.SKILL_QUEUE_REPLACED,
                    contentId = event.newSkillId
                )
                is SkillCastQueueDeferred -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.SKILL_QUEUE_DEFERRED,
                    contentId = event.skillId,
                    rejectionCode = event.reasonCode
                )
                is SkillCastQueueConsumed -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.SKILL_QUEUE_CONSUMED,
                    contentId = event.skillId
                )
                is SkillCastQueueCleared -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.SKILL_QUEUE_CLEARED,
                    contentId = event.skillId
                )
                else -> null
            }
            if (queueFeedback != null) {
                return queueFeedback
            }
        }

        for (envelope in transition.events.asReversed()) {
            val projected = when (val event = envelope.event) {
                is SkillUsed -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.SKILL_USED,
                    contentId = event.skillId
                )
                is DamageDealt -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.DAMAGE,
                    amountDisplay = GameNumberFormatter.compact(event.amount)
                )
                is StatusApplied -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.STATUS_APPLIED,
                    contentId = event.statusDefinitionId
                )
                is HealingApplied -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.HEALING,
                    amountDisplay = GameNumberFormatter.compact(event.amount)
                )
                is EnemyKilled -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.ENEMY_DEFEATED,
                    contentId = event.enemyDefinitionId
                )
                is ConvergenceTriggered -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.CONVERGENCE,
                    contentId = event.convergenceId
                )
                is ItemDropped -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.LOOT,
                    contentId = event.itemDefinitionId
                )
                is ItemSentToOverflow -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.LOOT,
                    contentId = event.itemDefinitionId
                )
                is PlayerLeveledUp -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.LEVEL_UP,
                    amountDisplay = event.newLevel.toString()
                )
                is UpgradePurchased -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.UPGRADE_PURCHASED,
                    contentId = event.upgradeId,
                    upgradePreviousLevel = event.newLevel - event.quantity,
                    upgradeNewLevel = event.newLevel,
                    upgradeCostDisplay = event.totalCost
                        .takeIf { it != GameNumber.ZERO }
                        ?.let(GameNumberFormatter::compact)
                )
                is PlayerDefeated -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.PLAYER_DEFEATED
                )
                is CombatEnded -> BattleFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = BattleFeedbackKind.COMBAT_ENDED
                )
                else -> null
            }
            if (projected != null) {
                return projected
            }
        }
        return null
    }
    /**
     * Presents only FUI-04 loadout command acknowledgement while the editor is open.
     *
     * Foreground simulation transitions carry no loadout event and therefore produce no
     * feedback here. Durable equipped order always comes from the canonical next snapshot.
     */
    fun presentSkillLoadout(
        transition: RuntimeTransition
    ): SkillLoadoutFeedbackUiState? {
        val rejection = transition.commandResult as? CommandResult.Rejected
        if (rejection != null) {
            return SkillLoadoutFeedbackUiState(
                sequenceNumber = transition.events.lastOrNull()?.sequenceNumber,
                kind = SkillLoadoutFeedbackKind.COMMAND_REJECTED,
                skillId = rejection.reason.subjectContentId,
                rejectionCode = rejection.reason.code
            )
        }

        for (envelope in transition.events.asReversed()) {
            val projected = when (val event = envelope.event) {
                is SkillEquipped -> SkillLoadoutFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = SkillLoadoutFeedbackKind.EQUIPPED,
                    skillId = event.skillId,
                    toIndex = event.index
                )
                is SkillUnequipped -> SkillLoadoutFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = SkillLoadoutFeedbackKind.UNEQUIPPED,
                    skillId = event.skillId
                )
                is SkillLoadoutMoved -> SkillLoadoutFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = SkillLoadoutFeedbackKind.MOVED,
                    skillId = event.skillId,
                    fromIndex = event.fromIndex,
                    toIndex = event.toIndex
                )
                is SkillEvolutionSelected -> SkillLoadoutFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = SkillLoadoutFeedbackKind.EVOLVED,
                    skillId = event.skillId
                )
                else -> null
            }
            if (projected != null) {
                return projected
            }
        }

        return null
    }

    /** Presents bounded FUI-05 World/Adaptation acknowledgement while World is visible. */
    fun presentWorld(transition: RuntimeTransition): WorldFeedbackUiState? {
        val rejection = transition.commandResult as? CommandResult.Rejected
        if (rejection != null) {
            return WorldFeedbackUiState(
                sequenceNumber = transition.events.lastOrNull()?.sequenceNumber,
                kind = WorldFeedbackKind.COMMAND_REJECTED,
                contentId = rejection.reason.subjectContentId,
                rejectionCode = rejection.reason.code
            )
        }

        for (envelope in transition.events.asReversed()) {
            val projected = when (val event = envelope.event) {
                is RegionSelected -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.REGION_SELECTED,
                    contentId = event.regionId
                )
                is EncounterStarted -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.ENCOUNTER_STARTED,
                    contentId = event.encounterDefinitionId
                )
                is EncounterCleared -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.ENCOUNTER_CLEARED,
                    contentId = event.encounterDefinitionId
                )
                is EncounterRetreated -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.ENCOUNTER_RETREATED,
                    contentId = event.encounterDefinitionId
                )
                is EncounterFailed -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.ENCOUNTER_FAILED,
                    contentId = event.encounterDefinitionId
                )
                is AdaptationTierChanged -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.ADAPTATION_TIER_CHANGED,
                    contentId = event.regionId,
                    affinityId = event.affinityId,
                    tier = event.newTier
                )
                is MutationRolled -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.MUTATION_ROLLED,
                    contentId = event.mutationId
                )
                is InventoryProgressionBlocked -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.INVENTORY_BLOCKED
                )
                is InventoryProgressionUnblocked -> WorldFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = WorldFeedbackKind.INVENTORY_UNBLOCKED
                )
                else -> null
            }
            if (projected != null) {
                return projected
            }
        }
        return null
    }

    /** Presents bounded FUI-06 Doctrine/Resonance acknowledgement while Doctrine is visible. */
    fun presentDoctrine(transition: RuntimeTransition): DoctrineFeedbackUiState? {
        val rejection = transition.commandResult as? CommandResult.Rejected
        if (rejection != null) {
            return DoctrineFeedbackUiState(
                sequenceNumber = transition.events.lastOrNull()?.sequenceNumber,
                kind = DoctrineFeedbackKind.COMMAND_REJECTED,
                contentId = rejection.reason.subjectContentId,
                rejectionCode = rejection.reason.code
            )
        }

        for (envelope in transition.events.asReversed()) {
            val projected = when (val event = envelope.event) {
                is DoctrineUpdated -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.DOCTRINE_UPDATED
                )
                is DoctrineRuleSelected -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.RULE_SELECTED,
                    ruleId = event.ruleId,
                    contentId = event.actionId, explanation = event.explanation
                )
                is DoctrineActionRejected -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.ACTION_REJECTED,
                    ruleId = event.ruleId,
                    contentId = event.actionId,
                    rejectionCode = event.reason.code
                )
                is DoctrineFallbackUsed -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.FALLBACK_USED,
                    contentId = event.actionId, explanation = event.explanation
                )
                is ResonanceGenerated -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.RESONANCE_GENERATED,
                    affinityId = event.affinityId,
                    amountDisplay = GameNumberFormatter.compact(event.amount)
                )
                is ResonanceConsumed -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.RESONANCE_CONSUMED,
                    contentId = event.convergenceId,
                    affinityId = event.affinityId,
                    amountDisplay = GameNumberFormatter.compact(event.amount)
                )
                is ConvergenceTriggered -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.CONVERGENCE_TRIGGERED,
                    contentId = event.convergenceId
                )
                is ConvergenceDiscovered -> DoctrineFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = DoctrineFeedbackKind.CONVERGENCE_DISCOVERED,
                    contentId = event.convergenceId
                )
                else -> null
            }
            if (projected != null) {
                return projected
            }
        }
        return null
    }


    /** Presents bounded FUI-07 inventory/equipment/capacity acknowledgement while Gear is visible. */
    fun presentGear(transition: RuntimeTransition): GearFeedbackUiState? {
        val rejection = transition.commandResult as? CommandResult.Rejected
        if (rejection != null) {
            return GearFeedbackUiState(
                sequenceNumber = transition.events.lastOrNull()?.sequenceNumber,
                kind = GearFeedbackKind.COMMAND_REJECTED,
                itemInstanceId = rejection.reason.subjectInstanceId,
                itemDefinitionId = rejection.reason.subjectContentId,
                rejectionCode = rejection.reason.code
            )
        }

        for (envelope in transition.events.asReversed()) {
            val projected = when (val event = envelope.event) {
                is ItemEquipped -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.EQUIPPED,
                    itemInstanceId = event.itemInstanceId,
                    slot = event.slot
                )
                is ItemUnequipped -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.UNEQUIPPED,
                    itemInstanceId = event.itemInstanceId,
                    slot = event.slot
                )
                is ItemLocked -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.LOCKED,
                    itemInstanceId = event.itemInstanceId
                )
                is ItemUnlocked -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.UNLOCKED,
                    itemInstanceId = event.itemInstanceId
                )
                is ItemSalvaged -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.SALVAGED,
                    itemInstanceId = event.itemInstanceId,
                    itemDefinitionId = event.itemDefinitionId,
                    amountDisplay = GameNumberFormatter.compact(event.goldGranted)
                )
                is InventoryCapacityExpanded -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.CAPACITY_EXPANDED,
                    amountDisplay = GameNumberFormatter.compact(event.totalGoldCost),
                    previousCapacity = event.previousCapacity,
                    newCapacity = event.newCapacity
                )
                is OverflowItemClaimed -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.OVERFLOW_CLAIMED,
                    itemInstanceId = event.itemInstanceId,
                    itemDefinitionId = event.itemDefinitionId
                )
                is OverflowItemSalvaged -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.OVERFLOW_SALVAGED,
                    itemInstanceId = event.itemInstanceId,
                    itemDefinitionId = event.itemDefinitionId,
                    amountDisplay = GameNumberFormatter.compact(event.goldGranted)
                )
                is GearEnhancementAttempted -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.ENHANCED,
                    itemInstanceId = event.itemInstanceId,
                    amountDisplay = event.resultingEnhancementLevel.toString(),
                    enhancementSucceeded = event.success
                )
                is GearRefined -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.REFINED,
                    itemInstanceId = event.itemInstanceId,
                    amountDisplay = event.resultingValue.toString()
                )
                is ItemSentToOverflow -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.ITEM_TO_OVERFLOW,
                    itemInstanceId = event.itemInstanceId,
                    itemDefinitionId = event.itemDefinitionId
                )
                is InventoryProgressionBlocked -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.PROGRESSION_BLOCKED
                )
                is InventoryProgressionUnblocked -> GearFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = GearFeedbackKind.PROGRESSION_UNBLOCKED,
                    availableStorageSlots = event.availableStorageSlots
                )
                else -> null
            }
            if (projected != null) return projected
        }
        return null
    }

    /** Extracts the exact canonical Chronicle preview event for presentation memory. */
    fun presentChroniclePreview(transition: RuntimeTransition): ChroniclePreviewUiState? {
        for (envelope in transition.events.asReversed()) {
            val event = envelope.event as? ChroniclePreviewPrepared ?: continue
            return ChroniclePreviewUiState(
                previewToken = event.previewToken,
                chronicleNumber = event.chronicleNumber,
                resetRuleVersion = event.resetRuleVersion,
                currentNormalClearsDisplay = GameNumberFormatter.full(
                    event.currentTotalNormalClears
                ),
                requiredNormalClearsDisplay = GameNumberFormatter.full(
                    event.requiredTotalNormalClears
                ),
                echoRewardDisplay = GameNumberFormatter.full(event.echoReward),
                resetScopes = event.resetScopes,
                persistScopes = event.persistScopes
            )
        }
        return null
    }

    /** Presents bounded FUI-09 Progress acknowledgement while Progress is visible. */
    fun presentProgress(transition: RuntimeTransition): ProgressFeedbackUiState? {
        val rejection = transition.commandResult as? CommandResult.Rejected
        if (rejection != null) {
            return ProgressFeedbackUiState(
                sequenceNumber = transition.events.lastOrNull()?.sequenceNumber,
                kind = ProgressFeedbackKind.COMMAND_REJECTED,
                contentId = rejection.reason.subjectContentId,
                rejectionCode = rejection.reason.code
            )
        }

        val containsEchoPurchase = transition.events.any { it.event is EchoOfferPurchased }
        val containsChronicleCollapse = transition.events.any { it.event is ChronicleCollapsed }

        for (envelope in transition.events.asReversed()) {
            val projected = when (val event = envelope.event) {
                is QuestRewardClaimed -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.QUEST_REWARD_CLAIMED,
                    contentId = event.questId
                )
                is AchievementRewardClaimed -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.ACHIEVEMENT_REWARD_CLAIMED,
                    contentId = event.achievementId
                )
                is EchoOfferPurchased -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.ECHO_OFFER_PURCHASED,
                    contentId = event.offerId,
                    amountDisplay = GameNumberFormatter.compact(event.cost)
                )
                is DiscoveryUnlocked -> if (containsEchoPurchase) {
                    null
                } else {
                    ProgressFeedbackUiState(
                        sequenceNumber = envelope.sequenceNumber,
                        kind = ProgressFeedbackKind.DISCOVERY_UNLOCKED,
                        contentId = event.discoveryId
                    )
                }
                is EchoGranted -> if (containsChronicleCollapse) {
                    null
                } else {
                    ProgressFeedbackUiState(
                        sequenceNumber = envelope.sequenceNumber,
                        kind = ProgressFeedbackKind.ECHO_GRANTED,
                        contentId = event.sourceId,
                        amountDisplay = GameNumberFormatter.compact(event.amount)
                    )
                }
                is ChroniclePreviewPrepared -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.CHRONICLE_PREVIEW_READY,
                    amountDisplay = GameNumberFormatter.compact(event.echoReward)
                )
                is ChronicleCollapsed -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.CHRONICLE_COLLAPSED,
                    amountDisplay = event.nextChronicleNumber.toString()
                )
                is NewChronicleStarted -> if (containsChronicleCollapse) {
                    null
                } else {
                    ProgressFeedbackUiState(
                        sequenceNumber = envelope.sequenceNumber,
                        kind = ProgressFeedbackKind.CHRONICLE_COLLAPSED,
                        amountDisplay = event.chronicleNumber.toString()
                    )
                }
                is QuestCompleted -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.QUEST_COMPLETED,
                    contentId = event.questId
                )
                is AchievementCompleted -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.ACHIEVEMENT_COMPLETED,
                    contentId = event.achievementId
                )
                is PlayerLeveledUp -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.PLAYER_LEVELED_UP,
                    amountDisplay = event.newLevel.toString()
                )
                is MasteryIncreased -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.MASTERY_INCREASED,
                    affinityId = event.affinityId,
                    amountDisplay = GameNumberFormatter.compact(event.amount)
                )
                is RebirthPerformed -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.REBIRTH_PERFORMED,
                    amountDisplay = event.rebirthNumber.toString()
                )
                is RebirthPointsAllocated -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.REBIRTH_POINTS_ALLOCATED,
                    amountDisplay = event.amount.toString()
                )
                is RebirthAllocationsReset -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.REBIRTH_RESPEC,
                    amountDisplay = GameNumberFormatter.compact(event.gemCost)
                )
                is UpgradePurchased -> ProgressFeedbackUiState(
                    sequenceNumber = envelope.sequenceNumber,
                    kind = ProgressFeedbackKind.CORE_GROWTH_PURCHASED,
                    contentId = event.upgradeId,
                    amountDisplay = event.quantity.toString()
                )
                else -> null
            }
            if (projected != null) return projected
        }
        return null
    }

}
