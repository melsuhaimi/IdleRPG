package com.idlerpg.game.ui.simulation

import androidx.lifecycle.ViewModel
import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.GameSessionFactory
import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.config.GameConfig
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.command.ClaimAchievementReward
import com.idlerpg.game.domain.command.ClaimOverflowItem
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.command.ClearQueuedSkillCast
import com.idlerpg.game.domain.command.EquipSkill
import com.idlerpg.game.domain.command.ExpandInventoryCapacity
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.PurchaseEchoOffer
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.command.RequestChroniclePreview
import com.idlerpg.game.domain.command.CommitChronicleCollapse
import com.idlerpg.game.domain.command.SalvageOverflowItem
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.event.AchievementCompleted
import com.idlerpg.game.domain.event.AchievementRewardClaimed
import com.idlerpg.game.domain.event.AdaptationTierChanged
import com.idlerpg.game.domain.event.ChronicleCollapsed
import com.idlerpg.game.domain.event.ChroniclePreviewPrepared
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.CurrencySpent
import com.idlerpg.game.domain.event.DiscoveryUnlocked
import com.idlerpg.game.domain.event.EchoGranted
import com.idlerpg.game.domain.event.EchoOfferPurchased
import com.idlerpg.game.domain.event.EchoSpent
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.FeatureUnlocked
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.InventoryCapacityExpanded
import com.idlerpg.game.domain.event.InventoryProgressionBlocked
import com.idlerpg.game.domain.event.InventoryProgressionUnblocked
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.event.MutationRolled
import com.idlerpg.game.domain.event.OverflowItemClaimed
import com.idlerpg.game.domain.event.OverflowItemSalvaged
import com.idlerpg.game.domain.event.PlayerLeveledUp
import com.idlerpg.game.domain.event.QuestCompleted
import com.idlerpg.game.domain.event.QuestRewardClaimed
import com.idlerpg.game.domain.event.SkillCastQueueCleared
import com.idlerpg.game.domain.event.SkillCastQueueConsumed
import com.idlerpg.game.domain.event.SkillCastQueueDeferred
import com.idlerpg.game.domain.event.SkillCastQueueReplaced
import com.idlerpg.game.domain.event.SkillCastQueued
import com.idlerpg.game.domain.event.SkillEquipped
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.system.inventory.InventoryCapacitySystem
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canonical on-device developer surface for Foundations 1-18 plus FBE-00 through FBE-05.
 *
 * The ViewModel serializes simulator operations and only uses GameRuntime commands/advance
 * for gameplay transitions. The one explicit exception is the labelled Forged Flame debug
 * fixture: it unlocks/equips authored skills in canonical state so the real Doctrine,
 * Resonance, Convergence, and achievement pipeline can be exercised without long setup.
 */
