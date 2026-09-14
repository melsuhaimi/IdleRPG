package com.idlerpg.game.ui.app

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.presentation.runtime.GameRuntimeController
import com.idlerpg.game.presentation.runtime.RuntimeHostStatus

/**
 * Android lifecycle boundary for foreground simulation.
 *
 * FUI-10 distinguishes genuine backgrounding from Activity recreation. Genuine background
 * transitions checkpoint through [GameRuntimeController.advanceAndSaveForBackground], then the
 * next foreground entry resumes through OfflineSessionCoordinator. Configuration recreation
 * only flushes already-measured foreground time and never creates a fake offline interval.
 */
class ForegroundSimulationDriver(
    private val runtimeController: GameRuntimeController,
    private val monotonicNowMillis: () -> Long = SystemClock::elapsedRealtime,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val pumpIntervalMillis: Long = DEFAULT_PUMP_INTERVAL_MILLIS
) {
    private var foregroundActive: Boolean = false
    private var pumpRunning: Boolean = false
    private var canonicalPreviewPaused: Boolean = false
    private var awaitingReadyBaseline: Boolean = true
    private var lastPumpAtMillis: Long = 0L

    private val pump = object : Runnable {
        override fun run() {
            if (!pumpRunning) {
                return
            }

            val now = monotonicNowMillis()
            val runtimeReady =
                runtimeController.state.value.status == RuntimeHostStatus.READY
            val lifecycleBarrier = runtimeController.backgroundResumePending()

            if (!runtimeReady || lifecycleBarrier || awaitingReadyBaseline) {
                // Never count boot/offline-resume/error time as foreground simulation. The first
                // READY observation establishes a fresh monotonic baseline and grants nothing.
                lastPumpAtMillis = now
                if (runtimeReady && !lifecycleBarrier) {
                    awaitingReadyBaseline = false
                }
            } else {
                submitElapsed(now)
            }

            handler.postDelayed(this, pumpIntervalMillis)
        }
    }

    init {
        require(pumpIntervalMillis > 0L) {
            "pumpIntervalMillis must be positive: $pumpIntervalMillis"
        }
    }

    fun start() {
        foregroundActive = true
        awaitingReadyBaseline =
            runtimeController.state.value.status != RuntimeHostStatus.READY ||
                runtimeController.backgroundResumePending()
        if (!canonicalPreviewPaused) {
            startPumpIfNeeded()
        }
    }

    /**
     * Genuine background stop. Flush active monotonic time and create the checkpoint that the
     * next onStart will consume through canonical offline resume.
     */
    fun stopAndCheckpointBackground() {
        foregroundActive = false
        if (!pumpRunning) {
            runtimeController.advanceAndSaveForBackground(GameDuration.ZERO)
            return
        }
        stopPump(
            flushMode = FlushMode.BACKGROUND_CHECKPOINT
        )
    }

    /**
     * Activity/configuration recreation. The Application-scoped runtime survives, so only
     * already-measured foreground elapsed time is flushed; no wall-clock offline checkpoint is
     * created and the new Activity continues the same canonical session.
     */
    fun stopForHostRecreation() {
        foregroundActive = false
        if (!pumpRunning) {
            return
        }
        stopPump(
            flushMode = FlushMode.FOREGROUND_ONLY
        )
    }

    /**
     * Stops active advancement before a Chronicle preview request is queued.
     * Time spent reading the destructive confirmation is intentionally not simulated.
     */
    fun pauseForCanonicalPreview() {
        if (canonicalPreviewPaused) {
            return
        }
        canonicalPreviewPaused = true
        if (pumpRunning) {
            stopPump(
                flushMode = FlushMode.FOREGROUND_ONLY
            )
        }
    }

    /** Resumes Chronicle-paused foreground measurement from a fresh monotonic baseline. */
    fun resumeAfterCanonicalPreview() {
        if (!canonicalPreviewPaused) {
            return
        }
        canonicalPreviewPaused = false
        awaitingReadyBaseline = true
        if (foregroundActive) {
            startPumpIfNeeded()
        }
    }

    private fun startPumpIfNeeded() {
        if (pumpRunning || !foregroundActive || canonicalPreviewPaused) {
            return
        }
        pumpRunning = true
        lastPumpAtMillis = monotonicNowMillis()
        handler.postDelayed(pump, pumpIntervalMillis)
    }

    private fun stopPump(flushMode: FlushMode) {
        if (!pumpRunning) {
            return
        }

        pumpRunning = false
        handler.removeCallbacks(pump)

        val now = monotonicNowMillis()
        val elapsed = if (awaitingReadyBaseline ||
            runtimeController.state.value.status != RuntimeHostStatus.READY ||
            runtimeController.backgroundResumePending()
        ) {
            0L
        } else {
            elapsedSinceLastPump(now)
        }
        lastPumpAtMillis = now
        awaitingReadyBaseline = true
        val duration = GameDuration.ofMillis(elapsed)

        when (flushMode) {
            FlushMode.FOREGROUND_ONLY ->
                if (duration != GameDuration.ZERO) {
                    runtimeController.advance(duration)
                }

            FlushMode.BACKGROUND_CHECKPOINT ->
                runtimeController.advanceAndSaveForBackground(duration)
        }
    }

    private fun submitElapsed(now: Long) {
        val elapsed = elapsedSinceLastPump(now)
        lastPumpAtMillis = now
        if (elapsed > 0L) {
            runtimeController.advance(GameDuration.ofMillis(elapsed))
        }
    }

    private fun elapsedSinceLastPump(now: Long): Long {
        if (now <= lastPumpAtMillis) {
            return 0L
        }
        return now - lastPumpAtMillis
    }

    private enum class FlushMode {
        FOREGROUND_ONLY,
        BACKGROUND_CHECKPOINT
    }

    companion object {
        const val DEFAULT_PUMP_INTERVAL_MILLIS: Long = 250L
    }
}
