package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.combat.StatusEffectState
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import com.idlerpg.game.domain.system.skill.SkillScalingSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.BattleCombatStatusUi
import com.idlerpg.game.presentation.model.BattleBossPhaseUi
import com.idlerpg.game.presentation.model.BattleDoctrineUiState
import com.idlerpg.game.presentation.model.DoctrineFeedbackUiState
import com.idlerpg.game.presentation.model.BattleEnemyUiState
import com.idlerpg.game.presentation.model.BattleFeedbackUiState
import com.idlerpg.game.presentation.model.BattleMutationUiState
import com.idlerpg.game.presentation.model.BattlePlayerUiState
import com.idlerpg.game.presentation.model.BattlePresentationRewardUiState
import com.idlerpg.game.presentation.model.BattleResourceCostUiState
import com.idlerpg.game.presentation.model.BattleResourceUiState
import com.idlerpg.game.presentation.model.BattleSkillReadinessUi
import com.idlerpg.game.presentation.model.BattleSkillUiState
import com.idlerpg.game.presentation.model.BattleStatusEffectUiState
import com.idlerpg.game.presentation.model.BattleStatusPolarityUi
import com.idlerpg.game.presentation.model.BattleUiState
import com.idlerpg.game.presentation.model.BattleUpgradeUiState
import com.idlerpg.game.presentation.model.ResonanceAffinityUiState
import com.idlerpg.game.presentation.query.GameReadQueries
import java.math.BigInteger

