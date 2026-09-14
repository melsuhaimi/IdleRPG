package com.idlerpg.game.presentation.projection

import com.idlerpg.game.application.OfflineProgressSummary
import com.idlerpg.game.application.OfflineTacticalInsight
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.OfflineProgressUiState
import com.idlerpg.game.presentation.model.OfflineTacticalInsightUi

/**
 * Pure OfflineProgressSummary -> presentation projection.
 *
 * The summary is informational output from OfflineSessionCoordinator. This projector never
 * computes rewards, dispatches commands, reads wall time, or advances simulation.
 */
class OfflineProgressProjector {
    fun project(summary: OfflineProgressSummary): OfflineProgressUiState? {
        if (!shouldPresent(summary)) {
            return null
        }

        return OfflineProgressUiState(
            requestedDurationDisplay = formatDuration(summary.requestedElapsed),
            simulatedDurationDisplay = formatDuration(summary.simulatedElapsed),
            durationClamped = summary.durationClamped,
            clockRollbackDetected = summary.clockRollbackDetected,
            enemiesDefeated = GameNumberFormatter.full(summary.enemiesDefeated),
            encountersCleared = GameNumberFormatter.full(summary.encountersCleared),
            goldGranted = GameNumberFormatter.full(summary.goldGranted),
            experienceGranted = GameNumberFormatter.full(summary.experienceGranted),
            masteryGranted = GameNumberFormatter.full(summary.masteryGranted),
            itemsFound = GameNumberFormatter.full(summary.itemsFound),
            itemsKept = GameNumberFormatter.full(summary.itemsKept),
            itemsOverflowed = GameNumberFormatter.full(summary.itemsOverflowed),
            itemsAutoSalvaged = GameNumberFormatter.full(summary.itemsAutoSalvaged),
            autoSalvageGold = GameNumberFormatter.full(summary.autoSalvageGold),
            eliteEncountersCleared = GameNumberFormatter.full(summary.eliteEncountersCleared),
            anomalyEncountersCleared = GameNumberFormatter.full(summary.anomalyEncountersCleared),
            bossesDefeated = GameNumberFormatter.full(summary.bossesDefeated),
            convergencesTriggered = GameNumberFormatter.full(summary.convergencesTriggered),
            adaptationTierChanges = GameNumberFormatter.full(summary.adaptationTierChanges),
            stageJourney = if (summary.startingStage != null && summary.endingStage != null) {
                "${summary.startingStage} → ${summary.endingStage}"
            } else null,
            deepestStage = summary.deepestStage?.toString(),
            currentWallStage = summary.currentWallStage?.toString(),
            notableDrops = summary.notableDrops.map { "${titleCase(it.rarity.name)} · ${it.displayName}" },
            tacticalInsight = summary.tacticalInsight?.let(::projectInsight)
        )
    }

    private fun shouldPresent(summary: OfflineProgressSummary): Boolean =
        summary.requestedElapsed.millis >= MIN_MEANINGFUL_OFFLINE_MILLIS ||
            summary.simulatedElapsed.millis >= MIN_MEANINGFUL_OFFLINE_MILLIS ||
            summary.durationClamped ||
            summary.clockRollbackDetected ||
            summary.enemiesDefeated != GameNumber.ZERO ||
            summary.encountersCleared != GameNumber.ZERO ||
            summary.goldGranted != GameNumber.ZERO ||
            summary.experienceGranted != GameNumber.ZERO ||
            summary.masteryGranted != GameNumber.ZERO ||
            summary.itemsFound != GameNumber.ZERO ||
            summary.itemsKept != GameNumber.ZERO ||
            summary.itemsOverflowed != GameNumber.ZERO ||
            summary.itemsAutoSalvaged != GameNumber.ZERO ||
            summary.bossesDefeated != GameNumber.ZERO ||
            summary.convergencesTriggered != GameNumber.ZERO ||
            summary.adaptationTierChanges != GameNumber.ZERO

    private fun projectInsight(insight: OfflineTacticalInsight): OfflineTacticalInsightUi =
        when (insight) {
            OfflineTacticalInsight.ADAPTATION_PRESSURE -> OfflineTacticalInsightUi.ADAPTATION_PRESSURE
            OfflineTacticalInsight.PROTECTOR_BLOCKING -> OfflineTacticalInsightUi.PROTECTOR_BLOCKING
            OfflineTacticalInsight.ADAPTIVE_RESISTANCE -> OfflineTacticalInsightUi.ADAPTIVE_RESISTANCE
            OfflineTacticalInsight.CASTER_DISRUPTION -> OfflineTacticalInsightUi.CASTER_DISRUPTION
            OfflineTacticalInsight.SWARM_PRESSURE -> OfflineTacticalInsightUi.SWARM_PRESSURE
            OfflineTacticalInsight.SURVIVAL_PRESSURE -> OfflineTacticalInsightUi.SURVIVAL_PRESSURE
        }

    private fun titleCase(value: String): String =
        value.lowercase().replaceFirstChar { it.titlecase() }

    private fun formatDuration(duration: GameDuration): String {
        var remaining = duration.millis
        if (remaining == 0L) {
            return "0s"
        }
        if (remaining < 1_000L) {
            return "${remaining}ms"
        }

        val days = remaining / MILLIS_PER_DAY
        remaining %= MILLIS_PER_DAY
        val hours = remaining / MILLIS_PER_HOUR
        remaining %= MILLIS_PER_HOUR
        val minutes = remaining / MILLIS_PER_MINUTE
        remaining %= MILLIS_PER_MINUTE
        val seconds = remaining / MILLIS_PER_SECOND
        val millis = remaining % MILLIS_PER_SECOND

        val parts = mutableListOf<String>()
        if (days > 0L) parts += "${days}d"
        if (hours > 0L) parts += "${hours}h"
        if (minutes > 0L) parts += "${minutes}m"
        if (seconds > 0L) parts += "${seconds}s"
        if (parts.isEmpty() && millis > 0L) parts += "${millis}ms"
        return parts.joinToString(" ")
    }

    companion object {
        private const val MIN_MEANINGFUL_OFFLINE_MILLIS: Long = 60_000L
        private const val MILLIS_PER_SECOND: Long = 1_000L
        private const val MILLIS_PER_MINUTE: Long = 60_000L
        private const val MILLIS_PER_HOUR: Long = 3_600_000L
        private const val MILLIS_PER_DAY: Long = 86_400_000L
    }
}
