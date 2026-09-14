package com.idlerpg.game.domain.system.world

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.RetreatEncounter
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.command.DeployStartingEncounter
import com.idlerpg.game.domain.command.WorldCommand
import com.idlerpg.game.domain.command.ConfigureWorldAutomation
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.engine.ScheduledAction
import com.idlerpg.game.domain.engine.ScheduledActionExecution
import com.idlerpg.game.domain.engine.ScheduledActionHandler
import com.idlerpg.game.domain.engine.ScheduledActionSource
import com.idlerpg.game.domain.engine.ScheduledActionType
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.system.combat.CombatSystem
import com.idlerpg.game.domain.system.combat.StatusEffectSystem
import com.idlerpg.game.domain.system.inventory.InventoryCapacitySystem

/**
 * High-level Foundation 7 world command and autonomous-lifecycle coordinator.
 *
 * Economy commands remain owned by EconomySystem; a future application CommandDispatcher
 * composes command families. This system composes only autonomous world and combat
 * scheduled actions because encounter continuation owns the combat lifecycle.
 */
object WorldSystem : GameCommandHandler, ScheduledActionHandler {

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult =
        when (command) {
            is SelectRegion ->
                handleSelectRegion(state, command, context)

            is StartEncounter ->
                handleStartEncounter(state, command, context)

            is DeployStartingEncounter ->
                handleDeployStartingEncounter(state, command, context)

            is RetreatEncounter ->
                handleRetreat(state)

            is ConfigureWorldAutomation ->
                handleConfigureAutomation(state, command, context)

            is WorldCommand ->
                rejected(CommandRejectionCode.UNSUPPORTED)

            else ->
                rejected(CommandRejectionCode.UNSUPPORTED)
        }

    /**
     * Creates the pure scheduled-action projection used by SimulationEngine.
     *
     * The immutable registry is captured explicitly; no Android/global mutable state is
     * introduced.
     */
    fun scheduledActionSource(
        contentRegistry: ContentRegistry,
        balanceConfig: BalanceConfig
    ): ScheduledActionSource =
        ScheduledActionSource { state ->
            buildList {
                addAll(
                    EncounterSystem.scheduledActions(
                        state = state,
                        contentRegistry = contentRegistry,
                        balanceConfig = balanceConfig
                    )
                )
                addAll(StatusEffectSystem.scheduledActions(state))
                addAll(CombatSystem.scheduledActions(state, contentRegistry))
            }
        }

    override fun execute(
        state: GameState,
        action: ScheduledAction,
        context: EngineContext
    ): ScheduledActionExecution =
        when (action.type) {
            ScheduledActionType.ENCOUNTER_LIFECYCLE ->
                EncounterSystem.execute(state, action, context)

            ScheduledActionType.STATUS_PERIODIC ->
                StatusEffectSystem.execute(state, action, context)

            ScheduledActionType.PLAYER_DECISION,
            ScheduledActionType.ENEMY_DECISION ->
                CombatSystem.execute(state, action, context)

            else ->
                error(
                    "WorldSystem has no Foundation 7 handler for ${action.type}"
                )
        }

    private fun handleSelectRegion(
        state: GameState,
        command: SelectRegion,
        context: EngineContext
    ): CommandHandlingResult {
        val region =
            context.contentRegistry.regionOrNull(command.regionId)
                ?: return rejected(
                    code = CommandRejectionCode.UNKNOWN_CONTENT,
                    subjectContentId = command.regionId
                )

        val currentEncounter = state.run.world.currentEncounter
        if (currentEncounter?.status == EncounterStatus.ACTIVE ||
            state.run.combat.status == CombatStatus.ACTIVE
        ) {
            return rejected(
                code = CommandRejectionCode.INVALID_STATE,
                subjectContentId = region.id
            )
        }

        val alreadyUnlocked =
            region.id in state.run.world.unlockedRegionIds
        val startingRegion =
            region.id in context.contentRegistry.world.startingRegionIds

        if (!alreadyUnlocked && !startingRegion) {
            return rejected(
                code = CommandRejectionCode.LOCKED,
                subjectContentId = region.id
            )
        }

        val transition = RegionSystem.selectRegion(
            state = state,
            regionId = region.id,
            unlockStartingRegion = !alreadyUnlocked && startingRegion
        )

        return CommandHandlingResult.Accepted(
            state = transition.state,
            events = transition.events
        )
    }