/** Pure FUI-03 canonical-state -> Battle presentation projection. */
class BattleProjector(
    private val contentRegistry: ContentRegistry,
    private val presentationContentRegistry: PresentationContentRegistry,
    private val readQueries: GameReadQueries
) {
    fun project(
        state: GameState,
        feedback: BattleFeedbackUiState? = null,
        doctrineFeedback: DoctrineFeedbackUiState? = null,
        presentationRewards: List<BattlePresentationRewardUiState> = emptyList()
    ): BattleUiState {
        val combat = state.run.combat
        val nowMillis = state.engine.simulationTime.millis
        val queuedSkillId = (combat.queuedPlayerAction as? QueuedPlayerAction.Skill)?.skillId
        val playerCombatant = combat.playerCombatant
        val playerCurrentHealth = playerCombatant?.currentHealth ?: state.run.player.currentHealth
        val playerMaximumHealth = DerivedStatSystem.maximumHealth(state, contentRegistry)
        val playerStatuses = playerCombatant?.statusEffects.orEmpty().map { status ->
            projectStatus(state, status)
        }

        val enemy = combat.enemies.firstOrNull { it.combatant.currentHealth > GameNumber.ZERO }
            ?: combat.enemies.firstOrNull()

        val skills = state.run.player.equippedSkillIds.mapNotNull { skillId ->
            contentRegistry.skillOrNull(skillId)?.let { definition ->
                val metadata = presentationContentRegistry.entry(definition.id)
                val readyAt = playerCombatant?.cooldowns?.readyAtByActionId?.get(definition.id)
                val cooldownRemaining = if (readyAt == null) {
                    0L
                } else {
                    (readyAt.millis - nowMillis).coerceAtLeast(0L)
                }
                val cooldownDuration = definition.cooldown.millis
                val cooldownProgress = when {
                    cooldownDuration <= 0L -> 10_000
                    cooldownRemaining <= 0L -> 10_000
                    else -> ratioUnits(
                        current = GameNumber.of(
                            (cooldownDuration - cooldownRemaining)
                                .coerceIn(0L, cooldownDuration)
                        ),
                        maximum = GameNumber.of(cooldownDuration)
                    )
                }
                val queueRejection = readQueries.skillQueueRejection(state, definition.id)
                val executionRejection = readQueries.skillExecutionRejection(state, definition.id)
                val readiness = when (executionRejection?.code) {
                    null -> BattleSkillReadinessUi.READY
                    CommandRejectionCode.COOLDOWN_ACTIVE ->
                        BattleSkillReadinessUi.COOLDOWN_WAIT
                    CommandRejectionCode.INSUFFICIENT_RESOURCE ->
                        BattleSkillReadinessUi.RESOURCE_WAIT
                    else -> BattleSkillReadinessUi.UNAVAILABLE
                }
                val resourceCosts = definition.resourceCosts.entries
                    .sortedBy { it.key }
                    .map { (resourceId, required) ->
                        val available = state.run.player.resources.amounts[resourceId]
                            ?: GameNumber.ZERO
                        BattleResourceCostUiState(
                            resourceId = resourceId,
                            titleStringKey = presentationContentRegistry
                                .entryOrNull(resourceId)
                                ?.titleStringKey,
                            requiredDisplay = GameNumberFormatter.compact(required),
                            availableDisplay = GameNumberFormatter.compact(available),
                            sufficient = available >= required
                        )
                    }

                val rank = SkillScalingSystem.rank(state, definition)

                BattleSkillUiState(
                    skillId = definition.id,
                    titleStringKey = metadata.titleStringKey,
                    assetKey = metadata.iconAssetKey,
                    affinityIds = definition.affinityTags
                        .map(Affinity::id)
                        .sorted(),
                    cooldownRemainingMillis = cooldownRemaining,
                    cooldownDurationMillis = cooldownDuration,
                    cooldownProgressUnits = cooldownProgress,
                    recoveryMillis = definition.recovery.millis,
                    resourceCosts = resourceCosts,
                    queued = queuedSkillId == definition.id,
                    queueAllowed = queueRejection == null,
                    readiness = readiness,
                    rank = rank,
                    scalingDisplay = "RANK $rank · ${formatPercent(definition.powerGrowthPerPlayerLevel)} / RANK"
                )
            }
        }

        val resonanceCap = readQueries.resonanceChargeCap()
        val resonance = Affinity.values().map { affinity ->
            val charge = state.run.resonance.chargeByAffinityId[affinity.id]
                ?: GameNumber.ZERO
            ResonanceAffinityUiState(
                affinityId = affinity.id,
                titleStringKey = presentationContentRegistry.entry(affinity.id).titleStringKey,
                chargeDisplay = GameNumberFormatter.compact(charge),
                capDisplay = GameNumberFormatter.compact(resonanceCap),
                chargeProgressUnits = ratioUnits(charge, resonanceCap)
            )
        }

        val gold = state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD]
            ?: GameNumber.ZERO
        val upgradeId = DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID
        val upgradeMetadata = presentationContentRegistry.entry(upgradeId)
        val upgradeCost = readQueries.upgradeCurrentCost(state, upgradeId)
        val currentStageNumber = state.run.world.currentEncounter?.definitionId?.let { encounterId ->
            state.run.world.activeRegionId
                ?.let(contentRegistry::region)
                ?.encounterIds
                ?.indexOf(encounterId)
                ?.takeIf { it >= 0 }
                ?.plus(1)
        }
        val stageLabel = state.run.world.currentEncounter?.definitionId
            ?.let(contentRegistry::encounter)
            ?.displayName
        val feedbackForUi = feedback?.let { currentFeedback ->
            val previousUpgradeLevel = currentFeedback.upgradePreviousLevel
            val newUpgradeLevel = currentFeedback.upgradeNewLevel
            if (currentFeedback.kind == com.idlerpg.game.presentation.model.BattleFeedbackKind.UPGRADE_PURCHASED &&
                currentFeedback.contentId == upgradeId &&
                previousUpgradeLevel != null &&
                newUpgradeLevel != null
            ) {
                val previousLevels = state.run.economy.upgrades.levelByUpgradeId.toMutableMap()
                    .apply { this[upgradeId] = previousUpgradeLevel }
                val stateBeforeUpgrade = state.copy(
                    run = state.run.copy(
                        economy = state.run.economy.copy(
                            upgrades = state.run.economy.upgrades.copy(
                                levelByUpgradeId = previousLevels
                            )
                        )
                    )
                )
                currentFeedback.copy(
                    upgradePreviousAttackDisplay = GameNumberFormatter.compact(
                        readQueries.attackPower(stateBeforeUpgrade)
                    ),
                    upgradeNewAttackDisplay = GameNumberFormatter.compact(
                        readQueries.attackPower(state)
                    )
                )
            } else {
                currentFeedback
            }
        }

        return BattleUiState(
            combatStatus = when (combat.status) {
                CombatStatus.IDLE -> BattleCombatStatusUi.IDLE
                CombatStatus.ACTIVE -> BattleCombatStatusUi.ACTIVE
                CombatStatus.VICTORY -> BattleCombatStatusUi.VICTORY
                CombatStatus.DEFEAT -> BattleCombatStatusUi.DEFEAT
            },
            regionTitleStringKey = state.run.world.activeRegionId
                ?.let(presentationContentRegistry::entryOrNull)
                ?.titleStringKey,
            encounterTitleStringKey = state.run.world.currentEncounter
                ?.definitionId
                ?.let(presentationContentRegistry::entryOrNull)
                ?.titleStringKey,
            combatSequenceId = combat.combatSequenceId,
            enemy = enemy?.let { projectEnemy(state, it) },
            player = BattlePlayerUiState(
                heroName = state.meta.heroName,
                currentHealthDisplay = GameNumberFormatter.compact(playerCurrentHealth),
                maximumHealthDisplay = GameNumberFormatter.compact(playerMaximumHealth),
                healthProgressUnits = ratioUnits(playerCurrentHealth, playerMaximumHealth),
                attackDisplay = GameNumberFormatter.compact(readQueries.attackPower(state)),
                armorDisplay = GameNumberFormatter.compact(readQueries.armor(state)),
                basicAttackDpsDisplay = GameNumberFormatter.compact(readQueries.basicAttackDps(state)),
                basicAttackIntervalMillis = readQueries.basicAttackInterval(state).millis,
                nextDecisionRemainingMillis = combat.nextPlayerDecisionAt?.let { decision ->
                    (decision.millis - nowMillis).coerceAtLeast(0L)
                },
                resources = state.run.player.resources.amounts.entries
                    .sortedBy { it.key }
                    .map { (resourceId, amount) ->
                        BattleResourceUiState(
                            resourceId = resourceId,
                            titleStringKey = presentationContentRegistry
                                .entryOrNull(resourceId)
                                ?.titleStringKey,
                            amountDisplay = GameNumberFormatter.compact(amount)
                        )
                    },
                statuses = playerStatuses,
                level = state.run.progression.playerLevel.level,
                criticalChanceDisplay = formatPercent(readQueries.criticalChance(state)),
                criticalMultiplierDisplay = formatPercent(readQueries.criticalMultiplier(state)),
                damageReductionDisplay = formatPercent(readQueries.damageReduction(state)),
                effectPowerDisplay = formatPercent(readQueries.effectPower(state)),
                healingPowerDisplay = formatPercent(readQueries.healingPower(state))
            ),
            equippedSkills = skills,
            queuedSkillId = queuedSkillId,
            resonance = resonance,
            resonanceSequence = state.run.resonance.sequence.affinityIds,
            doctrine = BattleDoctrineUiState(
                enabled = state.run.doctrine.enabled,
                ruleCount = state.run.doctrine.ruleCount,
                ruleCapacity = readQueries.doctrineCapacity(state),
                queuedSkillTitleStringKey = queuedSkillId
                    ?.let(presentationContentRegistry::entryOrNull)
                    ?.titleStringKey,
                decisionRuleNumber = doctrineFeedback?.ruleId?.let { ruleId ->
                    state.run.doctrine.rules.indexOfFirst { it.instanceId == ruleId }
                        .takeIf { it >= 0 }
                        ?.plus(1)
                },
                decisionExplanation = doctrineFeedback?.explanation
            ),
            basicAttackUpgrade = BattleUpgradeUiState(
                upgradeId = upgradeId,
                titleStringKey = upgradeMetadata.titleStringKey,
                assetKey = upgradeMetadata.iconAssetKey,
                level = readQueries.upgradeLevel(state, upgradeId),
                nextCostDisplay = GameNumberFormatter.compact(upgradeCost),
                affordable = gold >= upgradeCost
            ),
            canRetreat = combat.status == CombatStatus.ACTIVE &&
                state.run.world.currentEncounter != null,
            canRetry = state.run.world.currentEncounter?.status == com.idlerpg.game.domain.model.world.EncounterStatus.FAILED,
            retryEncounterId = state.run.world.currentEncounter
                ?.takeIf { it.status == com.idlerpg.game.domain.model.world.EncounterStatus.FAILED }
                ?.definitionId,
            feedback = feedbackForUi,
            presentationRewards = presentationRewards,
            enemies = combat.enemies.map { projectEnemy(state, it) },
            currentWave = state.run.world.currentEncounter?.currentWave ?: 1,
            totalWaves = state.run.world.currentEncounter?.definitionId
                ?.let(contentRegistry::encounter)?.waves ?: 1,
            stageNumber = currentStageNumber,
            stageLabel = stageLabel,
            backgroundAssetKey = currentStageNumber
                ?.takeIf {
                    state.run.world.activeRegionId == DefaultGameContent.TRAINING_HOLLOW_REGION_ID
                }
                ?.let { stage ->
                    when (stage) {
                        in 1..10 -> PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BACKGROUND
                        in 11..20 -> PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BACKGROUND
                        in 21..30 -> PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BACKGROUND
                        else -> PresentationAssetKey.TRAINING_HOLLOW_ILLUSTRATION
                    }
                }
                ?: PresentationAssetKey.TRAINING_HOLLOW_ILLUSTRATION,
            bossPhase = state.run.world.currentEncounter?.let { encounter ->
                val definition = contentRegistry.encounter(encounter.definitionId)
                if (definition.type != com.idlerpg.game.domain.definition.world.EncounterType.BOSS ||
                    encounter.status !in setOf(
                        com.idlerpg.game.domain.model.world.EncounterStatus.ACTIVE,
                        com.idlerpg.game.domain.model.world.EncounterStatus.FAILED
                    )
                ) {
                    null
                } else when (encounter.currentWave) {
                    1 -> BattleBossPhaseUi.BASTION
                    2 -> BattleBossPhaseUi.REFLECTION
                    3 -> BattleBossPhaseUi.FRACTURE
                    else -> null
                }
            },
            automationMode = state.run.world.automationMode
        )
    }

    private fun projectEnemy(
        state: GameState,
        enemy: EnemyState
    ): BattleEnemyUiState {
        val definition = contentRegistry.enemy(enemy.definitionId)
        val metadata = presentationContentRegistry.entry(enemy.definitionId)
        val maximumHealth = readQueries.enemyMaximumHealth(state, enemy)
        val attack = definition.attackDefinitionId?.let(contentRegistry::enemyAttack)
        val scaledArmor = EnemyScalingSystem.scaledArmor(definition, enemy.scalingTier)
        val nextAttackAt = state.run.combat.nextEnemyDecisionAt[enemy.instanceId]
        val encounterType = state.run.world.currentEncounter?.definitionId
            ?.let(contentRegistry::encounter)
            ?.type
        return BattleEnemyUiState(
            instanceId = enemy.instanceId,
            definitionId = enemy.definitionId,
            titleStringKey = metadata.titleStringKey,
            assetKey = metadata.iconAssetKey,
            illustrationAssetKey = metadata.illustrationAssetKey,
            currentHealthDisplay = GameNumberFormatter.full(enemy.combatant.currentHealth),
            maximumHealthDisplay = GameNumberFormatter.full(maximumHealth),
            healthProgressUnits = ratioUnits(enemy.combatant.currentHealth, maximumHealth),
            alive = enemy.combatant.currentHealth > GameNumber.ZERO,
            scalingTier = enemy.scalingTier,
            attackTitle = attack?.displayName,
            attackAffinityId = attack?.affinity?.id,
            attackDamageDisplay = attack?.let {
                GameNumberFormatter.full(
                    EnemyScalingSystem.scaledAttack(definition, it, enemy.scalingTier)
                )
            },
            armorPenetrationDisplay = attack?.let { GameNumberFormatter.full(it.armorPenetration) },
            attackIntervalMillis = attack?.let {
                EnemyScalingSystem.scaledAttackInterval(definition, it, enemy.scalingTier).millis
            },
            nextAttackRemainingMillis = nextAttackAt?.let { (it.millis - state.engine.simulationTime.millis).coerceAtLeast(0L) },
            attackFxAssetKey = enemyAttackFx(enemy.definitionId),
            encounterFrameAssetKey = when (encounterType) {
                com.idlerpg.game.domain.definition.world.EncounterType.ELITE -> PresentationAssetKey.ENCOUNTER_FRAME_ELITE
                com.idlerpg.game.domain.definition.world.EncounterType.ANOMALY -> PresentationAssetKey.ENCOUNTER_FRAME_ANOMALY
                else -> PresentationAssetKey.ENCOUNTER_FRAME_NORMAL
            },
            statuses = enemy.combatant.statusEffects.map { projectStatus(state, it) },
            mutations = enemy.activeMutations.mapNotNull { mutation ->
                presentationContentRegistry.entryOrNull(mutation.mutationId)?.let { entry ->
                    BattleMutationUiState(mutation.mutationId, entry.titleStringKey, entry.iconAssetKey)
                }
            },
            armorDisplay = GameNumberFormatter.full(scaledArmor),
            role = definition.role
        )
    }

    private fun enemyAttackFx(enemyId: ContentId): PresentationAssetKey? = when (enemyId) {
        DefaultGameContent.SLIME_ID -> PresentationAssetKey.SLIME_ATTACK_FX
        DefaultGameContent.RIFTFANG_ID -> PresentationAssetKey.RIFTFANG_ATTACK_FX
        DefaultGameContent.CINDER_WISP_ID -> PresentationAssetKey.CINDER_WISP_ATTACK_FX
        DefaultGameContent.HOLLOW_BULWARK_ID -> PresentationAssetKey.HOLLOW_BULWARK_ATTACK_FX
        DefaultGameContent.ARCANE_SEER_ID -> PresentationAssetKey.ARCANE_SEER_ATTACK_FX
        else -> null
    }

    private fun projectStatus(
        state: GameState,
        status: StatusEffectState
    ): BattleStatusEffectUiState {
        val definition = contentRegistry.status(status.definitionId)
        val metadata = presentationContentRegistry.entry(status.definitionId)
        val remainingMillis = (status.expiresAt.millis - state.engine.simulationTime.millis)
            .coerceAtLeast(0L)
        return BattleStatusEffectUiState(
            instanceId = status.instanceId,
            definitionId = status.definitionId,
            titleStringKey = metadata.titleStringKey,
            assetKey = metadata.iconAssetKey,
            stackCount = status.stackCount,
            remainingMillis = remainingMillis,
            durationMillis = definition.duration.millis,
            progressUnits = ratioUnits(
                current = GameNumber.of(remainingMillis.coerceAtMost(definition.duration.millis)),
                maximum = GameNumber.of(definition.duration.millis)
            ),
            polarity = statusPolarity(status.definitionId),
            detailStringKey = statusDetailKey(status.definitionId),
            potencyDisplay = formatPercent(status.potency)
        )
    }

    private fun statusPolarity(definitionId: ContentId): BattleStatusPolarityUi = when (definitionId.value) {
        "status.burning",
        "status.scorched",
        "status.stagger",
        "status.deep_stagger",
        "status.chill" -> BattleStatusPolarityUi.HARMFUL
        "status.guard_focus",
        "status.glacial_ward",
        "status.vital_regeneration" -> BattleStatusPolarityUi.BENEFICIAL
        else -> BattleStatusPolarityUi.NEUTRAL
    }

    private fun statusDetailKey(definitionId: ContentId): PresentationStringKey = when (definitionId.value) {
        "status.burning" -> PresentationStringKey.DESC_STATUS_BURNING
        "status.scorched" -> PresentationStringKey.DESC_STATUS_SCORCHED
        "status.guard_focus" -> PresentationStringKey.DESC_STATUS_GUARD_FOCUS
        "status.glacial_ward" -> PresentationStringKey.DESC_STATUS_GLACIAL_WARD
        "status.stagger" -> PresentationStringKey.DESC_STATUS_STAGGER
        "status.deep_stagger" -> PresentationStringKey.DESC_STATUS_DEEP_STAGGER
        "status.chill" -> PresentationStringKey.DESC_STATUS_CHILL
        "status.vital_regeneration" -> PresentationStringKey.DESC_STATUS_VITAL_REGENERATION
        else -> PresentationStringKey.DESC_STATUS
    }

    private fun ratioUnits(
        current: GameNumber,
        maximum: GameNumber
    ): Int {
        if (maximum == GameNumber.ZERO) {
            return 0
        }
        val numerator = current.toBigInteger().multiply(BigInteger.valueOf(10_000L))
        return numerator.divide(maximum.toBigInteger())
            .coerceIn(BigInteger.ZERO, BigInteger.valueOf(10_000L))
            .toInt()
    }

    private fun formatPercent(ratio: com.idlerpg.game.core.number.Ratio): String {
        val whole = ratio.units / 100L
        val fraction = ratio.units % 100L
        return if (fraction == 0L) {
            "$whole%"
        } else {
            "$whole.${fraction.toString().padStart(2, '0').trimEnd('0')}%"
        }
    }
}