class SimulationHarnessViewModel : ViewModel() {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "idle-rpg-simulation-harness")
    }
    private val operationRunning = AtomicBoolean(false)

    private val seed: Long = DEFAULT_SEED
    private var profile: SimulatorProfile = SimulatorProfile.VERIFICATION_24H
    private var factory: GameSessionFactory
    private var runtime: GameRuntime
    private var metrics: SimulationMetrics = SimulationMetrics()
    private var recentLog: List<String> = emptyList()
    private var lastCommandResult: String = "none"
    private var canonicalRegression: String? = null

    init {
        val bundle = createTrainingRuntime(seed, profile)
        factory = bundle.factory
        runtime = bundle.runtime
    }

    private val _uiState = MutableStateFlow(
        buildUiState(
            status = "Ready. Verification profile uses 10,000 inventory slots.",
            running = false,
            progressText = null
        )
    )
    val uiState: StateFlow<SimulationHarnessUiState> = _uiState.asStateFlow()

    fun resetCurrentProfile() {
        launchOperation("Resetting ${profile.label}") {
            resetRuntime(profile)
            publish(
                status = "Reset complete: ${profile.label}.",
                running = false,
                progressText = null
            )
        }
    }

    fun resetVerificationProfile() {
        launchOperation("Resetting verification profile") {
            resetRuntime(SimulatorProfile.VERIFICATION_24H)
            publish(
                status = "Verification profile ready. Training Hollow is active.",
                running = false,
                progressText = null
            )
        }
    }

    fun resetNormalCapacityProfile() {
        launchOperation("Resetting normal-capacity profile") {
            resetRuntime(SimulatorProfile.NORMAL_CAPACITY)
            publish(
                status = "Normal-capacity profile ready. Training Hollow is active.",
                running = false,
                progressText = null
            )
        }
    }

    fun advanceOneSecond() = runSimulation(GameDuration.ofSeconds(1L), "+1 second")

    fun advanceTenSeconds() = runSimulation(GameDuration.ofSeconds(10L), "+10 seconds")

    fun advanceOneMinute() = runSimulation(GameDuration.ofMinutes(1L), "+1 minute")

    fun advanceTenMinutes() = runSimulation(GameDuration.ofMinutes(10L), "+10 minutes")

    fun advanceOneHour() = runSimulation(GameDuration.ofHours(1L), "+1 hour")

    fun advanceTwentyFourHours() = runSimulation(GameDuration.ofHours(24L), "+24 hours")

    fun runCanonicalTwentyFourHourRegression() {
        launchOperation("Running canonical 24h regression") {
            resetRuntime(SimulatorProfile.VERIFICATION_24H)
            simulateDuration(
                duration = GameDuration.ofHours(24L),
                label = "canonical +24 hours"
            )
            val claim = dispatchAndObserve(
                ClaimQuestReward(DefaultGameContent.FIRST_HUNT_QUEST_ID)
            )
            check(claim.commandResult == CommandResult.Accepted) {
                "Canonical 24h First Hunt claim rejected: ${claim.commandResult}"
            }
            val firstState = runtime.state()
            val firstMetrics = metrics

            // Re-run from the same seed instead of pinning obsolete one-item-per-kill totals.
            // The regression now certifies canonical determinism and the bounded loot contract.
            resetRuntime(SimulatorProfile.VERIFICATION_24H)
            simulateDuration(GameDuration.ofHours(24L), "determinism replay +24 hours")
            val replayClaim = dispatchAndObserve(
                ClaimQuestReward(DefaultGameContent.FIRST_HUNT_QUEST_ID)
            )
            check(replayClaim.commandResult == CommandResult.Accepted) {
                "Canonical 24h replay claim rejected: ${replayClaim.commandResult}"
            }
            val state = runtime.state()
            val pass = state == firstState &&
                metrics == firstMetrics &&
                metrics.enemiesDefeated > 0L &&
                metrics.itemsFound in 1L until metrics.enemiesDefeated &&
                state.run.inventory.itemsById.size.toLong() <= VERIFICATION_INVENTORY_CAPACITY &&
                state.run.inventory.overflowItemsById.isEmpty()

            canonicalRegression = buildString {
                append(if (pass) "PASS" else "FAIL")
                append(" — kills=${metrics.enemiesDefeated}")
                append(", Gold=${metrics.goldGranted}")
                append(", XP=${metrics.experienceGranted}")
                append(", items=${metrics.itemsFound}")
            }
            check(pass) { "Canonical 24h regression mismatch: $canonicalRegression" }

            recentLog = (recentLog + "Canonical 24h regression PASS after explicit First Hunt claim.")
                .takeLast(MAX_LOG_LINES)
            publish(
                status = "Canonical 24h regression PASS.",
                running = false,
                progressText = null
            )
        }
    }

    fun equipHeavyStrike() = dispatchCommand(
        label = "Equipping Heavy Strike",
        command = EquipSkill(DefaultGameContent.HEAVY_STRIKE_ID)
    )

    fun queueHeavyStrike() = dispatchCommand(
        label = "Queueing Heavy Strike",
        command = QueueSkillCast(DefaultGameContent.HEAVY_STRIKE_ID)
    )

    fun clearQueuedSkill() = dispatchCommand(
        label = "Clearing queued skill",
        command = ClearQueuedSkillCast()
    )

    fun claimFirstHunt() = dispatchCommand(
        label = "Claiming First Hunt",
        command = ClaimQuestReward(DefaultGameContent.FIRST_HUNT_QUEST_ID)
    )

    fun runForgedFlameCompletionScenario() {
        launchOperation("Running Forged Flame completion scenario") {
            resetRuntime(profile)

            val current = runtime.state()
            val configured = current.copy(
                run = current.run.copy(
                    player = current.run.player.copy(
                        equippedSkillIds = listOf(
                            DefaultGameContent.HEAVY_STRIKE_ID,
                            DefaultGameContent.FLAME_BRAND_ID
                        )
                    ),
                    progression = current.run.progression.copy(
                        featureUnlocks = current.run.progression.featureUnlocks.copy(
                            unlockedFeatureIds =
                                current.run.progression.featureUnlocks.unlockedFeatureIds +
                                    DefaultGameContent.FLAME_BRAND_FEATURE_ID
                        )
                    )
                )
            )
            runtime.replaceLoadedState(configured)
            recentLog = (recentLog +
                "DEBUG FIXTURE: Flame Brand unlocked and Heavy Strike/Flame Brand equipped.")
                .takeLast(MAX_LOG_LINES)

            requireAccepted(
                dispatchAndObserve(
                    AddDoctrineRule(
                        condition = DoctrineCondition.Predicate(
                            DoctrinePredicate.ResonanceCharge(
                                affinity = Affinity.MIGHT,
                                comparison = DoctrineComparison.GREATER_THAN_OR_EQUAL,
                                amount = GameNumber.of(2L)
                            )
                        ),
                        action = DoctrineAction.UseSkill(DefaultGameContent.FLAME_BRAND_ID)
                    )
                ),
                "Forged Flame Doctrine rule 1"
            )
            requireAccepted(
                dispatchAndObserve(
                    AddDoctrineRule(
                        condition = DoctrineCondition.Predicate(
                            DoctrinePredicate.EnemyHealthPercent(
                                comparison = DoctrineComparison.LESS_THAN_OR_EQUAL,
                                threshold = Ratio.ONE
                            )
                        ),
                        action = DoctrineAction.UseSkill(DefaultGameContent.HEAVY_STRIKE_ID)
                    )
                ),
                "Forged Flame Doctrine rule 2"
            )

            observe(runtime.advance(GameDuration.ofSeconds(4L)).events)
            val progress = runtime.state().meta.achievements.progressFor(
                DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID
            )
            check(progress.completed && !progress.rewardClaimed) {
                "Forged Flame achievement did not become ready to claim"
            }
            publish(
                status = "Forged Flame achievement ready to claim.",
                running = false,
                progressText = null
            )
        }
    }

    fun claimForgedFlameAchievement() = dispatchCommand(
        label = "Claiming Forged Flame achievement",
        command = ClaimAchievementReward(DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID)
    )

    fun earnFiveEchoViaChronicle() {
        launchOperation("Running canonical Chronicle Echo scenario") {
            resetRuntime(profile)
            simulateDuration(GameDuration.ofSeconds(30L), "Chronicle eligibility setup")

            val preview = dispatchAndObserve(RequestChroniclePreview())
            requireAccepted(preview, "Chronicle preview")
            val prepared = preview.events
                .map { it.event }
                .filterIsInstance<ChroniclePreviewPrepared>()
                .single()
            requireAccepted(
                dispatchAndObserve(CommitChronicleCollapse(prepared.previewToken)),
                "Chronicle commit"
            )

            check(runtime.state().meta.echoes.available == GameNumber.of(5L)) {
                "Expected 5 available Echo after Chronicle scenario"
            }
            check(
                DefaultGameContent.ADAPTATION_FORECAST_DISCOVERY_ID !in
                    runtime.state().meta.discoveries.unlockedHiddenContentIds
            ) {
                "Adaptation Forecast must remain manual after earning Echo"
            }
            publish(
                status = "Chronicle complete: 5 Echo available; offer remains unpurchased.",
                running = false,
                progressText = null
            )
        }
    }

    fun purchaseAdaptationForecast() = dispatchCommand(
        label = "Purchasing Adaptation Forecast",
        command = PurchaseEchoOffer(DefaultGameContent.ADAPTATION_FORECAST_ECHO_OFFER_ID)
    )

    fun runNormalCapacityBlockScenario() {
        launchOperation("Filling normal inventory and safe overflow") {
            resetRuntime(SimulatorProfile.NORMAL_CAPACITY)
            val balance = factory.gameConfig.balance
            var simulatedMillis = 0L

            while (
                runtime.state().run.inventory.itemsById.size.toLong() <
                    balance.baseInventoryCapacity ||
                runtime.state().run.inventory.overflowItemsById.size.toLong() <
                    balance.inventoryOverflowCapacity
            ) {
                check(simulatedMillis < MAX_CAPACITY_SCENARIO_MILLIS) {
                    "Capacity scenario did not fill normal inventory and safe overflow within " +
                        formatDuration(MAX_CAPACITY_SCENARIO_MILLIS)
                }
                val result = runtime.advance(CAPACITY_SCENARIO_STEP)
                observe(result.events)
                simulatedMillis = Math.addExact(
                    simulatedMillis,
                    CAPACITY_SCENARIO_STEP.millis
                )
                publish(
                    status = "Filling normal inventory + safe overflow",
                    running = true,
                    progressText = "${formatDuration(simulatedMillis)} simulated"
                )
            }

            val inventory = runtime.state().run.inventory
            check(inventory.itemsById.size.toLong() == balance.baseInventoryCapacity)
            check(
                inventory.overflowItemsById.size.toLong() ==
                    balance.inventoryOverflowCapacity
            )
            publish(
                status = "Normal inventory and safe overflow are full; retained loot is safe.",
                running = false,
                progressText = null
            )
        }
    }

    fun expandInventoryCapacity() = dispatchCommand(
        label = "Expanding inventory capacity",
        command = ExpandInventoryCapacity(quantity = 1L)
    )

    fun claimFirstOverflowItem() {
        launchOperation("Claiming first overflow item") {
            val itemId = runtime.state().run.inventory.overflowItemsById.keys.minOrNull()
            if (itemId == null) {
                lastCommandResult = "not dispatched: overflow is empty"
                publish(
                    status = "No overflow item is available to claim.",
                    running = false,
                    progressText = null
                )
                return@launchOperation
            }
            dispatchAndObserve(ClaimOverflowItem(itemId))
            publish(
                status = "Overflow claim command completed.",
                running = false,
                progressText = null
            )
        }
    }

    fun salvageFirstOverflowItem() {
        launchOperation("Salvaging first overflow item") {
            val itemId = runtime.state().run.inventory.overflowItemsById.keys.minOrNull()
            if (itemId == null) {
                lastCommandResult = "not dispatched: overflow is empty"
                publish(
                    status = "No overflow item is available to salvage.",
                    running = false,
                    progressText = null
                )
                return@launchOperation
            }
            dispatchAndObserve(SalvageOverflowItem(itemId))
            publish(
                status = "Overflow salvage command completed.",
                running = false,
                progressText = null
            )
        }
    }

    private fun dispatchCommand(label: String, command: GameCommand) {
        launchOperation(label) {
            dispatchAndObserve(command)
            publish(
                status = "$label complete.",
                running = false,
                progressText = null
            )
        }
    }

    private fun dispatchAndObserve(command: GameCommand): EngineResult {
        val result = runtime.dispatch(command)
        lastCommandResult = describeCommandResult(result.commandResult)
        observe(result.events)
        return result
    }

    private fun runSimulation(duration: GameDuration, label: String) {
        launchOperation("Simulating $label") {
            simulateDuration(duration, label)
            publish(
                status = "Simulation complete: $label",
                running = false,
                progressText = null
            )
        }
    }

    private fun simulateDuration(duration: GameDuration, label: String) {
        val totalMillis = duration.millis
        var remainingMillis = totalMillis
        val chunkMillis = chooseChunkMillis(totalMillis)
        var processedMillis = 0L

        while (remainingMillis > 0L) {
            val stepMillis = minOf(remainingMillis, chunkMillis)
            val result = runtime.advance(GameDuration.ofMillis(stepMillis))
            observe(result.events)

            processedMillis = Math.addExact(processedMillis, stepMillis)
            remainingMillis -= stepMillis

            publish(
                status = "Simulating $label",
                running = true,
                progressText = progressText(processedMillis, totalMillis)
            )
        }
    }

    private fun launchOperation(initialStatus: String, block: () -> Unit) {
        if (!operationRunning.compareAndSet(false, true)) {
            return
        }

        _uiState.value = _uiState.value.copy(
            running = true,
            status = initialStatus,
            error = null
        )

        executor.execute {
            try {
                block()
            } catch (error: Throwable) {
                _uiState.value = buildUiState(
                    status = "Simulator operation stopped.",
                    running = false,
                    progressText = null,
                    error = error.message ?: error::class.java.simpleName
                )
            } finally {
                operationRunning.set(false)
            }
        }
    }

    private fun resetRuntime(targetProfile: SimulatorProfile) {
        profile = targetProfile
        val bundle = createTrainingRuntime(seed, targetProfile)
        factory = bundle.factory
        runtime = bundle.runtime
        metrics = SimulationMetrics()
        recentLog = listOf(
            "Simulator reset to seed $seed using ${targetProfile.label}."
        )
        lastCommandResult = "none"
        canonicalRegression = null
    }

    private fun observe(events: List<GameEventEnvelope>) {
        var kills = metrics.enemiesDefeated
        var clears = metrics.encountersCleared
        var gold = metrics.goldGranted
        var experience = metrics.experienceGranted
        var items = metrics.itemsFound
        var normalItems = metrics.normalItemsAdded
        var overflowItems = metrics.overflowItemsAdded
        var convergences = metrics.convergencesTriggered
        var tierChanges = metrics.adaptationTierChanges
        var questClaims = metrics.questClaims
        var achievementClaims = metrics.achievementClaims
        var echoPurchases = metrics.echoPurchases
        var capacityExpansions = metrics.capacityExpansions
        var blocks = metrics.progressionBlocks
        val log = recentLog.toMutableList()

        for (envelope in events) {
            when (val event = envelope.event) {
                is EnemyKilled -> kills = Math.addExact(kills, 1L)
                is EncounterCleared -> clears = Math.addExact(clears, 1L)
                is CurrencyGranted -> {
                    if (event.currencyId == CurrencyId.GOLD) {
                        gold = gold + event.amount
                    }
                }
                is ExperienceGranted -> experience = experience + event.amount
                is ItemAdded -> {
                    items = Math.addExact(items, 1L)
                    normalItems = Math.addExact(normalItems, 1L)
                }
                is ItemSentToOverflow -> {
                    items = Math.addExact(items, 1L)
                    overflowItems = Math.addExact(overflowItems, 1L)
                }
                is ConvergenceTriggered -> convergences = Math.addExact(convergences, 1L)
                is AdaptationTierChanged -> tierChanges = Math.addExact(tierChanges, 1L)
                is QuestRewardClaimed -> questClaims = Math.addExact(questClaims, 1L)
                is AchievementRewardClaimed -> achievementClaims = Math.addExact(achievementClaims, 1L)
                is EchoOfferPurchased -> echoPurchases = Math.addExact(echoPurchases, 1L)
                is InventoryCapacityExpanded -> capacityExpansions = Math.addExact(capacityExpansions, 1L)
                is InventoryProgressionBlocked -> blocks = Math.addExact(blocks, 1L)
                else -> Unit
            }

            importantEventText(envelope)?.let { log += it }
        }

        metrics = SimulationMetrics(
            enemiesDefeated = kills,
            encountersCleared = clears,
            goldGranted = gold,
            experienceGranted = experience,
            itemsFound = items,
            normalItemsAdded = normalItems,
            overflowItemsAdded = overflowItems,
            convergencesTriggered = convergences,
            adaptationTierChanges = tierChanges,
            questClaims = questClaims,
            achievementClaims = achievementClaims,
            echoPurchases = echoPurchases,
            capacityExpansions = capacityExpansions,
            progressionBlocks = blocks
        )
        recentLog = log.takeLast(MAX_LOG_LINES)
    }

    private fun publish(status: String, running: Boolean, progressText: String?) {
        _uiState.value = buildUiState(
            status = status,
            running = running,
            progressText = progressText
        )
    }

    private fun buildUiState(
        status: String,
        running: Boolean,
        progressText: String?,
        error: String? = null
    ): SimulationHarnessUiState {
        return SimulationHarnessUiState(
            running = running,
            seed = seed,
            profile = profile,
            status = status,
            progressText = progressText,
            error = error,
            lastCommandResult = lastCommandResult,
            canonicalRegression = canonicalRegression,
            snapshot = runtime.snapshot().toSimulationSnapshot(),
            metrics = metrics,
            recentLog = recentLog
        )
    }

    private fun GameState.toSimulationSnapshot(): SimulationSnapshot {
        val enemy = run.combat.enemies.minByOrNull { it.instanceId }
        val regionAdaptation = run.world.activeRegionId?.let { run.adaptation.regionStateById[it] }
        val balance = factory.gameConfig.balance
        val inventory = run.inventory

        val resonanceText = run.resonance.chargeByAffinityId
            .toSortedMap()
            .entries
            .joinToString(separator = ", ") { (id, amount) -> "$id=$amount" }
            .ifBlank { "none" }

        val adaptationText = regionAdaptation
            ?.tierByAffinityId
            ?.toSortedMap()
            ?.entries
            ?.joinToString(separator = ", ") { (id, tier) -> "$id=T$tier" }
            ?.ifBlank { "none" }
            ?: "none"

        val mutationsText = enemy
            ?.activeMutationIds
            ?.sorted()
            ?.joinToString(separator = ", ")
            ?.ifBlank { "none" }
            ?: "none"

        val equippedSkills = run.player.equippedSkillIds
            .joinToString(separator = ", ")
            .ifBlank { "none" }
        val queuedSkill = when (val queued = run.combat.queuedPlayerAction) {
            is QueuedPlayerAction.Skill -> queued.skillId.toString()
            null -> "none"
        }

        val questProgress = run.quests.progressFor(DefaultGameContent.FIRST_HUNT_QUEST_ID)
        val achievementProgress = meta.achievements.progressFor(
            DefaultGameContent.FORGED_FLAME_ACHIEVEMENT_ID
        )
        val purchasedOffers = meta.echoes.purchasedOfferIds
            .sorted()
            .joinToString(separator = ", ")
            .ifBlank { "none" }

        val effectiveCapacity = InventoryCapacitySystem.effectiveSlotCapacity(inventory)
        val nextExpansionCost = if (effectiveCapacity >= balance.maximumInventoryCapacity) {
            "MAX"
        } else {
            InventoryCapacitySystem.nextExpansionCost(inventory, balance).toString()
        }

        return SimulationSnapshot(
            simulationMillis = engine.simulationTime.millis,
            gold = run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD] ?: GameNumber.ZERO,
            playerLevel = run.progression.playerLevel.level,
            playerExperience = run.progression.playerLevel.currentExperience,
            inventoryItems = inventory.itemsById.size,
            inventoryCapacity = effectiveCapacity,
            overflowItems = inventory.overflowItemsById.size,
            overflowCapacity = balance.inventoryOverflowCapacity,
            inventoryBlocked = InventoryCapacitySystem.isProgressionBlocked(inventory, balance),
            nextExpansionGoldCost = nextExpansionCost,
            activeRegion = run.world.activeRegionId?.toString() ?: "none",
            encounterIndex = run.world.currentEncounter?.encounterIndex,
            enemyDefinitionId = enemy?.definitionId?.toString() ?: "none",
            enemyHealth = enemy?.combatant?.currentHealth ?: GameNumber.ZERO,
            enemyMutations = mutationsText,
            resonanceCharges = resonanceText,
            adaptationTiers = adaptationText,
            equippedSkills = equippedSkills,
            queuedSkill = queuedSkill,
            firstHuntCompletionCount = questProgress.completionCount,
            firstHuntClaimedCount = questProgress.claimedCount,
            firstHuntReadyToClaim = questProgress.completionCount > questProgress.claimedCount,
            forgedFlameAchievementCompleted = achievementProgress.completed,
            forgedFlameRewardClaimed = achievementProgress.rewardClaimed,
            forgedFlameReadyToClaim = achievementProgress.completed && !achievementProgress.rewardClaimed,
            chronicleNumber = meta.chronicle.currentChronicleNumber,
            echoAvailable = meta.echoes.available,
            echoSpent = meta.echoes.spent,
            purchasedEchoOffers = purchasedOffers,
            adaptationForecastUnlocked =
                DefaultGameContent.ADAPTATION_FORECAST_DISCOVERY_ID in
                    meta.discoveries.unlockedHiddenContentIds
        )
    }

    private fun importantEventText(envelope: GameEventEnvelope): String? {
        val prefix = "#${envelope.sequenceNumber} @${formatDuration(envelope.simulationTime.millis)}"
        return when (val event = envelope.event) {
            is EnemyKilled -> "$prefix Enemy killed: ${event.enemyDefinitionId}"
            is EncounterCleared -> "$prefix Encounter cleared: ${event.encounterIndex}"
            is CurrencyGranted ->
                if (event.currencyId == CurrencyId.GOLD) "$prefix Gold +${event.amount}" else null
            is CurrencySpent ->
                if (event.currencyId == CurrencyId.GOLD) "$prefix Gold -${event.amount}" else null
            is ExperienceGranted -> "$prefix XP +${event.amount}"
            is ItemAdded -> "$prefix Item to inventory: ${event.itemDefinitionId}"
            is ItemSentToOverflow -> "$prefix Item to overflow: ${event.itemDefinitionId}"
            is OverflowItemClaimed -> "$prefix Overflow claimed: ${event.itemDefinitionId}"
            is OverflowItemSalvaged -> "$prefix Overflow salvaged: ${event.itemDefinitionId}"
            is InventoryCapacityExpanded ->
                "$prefix Capacity ${event.previousCapacity} -> ${event.newCapacity}"
            is InventoryProgressionBlocked -> "$prefix INVENTORY PROGRESSION BLOCKED"
            is InventoryProgressionUnblocked -> "$prefix Inventory progression unblocked"
            is PlayerLeveledUp -> "$prefix Level ${event.previousLevel} -> ${event.newLevel}"
            is FeatureUnlocked -> "$prefix Feature unlocked: ${event.featureId}"
            is ConvergenceTriggered -> "$prefix Convergence: ${event.convergenceId}"
            is AdaptationTierChanged ->
                "$prefix Adaptation ${event.affinityId}: T${event.previousTier} -> T${event.newTier}"
            is MutationRolled -> "$prefix Mutation: ${event.mutationId}"
            is SkillEquipped -> "$prefix Skill equipped: ${event.skillId}"
            is SkillCastQueued -> "$prefix Skill queued: ${event.skillId}"
            is SkillCastQueueReplaced ->
                "$prefix Queue replaced: ${event.previousSkillId} -> ${event.newSkillId}"
            is SkillCastQueueDeferred -> "$prefix Queue deferred: ${event.skillId} (${event.reasonCode})"
            is SkillCastQueueConsumed -> "$prefix Queue consumed: ${event.skillId}"
            is SkillCastQueueCleared -> "$prefix Queue cleared: ${event.skillId} (${event.reason})"
            is QuestCompleted -> "$prefix Quest ready: ${event.questId}"
            is QuestRewardClaimed -> "$prefix Quest claimed: ${event.questId}"
            is AchievementCompleted -> "$prefix Achievement ready: ${event.achievementId}"
            is AchievementRewardClaimed -> "$prefix Achievement claimed: ${event.achievementId}"
            is EchoGranted -> "$prefix Echo +${event.amount}"
            is EchoSpent -> "$prefix Echo -${event.amount}: ${event.offerId}"
            is EchoOfferPurchased -> "$prefix Echo offer purchased: ${event.offerId}"
            is DiscoveryUnlocked -> "$prefix Discovery unlocked: ${event.discoveryId}"
            is ChronicleCollapsed ->
                "$prefix Chronicle ${event.completedChronicleNumber} -> ${event.nextChronicleNumber}"
            else -> null
        }
    }

    private fun describeCommandResult(result: CommandResult?): String = when (result) {
        CommandResult.Accepted -> "ACCEPTED"
        is CommandResult.Rejected -> buildString {
            append("REJECTED: ")
            append(result.reason.code)
            result.reason.subjectContentId?.let { append(" content=").append(it) }
            result.reason.subjectInstanceId?.let { append(" instance=").append(it) }
        }
        null -> "none"
    }

    private fun requireAccepted(result: EngineResult, label: String) {
        check(result.commandResult == CommandResult.Accepted) {
            "$label rejected: ${result.commandResult}"
        }
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_SEED: Long = 404L
        private const val MAX_LOG_LINES: Int = 80
        private const val VERIFICATION_INVENTORY_CAPACITY: Long = 10_000L
        private const val MAX_CAPACITY_SCENARIO_MILLIS: Long = 1_200_000L
        private val CAPACITY_SCENARIO_STEP: GameDuration = GameDuration.ofSeconds(10L)

        private data class RuntimeBundle(
            val factory: GameSessionFactory,
            val runtime: GameRuntime
        )

        private fun createTrainingRuntime(seed: Long, profile: SimulatorProfile): RuntimeBundle {
            val balance = when (profile) {
                SimulatorProfile.VERIFICATION_24H -> BalanceConfig(
                    baseInventoryCapacity = VERIFICATION_INVENTORY_CAPACITY,
                    maximumInventoryCapacity = VERIFICATION_INVENTORY_CAPACITY
                )
                SimulatorProfile.NORMAL_CAPACITY -> BalanceConfig()
            }
            val factory = GameSessionFactory.default(
                GameConfig(balance = balance)
            )
            val runtime = GameRuntime(
                initialSession = factory.newGame(seed),
                sessionFactory = factory
            )

            requireAcceptedStatic(
                runtime.dispatch(SelectRegion(DefaultGameContent.TRAINING_HOLLOW_REGION_ID))
            )
            requireAcceptedStatic(
                runtime.dispatch(StartEncounter(DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID))
            )
            return RuntimeBundle(factory, runtime)
        }

        private fun requireAcceptedStatic(result: EngineResult) {
            check(result.commandResult == CommandResult.Accepted) {
                "Simulator setup command rejected: ${result.commandResult}"
            }
        }

        private fun chooseChunkMillis(totalMillis: Long): Long =
            when {
                totalMillis <= 60_000L -> 1_000L
                totalMillis <= 600_000L -> 10_000L
                totalMillis <= 3_600_000L -> 60_000L
                else -> 900_000L
            }

        private fun progressText(processedMillis: Long, totalMillis: Long): String {
            if (totalMillis <= 0L) return "100%"
            val percent = (processedMillis * 100L / totalMillis).coerceIn(0L, 100L)
            return "$percent% (${formatDuration(processedMillis)} / ${formatDuration(totalMillis)})"
        }

        fun formatDuration(millis: Long): String {
            val totalSeconds = millis / 1_000L
            val seconds = totalSeconds % 60L
            val totalMinutes = totalSeconds / 60L
            val minutes = totalMinutes % 60L
            val totalHours = totalMinutes / 60L
            val hours = totalHours % 24L
            val days = totalHours / 24L

            return buildString {
                if (days > 0L) append("${days}d ")
                if (days > 0L || hours > 0L) append("${hours}h ")
                if (days > 0L || hours > 0L || minutes > 0L) append("${minutes}m ")
                append("${seconds}s")
            }.trim()
        }
    }
}

