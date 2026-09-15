package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.achievement.AchievementDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoUnlockEffect
import com.idlerpg.game.domain.definition.economy.UpgradeEffectDefinition
import com.idlerpg.game.domain.definition.quest.QuestDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.AchievementProgressUiState
import com.idlerpg.game.presentation.model.ChronicleMilestoneUiState
import com.idlerpg.game.presentation.model.ChroniclePreviewUiState
import com.idlerpg.game.presentation.model.ChronicleProgressUiState
import com.idlerpg.game.presentation.model.CoreGrowthEffectKind
import com.idlerpg.game.presentation.model.CoreGrowthPurchaseOptionUiState
import com.idlerpg.game.presentation.model.CoreGrowthTrackUiState
import com.idlerpg.game.presentation.model.EchoOfferEffectKind
import com.idlerpg.game.presentation.model.EchoOfferEffectUiState
import com.idlerpg.game.presentation.model.EchoOfferStatus
import com.idlerpg.game.presentation.model.EchoOfferUiState
import com.idlerpg.game.presentation.model.EchoShopUiState
import com.idlerpg.game.presentation.model.MasteryProgressUiState
import com.idlerpg.game.presentation.model.MasteryUnlockUiState
import com.idlerpg.game.domain.system.quest.QuestClaimSystem
import com.idlerpg.game.domain.system.stats.PowerScoreSystem
import com.idlerpg.game.presentation.model.ObjectiveProgressUiState
import com.idlerpg.game.presentation.model.PersistentDiscoveryKind
import com.idlerpg.game.presentation.model.PersistentDiscoveryUiState
import com.idlerpg.game.presentation.model.ProgressClaimStatus
import com.idlerpg.game.presentation.model.ProgressFeatureUiState
import com.idlerpg.game.presentation.model.ProgressFeedbackUiState
import com.idlerpg.game.presentation.model.ProgressGoalRequirementUiState
import com.idlerpg.game.presentation.model.PowerScoreComponentUiState
import com.idlerpg.game.presentation.model.PowerScoreUiState
import com.idlerpg.game.presentation.model.ProgressOverviewUiState
import com.idlerpg.game.presentation.model.ProgressNextGoalKind
import com.idlerpg.game.presentation.model.ProgressNextGoalUiState
import com.idlerpg.game.presentation.model.ProgressRewardUiState
import com.idlerpg.game.presentation.model.ProgressUiState
import com.idlerpg.game.presentation.model.QuestProgressUiState
import com.idlerpg.game.presentation.model.StatOverviewUiState
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.presentation.query.GameReadQueries
import com.idlerpg.game.presentation.query.ProgressReadQueries
import java.math.BigInteger

