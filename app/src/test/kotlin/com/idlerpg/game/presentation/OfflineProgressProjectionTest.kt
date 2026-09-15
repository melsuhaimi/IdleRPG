package com.idlerpg.game.presentation

import com.idlerpg.game.application.OfflineProgressSummary
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.presentation.projection.OfflineProgressProjector

/** Canonical offline summary -> FUI-10 presentation regression. */
object OfflineProgressProjectionTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val projector = OfflineProgressProjector()

        check(
            projector.project(
                OfflineProgressSummary(
                    requestedElapsed = GameDuration.ZERO,
                    simulatedElapsed = GameDuration.ZERO,
                    durationClamped = false,
                    clockRollbackDetected = false,
                    enemiesDefeated = GameNumber.ZERO,
                    encountersCleared = GameNumber.ZERO,
                    goldGranted = GameNumber.ZERO,
                    experienceGranted = GameNumber.ZERO,
                    masteryGranted = GameNumber.ZERO,
                    itemsFound = GameNumber.ZERO,
                    itemsKept = GameNumber.ZERO,
                    itemsOverflowed = GameNumber.ZERO,
                    itemsAutoSalvaged = GameNumber.ZERO,
                    autoSalvageGold = GameNumber.ZERO,
                    eliteEncountersCleared = GameNumber.ZERO,
                    anomalyEncountersCleared = GameNumber.ZERO,
                    bossesDefeated = GameNumber.ZERO,
                    convergencesTriggered = GameNumber.ZERO,
                    adaptationTierChanges = GameNumber.ZERO,
                    startingStage = null,
                    endingStage = null,
                    deepestStage = null,
                    currentWallStage = null,
                    notableDrops = emptyList(),
                    tacticalInsight = null,
                    eventCount = 0
                )
            ) == null
        )

        check(
            projector.project(
                OfflineProgressSummary(
                    requestedElapsed = GameDuration.ofMillis(3_000L), simulatedElapsed = GameDuration.ofMillis(3_000L),
                    durationClamped = false, clockRollbackDetected = false,
                    enemiesDefeated = GameNumber.ZERO, encountersCleared = GameNumber.ZERO,
                    goldGranted = GameNumber.ZERO, experienceGranted = GameNumber.ZERO, masteryGranted = GameNumber.ZERO,
                    itemsFound = GameNumber.ZERO, itemsKept = GameNumber.ZERO, itemsOverflowed = GameNumber.ZERO,
                    itemsAutoSalvaged = GameNumber.ZERO, autoSalvageGold = GameNumber.ZERO,
                    eliteEncountersCleared = GameNumber.ZERO, anomalyEncountersCleared = GameNumber.ZERO,
                    bossesDefeated = GameNumber.ZERO, convergencesTriggered = GameNumber.ZERO,
                    adaptationTierChanges = GameNumber.ZERO, startingStage = 3, endingStage = 3, deepestStage = 3,
                    currentWallStage = null, notableDrops = emptyList(), tacticalInsight = null, eventCount = 0
                )
            ) == null
        )

        val projected = checkNotNull(
            projector.project(
                OfflineProgressSummary(
                    requestedElapsed = GameDuration.ofMillis(90_061_000L),
                    simulatedElapsed = GameDuration.ofMillis(86_400_000L),
                    durationClamped = true,
                    clockRollbackDetected = false,
                    enemiesDefeated = GameNumber.of(8_640L),
                    encountersCleared = GameNumber.of(2_880L),
                    goldGranted = GameNumber.of(86_400L),
                    experienceGranted = GameNumber.of(86_400L),
                    masteryGranted = GameNumber.ZERO,
                    itemsFound = GameNumber.ZERO,
                    itemsKept = GameNumber.ZERO,
                    itemsOverflowed = GameNumber.ZERO,
                    itemsAutoSalvaged = GameNumber.ZERO,
                    autoSalvageGold = GameNumber.ZERO,
                    eliteEncountersCleared = GameNumber.ZERO,
                    anomalyEncountersCleared = GameNumber.ZERO,
                    bossesDefeated = GameNumber.ZERO,
                    convergencesTriggered = GameNumber.ZERO,
                    adaptationTierChanges = GameNumber.ZERO,
                    startingStage = 8,
                    endingStage = 8,
                    deepestStage = 8,
                    currentWallStage = null,
                    notableDrops = emptyList(),
                    tacticalInsight = null,
                    eventCount = 12345
                )
            )
        )

        check(projected.requestedDurationDisplay == "1d 1h 1m 1s")
        check(projected.simulatedDurationDisplay == "1d")
        check(projected.durationClamped)
        check(!projected.clockRollbackDetected)
        check(projected.enemiesDefeated == "0")
        check(projected.encountersCleared == "0")
        check(projected.goldGranted == "86400")
        check(projected.experienceGranted == "86400")
        check(projected.masteryGranted == "0")
        check(projected.itemsFound == "0")
        check(projected.itemsKept == "0")
        check(projected.itemsOverflowed == "0")
        check(projected.itemsAutoSalvaged == "0")
        check(projected.autoSalvageGold == "0")
        check(projected.stageJourney == "8 → 8")
        check(projected.deepestStage == "8")
        check(projected.currentWallStage == null)
        check(projected.notableDrops.isEmpty())
        check(projected.convergencesTriggered == "0")
        check(projected.adaptationTierChanges == "0")

        val rollback = checkNotNull(
            projector.project(
                OfflineProgressSummary(
                    requestedElapsed = GameDuration.ZERO,
                    simulatedElapsed = GameDuration.ZERO,
                    durationClamped = false,
                    clockRollbackDetected = true,
                    enemiesDefeated = GameNumber.ZERO,
                    encountersCleared = GameNumber.ZERO,
                    goldGranted = GameNumber.ZERO,
                    experienceGranted = GameNumber.ZERO,
                    masteryGranted = GameNumber.ZERO,
                    itemsFound = GameNumber.ZERO,
                    itemsKept = GameNumber.ZERO,
                    itemsOverflowed = GameNumber.ZERO,
                    itemsAutoSalvaged = GameNumber.ZERO,
                    autoSalvageGold = GameNumber.ZERO,
                    eliteEncountersCleared = GameNumber.ZERO,
                    anomalyEncountersCleared = GameNumber.ZERO,
                    bossesDefeated = GameNumber.ZERO,
                    convergencesTriggered = GameNumber.ZERO,
                    adaptationTierChanges = GameNumber.ZERO,
                    startingStage = null,
                    endingStage = null,
                    deepestStage = null,
                    currentWallStage = null,
                    notableDrops = emptyList(),
                    tacticalInsight = null,
                    eventCount = 0
                )
            )
        )
        check(rollback.clockRollbackDetected)
        check(rollback.simulatedDurationDisplay == "0s")

        println("FUI10_OFFLINE_PROGRESS_PROJECTION_PASS")
    }
}
