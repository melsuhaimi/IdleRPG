package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.adaptation.RegionAdaptationState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.WorldAffinityAdaptationUiState
import com.idlerpg.game.presentation.model.WorldEncounterStatusUi
import com.idlerpg.game.presentation.model.WorldEncounterTypeUi
import com.idlerpg.game.presentation.model.WorldEncounterUiState
import com.idlerpg.game.presentation.model.WorldFeedbackUiState
import com.idlerpg.game.presentation.model.WorldInventoryBlockUiState
import com.idlerpg.game.presentation.model.WorldMutationUiState
import com.idlerpg.game.presentation.model.WorldMutationEffectUi
import com.idlerpg.game.presentation.model.WorldMutationEffectKindUi
import com.idlerpg.game.domain.definition.adaptation.MutationEffectDefinition
import com.idlerpg.game.presentation.model.WorldRegionUiState
import com.idlerpg.game.presentation.model.WorldUiState
import com.idlerpg.game.presentation.query.GameReadQueries
import com.idlerpg.game.presentation.query.WorldReadQueries

/** Canonical World/Adaptation state -> FUI-05 immutable presentation projection. */
class WorldProjector(
    private val contentRegistry: ContentRegistry,
    private val presentationContentRegistry: PresentationContentRegistry,
    private val readQueries: GameReadQueries,
    private val worldReadQueries: WorldReadQueries
) {
    fun project(
        state: GameState,
        feedback: WorldFeedbackUiState? = null
    ): WorldUiState {
        val world = state.run.world
        val inventory = state.run.inventory
        val inventoryBlocked = readQueries.inventoryProgressionBlocked(state)
        val activeMutationIds = state.run.combat.enemies
            .flatMap { enemy -> enemy.activeMutations.map { it.mutationId } }
            .toSet()
        val forecastUnlocked = ADAPTATION_FORECAST_DISCOVERY_ID in
            state.meta.discoveries.unlockedHiddenContentIds
        val activeRewardMultiplier = worldReadQueries.activeEnemyRewardMultiplier(state)

        val regions = contentRegistry.allRegions().map { region ->
            val metadata = presentationContentRegistry.entry(region.id)
            val regionState = state.run.adaptation.regionStateById[region.id]
                ?: RegionAdaptationState()
            val selected = world.activeRegionId == region.id
            val unlocked = region.id in world.unlockedRegionIds ||
                region.id in contentRegistry.world.startingRegionIds
            val progress = world.regionProgressById[region.id]

            val encounters = region.encounterIds.mapIndexed { ordinal, encounterId ->
                val stageNumber = ordinal + 1L
                val scalingTier = EnemyScalingSystem.scalingTierForEncounter(stageNumber)
                val definition = contentRegistry.encounter(encounterId)
                val encounterMetadata = presentationContentRegistry.entry(encounterId)
                val runtimeEncounter = world.currentEncounter
                    ?.takeIf { it.definitionId == encounterId }
                val status = runtimeEncounter?.status?.toUiStatus()
                    ?: WorldEncounterStatusUi.IDLE
                val active = status == WorldEncounterStatusUi.ACTIVE
                val enemyDefinition = contentRegistry.enemy(definition.enemyDefinitionIdsForWave(1).first())
                val enemyMetadata = presentationContentRegistry.entry(enemyDefinition.id)
                val attack = contentRegistry.enemyAttack(enemyDefinition.attackDefinitionId!!)
                val scaledAttackDamage = EnemyScalingSystem.scaledAttack(
                    enemyDefinition,
                    attack,
                    scalingTier
                )
                val scaledAttackInterval = EnemyScalingSystem.scaledAttackInterval(
                    enemyDefinition,
                    attack,
                    scalingTier
                )
                var expectedGold = GameNumber.ZERO
                var expectedExperience = GameNumber.ZERO
                for (wave in 1..definition.waves) {
                    definition.enemyDefinitionIdsForWave(wave).forEach { enemyId ->
                        val waveEnemy = contentRegistry.enemy(enemyId)
                        expectedGold += EnemyScalingSystem.scaledGoldReward(
                            waveEnemy,
                            scalingTier
                        )
                        expectedExperience += EnemyScalingSystem.scaledExperienceReward(
                            waveEnemy,
                            scalingTier
                        )
                    }
                }
                expectedGold = GameMath.applyRatio(expectedGold, definition.rewardMultiplier)
                expectedExperience = GameMath.applyRatio(expectedExperience, definition.rewardMultiplier)
                val current = world.currentEncounter
                val expectedStartId = when (current?.status) {
                    null -> region.encounterIds.firstOrNull()
                    EncounterStatus.FAILED, EncounterStatus.RETREATED -> current.definitionId
                    EncounterStatus.CLEARED -> contentRegistry.encounter(current.definitionId).nextEncounterId
                    EncounterStatus.ACTIVE -> null
                }
                val affinityMetadata = presentationContentRegistry.entry(attack.affinity.id)
                WorldEncounterUiState(
                    encounterId = encounterId,
                    titleStringKey = encounterMetadata.titleStringKey,
                    iconAssetKey = encounterMetadata.iconAssetKey,
                    type = definition.type.toUiType(),
                    status = status,
                    encounterIndex = runtimeEncounter?.encounterIndex,
                    enemyTitleStringKey = enemyMetadata.titleStringKey,
                    enemyIllustrationAssetKey = enemyMetadata.illustrationAssetKey,
                    encounterFrameAssetKey = when (definition.type) {
                        EncounterType.ELITE -> PresentationAssetKey.ENCOUNTER_FRAME_ELITE
                        EncounterType.ANOMALY -> PresentationAssetKey.ENCOUNTER_FRAME_ANOMALY
                        else -> PresentationAssetKey.ENCOUNTER_FRAME_NORMAL
                    },
                    attackTitle = attack.displayName,
                    attackAffinityStringKey = affinityMetadata.titleStringKey,
                    attackDamageDisplay = GameNumberFormatter.full(scaledAttackDamage),
                    attackIntervalMillis = scaledAttackInterval.millis,
                    armorPenetrationDisplay = GameNumberFormatter.full(attack.armorPenetration),
                    selectedRegion = selected,
                    canStart = selected && !active && !inventoryBlocked &&
                        (expectedStartId == encounterId ||
                            (world.automationMode == com.idlerpg.game.domain.model.world.WorldAutomationMode.FARM &&
                                world.selectedFarmEncounterId == encounterId &&
                                encounterId in world.clearedEncounterIds)),
                    canRetreat = selected && active,
                    cleared = encounterId in world.clearedEncounterIds,
                    // The original three visual sectors remain stable; long-run stages
                    // continue inside the Warden Core route instead of creating empty headers.
                    sectorIndex = (ordinal / 10 + 1).coerceAtMost(3),
                    sectorTitleStringKey = when (ordinal / 10) {
                        0 -> PresentationStringKey.OUTER_FRACTURE
                        1 -> PresentationStringKey.RESONANT_DEPTHS
                        else -> PresentationStringKey.WARDEN_CORE
                    },
                    sectorBackgroundAssetKey = when (ordinal / 10) {
                        0 -> PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BANNER
                        1 -> PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BANNER
                        else -> PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BANNER
                    },
                    rewardFocus = when (definition.type) {
                        EncounterType.NORMAL -> "Gold · XP · Skill Mastery"
                        EncounterType.ELITE -> "High gear chance · elite rewards"
                        EncounterType.ANOMALY -> "Special loot · resistance gain"
                        EncounterType.BOSS -> "Warden reward focus"
                    },
                    stageLabel = definition.displayName,
                    waveCount = definition.waves,
                    difficultyDisplay = "TIER ${scalingTier + 1L}",
                    expectedGoldDisplay = GameNumberFormatter.full(expectedGold),
                    expectedExperienceDisplay = GameNumberFormatter.full(expectedExperience),
                    rewardMultiplierDisplay = formatRatioPercent(definition.rewardMultiplier)
                )
            }

            val adaptation = Affinity.values().map { affinity ->
                val exposure = regionState.exposureByAffinityId[affinity.id]
                val tier = regionState.tierByAffinityId[affinity.id] ?: 0
                val nextThreshold = worldReadQueries.nextAdaptationThreshold(region.id, tier)
                val affinityMetadata = presentationContentRegistry.entry(affinity.id)
                WorldAffinityAdaptationUiState(
                    affinityId = affinity.id,
                    titleStringKey = affinityMetadata.titleStringKey,
                    iconAssetKey = affinityMetadata.iconAssetKey,
                    pressureDisplay = GameNumberFormatter.full(
                        exposure?.pressure ?: GameNumber.ZERO
                    ),
                    tier = tier,
                    nextTier = nextThreshold?.let { tier + 1 },
                    nextThresholdDisplay = nextThreshold?.let(GameNumberFormatter::full),
                    currentEncounterContributionDisplay = GameNumberFormatter.full(
                        exposure?.currentEncounterContribution ?: GameNumber.ZERO
                    ),
                    recentEncounterContributionDisplay = GameNumberFormatter.full(
                        exposure?.recentEncounterContribution ?: GameNumber.ZERO
                    ),
                    hasPressure = (exposure?.pressure ?: GameNumber.ZERO) > GameNumber.ZERO
                )
            }

            val mutations = region.adaptationMutationIds
                .sorted()
                .mapNotNull { mutationId ->
                    val definition = contentRegistry.mutation(mutationId)
                    val eligible = mutationId in regionState.unlockedMutationIds
                    val active = selected && mutationId in activeMutationIds
                    val visible = eligible || active || forecastUnlocked
                    if (!visible) {
                        null
                    } else {
                        val mutationMetadata = presentationContentRegistry.entry(mutationId)
                        val displayTier = if (active) state.run.combat.enemies
                            .flatMap { it.activeMutations }
                            .filter { it.mutationId == mutationId }
                            .maxOf { it.adaptationTier }
                        else regionState.tierByAffinityId[definition.triggerAffinity.id] ?: 0
                        val affinityMetadata = presentationContentRegistry.entry(
                            definition.triggerAffinity.id
                        )
                        WorldMutationUiState(
                            mutationId = mutationId,
                            titleStringKey = mutationMetadata.titleStringKey,
                            iconAssetKey = mutationMetadata.iconAssetKey,
                            triggerAffinityStringKey = affinityMetadata.titleStringKey,
                            minimumTier = definition.minimumAdaptationTier,
                            currentlyEligible = eligible,
                            activeOnCurrentEnemy = active,
                            forecastOnly = forecastUnlocked && !eligible && !active,
                            effects = definition.effects.map { effect ->
                                when (effect) {
                                    is MutationEffectDefinition.DamageTakenMultiplierForAffinity ->
                                        WorldMutationEffectUi(
                                            kind = WorldMutationEffectKindUi.AFFINITY_RESISTANCE,
                                            magnitudeDisplay = formatRatioPercent(
                                                Ratio.ofUnits(Ratio.UNITS_PER_ONE -
                                                    effect.multiplier.units * com.idlerpg.game.core.config.AdaptationCurve
                                                        .resistanceMultiplier(displayTier).units / Ratio.UNITS_PER_ONE)
                                            )
                                        )
                                    is MutationEffectDefinition.EnemyActionIntervalMultiplier ->
                                        WorldMutationEffectUi(
                                            kind = WorldMutationEffectKindUi.FASTER_CADENCE,
                                            magnitudeDisplay = formatRatioPercent(
                                                Ratio.ofUnits(Ratio.UNITS_PER_ONE - effect.multiplier.units)
                                            )
                                        )
                                    is MutationEffectDefinition.AdditionalResonanceDrain ->
                                        WorldMutationEffectUi(
                                            kind = WorldMutationEffectKindUi.EXTRA_RESONANCE_DRAIN,
                                            magnitudeDisplay = GameNumberFormatter.full(effect.amount)
                                        )
                                    is MutationEffectDefinition.PlayerHealingMultiplier ->
                                        WorldMutationEffectUi(
                                            kind = WorldMutationEffectKindUi.HEALING_SUPPRESSION,
                                            magnitudeDisplay = formatRatioPercent(
                                                Ratio.ofUnits(Ratio.UNITS_PER_ONE - effect.multiplier.units)
                                            )
                                        )
                                    is MutationEffectDefinition.EnemyDamageMultiplierWhilePlayerHasAffinityStatus ->
                                        WorldMutationEffectUi(
                                            kind = WorldMutationEffectKindUi.GUARD_PRESSURE,
                                            magnitudeDisplay = formatRatioPercent(
                                                Ratio.ofUnits(effect.multiplier.units - Ratio.UNITS_PER_ONE)
                                            )
                                        )
                                }
                            }
                        )
                    }
                }
            val hiddenMutationCount = region.adaptationMutationIds.count { mutationId ->
                mutationId !in regionState.unlockedMutationIds &&
                    mutationId !in activeMutationIds &&
                    !forecastUnlocked
            }

            WorldRegionUiState(
                regionId = region.id,
                titleStringKey = metadata.titleStringKey,
                descriptionStringKey = metadata.shortDescriptionStringKey,
                iconAssetKey = metadata.iconAssetKey,
                illustrationAssetKey = metadata.illustrationAssetKey,
                unlocked = unlocked,
                selected = selected,
                highestClearedEncounterTier = progress?.highestClearedEncounterTier ?: 0L,
                normalClearsDisplay = GameNumberFormatter.full(
                    progress?.normalClears ?: GameNumber.ZERO
                ),
                eliteClearsDisplay = GameNumberFormatter.full(
                    progress?.eliteClears ?: GameNumber.ZERO
                ),
                encounters = encounters,
                adaptation = adaptation,
                mutations = mutations,
                hiddenMutationCount = hiddenMutationCount,
                adaptationForecastUnlocked = forecastUnlocked,
                activeEnemyRewardMultiplierDisplay = if (selected) {
                    formatRatioPercent(activeRewardMultiplier)
                } else {
                    formatRatioPercent(Ratio.ONE)
                }
            )
        }

        return WorldUiState(
            regions = regions,
            activeRegionId = world.activeRegionId,
            inventory = WorldInventoryBlockUiState(
                blocked = inventoryBlocked,
                normalUsed = inventory.itemsById.size.toLong(),
                normalCapacity = readQueries.effectiveInventoryCapacity(state),
                overflowUsed = inventory.overflowItemsById.size.toLong(),
                overflowCapacity = readQueries.inventoryOverflowCapacity()
            ),
            automationMode = world.automationMode,
            selectedFarmEncounterId = world.selectedFarmEncounterId,
            pushFailurePolicy = world.pushFailurePolicy,
            feedback = feedback
        )
    }

    private fun EncounterType.toUiType(): WorldEncounterTypeUi = when (this) {
        EncounterType.NORMAL -> WorldEncounterTypeUi.NORMAL
        EncounterType.ELITE -> WorldEncounterTypeUi.ELITE
        EncounterType.ANOMALY -> WorldEncounterTypeUi.ANOMALY
        EncounterType.BOSS -> WorldEncounterTypeUi.BOSS
    }

    private fun EncounterStatus.toUiStatus(): WorldEncounterStatusUi = when (this) {
        EncounterStatus.ACTIVE -> WorldEncounterStatusUi.ACTIVE
        EncounterStatus.CLEARED -> WorldEncounterStatusUi.CLEARED
        EncounterStatus.FAILED -> WorldEncounterStatusUi.FAILED
        EncounterStatus.RETREATED -> WorldEncounterStatusUi.RETREATED
    }

    private fun formatRatioPercent(ratio: Ratio): String {
        val whole = ratio.units / 100L
        val fractional = ratio.units % 100L
        return if (fractional == 0L) {
            "$whole%"
        } else {
            "$whole.${fractional.toString().padStart(2, '0').trimEnd('0')}%"
        }
    }

    companion object {
        private val ADAPTATION_FORECAST_DISCOVERY_ID =
            ContentId("discovery.adaptation_forecast")
    }
}
