package com.idlerpg.game.presentation.runtime

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.SetHeroName
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.model.EngineState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-writer production boundary around [GameRuntime].
 *
 * FUI-10 extends the original runtime host with a lifecycle-safe background checkpoint ->
 * offline-resume bridge. A same-process background interval therefore uses the exact same
 * OfflineSessionCoordinator path as a fresh process launch instead of silently losing time.
 */
class GameRuntimeController(
    private val runtime: GameRuntime,
    private val repository: GameRepository,
    private val executor: ExecutorService = newRuntimeExecutor()
) {
    private val initializationInFlight = AtomicBoolean(false)
    private val nextCorrelationValue = AtomicLong(1L)

    /**
     * True from a genuine Activity background stop until its checkpoint has been consumed by
     * canonical offline resume (or explicitly abandoned after a resume failure).
     */
    private val backgroundTransitionPending = AtomicBoolean(false)
    private val backgroundCheckpointReady = AtomicBoolean(false)

    private val _state = MutableStateFlow(GameRuntimeHostState())
    val state: StateFlow<GameRuntimeHostState> = _state.asStateFlow()

    private val _transitions = MutableSharedFlow<RuntimeTransition>(
        replay = 0,
        extraBufferCapacity = TRANSITION_BUFFER_CAPACITY
    )
    val transitions: SharedFlow<RuntimeTransition> = _transitions.asSharedFlow()

    /** Starts the one process boot/load/offline-resume sequence. Safe to call repeatedly. */
    fun initialize() {
        requestInitialization(forceRetry = false)
    }

    /** Retries only a failed process boot/load/offline-resume sequence. */
    fun retryInitialization() {
        requestInitialization(forceRetry = true)
    }

    /** True while foreground pumping must wait for a background checkpoint/resume barrier. */
    fun backgroundResumePending(): Boolean = backgroundTransitionPending.get()

    /**
     * Resumes a genuine same-process background interval through OfflineSessionCoordinator.
     *
     * The background checkpoint request is always queued first from Activity.onStop, so this
     * request is FIFO behind it on the same worker. No wall-time interval is simulated twice.
     */
    fun resumeAfterBackgroundIfNeeded(): Boolean {
        if (!backgroundTransitionPending.get()) {
            return false
        }

        executor.execute {
            if (!backgroundTransitionPending.get()) {
                return@execute
            }
            if (!backgroundCheckpointReady.get()) {
                // A failed checkpoint clears pending. Reaching this branch means the lifecycle
                // contract was violated; do not guess or load an older save over live state.
                publishBackgroundResumeFailure(
                    IllegalStateException("Background checkpoint was not ready for resume")
                )
                return@execute
            }
            resumeBackgroundInternal()
        }
        return true
    }

    fun retryBackgroundResume() {
        executor.execute {
            val current = _state.value
            if (current.failure?.kind != RuntimeFailureKind.BACKGROUND_RESUME ||
                !backgroundTransitionPending.get() ||
                !backgroundCheckpointReady.get()
            ) {
                return@execute
            }
            resumeBackgroundInternal()
        }
    }

    /**
     * Explicitly abandons one failed same-process offline interval while preserving the live
     * pre-resume canonical state. The user chooses loss of that background interval; no fake
     * rewards are created and no existing save is deleted.
     */
    fun continueWithoutBackgroundProgress() {
        executor.execute {
            val current = _state.value
            if (current.failure?.kind != RuntimeFailureKind.BACKGROUND_RESUME ||
                current.snapshot == null
            ) {
                return@execute
            }
            backgroundTransitionPending.set(false)
            backgroundCheckpointReady.set(false)
            _state.value = current.copy(
                status = RuntimeHostStatus.READY,
                snapshot = runtime.snapshot(),
                offlineSummary = null,
                failure = null,
                canRetryInitialization = false
            )
        }
    }

    /** Presentation acknowledgement only; canonical gameplay state is untouched. */
    fun acknowledgeOfflineSummary() {
        executor.execute {
            val current = _state.value
            if (current.offlineSummary != null) {
                _state.value = current.copy(offlineSummary = null)
            }
        }
    }

    /**
     * Creates one presentation-only correlation ID and dispatches the resulting command on
     * the same canonical single-writer lane. Correlation never changes gameplay semantics.
     */
    fun dispatchCorrelated(commandFactory: (CommandCorrelationId) -> GameCommand) {
        val raw = nextCorrelationValue.getAndIncrement()
        check(raw > 0L) { "Command correlation ID space exhausted" }
        dispatch(commandFactory(CommandCorrelationId(raw)))
    }

    /** Serializes one explicit gameplay command through the canonical runtime. */
    fun dispatch(command: GameCommand) {
        executor.execute {
            if (_state.value.status != RuntimeHostStatus.READY) {
                return@execute
            }
            try {
                publishTransition(runtime.dispatch(command))
            } catch (error: Throwable) {
                publishReadyFailure(RuntimeFailureKind.COMMAND, error)
            }
        }
    }

    /** Serializes one exact foreground elapsed duration through SimulationEngine. */
    fun advance(duration: GameDuration) {
        if (duration == GameDuration.ZERO) {
            return
        }
        executor.execute {
            if (_state.value.status != RuntimeHostStatus.READY) {
                return@execute
            }
            try {
                publishTransition(runtime.advance(duration))
            } catch (error: Throwable) {
                publishSimulationFailure(error)
            }
        }
    }

    /** Flushes a final foreground duration and then checkpoints on the same writer lane. */
    fun advanceAndSave(duration: GameDuration) {
        executor.execute {
            if (_state.value.status != RuntimeHostStatus.READY) {
                return@execute
            }
            try {
                if (duration != GameDuration.ZERO) {
                    publishTransition(runtime.advance(duration))
                }
                saveInternal()
            } catch (error: Throwable) {
                publishSimulationFailure(error)
            }
        }
    }

    /**
     * Genuine background transition checkpoint.
     *
     * This marks the lifecycle barrier synchronously before queueing work. A later onStart can
     * therefore know that foreground pumping must wait for OfflineSessionCoordinator even if
     * the checkpoint has not completed yet.
     */
    fun advanceAndSaveForBackground(duration: GameDuration) {
        backgroundTransitionPending.set(true)
        backgroundCheckpointReady.set(false)

        executor.execute {
            if (_state.value.status != RuntimeHostStatus.READY) {
                backgroundTransitionPending.set(false)
                backgroundCheckpointReady.set(false)
                return@execute
            }

            try {
                if (duration != GameDuration.ZERO) {
                    publishTransition(runtime.advance(duration))
                }
                if (saveInternal()) {
                    backgroundCheckpointReady.set(true)
                } else {
                    backgroundTransitionPending.set(false)
                    backgroundCheckpointReady.set(false)
                }
            } catch (error: Throwable) {
                backgroundTransitionPending.set(false)
                backgroundCheckpointReady.set(false)
                publishSimulationFailure(error)
            }
        }
    }

    /** Requests an explicit checkpoint after all earlier writer-lane work. */
    fun save() {
        executor.execute {
            if (_state.value.status != RuntimeHostStatus.READY) {
                return@execute
            }
            saveInternal()
        }
    }

    /** Explicit application action for a new canonical run. Never called automatically. */
    fun startNewGame(
        randomSeed: Long = EngineState.DEFAULT_RANDOM_SEED
    ) {
        executor.execute {
            val before = _state.value
            if (before.status != RuntimeHostStatus.READY &&
                before.status != RuntimeHostStatus.MENU
            ) {
                return@execute
            }
            try {
                val newState = runtime.startNewGame(randomSeed)
                backgroundTransitionPending.set(false)
                backgroundCheckpointReady.set(false)
                _state.value = before.copy(
                    status = RuntimeHostStatus.NAME_REQUIRED,
                    snapshot = newState,
                    hasExistingSave = false,
                    offlineSummary = null,
                    saveStatus = RuntimeSaveStatus.IDLE,
                    failure = null,
                    canRetryInitialization = false
                )
            } catch (error: Throwable) {
                // Factory/deployment failures must not kill the single writer or move a menu
                // into a false READY state. GameRuntime swaps its session only after a fully
                // deployed game succeeds, so the previous canonical snapshot remains valid.
                _state.value = before.copy(
                    snapshot = runtime.snapshot(),
                    failure = RuntimeFailure(
                        kind = RuntimeFailureKind.COMMAND,
                        diagnosticMessage = error.toDiagnosticMessage()
                    ),
                    canRetryInitialization = false
                )
            }
        }
    }

    /** Starts a fresh canonical run, assigns its name, then opens the playable shell. */
    fun startNewGameWithName(
        name: String,
        randomSeed: Long = EngineState.DEFAULT_RANDOM_SEED
    ) {
        executor.execute {
            val before = _state.value
            if (before.status != RuntimeHostStatus.MENU) {
                return@execute
            }
            var freshState: com.idlerpg.game.domain.model.GameState? = null
            try {
                val newState = runtime.startNewGame(randomSeed)
                freshState = newState
                val result = runtime.dispatch(SetHeroName(name))
                if (result.commandResult != CommandResult.Accepted) {
                    _state.value = _state.value.copy(
                        status = RuntimeHostStatus.NAME_REQUIRED,
                        snapshot = newState,
                        hasExistingSave = false
                    )
                    return@execute
                }
                _state.value = _state.value.copy(
                    status = RuntimeHostStatus.READY,
                    snapshot = result.state,
                    hasExistingSave = true,
                    offlineSummary = null,
                    saveStatus = RuntimeSaveStatus.IDLE,
                    failure = null,
                    canRetryInitialization = false
                )
                _transitions.tryEmit(
                    RuntimeTransition(
                        state = result.state,
                        events = result.events,
                        commandResult = result.commandResult
                    )
                )
                saveInternal()
            } catch (error: Throwable) {
                _state.value = before.copy(
                    status = if (freshState == null) {
                        before.status
                    } else {
                        RuntimeHostStatus.NAME_REQUIRED
                    },
                    snapshot = freshState ?: runtime.snapshot(),
                    hasExistingSave = if (freshState == null) {
                        before.hasExistingSave
                    } else {
                        false
                    },
                    failure = RuntimeFailure(
                        kind = RuntimeFailureKind.COMMAND,
                        diagnosticMessage = error.toDiagnosticMessage()
                    ),
                    canRetryInitialization = false
                )
            }
        }
    }

    /** Opens a loaded run after the menu has been shown. */
    fun continueGame() {
        executor.execute {
            val current = _state.value
            if (current.status != RuntimeHostStatus.MENU ||
                current.snapshot?.meta?.heroName.isNullOrBlank()
            ) {
                return@execute
            }
            _state.value = current.copy(
                status = RuntimeHostStatus.READY,
                failure = null,
                canRetryInitialization = false
            )
        }
    }

    /** Completes the naming gate for a migrated/legacy save without replacing it. */
    fun setHeroName(name: String) {
        executor.execute {
            val current = _state.value
            if (current.status != RuntimeHostStatus.NAME_REQUIRED) {
                return@execute
            }
            try {
                val result = runtime.dispatch(SetHeroName(name))
                if (result.commandResult != CommandResult.Accepted) {
                    return@execute
                }
                _state.value = current.copy(
                    status = RuntimeHostStatus.READY,
                    snapshot = result.state,
                    hasExistingSave = true,
                    failure = null,
                    canRetryInitialization = false
                )
                _transitions.tryEmit(
                    RuntimeTransition(
                        state = result.state,
                        events = result.events,
                        commandResult = result.commandResult
                    )
                )
                saveInternal()
            } catch (error: Throwable) {
                publishReadyFailure(RuntimeFailureKind.COMMAND, error)
            }
        }
    }

    /** Clears a non-blocking save/command error after it has been presented. */
    fun clearOperationError() {
        executor.execute {
            val current = _state.value
            val failureKind = current.failure?.kind
            if (current.status == RuntimeHostStatus.READY &&
                (failureKind == RuntimeFailureKind.SAVE ||
                    failureKind == RuntimeFailureKind.COMMAND)
            ) {
                _state.value = current.copy(
                    failure = null,
                    saveStatus = if (
                        current.saveStatus == RuntimeSaveStatus.ERROR &&
                        failureKind == RuntimeFailureKind.SAVE
                    ) {
                        RuntimeSaveStatus.IDLE
                    } else {
                        current.saveStatus
                    }
                )
            }
        }
    }

    /** Test/process teardown only. The application-scoped production controller stays alive. */
    fun close() {
        executor.shutdownNow()
    }

    private fun requestInitialization(forceRetry: Boolean) {
        val current = _state.value
        val allowed = when {
            forceRetry -> current.status == RuntimeHostStatus.ERROR &&
                current.canRetryInitialization &&
                current.failure?.kind == RuntimeFailureKind.INITIALIZATION
            else -> current.status == RuntimeHostStatus.BOOTING
        }
        if (!allowed) {
            return
        }
        if (!initializationInFlight.compareAndSet(false, true)) {
            return
        }

        executor.execute {
            try {
                initializeInternal()
            } finally {
                initializationInFlight.set(false)
            }
        }
    }

    private fun initializeInternal() {
        _state.value = GameRuntimeHostState(
            status = RuntimeHostStatus.LOADING
        )

        try {
            if (!repository.exists()) {
                val newState = runtime.snapshot()
                _state.value = GameRuntimeHostState(
                    status = RuntimeHostStatus.MENU,
                    snapshot = newState,
                    hasExistingSave = false
                )
                return
            }

            _state.value = GameRuntimeHostState(
                status = RuntimeHostStatus.RESUMING_OFFLINE,
                hasExistingSave = true
            )

            val resumed = runtime.resumeOffline()
                ?: error("Save disappeared during offline resume")

            _state.value = GameRuntimeHostState(
                status = if (resumed.state.meta.heroName.isNullOrBlank()) {
                    RuntimeHostStatus.NAME_REQUIRED
                } else {
                    RuntimeHostStatus.MENU
                },
                snapshot = resumed.state,
                hasExistingSave = true,
                offlineSummary = resumed.summary,
                saveStatus = RuntimeSaveStatus.SAVED
            )
        } catch (error: Throwable) {
            // Existing save data is never deleted or replaced by a fresh game here.
            _state.value = GameRuntimeHostState(
                status = RuntimeHostStatus.ERROR,
                snapshot = null,
                failure = RuntimeFailure(
                    kind = RuntimeFailureKind.INITIALIZATION,
                    diagnosticMessage = error.toDiagnosticMessage()
                ),
                canRetryInitialization = true
            )
        }
    }

    private fun resumeBackgroundInternal() {
        val preservedSnapshot = runtime.snapshot()
        val before = _state.value
        _state.value = before.copy(
            status = RuntimeHostStatus.RESUMING_OFFLINE,
            snapshot = preservedSnapshot,
            failure = null,
            canRetryInitialization = false
        )

        try {
            val resumed = runtime.resumeOffline()
                ?: error("Background checkpoint disappeared during offline resume")
            backgroundTransitionPending.set(false)
            backgroundCheckpointReady.set(false)
            _state.value = _state.value.copy(
                status = RuntimeHostStatus.READY,
                snapshot = resumed.state,
                offlineSummary = resumed.summary,
                saveStatus = RuntimeSaveStatus.SAVED,
                failure = null,
                canRetryInitialization = false
            )
        } catch (error: Throwable) {
            // GameRuntime replaces its active session only after OfflineSessionCoordinator
            // returns successfully. The pre-resume in-memory snapshot is therefore retained.
            _state.value = _state.value.copy(
                status = RuntimeHostStatus.ERROR,
                snapshot = preservedSnapshot,
                failure = RuntimeFailure(
                    kind = RuntimeFailureKind.BACKGROUND_RESUME,
                    diagnosticMessage = error.toDiagnosticMessage()
                ),
                canRetryInitialization = false
            )
        }
    }

    private fun publishBackgroundResumeFailure(error: Throwable) {
        val snapshot = _state.value.snapshot ?: runtime.snapshot()
        _state.value = _state.value.copy(
            status = RuntimeHostStatus.ERROR,
            snapshot = snapshot,
            failure = RuntimeFailure(
                kind = RuntimeFailureKind.BACKGROUND_RESUME,
                diagnosticMessage = error.toDiagnosticMessage()
            ),
            canRetryInitialization = false
        )
    }

    private fun publishTransition(result: EngineResult) {
        _transitions.tryEmit(
            RuntimeTransition(
                state = result.state,
                events = result.events,
                commandResult = result.commandResult
            )
        )

        val current = _state.value
        _state.value = current.copy(
            snapshot = result.state,
            canRetryInitialization = false
        )
    }

    private fun saveInternal(): Boolean {
        val before = _state.value
        _state.value = before.copy(
            saveStatus = RuntimeSaveStatus.SAVING,
            failure = if (before.failure?.kind == RuntimeFailureKind.SAVE) null else before.failure
        )

        return try {
            runtime.saveNow()
            _state.value = _state.value.copy(
                saveStatus = RuntimeSaveStatus.SAVED,
                failure = if (_state.value.failure?.kind == RuntimeFailureKind.SAVE) {
                    null
                } else {
                    _state.value.failure
                }
            )
            true
        } catch (error: Throwable) {
            _state.value = _state.value.copy(
                saveStatus = RuntimeSaveStatus.ERROR,
                failure = RuntimeFailure(
                    kind = RuntimeFailureKind.SAVE,
                    diagnosticMessage = error.toDiagnosticMessage()
                ),
                canRetryInitialization = false
            )
            false
        }
    }

    private fun publishReadyFailure(kind: RuntimeFailureKind, error: Throwable) {
        val current = _state.value
        _state.value = current.copy(
            status = RuntimeHostStatus.READY,
            snapshot = runtime.snapshot(),
            failure = RuntimeFailure(
                kind = kind,
                diagnosticMessage = error.toDiagnosticMessage()
            ),
            canRetryInitialization = false
        )
    }

    private fun publishSimulationFailure(error: Throwable) {
        backgroundTransitionPending.set(false)
        backgroundCheckpointReady.set(false)
        _state.value = _state.value.copy(
            status = RuntimeHostStatus.ERROR,
            snapshot = runtime.snapshot(),
            failure = RuntimeFailure(
                kind = RuntimeFailureKind.SIMULATION,
                diagnosticMessage = error.toDiagnosticMessage()
            ),
            canRetryInitialization = false
        )
    }

    private fun Throwable.toDiagnosticMessage(): String {
        val detail = message?.trim().orEmpty()
        val type = this::class.java.simpleName.ifBlank { "RuntimeError" }
        return if (detail.isEmpty()) type else "$type: $detail"
    }

    companion object {
        private const val TRANSITION_BUFFER_CAPACITY: Int = 32

        private fun newRuntimeExecutor(): ExecutorService =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "idle-rpg-runtime").apply {
                    isDaemon = true
                }
            }
    }
}
