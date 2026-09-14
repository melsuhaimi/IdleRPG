package com.idlerpg.game.presentation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.RetreatEncounter
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.presentation.intent.WorldUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand

/** Dependency-free FUI-05 proof that World mutation gestures map one-to-one to commands. */
object WorldIntentMappingTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val correlation = CommandCorrelationId(505L)

        val select = WorldUiIntent.SelectRegionIntent(
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ).toGameCommand(correlation) as SelectRegion
        check(select.regionId == DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
        check(select.correlationId == correlation)

        val start = WorldUiIntent.StartEncounterIntent(
            DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID
        ).toGameCommand(correlation) as StartEncounter
        check(start.encounterId == DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID)
        check(start.correlationId == correlation)

        val retreat = WorldUiIntent.RetreatEncounterIntent
            .toGameCommand(correlation) as RetreatEncounter
        check(retreat.correlationId == correlation)

        println("FUI05_WORLD_INTENT_MAPPING_PASS")
    }
}
