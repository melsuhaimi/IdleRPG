package com.idlerpg.game.presentation.model

/**
 * Presentation-only summary of one canonical offline resume.
 *
 * Every numeric gameplay result has already been granted by SimulationEngine before this
 * model exists. Nothing in this model can be converted back into gameplay mutation.
 */
data class OfflineProgressUiState(
    val requestedDurationDisplay: String,
    val simulatedDurationDisplay: String,
    val durationClamped: Boolean,
    val clockRollbackDetected: Boolean,
    val levelJourney: String?,
    val stoppingReason: OfflineStoppingReasonUi,
    val enemiesDefeated: String,
    val encountersCleared: String,
    val goldGranted: String,
    val experienceGranted: String,
    val masteryGranted: String,
    val itemsFound: String,
    val itemsKept: String,
    val itemsOverflowed: String,
    val itemsAutoSalvaged: String,
    val autoSalvageGold: String,
    val eliteEncountersCleared: String,
    val anomalyEncountersCleared: String,
    val bossesDefeated: String,
    val convergencesTriggered: String,
    val adaptationTierChanges: String,
    val stageJourney: String?,
    val deepestStage: String?,
    val currentWallStage: String?,
    val notableDrops: List<String>,
    val tacticalInsight: OfflineTacticalInsightUi?
)

enum class OfflineStoppingReasonUi {
    ELAPSED,
    CLAIM_WINDOW_CAPPED,
    CLOCK_ROLLBACK,
    NO_ELIGIBLE_FARM_STAGE
}

enum class OfflineTacticalInsightUi {
    ADAPTATION_PRESSURE,
    PROTECTOR_BLOCKING,
    ADAPTIVE_RESISTANCE,
    CASTER_DISRUPTION,
    SWARM_PRESSURE,
    SURVIVAL_PRESSURE
}
