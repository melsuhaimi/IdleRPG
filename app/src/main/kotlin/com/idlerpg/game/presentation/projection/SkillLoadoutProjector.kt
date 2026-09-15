package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.skill.SkillProgressionSystem
import com.idlerpg.game.domain.system.skill.SkillScalingSystem
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.model.SkillLoadoutFeedbackUiState
import com.idlerpg.game.presentation.model.SkillLoadoutMasteryRequirementUiState
import com.idlerpg.game.presentation.model.SkillLoadoutSkillUiState
import com.idlerpg.game.presentation.model.SkillLoadoutSlotUiState
import com.idlerpg.game.presentation.model.SkillLoadoutUiState
import com.idlerpg.game.presentation.model.SkillLoadoutUnlockUiState
import com.idlerpg.game.presentation.model.SkillEvolutionBranchUiState
import com.idlerpg.game.presentation.query.GameReadQueries

/** Pure canonical-state/content -> FUI-04 skill-loadout projection. */
class SkillLoadoutProjector(
    private val contentRegistry: ContentRegistry,
    private val presentationContentRegistry: PresentationContentRegistry,
    private val readQueries: GameReadQueries
) {
    fun project(
        state: GameState,
        feedback: SkillLoadoutFeedbackUiState? = null
    ): SkillLoadoutUiState {
        val capacity = readQueries.skillLoadoutCapacity()
        val equippedIds = state.run.player.equippedSkillIds
        val equippedCount = equippedIds.size
        val queuedSkillId = (state.run.combat.queuedPlayerAction as? QueuedPlayerAction.Skill)
            ?.skillId

        val skillStatesById = contentRegistry.allSkills()
            .associate { definition ->
                definition.id to projectSkill(
                    state = state,
                    definition = definition,
                    equippedIndex = equippedIds.indexOf(definition.id).takeIf { it >= 0 },
                    equippedCount = equippedCount,
                    capacity = capacity,
                    queuedSkillId = queuedSkillId
                )
            }

        val slots = (0 until capacity).map { index ->
            val equippedId = equippedIds.getOrNull(index)
            SkillLoadoutSlotUiState(
                index = index,
                skill = equippedId?.let { id ->
                    skillStatesById[id]
                        ?: error("Equipped skill is missing from ContentRegistry: $id")
                }
            )
        }

        val available = contentRegistry.allSkills()
            .filter { definition -> definition.id !in equippedIds }
            .map { definition ->
                skillStatesById.getValue(definition.id)
            }

        return SkillLoadoutUiState(
            capacity = capacity,
            equippedCount = equippedCount,
            capacityFull = equippedCount >= capacity,
            slots = slots,
            availableSkills = available,
            feedback = feedback
        )
    }

    private fun projectSkill(
        state: GameState,
        definition: SkillDefinition,
        equippedIndex: Int?,
        equippedCount: Int,
        capacity: Int,
        queuedSkillId: ContentId?
    ): SkillLoadoutSkillUiState {
        val metadata = presentationContentRegistry.entry(definition.id)
        val unlock = projectUnlock(state, definition)
        val lastEquippedIndex = state.run.player.equippedSkillIds.lastIndex
        val rank = SkillScalingSystem.rank(state, definition)
        val mastery = SkillScalingSystem.mastery(state, definition)
        val refinement = SkillScalingSystem.refinement(state, definition)
        val gold = state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD]
            ?: com.idlerpg.game.core.number.GameNumber.ZERO
        val rankCost = SkillProgressionSystem.rankUpgradeCost(rank)
        val masteryCost = SkillProgressionSystem.masteryUpgradeCost(mastery)
        val refinementCost = SkillProgressionSystem.refinementCost(refinement)
        val maximumRank = definition.maxRank
        val unlocked = unlock.unlocked

        return SkillLoadoutSkillUiState(
            skillId = definition.id,
            titleStringKey = metadata.titleStringKey,
            descriptionStringKey = metadata.shortDescriptionStringKey,
            assetKey = metadata.iconAssetKey,
            affinityIds = definition.affinityTags
                .map { it.id }
                .sorted(),
            cooldownMillis = definition.cooldown.millis,
            recoveryMillis = definition.recovery.millis,
            equippedIndex = equippedIndex,
            queued = definition.id == queuedSkillId,
            unlock = unlock,
            canEquip = equippedIndex == null && unlock.unlocked && equippedCount < capacity,
            canUnequip = equippedIndex != null,
            canMoveEarlier = equippedIndex != null && equippedIndex > 0,
            canMoveLater = equippedIndex != null && equippedIndex < lastEquippedIndex,
            evolutions = contentRegistry.skillEvolutionsFor(definition.id).map { evolution ->
                val evolutionMetadata = presentationContentRegistry.entry(evolution.id)
                val currentLevel = readQueries.masteryLevel(state, evolution.requiredAffinity.id)
                val selected = state.run.player.selectedSkillEvolutionBySkillId[definition.id] == evolution.id
                SkillEvolutionBranchUiState(
                    evolutionId = evolution.id,
                    titleStringKey = evolutionMetadata.titleStringKey,
                    descriptionStringKey = evolutionMetadata.shortDescriptionStringKey,
                    requiredAffinityTitleStringKey = presentationContentRegistry
                        .entry(evolution.requiredAffinity.id)
                        .titleStringKey,
                    requiredMasteryLevel = evolution.requiredMasteryLevel,
                    currentMasteryLevel = currentLevel,
                    selected = selected,
                    canSelect = unlock.unlocked && currentLevel >= evolution.requiredMasteryLevel && !selected
                )
            },
            rank = rank,
            maxRank = maximumRank,
            mastery = mastery,
            masteryCap = SkillProgressionSystem.MAX_MASTERY,
            refinement = refinement,
            refinementCap = SkillProgressionSystem.MAX_REFINEMENT,
            rankUpgradeCostDisplay = com.idlerpg.game.presentation.format.GameNumberFormatter.compact(rankCost),
            masteryUpgradeCostDisplay = com.idlerpg.game.presentation.format.GameNumberFormatter.compact(masteryCost),
            refinementCostDisplay = com.idlerpg.game.presentation.format.GameNumberFormatter.compact(refinementCost),
            canUpgradeRank = unlocked &&
                (maximumRank == null || rank < maximumRank) && gold >= rankCost,
            canUpgradeMastery = unlocked &&
                mastery < SkillProgressionSystem.MAX_MASTERY && gold >= masteryCost,
            canRefine = unlocked &&
                refinement < SkillProgressionSystem.MAX_REFINEMENT && gold >= refinementCost,
            technicalDetails = technicalDetails(definition)
        )
    }

    private fun technicalDetails(definition: SkillDefinition): List<String> = buildList {
        add("Target: " + readable(definition.targetingRule.name))
        add(
            "Timing: cooldown " + definition.cooldown.millis + " ms; recovery " +
                definition.recovery.millis + " ms"
        )
        if (definition.resourceCosts.isNotEmpty()) {
            add(
                "Resources: " + definition.resourceCosts.entries
                    .sortedBy { it.key }
                    .joinToString(", ") { (id, amount) ->
                        id.value + " " + amount.toPlainString()
                    }
            )
        }
        if (definition.affinityTags.isNotEmpty()) {
            add(
                "Affinities: " + definition.affinityTags
                    .map { it.id.value }
                    .sorted()
                    .joinToString(", ")
            )
        }
        definition.effects.forEachIndexed { index, effect ->
            val prefix = "Effect " + (index + 1) + ": "
            when (effect) {
                is com.idlerpg.game.domain.definition.combat.EffectSpec.DealDamage -> {
                    add(
                        prefix + "damage " + readable(effect.damageKind.name) +
                            " · " + percentage(effect.powerRatio) + " coefficient · " +
                            readable(effect.scalingPolicy.name) +
                            " · flat +" + effect.flatBonus.toPlainString() +
                            " · " + effect.hitCount + " hit(s) · targets " +
                            readable(effect.targetPattern.name) +
                            " · critical " + if (effect.canCritical) "eligible" else "ineligible"
                    )
                    effect.conditions.forEach { condition ->
                        when (condition) {
                            is com.idlerpg.game.domain.definition.combat.EffectSpec.DamageCondition.TargetHasStatus ->
                                add(
                                    "Condition: target has " +
                                        condition.statusDefinitionId.value +
                                        " → +" + percentage(condition.bonusPowerRatio) +
                                        " power"
                                )
                            is com.idlerpg.game.domain.definition.combat.EffectSpec.DamageCondition.TargetHealthAtOrBelow ->
                                add(
                                    "Condition: target HP ≤ " +
                                        percentage(condition.threshold) + " → +" +
                                        percentage(condition.bonusPowerRatio) + " power"
                                )
                        }
                    }
                }
                is com.idlerpg.game.domain.definition.combat.EffectSpec.Heal ->
                    add(
                        prefix + "heal " + effect.flatAmount.toPlainString() +
                            " · targets " + readable(effect.targetPattern.name)
                    )
                is com.idlerpg.game.domain.definition.combat.EffectSpec.ApplyStatus ->
                    add(
                        prefix + "apply " + effect.statusDefinitionId.value +
                            " · targets " + readable(effect.targetPattern.name)
                    )
                is com.idlerpg.game.domain.definition.combat.EffectSpec.RemoveStatus ->
                    add(
                        prefix + "remove " + effect.statusDefinitionId.value +
                            " · targets " + readable(effect.targetPattern.name)
                    )
                is com.idlerpg.game.domain.definition.combat.EffectSpec.ShiftResonance ->
                    add(
                        prefix + "shift " + effect.amount.toPlainString() + " " +
                            effect.fromAffinity.id.value + " → " +
                            effect.toAffinity.id.value
                    )
            }
        }
        add(
            "Investments: rank +" + percentage(definition.powerGrowthPerPlayerLevel) +
                " power/step; mastery +" +
                percentage(Ratio.ofUnits(SkillScalingSystem.MASTERY_DAMAGE_UNITS_PER_LEVEL)) +
                " damage/level and +" +
                percentage(Ratio.ofUnits(SkillScalingSystem.MASTERY_HEALING_UNITS_PER_LEVEL)) +
                " healing/level; refinement +" +
                percentage(Ratio.ofUnits(SkillScalingSystem.REFINEMENT_DAMAGE_UNITS_PER_LEVEL)) +
                " damage/level and +" +
                percentage(Ratio.ofUnits(SkillScalingSystem.REFINEMENT_HEALING_UNITS_PER_LEVEL)) +
                " healing/level"
        )
    }

    private fun percentage(ratio: Ratio): String {
        val whole = ratio.units / 100L
        val fraction = (ratio.units % 100L).toString().padStart(2, '0')
        return whole.toString() + "." + fraction + "%"
    }

    private fun readable(value: String): String =
        value.lowercase().replace('_', ' ')

    private fun projectUnlock(
        state: GameState,
        definition: SkillDefinition
    ): SkillLoadoutUnlockUiState {
        val currentPlayerLevel = state.run.progression.playerLevel.level
        val requiredFeatureId = definition.requiredFeatureId
            ?: return SkillLoadoutUnlockUiState(
                requiredFeatureId = null,
                currentPlayerLevel = currentPlayerLevel,
                requiredPlayerLevel = null,
                masteryRequirements = emptyList(),
                unlocked = true
            )

        val feature = contentRegistry.featureUnlockOrNull(requiredFeatureId)
            ?: error("Skill ${definition.id} references missing feature unlock $requiredFeatureId")

        val masteryRequirements = feature.requiredMasteryLevels
            .entries
            .sortedBy { it.key }
            .map { (affinityId, requiredLevel) ->
                val metadata = presentationContentRegistry.entry(affinityId)
                val currentLevel = readQueries.masteryLevel(state, affinityId)
                SkillLoadoutMasteryRequirementUiState(
                    affinityId = affinityId,
                    titleStringKey = metadata.titleStringKey,
                    currentLevel = currentLevel,
                    requiredLevel = requiredLevel,
                    met = currentLevel >= requiredLevel
                )
            }

        return SkillLoadoutUnlockUiState(
            requiredFeatureId = requiredFeatureId,
            currentPlayerLevel = currentPlayerLevel,
            requiredPlayerLevel = feature.requiredPlayerLevel,
            masteryRequirements = masteryRequirements,
            unlocked = readQueries.isFeatureUnlocked(state, requiredFeatureId)
        )
    }
}
