package com.idlerpg.game.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.presentation.doctrine.DoctrineDraftAction
import com.idlerpg.game.presentation.doctrine.DoctrineDraftController
import com.idlerpg.game.presentation.doctrine.DoctrineDraftConversionResult
import com.idlerpg.game.presentation.event.GameEventPresenter
import com.idlerpg.game.presentation.intent.BattleUiIntent
import com.idlerpg.game.presentation.intent.DoctrineUiIntent
import com.idlerpg.game.presentation.intent.GearUiIntent
import com.idlerpg.game.presentation.intent.ProgressUiIntent
import com.idlerpg.game.presentation.intent.SkillLoadoutUiIntent
import com.idlerpg.game.presentation.intent.WorldUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand
import com.idlerpg.game.presentation.model.BattleFeedbackUiState
import com.idlerpg.game.presentation.model.BattlePresentationRewardUiState
import com.idlerpg.game.presentation.model.ChroniclePreviewUiState
import com.idlerpg.game.presentation.model.DoctrineFeedbackKind
import com.idlerpg.game.presentation.model.DoctrineFeedbackUiState
import com.idlerpg.game.presentation.model.DoctrineRuleDraftUiState
import com.idlerpg.game.presentation.model.GearFeedbackUiState
import com.idlerpg.game.presentation.model.ProgressFeedbackUiState
import com.idlerpg.game.presentation.model.SkillLoadoutFeedbackUiState
import com.idlerpg.game.presentation.model.WorldFeedbackUiState
import com.idlerpg.game.presentation.projection.BattleProjector
import com.idlerpg.game.presentation.projection.DoctrineProjector
import com.idlerpg.game.presentation.projection.GlobalHudProjector
import com.idlerpg.game.presentation.projection.OfflineProgressProjector
import com.idlerpg.game.presentation.projection.GearProjector
import com.idlerpg.game.presentation.projection.ProgressProjector
import com.idlerpg.game.presentation.projection.SkillLoadoutProjector
import com.idlerpg.game.presentation.projection.WorldProjector
import com.idlerpg.game.presentation.runtime.GameRuntimeController
import com.idlerpg.game.presentation.runtime.GameRuntimeHostState
import com.idlerpg.game.presentation.runtime.RuntimeFailureKind
import com.idlerpg.game.presentation.runtime.RuntimeHostStatus
import com.idlerpg.game.presentation.runtime.RuntimeSaveStatus
import com.idlerpg.game.ui.navigation.GameDestination
import com.idlerpg.game.ui.navigation.ProgressiveDisclosurePolicy
import com.idlerpg.game.ui.navigation.ProgressDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Production presentation adapter. It owns navigation/drafts/effects, never gameplay truth. */
class IdleRpgViewModel(
    private val runtimeController: GameRuntimeController,
    private val globalHudProjector: GlobalHudProjector,
    private val battleProjector: BattleProjector,
    private val skillLoadoutProjector: SkillLoadoutProjector,
    private val worldProjector: WorldProjector,
    private val doctrineProjector: DoctrineProjector,
    private val gearProjector: GearProjector,
    private val progressProjector: ProgressProjector,
    private val offlineProgressProjector: OfflineProgressProjector,
    private val gameEventPresenter: GameEventPresenter
) : ViewModel() {
    private val destination = MutableStateFlow(GameDestination.BATTLE)
    private val progressDestination = MutableStateFlow(ProgressDestination.OVERVIEW)
    private val skillLoadoutOpen = MutableStateFlow(false)
    private val gearFocusItemId = MutableStateFlow<InstanceId?>(null)
    private val doctrineDraft = MutableStateFlow<DoctrineRuleDraftUiState?>(null)
    private val doctrineMutationPending = MutableStateFlow(false)
    private val latestBattleFeedback = MutableStateFlow<BattleFeedbackUiState?>(null)
    /**
     * Presentation-only reward feed. The canonical runtime continues advancing normally; this
     * acknowledged list prevents rapid foreground transitions from replacing every reward float
     * before it can be read.
     */
    private val battlePresentationRewards =
        MutableStateFlow<List<BattlePresentationRewardUiState>>(emptyList())
    private val battlePresentationSeenSequences = linkedSetOf<Long>()
    private var nextBattlePresentationToken: Long = 1L
    private var battlePresentationCombatSequenceId: Long? = null
    private val latestSkillLoadoutFeedback =
        MutableStateFlow<SkillLoadoutFeedbackUiState?>(null)
    private val latestWorldFeedback = MutableStateFlow<WorldFeedbackUiState?>(null)
    private val latestDoctrineFeedback = MutableStateFlow<DoctrineFeedbackUiState?>(null)
    private val latestGearFeedback = MutableStateFlow<GearFeedbackUiState?>(null)
    private val latestProgressFeedback = MutableStateFlow<ProgressFeedbackUiState?>(null)
    private val latestChroniclePreview = MutableStateFlow<ChroniclePreviewUiState?>(null)
    private val chroniclePreviewRequestPending = MutableStateFlow(false)
    private val chronicleCommitPending = MutableStateFlow(false)
    /** Destination restored when the nested Skills route is closed. ViewModel scope survives rotation. */
    private var skillLoadoutOrigin: GameDestination? = null
    private var doctrineDraftCommitPending: Boolean = false

    private val _uiState = MutableStateFlow(
        runtimeController.state.value.toUiState(
            selection = PresentationSelection(
                destination = destination.value,
                progressDestination = progressDestination.value,
                skillLoadoutOpen = skillLoadoutOpen.value,
                gearFocusItemId = gearFocusItemId.value,
                doctrineDraft = doctrineDraft.value,
                doctrineMutationPending = doctrineMutationPending.value,
                battleFeedback = latestBattleFeedback.value,
                battlePresentationRewards = battlePresentationRewards.value,
                skillLoadoutFeedback = latestSkillLoadoutFeedback.value,
                worldFeedback = latestWorldFeedback.value,
                doctrineFeedback = latestDoctrineFeedback.value,
                gearFeedback = latestGearFeedback.value,
                progressFeedback = latestProgressFeedback.value,
                chroniclePreview = latestChroniclePreview.value,
                chroniclePreviewRequestPending = chroniclePreviewRequestPending.value,
                chronicleCommitPending = chronicleCommitPending.value
            )
        )
    )
    val uiState: StateFlow<IdleRpgUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runtimeController.transitions.collect { transition ->
                if (doctrineMutationPending.value && transition.commandResult != null) {
                    doctrineMutationPending.value = false
                }
                val resolvedNavigation = navigationAvailability(transition.state).resolve(
                    destination.value,
                    progressDestination.value
                )
                if (resolvedNavigation.destination != destination.value) {
                    val previousDestination = destination.value
                    destination.value = resolvedNavigation.destination
                    if (previousDestination == GameDestination.PROGRESS) {
                        progressDestination.value = ProgressDestination.OVERVIEW
                    }
                    if (resolvedNavigation.destination != GameDestination.GEAR) {
                        clearSkillLoadoutState()
                    }
                }
                if (resolvedNavigation.progressDestination != progressDestination.value) {
                    progressDestination.value = resolvedNavigation.progressDestination
                }
                val effectiveDestination = resolvedNavigation.destination
                when {
                    effectiveDestination == GameDestination.GEAR && skillLoadoutOpen.value -> {
                        val feedback = gameEventPresenter.presentSkillLoadout(transition)
                        if (feedback != null) {
                            latestSkillLoadoutFeedback.value = feedback
                        }
                    }
                    effectiveDestination == GameDestination.BATTLE -> {
                        prepareBattlePresentationSequence(
                            transition.state.run.combat.combatSequenceId
                        )
                        val feedback = gameEventPresenter.presentBattle(transition)
                        if (feedback != null) {
                            enqueueBattlePresentationReward(feedback)
                            latestBattleFeedback.value = feedback
                        }
                    }
                    effectiveDestination == GameDestination.WORLD -> {
                        val feedback = gameEventPresenter.presentWorld(transition)
                        if (feedback != null) {
                            latestWorldFeedback.value = feedback
                        }
                    }
                    effectiveDestination == GameDestination.GEAR -> {
                        val feedback = gameEventPresenter.presentGear(transition)
                        if (feedback != null) {
                            latestGearFeedback.value = feedback
                        }
                    }
                    effectiveDestination == GameDestination.PROGRESS -> {
                        val preview = gameEventPresenter.presentChroniclePreview(transition)
                        val feedback = gameEventPresenter.presentProgress(transition)
                        if (feedback != null) {
                            latestProgressFeedback.value = feedback
                        }

                        if (chroniclePreviewRequestPending.value &&
                            transition.commandResult != null
                        ) {
                            chroniclePreviewRequestPending.value = false
                            if (preview != null) {
                                latestChroniclePreview.value = preview
                            } else {
                                latestChroniclePreview.value = null
                            }
                        } else if (preview != null) {
                            // Defensive capture for any canonical preview transition produced
                            // while Progress is visible. Foreground should already be paused.
                            latestChroniclePreview.value = preview
                        }

                        if (chronicleCommitPending.value && transition.commandResult != null) {
                            chronicleCommitPending.value = false
                            latestChroniclePreview.value = null
                            if (transition.events.any { it.event is com.idlerpg.game.domain.event.ChronicleCollapsed }) {
                                // Chronicle is a high-value destructive transition; checkpoint
                                // only after its accepted canonical transition is complete.
                                runtimeController.save()
                            }
                        }
                    }
                    effectiveDestination == GameDestination.DOCTRINE -> {
                        val feedback = gameEventPresenter.presentDoctrine(transition)
                        if (feedback != null) {
                            latestDoctrineFeedback.value = feedback
                        }
                        if (doctrineDraftCommitPending) {
                            when {
                                transition.commandResult is CommandResult.Rejected -> {
                                    doctrineDraftCommitPending = false
                                }
                                feedback?.kind == DoctrineFeedbackKind.DOCTRINE_UPDATED -> {
                                    doctrineDraftCommitPending = false
                                    doctrineDraft.value = null
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            val battleFeedbackFlow = combine(
                latestBattleFeedback,
                battlePresentationRewards
            ) { battleFeedback, presentationRewards ->
                BattleFeedbackSelection(
                    battleFeedback = battleFeedback,
                    battlePresentationRewards = presentationRewards
                )
            }

            val baseFeedbackFlow = combine(
                battleFeedbackFlow,
                latestSkillLoadoutFeedback,
                latestWorldFeedback,
                latestDoctrineFeedback,
                latestGearFeedback
            ) { battleSelection, loadoutFeedback, worldFeedback, doctrineFeedback, gearFeedback ->
                FeedbackSelection(
                    battleFeedback = battleSelection.battleFeedback,
                    battlePresentationRewards = battleSelection.battlePresentationRewards,
                    skillLoadoutFeedback = loadoutFeedback,
                    worldFeedback = worldFeedback,
                    doctrineFeedback = doctrineFeedback,
                    gearFeedback = gearFeedback,
                    progressFeedback = null
                )
            }

            val feedbackFlow = combine(
                baseFeedbackFlow,
                latestProgressFeedback
            ) { baseFeedback, progressFeedback ->
                baseFeedback.copy(progressFeedback = progressFeedback)
            }

            val chronicleSelectionFlow = combine(
                latestChroniclePreview,
                chroniclePreviewRequestPending,
                chronicleCommitPending
            ) { preview, previewPending, commitPending ->
                ChronicleSelection(
                    preview = preview,
                    previewRequestPending = previewPending,
                    commitPending = commitPending
                )
            }

            val coreSelectionFlow = combine(
                destination,
                progressDestination,
                skillLoadoutOpen,
                doctrineDraft,
                doctrineMutationPending
            ) { selectedDestination, selectedProgressDestination, loadoutOpen, draft, mutationPending ->
                CoreSelection(
                    destination = selectedDestination,
                    progressDestination = selectedProgressDestination,
                    skillLoadoutOpen = loadoutOpen,
                    doctrineDraft = draft,
                    doctrineMutationPending = mutationPending
                )
            }

            val baseSelectionFlow = combine(
                coreSelectionFlow,
                feedbackFlow
            ) { core, feedback ->
                PresentationSelection(
                    destination = core.destination,
                    progressDestination = core.progressDestination,
                    skillLoadoutOpen = core.skillLoadoutOpen,
                    gearFocusItemId = null,
                    doctrineDraft = core.doctrineDraft,
                    doctrineMutationPending = core.doctrineMutationPending,
                    battleFeedback = feedback.battleFeedback,
                    battlePresentationRewards = feedback.battlePresentationRewards,
                    skillLoadoutFeedback = feedback.skillLoadoutFeedback,
                    worldFeedback = feedback.worldFeedback,
                    doctrineFeedback = feedback.doctrineFeedback,
                    gearFeedback = feedback.gearFeedback,
                    progressFeedback = feedback.progressFeedback,
                    chroniclePreview = null,
                    chroniclePreviewRequestPending = false,
                    chronicleCommitPending = false
                )
            }

            val selectionFlow = combine(
                baseSelectionFlow,
                chronicleSelectionFlow,
                gearFocusItemId
            ) { base, chronicle, focusedItemId ->
                base.copy(
                    gearFocusItemId = focusedItemId,
                    chroniclePreview = chronicle.preview,
                    chroniclePreviewRequestPending = chronicle.previewRequestPending,
                    chronicleCommitPending = chronicle.commitPending
                )
            }

            combine(
                runtimeController.state,
                selectionFlow
            ) { hostState, selection ->
                hostState.toUiState(selection)
            }.collect { projected ->
                _uiState.value = projected
            }
        }
    }

    fun retryInitialization() {
        runtimeController.retryInitialization()
    }

    fun continueGame() {
        runtimeController.continueGame()
    }

    fun startNewGame(heroName: String) {
        runtimeController.startNewGameWithName(heroName)
    }

    fun setHeroName(heroName: String) {
        runtimeController.setHeroName(heroName)
    }

    fun saveNow() {
        runtimeController.save()
    }

    /** Removes a reward only after its Battle presentation has finished rendering. */
    fun consumeBattlePresentationReward(token: Long) {
        battlePresentationRewards.value = battlePresentationRewards.value.filterNot {
            it.token == token
        }
    }

    fun retrySave() {
        runtimeController.save()
    }

    fun acknowledgeOfflineSummary() {
        runtimeController.acknowledgeOfflineSummary()
    }

    fun retryBackgroundResume() {
        runtimeController.retryBackgroundResume()
    }

    fun continueWithoutBackgroundProgress() {
        runtimeController.continueWithoutBackgroundProgress()
    }

    /** Genuine backgrounding invalidates any displayed Chronicle preview token. */
    fun onAppBackgrounded() {
        dismissChroniclePreview()
    }

    fun clearOperationError() {
        runtimeController.clearOperationError()
    }

    fun selectDestination(selected: GameDestination) {
        val effective = currentNavigationAvailability()
            .resolve(selected, progressDestination.value)
            .destination
        val previousDestination = destination.value
        val loadoutWasOpen = skillLoadoutOpen.value
        if (effective != GameDestination.PROGRESS) {
            dismissChroniclePreview()
            if (previousDestination == GameDestination.PROGRESS) {
                progressDestination.value = ProgressDestination.OVERVIEW
            }
        }
        destination.value = effective
        // Selecting Build while its nested Skills route is open is an explicit request for the
        // Build root. Any other destination also tears down the nested route.
        if (effective != GameDestination.GEAR || loadoutWasOpen) {
            clearSkillLoadoutState()
        }
        if (effective != GameDestination.GEAR) {
            gearFocusItemId.value = null
        }
        if (effective != GameDestination.BATTLE) {
            clearBattlePresentationState()
        }
        if (effective != GameDestination.WORLD) {
            latestWorldFeedback.value = null
        }
        if (effective != GameDestination.DOCTRINE) {
            doctrineDraft.value = null
            doctrineDraftCommitPending = false
            latestDoctrineFeedback.value = null
        }
        if (effective != GameDestination.GEAR) {
            latestGearFeedback.value = null
        }
        if (effective != GameDestination.PROGRESS) {
            latestProgressFeedback.value = null
        }
    }

    fun selectProgressDestination(selected: ProgressDestination) {
        val resolved = currentNavigationAvailability().resolve(
            GameDestination.PROGRESS,
            selected
        )
        if (resolved.destination != GameDestination.PROGRESS) {
            progressDestination.value = ProgressDestination.OVERVIEW
            selectDestination(GameDestination.BATTLE)
            return
        }
        val effective = resolved.progressDestination
        if (effective != ProgressDestination.CHRONICLE) {
            dismissChroniclePreview()
        }
        progressDestination.value = effective
        destination.value = GameDestination.PROGRESS
        clearSkillLoadoutState()
        doctrineDraft.value = null
        doctrineDraftCommitPending = false
        clearBattlePresentationState()
        latestSkillLoadoutFeedback.value = null
        latestWorldFeedback.value = null
        latestDoctrineFeedback.value = null
        latestGearFeedback.value = null
        latestProgressFeedback.value = null
    }

    fun openSkillLoadout() {
        if (skillLoadoutOpen.value) {
            return
        }
        val availability = currentNavigationAvailability()
        if (!availability.allows(GameDestination.GEAR)) {
            return
        }
        val origin = when (destination.value) {
            GameDestination.BATTLE,
            GameDestination.GEAR -> destination.value
            else -> GameDestination.BATTLE
        }
        selectDestination(GameDestination.GEAR)
        skillLoadoutOrigin = origin
        doctrineDraft.value = null
        doctrineDraftCommitPending = false
        clearBattlePresentationState()
        latestSkillLoadoutFeedback.value = null
        latestWorldFeedback.value = null
        latestDoctrineFeedback.value = null
        latestGearFeedback.value = null
        latestProgressFeedback.value = null
        skillLoadoutOpen.value = true
    }

    fun closeSkillLoadout() {
        val returnDestination = skillLoadoutOrigin ?: GameDestination.GEAR
        clearSkillLoadoutState()
        if (destination.value != returnDestination) {
            selectDestination(returnDestination)
        }
    }

    fun openGear(focusItemId: InstanceId? = null) {
        selectDestination(GameDestination.GEAR)
        gearFocusItemId.value = focusItemId
    }

    fun clearGearFocus() {
        gearFocusItemId.value = null
    }

    fun handleBattleIntent(intent: BattleUiIntent) {
        runtimeController.dispatchCorrelated { correlationId ->
            intent.toGameCommand(correlationId)
        }
    }

    fun handleSkillLoadoutIntent(intent: SkillLoadoutUiIntent) {
        runtimeController.dispatchCorrelated { correlationId ->
            intent.toGameCommand(correlationId)
        }
    }

    fun handleWorldIntent(intent: WorldUiIntent) {
        runtimeController.dispatchCorrelated { correlationId ->
            intent.toGameCommand(correlationId)
        }
    }

    fun handleGearIntent(intent: GearUiIntent) {
        runtimeController.dispatchCorrelated { correlationId ->
            intent.toGameCommand(correlationId)
        }
    }

    fun handleProgressIntent(intent: ProgressUiIntent) {
        when (intent) {
            ProgressUiIntent.RequestChronicle -> requestChroniclePreview()
            ProgressUiIntent.ConfirmChronicleCollapse -> commitChroniclePreview()
            else -> runtimeController.dispatchCorrelated { correlationId ->
                intent.toGameCommand(correlationId)
            }
        }
    }

    fun dismissChroniclePreview() {
        if (chronicleCommitPending.value) {
            return
        }
        val hadSession = latestChroniclePreview.value != null ||
            chroniclePreviewRequestPending.value
        latestChroniclePreview.value = null
        chroniclePreviewRequestPending.value = false
        if (hadSession) {
        }
    }

    private fun requestChroniclePreview() {
        if (chroniclePreviewRequestPending.value || chronicleCommitPending.value ||
            latestChroniclePreview.value != null
        ) {
            return
        }
        chroniclePreviewRequestPending.value = true
        latestProgressFeedback.value = null
        runtimeController.dispatchCorrelated { correlationId ->
            ProgressUiIntent.RequestChronicle.toGameCommand(correlationId)
        }
    }

    private fun commitChroniclePreview() {
        if (chronicleCommitPending.value || chroniclePreviewRequestPending.value) {
            return
        }
        val preview = latestChroniclePreview.value ?: return
        chronicleCommitPending.value = true
        runtimeController.dispatchCorrelated { correlationId ->
            ProgressUiIntent.ConfirmChronicleCollapse.toGameCommand(
                correlationId = correlationId,
                chroniclePreviewToken = preview.previewToken
            )
        }
    }

    fun setDoctrineEnabled(enabled: Boolean) {
        if (doctrineMutationPending.value) return
        dispatchDoctrineIntent(DoctrineUiIntent.SetDoctrineEnabled(enabled))
    }
    fun applyDoctrinePreset(preset: com.idlerpg.game.domain.command.DoctrinePreset) = dispatchDoctrineIntent(DoctrineUiIntent.ApplyPreset(preset))

    fun setDoctrineRuleEnabled(ruleId: com.idlerpg.game.core.id.InstanceId, enabled: Boolean) {
        if (doctrineMutationPending.value) return
        dispatchDoctrineIntent(DoctrineUiIntent.SetRuleEnabled(ruleId, enabled))
    }

    fun moveDoctrineRule(ruleId: com.idlerpg.game.core.id.InstanceId, newIndex: Int) {
        if (doctrineMutationPending.value) return
        dispatchDoctrineIntent(DoctrineUiIntent.MoveRule(ruleId, newIndex))
    }

    fun removeDoctrineRule(ruleId: com.idlerpg.game.core.id.InstanceId) {
        if (doctrineMutationPending.value) return
        dispatchDoctrineIntent(DoctrineUiIntent.RemoveRule(ruleId))
    }

    fun beginAddDoctrineRule() {
        if (doctrineMutationPending.value) return
        if (_uiState.value.doctrine?.capacityFull == true) {
            return
        }
        doctrineDraftCommitPending = false
        doctrineDraft.value = DoctrineDraftController.newDraft()
    }

    fun beginEditDoctrineRule(ruleId: com.idlerpg.game.core.id.InstanceId) {
        if (doctrineMutationPending.value) return
        val rule = runtimeController.state.value.snapshot
            ?.run
            ?.doctrine
            ?.rules
            ?.firstOrNull { it.instanceId == ruleId }
            ?: return
        doctrineDraftCommitPending = false
        doctrineDraft.value = DoctrineDraftController.fromRule(rule)
    }

    fun updateDoctrineDraft(action: DoctrineDraftAction) {
        if (doctrineMutationPending.value) return
        val current = doctrineDraft.value ?: return
        val doctrineState = _uiState.value.doctrine ?: return
        doctrineDraft.value = DoctrineDraftController.reduce(
            draft = current,
            action = action,
            maximumDepth = doctrineState.conditionMaxDepth,
            sequenceCapacity = doctrineState.sequenceCapacity
        )
    }

    fun cancelDoctrineDraft() {
        if (doctrineMutationPending.value) return
        doctrineDraftCommitPending = false
        doctrineDraft.value = null
    }

    fun commitDoctrineDraft() {
        if (doctrineMutationPending.value) return
        if (doctrineDraftCommitPending) {
            return
        }
        val current = doctrineDraft.value ?: return
        val doctrineState = _uiState.value.doctrine ?: return
        when (
            val converted = DoctrineDraftController.toDomain(
                draft = current,
                maximumDepth = doctrineState.conditionMaxDepth,
                sequenceCapacity = doctrineState.sequenceCapacity
            )
        ) {
            is DoctrineDraftConversionResult.Invalid -> {
                doctrineDraft.value = DoctrineDraftController.withError(
                    current,
                    converted.error
                )
            }
            is DoctrineDraftConversionResult.Ready -> {
                val value = converted.value
                val ruleId = value.ruleId
                val intent = if (ruleId == null) {
                    DoctrineUiIntent.AddRule(
                        condition = value.condition,
                        action = value.action,
                        enabled = value.enabled
                    )
                } else {
                    DoctrineUiIntent.ReplaceRule(
                        ruleId = ruleId,
                        condition = value.condition,
                        action = value.action,
                        enabled = value.enabled
                    )
                }
                doctrineDraftCommitPending = true
                dispatchDoctrineIntent(intent)
            }
        }
    }

    private fun dispatchDoctrineIntent(intent: DoctrineUiIntent) {
        if (doctrineMutationPending.value) return
        val snapshot = runtimeController.state.value.snapshot ?: return
        doctrineMutationPending.value = true
        runtimeController.dispatchCorrelated { correlationId ->
            intent.toGameCommand(
                correlationId = correlationId,
                currentState = snapshot
            )
        }
    }

    private fun GameRuntimeHostState.toUiState(
        selection: PresentationSelection
    ): IdleRpgUiState {
        val gameState = snapshot
        val offline = offlineSummary
        val availability = navigationAvailability(gameState)
        val resolvedSelection = availability.resolve(
            selection.destination,
            selection.progressDestination
        )
        val effectiveDestination = resolvedSelection.destination
        val effectiveProgressDestination = resolvedSelection.progressDestination
        val loadoutVisible =
            effectiveDestination == GameDestination.GEAR && selection.skillLoadoutOpen

        return IdleRpgUiState(
            status = when (status) {
                RuntimeHostStatus.BOOTING -> IdleRpgAppStatus.BOOTING
                RuntimeHostStatus.NEW_GAME -> IdleRpgAppStatus.NEW_GAME
                RuntimeHostStatus.LOADING -> IdleRpgAppStatus.LOADING
                RuntimeHostStatus.RESUMING_OFFLINE -> IdleRpgAppStatus.RESUMING_OFFLINE
                RuntimeHostStatus.MENU -> IdleRpgAppStatus.MENU
                RuntimeHostStatus.NAME_REQUIRED -> IdleRpgAppStatus.NAME_REQUIRED
                RuntimeHostStatus.READY -> IdleRpgAppStatus.READY
                RuntimeHostStatus.ERROR -> IdleRpgAppStatus.ERROR
            },
            saveStatus = when (saveStatus) {
                RuntimeSaveStatus.IDLE -> IdleRpgSaveStatus.IDLE
                RuntimeSaveStatus.SAVING -> IdleRpgSaveStatus.SAVING
                RuntimeSaveStatus.SAVED -> IdleRpgSaveStatus.SAVED
                RuntimeSaveStatus.ERROR -> IdleRpgSaveStatus.ERROR
            },
            hasExistingSave = hasExistingSave,
            destination = effectiveDestination,
            progressDestination = effectiveProgressDestination,
            navigationAvailability = availability,
            globalHud = gameState?.let { globalHudProjector.project(it, saveStatus) },
            battle = if (
                effectiveDestination == GameDestination.BATTLE &&
                !loadoutVisible
            ) {
                gameState?.let {
                    battleProjector.project(
                        state = it,
                        feedback = selection.battleFeedback,
                        doctrineFeedback = selection.doctrineFeedback,
                        presentationRewards = selection.battlePresentationRewards
                    )
                }
            } else {
                null
            },
            skillLoadoutOpen = loadoutVisible,
            skillLoadout = if (loadoutVisible) {
                gameState?.let {
                    skillLoadoutProjector.project(
                        state = it,
                        feedback = selection.skillLoadoutFeedback
                    )
                }
            } else {
                null
            },
            world = if (effectiveDestination == GameDestination.WORLD) {
                gameState?.let {
                    worldProjector.project(
                        state = it,
                        feedback = selection.worldFeedback
                    )
                }
            } else {
                null
            },
            doctrine = if (effectiveDestination == GameDestination.DOCTRINE) {
                gameState?.let {
                    doctrineProjector.project(
                        state = it,
                        draft = selection.doctrineDraft,
                        feedback = selection.doctrineFeedback,
                        mutationPending = selection.doctrineMutationPending
                    )
                }
            } else {
                null
            },
            gear = if (effectiveDestination == GameDestination.GEAR) {
                gameState?.let {
                    gearProjector.project(
                        state = it,
                        feedback = selection.gearFeedback
                    )
                }
            } else {
                null
            },
            gearFocusItemId = selection.gearFocusItemId,
            progress = if (effectiveDestination == GameDestination.PROGRESS) {
                gameState?.let {
                    progressProjector.project(
                        state = it,
                        feedback = selection.progressFeedback,
                        chroniclePreview = selection.chroniclePreview,
                        chroniclePreviewRequestPending =
                            selection.chroniclePreviewRequestPending,
                        chronicleCommitPending = selection.chronicleCommitPending
                    )
                }
            } else {
                null
            },
            simulationTimeMillis = gameState?.engine?.simulationTime?.millis,
            offlineSummary = offline?.let(offlineProgressProjector::project),
            runtimeFailure = failure?.let { runtimeFailure ->
                RuntimeFailureUiState(
                    kind = when (runtimeFailure.kind) {
                        RuntimeFailureKind.INITIALIZATION ->
                            IdleRpgRuntimeFailureKind.INITIALIZATION
                        RuntimeFailureKind.BACKGROUND_RESUME ->
                            IdleRpgRuntimeFailureKind.BACKGROUND_RESUME
                        RuntimeFailureKind.SAVE ->
                            IdleRpgRuntimeFailureKind.SAVE
                        RuntimeFailureKind.SIMULATION ->
                            IdleRpgRuntimeFailureKind.SIMULATION
                        RuntimeFailureKind.COMMAND ->
                            IdleRpgRuntimeFailureKind.COMMAND
                    },
                    diagnosticMessage = runtimeFailure.diagnosticMessage
                )
            },
            canRetryInitialization = canRetryInitialization
        )
    }

    private fun currentNavigationAvailability() =
        navigationAvailability(runtimeController.state.value.snapshot)

    private fun navigationAvailability(
        state: com.idlerpg.game.domain.model.GameState?
    ): com.idlerpg.game.ui.navigation.NavigationAvailability {
        val withoutAffordability = ProgressiveDisclosurePolicy.project(state)
        if (state == null || withoutAffordability.allows(GameDestination.PROGRESS)) {
            return withoutAffordability
        }
        return ProgressiveDisclosurePolicy.project(
            state = state,
            coreGrowthAffordable = progressProjector.hasAffordableCoreGrowth(state)
        )
    }

    private fun clearSkillLoadoutState() {
        skillLoadoutOpen.value = false
        skillLoadoutOrigin = null
        latestSkillLoadoutFeedback.value = null
    }

    private fun enqueueBattlePresentationReward(
        feedback: BattleFeedbackUiState
    ) {
        val killReward = feedback.killReward ?: return

        feedback.sequenceNumber?.let { sequenceNumber ->
            if (!battlePresentationSeenSequences.add(sequenceNumber)) {
                return
            }
        }

        val token = nextBattlePresentationToken
        check(token > 0L) { "Battle presentation token space exhausted" }
        nextBattlePresentationToken += 1L
        val next = battlePresentationRewards.value + BattlePresentationRewardUiState(
            token = token,
            sequenceNumber = feedback.sequenceNumber,
            defeatedEnemyInstanceIds = killReward.defeatedEnemyInstanceIds,
            reward = killReward.reward
        )
        // Items leave this transient feed only after the Battle animation acknowledges them.
        // Never discard an unrendered canonical reward when simulation outpaces presentation.
        battlePresentationRewards.value = next
    }

    private fun prepareBattlePresentationSequence(combatSequenceId: Long) {
        if (battlePresentationCombatSequenceId == null) {
            battlePresentationCombatSequenceId = combatSequenceId
        } else if (battlePresentationCombatSequenceId != combatSequenceId) {
            battlePresentationRewards.value = emptyList()
            battlePresentationSeenSequences.clear()
            battlePresentationCombatSequenceId = combatSequenceId
        }
    }

    private fun clearBattlePresentationState() {
        latestBattleFeedback.value = null
        battlePresentationRewards.value = emptyList()
        battlePresentationSeenSequences.clear()
        battlePresentationCombatSequenceId = null
    }

    private data class FeedbackSelection(
        val battleFeedback: BattleFeedbackUiState?,
        val battlePresentationRewards: List<BattlePresentationRewardUiState>,
        val skillLoadoutFeedback: SkillLoadoutFeedbackUiState?,
        val worldFeedback: WorldFeedbackUiState?,
        val doctrineFeedback: DoctrineFeedbackUiState?,
        val gearFeedback: GearFeedbackUiState?,
        val progressFeedback: ProgressFeedbackUiState?
    )

    private data class BattleFeedbackSelection(
        val battleFeedback: BattleFeedbackUiState?,
        val battlePresentationRewards: List<BattlePresentationRewardUiState>
    )

    private data class CoreSelection(
        val destination: GameDestination,
        val progressDestination: ProgressDestination,
        val skillLoadoutOpen: Boolean,
        val doctrineDraft: DoctrineRuleDraftUiState?,
        val doctrineMutationPending: Boolean
    )

    private data class ChronicleSelection(
        val preview: ChroniclePreviewUiState?,
        val previewRequestPending: Boolean,
        val commitPending: Boolean
    )

    private data class PresentationSelection(
        val destination: GameDestination,
        val progressDestination: ProgressDestination,
        val skillLoadoutOpen: Boolean,
        val gearFocusItemId: InstanceId?,
        val doctrineDraft: DoctrineRuleDraftUiState?,
        val doctrineMutationPending: Boolean,
        val battleFeedback: BattleFeedbackUiState?,
        val battlePresentationRewards: List<BattlePresentationRewardUiState>,
        val skillLoadoutFeedback: SkillLoadoutFeedbackUiState?,
        val worldFeedback: WorldFeedbackUiState?,
        val doctrineFeedback: DoctrineFeedbackUiState?,
        val gearFeedback: GearFeedbackUiState?,
        val progressFeedback: ProgressFeedbackUiState?,
        val chroniclePreview: ChroniclePreviewUiState?,
        val chroniclePreviewRequestPending: Boolean,
        val chronicleCommitPending: Boolean
    )

    companion object {
        fun factory(
            runtimeController: GameRuntimeController,
            globalHudProjector: GlobalHudProjector,
            battleProjector: BattleProjector,
            skillLoadoutProjector: SkillLoadoutProjector,
            worldProjector: WorldProjector,
            doctrineProjector: DoctrineProjector,
            gearProjector: GearProjector,
            progressProjector: ProgressProjector,
            offlineProgressProjector: OfflineProgressProjector,
            gameEventPresenter: GameEventPresenter
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(IdleRpgViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return IdleRpgViewModel(
                    runtimeController = runtimeController,
                    globalHudProjector = globalHudProjector,
                    battleProjector = battleProjector,
                    skillLoadoutProjector = skillLoadoutProjector,
                    worldProjector = worldProjector,
                    doctrineProjector = doctrineProjector,
                    gearProjector = gearProjector,
                    progressProjector = progressProjector,
                    offlineProgressProjector = offlineProgressProjector,
                    gameEventPresenter = gameEventPresenter
                ) as T
            }
        }
    }
}