/** Canonical GameState/content -> FUI-09 Progress presentation. */
class ProgressProjector(
    private val contentRegistry: ContentRegistry,
    private val presentationContentRegistry: PresentationContentRegistry,
    private val readQueries: GameReadQueries,
    private val progressReadQueries: ProgressReadQueries
) {
    /** True when at least one authored Core Growth track can currently buy one level. */
    fun hasAffordableCoreGrowth(state: GameState): Boolean =
        contentRegistry.allUpgrades().any { definition ->
            val balance = state.run.economy.wallet.amountsByCurrencyId[definition.currencyId]
                ?: GameNumber.ZERO
            val cost = readQueries.upgradePurchaseCost(state, definition.id, 1L)
            cost != null && cost <= balance
        }

    fun project(
        state: GameState,
        feedback: ProgressFeedbackUiState? = null,
        chroniclePreview: ChroniclePreviewUiState? = null,
        chroniclePreviewRequestPending: Boolean = false,
        chronicleCommitPending: Boolean = false
    ): ProgressUiState = ProgressUiState(
        overview = projectOverview(state),
        coreGrowthGoldDisplay = GameNumberFormatter.full(
            state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD] ?: GameNumber.ZERO
        ),
        coreGrowth = projectCoreGrowth(state),
        masteries = contentRegistry.allMasteries().sortedBy { it.id }.map { definition ->
            val totalExperience = state.run.progression.affinityMastery
                .experienceByAffinityId[definition.affinity.id] ?: GameNumber.ZERO
            val level = readQueries.masteryLevel(state, definition.affinity.id)
            val remaining = readQueries.masteryExperienceToNextLevel(state, definition.affinity.id)
            val masteryMetadata = presentationContentRegistry.entry(definition.id)
            val affinityMetadata = presentationContentRegistry.entry(definition.affinity.id)
            MasteryProgressUiState(
                masteryId = definition.id,
                affinityId = definition.affinity.id,
                titleStringKey = masteryMetadata.titleStringKey,
                affinityTitleStringKey = affinityMetadata.titleStringKey,
                iconAssetKey = masteryMetadata.iconAssetKey,
                level = level,
                maximumLevel = definition.maxLevel,
                totalExperienceDisplay = GameNumberFormatter.full(totalExperience),
                experienceToNextLevelDisplay = GameNumberFormatter.full(remaining),
                atMaximumLevel = definition.maxLevel?.let { level >= it } == true,
                unlocks = buildList {
                    contentRegistry.allSkills().forEach { skill ->
                        val feature = contentRegistry.allFeatureUnlocks().firstOrNull { it.id == skill.requiredFeatureId }
                        val required = feature?.requiredMasteryLevels?.get(definition.affinity.id)
                        if (required != null) add(MasteryUnlockUiState(presentationContentRegistry.entry(skill.id).titleStringKey, required))
                    }
                    contentRegistry.allSkillEvolutions().filter { it.requiredAffinity == definition.affinity }.forEach {
                        add(MasteryUnlockUiState(presentationContentRegistry.entry(it.id).titleStringKey, it.requiredMasteryLevel))
                    }
                }.sortedBy { it.requiredLevel }
            )
        },
        quests = contentRegistry.allQuests().sortedBy { it.id }.map { definition ->
            projectQuest(state, definition)
        },
        achievements = contentRegistry.allAchievements().sortedBy { it.id }.map { definition ->
            projectAchievement(state, definition)
        },
        discoveries = projectDiscoveries(state),
        echoShop = projectEchoShop(state),
        chronicle = projectChronicle(state),
        chroniclePreview = chroniclePreview,
        chroniclePreviewRequestPending = chroniclePreviewRequestPending,
        chronicleCommitPending = chronicleCommitPending,
        feedback = feedback
    )

    private fun projectCoreGrowth(state: GameState): List<CoreGrowthTrackUiState> =
        contentRegistry.allUpgrades().sortedBy { it.id }.map { definition ->
            val metadata = presentationContentRegistry.entry(definition.id)
            val level = readQueries.upgradeLevel(state, definition.id)
            val effectiveLevel = definition.effectiveLevel(level)
            val maximumAffordable = readQueries.maximumAffordableUpgradeQuantity(state, definition.id)
            val options = listOf(1L, 10L, 25L).map { quantity ->
                val cost = readQueries.upgradePurchaseCost(state, definition.id, quantity)
                CoreGrowthPurchaseOptionUiState(
                    quantity = quantity,
                    quantityLabel = "x$quantity",
                    costDisplay = cost?.let(GameNumberFormatter::full) ?: "—",
                    enabled = cost != null && maximumAffordable >= quantity
                )
            } + CoreGrowthPurchaseOptionUiState(
                quantity = maximumAffordable,
                quantityLabel = if (maximumAffordable > 0L) "MAX ($maximumAffordable)" else "MAX",
                costDisplay = if (maximumAffordable > 0L) {
                    readQueries.upgradePurchaseCost(state, definition.id, maximumAffordable)
                        ?.let(GameNumberFormatter::full) ?: "—"
                } else {
                    "—"
                },
                enabled = maximumAffordable > 0L,
                maximum = true
            )
            val nextMilestone = definition.milestones.firstOrNull { it.level > level }
            val (effectKind, display) = effectUi(definition.effect, effectiveLevel)
            CoreGrowthTrackUiState(
                upgradeId = definition.id,
                titleStringKey = metadata.titleStringKey,
                iconAssetKey = metadata.iconAssetKey,
                effectKind = effectKind,
                level = level,
                currentEffectDisplay = display,
                nextMilestoneLevel = nextMilestone?.level,
                nextMilestoneBonusLevels = nextMilestone?.bonusEquivalentLevels,
                purchaseOptions = options,
                legacyStartingLevel = com.idlerpg.game.domain.system.chronicle.EchoTrainingSystem.startingLevels(state.meta)[definition.id] ?: 0L
            )
        }

    private fun effectUi(
        effect: UpgradeEffectDefinition,
        effectiveLevel: Long
    ): Pair<CoreGrowthEffectKind, String> = when (effect) {
        is UpgradeEffectDefinition.FlatAttackPowerPerLevel ->
            CoreGrowthEffectKind.ATTACK_POWER to
                GameNumberFormatter.full(effect.amountPerLevel * effectiveLevel)
        is UpgradeEffectDefinition.FlatMaxHealthPerLevel ->
            CoreGrowthEffectKind.MAX_HEALTH to
                GameNumberFormatter.full(effect.amountPerLevel * effectiveLevel)
        is UpgradeEffectDefinition.FlatArmorPerLevel ->
            CoreGrowthEffectKind.ARMOR to
                GameNumberFormatter.full(effect.amountPerLevel * effectiveLevel)
        is UpgradeEffectDefinition.ActionSpeedPerLevel ->
            CoreGrowthEffectKind.ACTION_SPEED to percent(effect.ratioPerLevel.units * effectiveLevel)
        is UpgradeEffectDefinition.CriticalChancePerLevel ->
            CoreGrowthEffectKind.CRITICAL_CHANCE to percent(effect.ratioPerLevel.units * effectiveLevel)
        is UpgradeEffectDefinition.CriticalMultiplierPerLevel ->
            CoreGrowthEffectKind.CRITICAL_MULTIPLIER to percent(effect.ratioPerLevel.units * effectiveLevel)
        is UpgradeEffectDefinition.EffectPowerPerLevel ->
            CoreGrowthEffectKind.EFFECT_POWER to percent(effect.ratioPerLevel.units * effectiveLevel)
        is UpgradeEffectDefinition.HealingPowerPerLevel ->
            CoreGrowthEffectKind.HEALING_POWER to percent(effect.ratioPerLevel.units * effectiveLevel)
    }

    private fun percent(ratioUnits: Long): String {
        val whole = ratioUnits / 100L
        val fraction = ratioUnits % 100L
        return if (fraction == 0L) "+$whole%" else "+$whole.${fraction.toString().padStart(2, '0')}%"
    }

    private fun projectOverview(state: GameState): ProgressOverviewUiState {
        val player = state.run.progression.playerLevel
        val required = readQueries.playerExperienceToNextLevel(state)
        val maximumLevel = contentRegistry.defaultPlayerLevelCurve().maxLevel
        val unlockedFeatureIds = (
            state.run.progression.featureUnlocks.unlockedFeatureIds +
                state.meta.persistentFeatureUnlocks.unlockedFeatureIds
            ).toSortedSet()
        val statCards = listOf(
            StatOverviewUiState(
                id = "attack",
                label = "ATTACK",
                valueDisplay = GameNumberFormatter.full(readQueries.attackPower(state)),
                description = "Damage before skill scaling and mitigation.",
                formula = "Level baseline + upgrades + equipment + effects"
            ),
            StatOverviewUiState(
                id = "max-health",
                label = "MAX HEALTH",
                valueDisplay = GameNumberFormatter.full(readQueries.maximumHealth(state)),
                description = "Maximum health available in combat.",
                formula = "Base health + level + gear + effects"
            ),
            StatOverviewUiState(
                id = "armor",
                label = "ARMOR",
                valueDisplay = GameNumberFormatter.full(readQueries.armor(state)),
                description = "Reduces incoming physical damage.",
                formula = "Damage reduction = Armor / (100 + Armor)"
            ),
            StatOverviewUiState(
                id = "crit-chance",
                label = "CRIT CHANCE",
                valueDisplay = formatRatioPercent(readQueries.criticalChance(state)),
                description = "Chance for a hit to become a critical strike.",
                formula = "Clamped to 100% before the roll"
            ),
            StatOverviewUiState(
                id = "crit-damage",
                label = "CRIT DAMAGE",
                valueDisplay = formatRatioPercent(readQueries.criticalMultiplier(state)),
                description = "Multiplier applied when a critical strike lands.",
                formula = "Final hit × critical multiplier"
            ),
            StatOverviewUiState(
                id = "attack-speed",
                label = "ATTACK SPEED",
                valueDisplay = formatRatioPercent(readQueries.actionSpeed(state)),
                description = "Higher speed shortens action intervals.",
                formula = "Interval = base interval × 100% / speed"
            ),
            StatOverviewUiState(
                id = "effect-power",
                label = "EFFECT POWER",
                valueDisplay = formatRatioPercent(readQueries.effectPower(state)),
                description = "Scales elemental and status effects.",
                formula = "Base effect × effect power"
            ),
            StatOverviewUiState(
                id = "healing-power",
                label = "HEALING POWER",
                valueDisplay = formatRatioPercent(readQueries.healingPower(state)),
                description = "Scales all healing received or cast.",
                formula = "Base healing × healing power"
            )
        )

        val powerScore = PowerScoreSystem.calculate(state, contentRegistry)
        return ProgressOverviewUiState(
            playerLevel = player.level,
            currentExperienceDisplay = GameNumberFormatter.full(player.currentExperience),
            experienceToNextLevelDisplay = GameNumberFormatter.full(required),
            experienceRemainingDisplay = GameNumberFormatter.full(
                if (required > player.currentExperience) required - player.currentExperience
                else GameNumber.ZERO
            ),
            experienceProgressUnits = ratioUnits(player.currentExperience, required),
            atMaximumPlayerLevel = maximumLevel?.let { player.level >= it } == true,
            unlockedFeatures = unlockedFeatureIds.mapNotNull { featureId ->
                presentationContentRegistry.entryOrNull(featureId)?.let { metadata ->
                    ProgressFeatureUiState(
                        featureId = featureId,
                        titleStringKey = metadata.titleStringKey,
                        iconAssetKey = metadata.iconAssetKey
                    )
                }
            },
            echoAvailableDisplay = GameNumberFormatter.full(state.meta.echoes.available),
            echoSpentDisplay = GameNumberFormatter.full(state.meta.echoes.spent),
            chronicleEligible = readQueries.chronicleEligible(state),
            statCards = statCards,
            nextGoal = projectNextGoal(state),
            powerScore = PowerScoreUiState(
                totalDisplay = GameNumberFormatter.full(powerScore.total),
                components = listOf(
                    PowerScoreComponentUiState(
                        id = "offense",
                        label = "OFFENSE",
                        valueDisplay = GameNumberFormatter.full(powerScore.offense),
                        formula = "Expected Basic Attack damage with critical chance and multiplier"
                    ),
                    PowerScoreComponentUiState(
                        id = "defense",
                        label = "DEFENSE",
                        valueDisplay = GameNumberFormatter.full(powerScore.defense),
                        formula = "Max Health + Armor × 10"
                    ),
                    PowerScoreComponentUiState(
                        id = "gear",
                        label = "GEAR",
                        valueDisplay = GameNumberFormatter.full(powerScore.gear),
                        formula = "Equipped rarity + enhancement + rolled affix values"
                    ),
                    PowerScoreComponentUiState(
                        id = "skills",
                        label = "SKILLS",
                        valueDisplay = GameNumberFormatter.full(powerScore.skills),
                        formula = "Equipped skill rank + mastery + refinement"
                    ),
                    PowerScoreComponentUiState(
                        id = "rebirth",
                        label = "REBIRTH",
                        valueDisplay = GameNumberFormatter.full(powerScore.rebirth),
                        formula = "Allocated Normal and Legacy points"
                    )
                ),
                expectedBasicAttackDamageDisplay =
                    GameNumberFormatter.full(powerScore.expectedBasicAttackDamage),
                effectiveHealthDisplay = GameNumberFormatter.full(powerScore.effectiveHealth)
            )
        )
    }

    private data class NextGoalCandidate(
        val kind: ProgressNextGoalKind,
        val targetId: ContentId,
        val requiredLevel: Long,
        val currentLevel: Long,
        val titleStringKey: PresentationStringKey?,
        val bonusLevels: Long = 0L,
        val requirements: List<ProgressGoalRequirementUiState> = emptyList()
    ) {
        val remainingLevels: Long
            get() = requirements.maxOfOrNull { it.remainingLevels }
                ?: (requiredLevel - currentLevel).coerceAtLeast(0L)
    }

    private fun projectNextGoal(state: GameState): ProgressNextGoalUiState {
        val masteryCandidates = mutableListOf<NextGoalCandidate>()
        val featureById = contentRegistry.allFeatureUnlocks().associateBy { it.id }
        val skills = contentRegistry.allSkills()
        val evolutions = contentRegistry.allSkillEvolutions()
        val masteryLevels = contentRegistry.allMasteries().associate { mastery ->
            mastery.affinity.id to readQueries.masteryLevel(state, mastery.affinity.id)
        }
        skills.forEach { skill ->
            val feature = featureById[skill.requiredFeatureId]
            val requirements = feature?.requiredMasteryLevels.orEmpty()
                .entries
                .sortedBy { it.key }
                .map { (affinityId, required) ->
                    ProgressGoalRequirementUiState(
                        requirementId = affinityId,
                        titleStringKey = presentationContentRegistry
                            .entryOrNull(affinityId)?.titleStringKey,
                        currentLevel = masteryLevels[affinityId] ?: 0L,
                        requiredLevel = required
                    )
                }
            if (requirements.any { it.remainingLevels > 0L }) {
                masteryCandidates += NextGoalCandidate(
                    kind = ProgressNextGoalKind.MASTERY_UNLOCK,
                    targetId = skill.id,
                    requiredLevel = requirements.maxOf { it.requiredLevel },
                    currentLevel = requirements.minOf { it.currentLevel },
                    titleStringKey = presentationContentRegistry.entry(skill.id).titleStringKey,
                    requirements = requirements
                )
            }
        }
        evolutions.forEach { evolution ->
            val level = masteryLevels[evolution.requiredAffinity.id] ?: 0L
            if (level < evolution.requiredMasteryLevel) {
                val requirement = ProgressGoalRequirementUiState(
                    requirementId = evolution.requiredAffinity.id,
                    titleStringKey = presentationContentRegistry
                        .entryOrNull(evolution.requiredAffinity.id)?.titleStringKey,
                    currentLevel = level,
                    requiredLevel = evolution.requiredMasteryLevel
                )
                masteryCandidates += NextGoalCandidate(
                    kind = ProgressNextGoalKind.MASTERY_UNLOCK,
                    targetId = evolution.id,
                    requiredLevel = evolution.requiredMasteryLevel,
                    currentLevel = level,
                    titleStringKey = presentationContentRegistry.entry(evolution.id).titleStringKey,
                    requirements = listOf(requirement)
                )
            }
        }
        val coreCandidates = contentRegistry.allUpgrades()
            .mapNotNull { definition ->
                val level = readQueries.upgradeLevel(state, definition.id)
                definition.milestones.firstOrNull { it.level > level }?.let { milestone ->
                    NextGoalCandidate(
                        kind = ProgressNextGoalKind.CORE_MILESTONE,
                        targetId = definition.id,
                        requiredLevel = milestone.level,
                        currentLevel = level,
                        titleStringKey = presentationContentRegistry.entry(definition.id).titleStringKey,
                        bonusLevels = milestone.bonusEquivalentLevels
                    )
                }
            }
        // Mastery unlocks and core milestones are different progression currencies. Keep the
        // authored mastery-first priority, then rank only within the chosen family.
        val nextCandidate = masteryCandidates.minWithOrNull(nextGoalComparator)
            ?: coreCandidates.minWithOrNull(nextGoalComparator)
        return nextCandidate
            ?.toUiState()
            ?: ProgressNextGoalUiState()
    }

    private val nextGoalComparator = compareBy<NextGoalCandidate> {
        it.remainingLevels
    }.thenBy { it.requiredLevel }
        .thenBy { it.targetId.value }

    private fun NextGoalCandidate.toUiState(): ProgressNextGoalUiState =
        ProgressNextGoalUiState(
            kind = kind,
            requiredLevel = requiredLevel,
            titleStringKey = titleStringKey,
            bonusLevels = bonusLevels,
            targetId = targetId,
            remainingLevels = remainingLevels,
            requirements = requirements
        )

    private fun formatRatioPercent(ratio: com.idlerpg.game.core.number.Ratio): String {
        val whole = ratio.units / 100L
        val fraction = ratio.units % 100L
        return if (fraction == 0L) {
            "$whole%"
        } else {
            "$whole.${fraction.toString().padStart(2, '0').trimEnd('0')}%"
        }
    }

    private fun projectQuest(
        state: GameState,
        definition: QuestDefinition
    ): QuestProgressUiState {
        val progress = state.run.quests.progressFor(definition.id)
        val eligible = progressReadQueries.isQuestEligible(state, definition.id)
        val status = when {
            !definition.repeatable && progress.claimedCount >= progress.completionCount &&
                progress.completionCount > GameNumber.ZERO -> ProgressClaimStatus.CLAIMED
            eligible && progress.completionCount > progress.claimedCount ->
                ProgressClaimStatus.READY_TO_CLAIM
            !eligible -> ProgressClaimStatus.LOCKED
            else -> ProgressClaimStatus.IN_PROGRESS
        }
        val metadata = presentationContentRegistry.entry(definition.id)
        val reward = QuestClaimSystem.rewardForClaim(state, definition)

        return QuestProgressUiState(
            questId = definition.id,
            titleStringKey = metadata.titleStringKey,
            iconAssetKey = metadata.iconAssetKey,
            status = status,
            requiredPlayerLevel = definition.requiredPlayerLevel,
            objectives = definition.objectives.sortedBy { it.id }.map { objective ->
                val current = progress.progressByObjectiveId[objective.id] ?: GameNumber.ZERO
                objectiveUi(
                    objectiveId = objective.id,
                    current = current,
                    required = objective.requiredCount
                )
            },
            reward = rewardUi(
                gold = reward.currencies[CurrencyId.GOLD] ?: GameNumber.ZERO,
                experience = reward.experience,
                lootTableCount = reward.lootTableIds.size
            ),
            completionCountDisplay = GameNumberFormatter.full(progress.completionCount),
            claimedCountDisplay = GameNumberFormatter.full(progress.claimedCount),
            canClaim = status == ProgressClaimStatus.READY_TO_CLAIM,
            repeatable = definition.repeatable,
            pendingCountDisplay = GameNumberFormatter.full(progress.completionCount - progress.claimedCount)
        )
    }

    private fun projectAchievement(
        state: GameState,
        definition: AchievementDefinition
    ): AchievementProgressUiState {
        val progress = state.meta.achievements.progressFor(definition.id)
        val eligible = progressReadQueries.isAchievementEligible(state, definition.id)
        val status = when {
            progress.rewardClaimed -> ProgressClaimStatus.CLAIMED
            eligible && progress.completed -> ProgressClaimStatus.READY_TO_CLAIM
            !eligible -> ProgressClaimStatus.LOCKED
            else -> ProgressClaimStatus.IN_PROGRESS
        }
        val metadata = presentationContentRegistry.entry(definition.id)

        return AchievementProgressUiState(
            achievementId = definition.id,
            titleStringKey = metadata.titleStringKey,
            iconAssetKey = metadata.iconAssetKey,
            status = status,
            objectives = definition.objectives.sortedBy { it.id }.map { objective ->
                val current = progress.progressByObjectiveId[objective.id] ?: GameNumber.ZERO
                objectiveUi(
                    objectiveId = objective.id,
                    current = current,
                    required = objective.requiredCount
                )
            },
            reward = rewardUi(
                gold = definition.reward.currencies[CurrencyId.GOLD] ?: GameNumber.ZERO,
                experience = definition.reward.experience,
                lootTableCount = definition.reward.lootTableIds.size
            ),
            canClaim = status == ProgressClaimStatus.READY_TO_CLAIM
        )
    }

    private fun projectDiscoveries(state: GameState): List<PersistentDiscoveryUiState> =
        buildList {
            state.meta.discoveries.discoveredConvergenceIds.toSortedSet().forEach { id ->
                add(discoveryUi(id, PersistentDiscoveryKind.CONVERGENCE))
            }
            state.meta.discoveries.discoveredMutationIds.toSortedSet().forEach { id ->
                add(discoveryUi(id, PersistentDiscoveryKind.MUTATION))
            }
            state.meta.discoveries.discoveredEnemyKnowledgeIds.toSortedSet().forEach { id ->
                add(discoveryUi(id, PersistentDiscoveryKind.ENEMY_KNOWLEDGE))
            }
            state.meta.discoveries.unlockedHiddenContentIds.toSortedSet().forEach { id ->
                add(discoveryUi(id, PersistentDiscoveryKind.HIDDEN_CONTENT))
            }
        }

    private fun discoveryUi(
        contentId: com.idlerpg.game.core.id.ContentId,
        kind: PersistentDiscoveryKind
    ): PersistentDiscoveryUiState {
        val metadata = presentationContentRegistry.entry(contentId)
        return PersistentDiscoveryUiState(
            contentId = contentId,
            kind = kind,
            titleStringKey = metadata.titleStringKey,
            iconAssetKey = metadata.iconAssetKey
        )
    }

    private fun projectEchoShop(state: GameState): EchoShopUiState {
        val available = state.meta.echoes.available
        val spent = state.meta.echoes.spent
        return EchoShopUiState(
            availableDisplay = GameNumberFormatter.full(available),
            spentDisplay = GameNumberFormatter.full(spent),
            lifetimeEarnedDisplay = GameNumberFormatter.full(available + spent),
            offers = contentRegistry.allEchoOffers().sortedBy { it.id }.map { definition ->
                val purchased = progressReadQueries.isEchoOfferPurchased(state, definition.id)
                val prerequisitesSatisfied =
                    progressReadQueries.areEchoOfferPrerequisitesSatisfied(state, definition.id)
                val status = when {
                    purchased -> EchoOfferStatus.PURCHASED
                    !prerequisitesSatisfied -> EchoOfferStatus.LOCKED_BY_PREREQUISITE
                    available >= definition.cost -> EchoOfferStatus.AFFORDABLE
                    else -> EchoOfferStatus.INSUFFICIENT_ECHO
                }
                val metadata = presentationContentRegistry.entry(definition.id)
                EchoOfferUiState(
                    offerId = definition.id,
                    titleStringKey = metadata.titleStringKey,
                    iconAssetKey = metadata.iconAssetKey,
                    costDisplay = GameNumberFormatter.full(definition.cost),
                    descriptionStringKey = metadata.shortDescriptionStringKey,
                    effects = definition.effects.map { effect ->
                        when (effect) {
                            is EchoUnlockEffect.RevealHiddenContent -> {
                                val target = presentationContentRegistry.entry(effect.contentId)
                                EchoOfferEffectUiState(
                                    kind = EchoOfferEffectKind.REVEAL_HIDDEN_CONTENT,
                                    targetId = effect.contentId,
                                    titleStringKey = target.titleStringKey,
                                    iconAssetKey = target.iconAssetKey
                                )
                            }
                            is EchoUnlockEffect.UnlockPersistentFeature -> {
                                val target = presentationContentRegistry.entry(effect.featureId)
                                EchoOfferEffectUiState(
                                    kind = EchoOfferEffectKind.UNLOCK_PERSISTENT_FEATURE,
                                    targetId = effect.featureId,
                                    titleStringKey = target.titleStringKey,
                                    iconAssetKey = target.iconAssetKey
                                )
                            }
                        }
                    },
                    status = status,
                    canPurchase = status == EchoOfferStatus.AFFORDABLE
                )
            }
        )
    }

    private fun projectChronicle(state: GameState): ChronicleProgressUiState {
        val definition = contentRegistry.defaultChronicleDefinition()
        val metadata = presentationContentRegistry.entry(definition.id)
        val currentClears = progressReadQueries.chronicleTotalNormalClears(state)
        return ChronicleProgressUiState(
            chronicleId = definition.id,
            titleStringKey = metadata.titleStringKey,
            iconAssetKey = metadata.iconAssetKey,
            currentChronicleNumber = state.meta.chronicle.currentChronicleNumber,
            completedChroniclesDisplay = GameNumberFormatter.full(
                state.meta.chronicle.completedChronicles
            ),
            currentNormalClearsDisplay = GameNumberFormatter.full(currentClears),
            requiredNormalClearsDisplay = GameNumberFormatter.full(
                definition.requiredTotalNormalClears
            ),
            echoRewardDisplay = GameNumberFormatter.full(
                com.idlerpg.game.domain.system.chronicle.ChronicleSystem.echoReward(state, definition)
            ),
            eligible = readQueries.chronicleEligible(state),
            requiredBossTitle = definition.requiredBossId?.let { presentationContentRegistry.entry(it).titleStringKey },
            requiredBossDefeated = definition.requiredBossId == null || state.run.world.regionProgressById.values.any { definition.requiredBossId in it.clearedBossIds },
            bestMilestones = state.meta.chronicle.bestMilestoneIds.toSortedSet().map { id ->
                val milestone = presentationContentRegistry.entry(id)
                ChronicleMilestoneUiState(
                    milestoneId = id,
                    titleStringKey = milestone.titleStringKey,
                    iconAssetKey = milestone.iconAssetKey
                )
            }
        )
    }

    private fun objectiveUi(
        objectiveId: com.idlerpg.game.core.id.ContentId,
        current: GameNumber,
        required: GameNumber
    ): ObjectiveProgressUiState {
        val metadata = presentationContentRegistry.entry(objectiveId)
        return ObjectiveProgressUiState(
            objectiveId = objectiveId,
            titleStringKey = metadata.titleStringKey,
            iconAssetKey = metadata.iconAssetKey,
            currentDisplay = GameNumberFormatter.full(current),
            requiredDisplay = GameNumberFormatter.full(required),
            progressUnits = ratioUnits(current, required),
            complete = current >= required
        )
    }

    private fun rewardUi(
        gold: GameNumber,
        experience: GameNumber,
        lootTableCount: Int
    ): ProgressRewardUiState = ProgressRewardUiState(
        goldDisplay = gold.takeIf { it > GameNumber.ZERO }?.let(GameNumberFormatter::full),
        experienceDisplay = experience.takeIf { it > GameNumber.ZERO }?.let(GameNumberFormatter::full),
        lootTableCount = lootTableCount
    )

    private fun ratioUnits(current: GameNumber, required: GameNumber): Int {
        if (required <= GameNumber.ZERO) return 10_000
        val bounded = when {
            current <= GameNumber.ZERO -> GameNumber.ZERO
            current >= required -> required
            else -> current
        }
        return bounded.toBigInteger()
            .multiply(BigInteger.valueOf(10_000L))
            .divide(required.toBigInteger())
            .toInt()
            .coerceIn(0, 10_000)
    }
}