    private fun handleStartEncounter(
        state: GameState,
        command: StartEncounter,
        context: EngineContext
    ): CommandHandlingResult {
        val encounter =
            context.contentRegistry.encounterOrNull(command.encounterId)
                ?: return rejected(
                    code = CommandRejectionCode.UNKNOWN_CONTENT,
                    subjectContentId = command.encounterId
                )

        val activeRegionId =
            state.run.world.activeRegionId
                ?: return rejected(
                    code = CommandRejectionCode.INVALID_STATE,
                    subjectContentId = encounter.id
                )

        if (encounter.regionId != activeRegionId) {
            return rejected(
                code = CommandRejectionCode.INVALID_ARGUMENT,
                subjectContentId = encounter.id
            )
        }

        val region =
            context.contentRegistry.region(activeRegionId)

        if (encounter.id !in region.encounterIds) {
            return rejected(
                code = CommandRejectionCode.INVALID_ARGUMENT,
                subjectContentId = encounter.id
            )
        }

        val farmSelection = state.run.world.automationMode ==
            com.idlerpg.game.domain.model.world.WorldAutomationMode.FARM &&
            state.run.world.selectedFarmEncounterId == encounter.id &&
            encounter.id in state.run.world.clearedEncounterIds
        val currentEncounter = state.run.world.currentEncounter
        if (!farmSelection && currentEncounter != null && currentEncounter.status != EncounterStatus.ACTIVE) {
            val allowedEncounterId = when (currentEncounter.status) {
                EncounterStatus.FAILED, EncounterStatus.RETREATED -> currentEncounter.definitionId
                EncounterStatus.CLEARED -> region.encounterIds
                    .firstOrNull { it !in state.run.world.clearedEncounterIds }
                EncounterStatus.ACTIVE -> null
            }
            if (allowedEncounterId != null && encounter.id != allowedEncounterId) {
                return rejected(
                    code = CommandRejectionCode.LOCKED,
                    subjectContentId = encounter.id
                )
            }
        } else if (!farmSelection && currentEncounter == null) {
            val firstEncounterId = region.encounterIds
                .firstOrNull { it !in state.run.world.clearedEncounterIds }
                ?: region.encounterIds.lastOrNull()
            if (firstEncounterId != null && encounter.id != firstEncounterId) {
                return rejected(
                    code = CommandRejectionCode.LOCKED,
                    subjectContentId = encounter.id
                )
            }
        }

        if (encounter.waves !in 1..10 ||
            (1..encounter.waves).any { encounter.enemyDefinitionIdsForWave(it).size !in 1..5 }
        ) {
            return rejected(
                code = CommandRejectionCode.UNSUPPORTED,
                subjectContentId = encounter.id
            )
        }

        val boss = BossSystem.bossForEncounter(
            encounterDefinition = encounter,
            contentRegistry = context.contentRegistry
        )
        if (boss != null) {
            val progress =
                state.run.world.regionProgressById[activeRegionId]
                    ?: return rejected(
                        code = CommandRejectionCode.INVALID_STATE,
                        subjectContentId = encounter.id
                    )

            if (!BossSystem.isUnlocked(progress, boss)) {
                return rejected(
                    code = CommandRejectionCode.LOCKED,
                    subjectContentId = boss.id
                )
            }
        }

        if (state.run.world.currentEncounter?.status == EncounterStatus.ACTIVE ||
            state.run.combat.status == CombatStatus.ACTIVE
        ) {
            return rejected(
                code = CommandRejectionCode.INVALID_STATE,
                subjectContentId = encounter.id
            )
        }

        if (InventoryCapacitySystem.isProgressionBlocked(
                state.run.inventory,
                context.balanceConfig
            )
        ) {
            return rejected(
                code = CommandRejectionCode.CAPACITY_EXCEEDED,
                subjectContentId = encounter.id
            )
        }

        val transition = EncounterSystem.startEncounter(
            state = state,
            encounterDefinitionId = encounter.id,
            context = context
        )

        return CommandHandlingResult.Accepted(
            state = transition.state,
            events = transition.events
        )
    }