enum class SimulatorProfile(val label: String) {
    VERIFICATION_24H("Verification 24h / 10,000 slots"),
    NORMAL_CAPACITY("Normal capacity / 60 + 20 overflow")
}

data class SimulationHarnessUiState(
    val running: Boolean,
    val seed: Long,
    val profile: SimulatorProfile,
    val status: String,
    val progressText: String?,
    val error: String?,
    val lastCommandResult: String,
    val canonicalRegression: String?,
    val snapshot: SimulationSnapshot,
    val metrics: SimulationMetrics,
    val recentLog: List<String>
)

data class SimulationSnapshot(
    val simulationMillis: Long,
    val gold: GameNumber,
    val playerLevel: Long,
    val playerExperience: GameNumber,
    val inventoryItems: Int,
    val inventoryCapacity: Long,
    val overflowItems: Int,
    val overflowCapacity: Long,
    val inventoryBlocked: Boolean,
    val nextExpansionGoldCost: String,
    val activeRegion: String,
    val encounterIndex: Long?,
    val enemyDefinitionId: String,
    val enemyHealth: GameNumber,
    val enemyMutations: String,
    val resonanceCharges: String,
    val adaptationTiers: String,
    val equippedSkills: String,
    val queuedSkill: String,
    val firstHuntCompletionCount: GameNumber,
    val firstHuntClaimedCount: GameNumber,
    val firstHuntReadyToClaim: Boolean,
    val forgedFlameAchievementCompleted: Boolean,
    val forgedFlameRewardClaimed: Boolean,
    val forgedFlameReadyToClaim: Boolean,
    val chronicleNumber: Long,
    val echoAvailable: GameNumber,
    val echoSpent: GameNumber,
    val purchasedEchoOffers: String,
    val adaptationForecastUnlocked: Boolean
)

data class SimulationMetrics(
    val enemiesDefeated: Long = 0L,
    val encountersCleared: Long = 0L,
    val goldGranted: GameNumber = GameNumber.ZERO,
    val experienceGranted: GameNumber = GameNumber.ZERO,
    val itemsFound: Long = 0L,
    val normalItemsAdded: Long = 0L,
    val overflowItemsAdded: Long = 0L,
    val convergencesTriggered: Long = 0L,
    val adaptationTierChanges: Long = 0L,
    val questClaims: Long = 0L,
    val achievementClaims: Long = 0L,
    val echoPurchases: Long = 0L,
    val capacityExpansions: Long = 0L,
    val progressionBlocks: Long = 0L
)
