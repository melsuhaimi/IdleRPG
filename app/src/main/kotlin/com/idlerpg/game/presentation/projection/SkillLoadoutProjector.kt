package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.model.GameState
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
            }
        )
    }

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