    /**
     * Atomic first deployment used by production new-game bootstrap and the visible recovery CTA.
     * Selection and encounter start reuse their normal domain handlers; a rejected second step
     * commits neither step because GameEngine receives one rejected command result.
     */
    private fun handleDeployStartingEncounter(
        state: GameState,
        command: DeployStartingEncounter,
        context: EngineContext
    ): CommandHandlingResult {
        val regionId = context.contentRegistry.world.startingRegionIds
            .sorted()
            .firstOrNull()
            ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT)
        val region = context.contentRegistry.regionOrNull(regionId)
            ?: return rejected(
                code = CommandRejectionCode.UNKNOWN_CONTENT,
                subjectContentId = regionId
            )
        val encounterId = region.encounterIds
            .firstOrNull { it !in state.run.world.clearedEncounterIds }
            ?: region.encounterIds.lastOrNull()
            ?: return rejected(
                code = CommandRejectionCode.UNKNOWN_CONTENT,
                subjectContentId = regionId
            )

        val selection = handleSelectRegion(
            state = state,
            command = SelectRegion(regionId, command.correlationId),
            context = context
        )
        if (selection is CommandHandlingResult.Rejected) return selection
        selection as CommandHandlingResult.Accepted

        val deployment = handleStartEncounter(
            state = selection.state,
            command = StartEncounter(encounterId, command.correlationId),
            context = context
        )
        return when (deployment) {
            is CommandHandlingResult.Rejected -> deployment
            is CommandHandlingResult.Accepted -> CommandHandlingResult.Accepted(
                state = deployment.state,
                events = selection.events + deployment.events
            )
        }
    }

    private fun handleRetreat(
        state: GameState
    ): CommandHandlingResult {
        val encounter =
            state.run.world.currentEncounter
                ?: return rejected(CommandRejectionCode.INVALID_STATE)

        if (encounter.status != EncounterStatus.ACTIVE ||
            state.run.combat.status != CombatStatus.ACTIVE
        ) {
            return rejected(
                code = CommandRejectionCode.INVALID_STATE,
                subjectContentId = encounter.definitionId
            )
        }

        val transition = EncounterSystem.retreat(state)
        return CommandHandlingResult.Accepted(
            state = transition.state,
            events = transition.events
        )
    }

    private fun handleConfigureAutomation(
        state: GameState,
        command: ConfigureWorldAutomation,
        context: EngineContext
    ): CommandHandlingResult {
        val farmId = command.farmEncounterId
        if (farmId != null) {
            val encounter = context.contentRegistry.encounterOrNull(farmId)
                ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT, farmId)
            val activeRegionId = state.run.world.activeRegionId
                ?: return rejected(CommandRejectionCode.INVALID_STATE, farmId)
            val activeRegion = context.contentRegistry.regionOrNull(activeRegionId)
                ?: return rejected(CommandRejectionCode.INVALID_STATE, farmId)
            if (encounter.regionId != activeRegionId) {
                return rejected(CommandRejectionCode.INVALID_ARGUMENT, farmId)
            }
            if (farmId !in activeRegion.encounterIds) {
                return rejected(CommandRejectionCode.INVALID_ARGUMENT, farmId)
            }
            if (farmId !in state.run.world.clearedEncounterIds) {
                return rejected(CommandRejectionCode.LOCKED, farmId)
            }
        }

        val persistedFarmId = state.run.world.selectedFarmEncounterId
        val selectedFarmId = farmId ?: persistedFarmId
        val validatedFarmId = if (farmId != null) {
            farmId
        } else {
            val activeRegionId = state.run.world.activeRegionId
            val activeRegion = activeRegionId?.let(context.contentRegistry::regionOrNull)
            persistedFarmId?.takeIf { candidate ->
                val encounter = context.contentRegistry.encounterOrNull(candidate)
                encounter != null &&
                    activeRegionId != null &&
                    activeRegion != null &&
                    encounter.regionId == activeRegionId &&
                    candidate in activeRegion.encounterIds &&
                    candidate in state.run.world.clearedEncounterIds
            }
        }

        if (command.mode == com.idlerpg.game.domain.model.world.WorldAutomationMode.FARM &&
            validatedFarmId == null
        ) {
            return rejected(CommandRejectionCode.LOCKED, selectedFarmId)
        }
        val world = state.run.world.copy(
            automationMode = command.mode,
            selectedFarmEncounterId = validatedFarmId,
            pushFailurePolicy = command.failurePolicy
        )
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(world = world)),
            emptyList()
        )
    }

    private fun rejected(
        code: CommandRejectionCode,
        subjectContentId: com.idlerpg.game.core.id.ContentId? = null
    ): CommandHandlingResult.Rejected =
        CommandHandlingResult.Rejected(
            CommandRejectionReason(
                code = code,
                subjectContentId = subjectContentId
            )
        )
}
