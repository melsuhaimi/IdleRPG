package com.idlerpg.game.simulation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.WorldState
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.presentation.projection.BattleProjector
import com.idlerpg.game.presentation.projection.WorldProjector
import com.idlerpg.game.presentation.query.DefaultGameReadQueries
import com.idlerpg.game.presentation.query.DefaultWorldReadQueries

/** Sector art is presentation-only and follows stable Training Hollow stage boundaries. */
object SectorVisualProjectionScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val content = factory.contentRegistry
        val presentation = PresentationContentRegistry.default()
        val queries = DefaultGameReadQueries(
            contentRegistry = content,
            balanceConfig = factory.gameConfig.balance,
            readEngineContext = factory.createEngineContext()
        )
        val battleProjector = BattleProjector(content, presentation, queries)
        val worldProjector = WorldProjector(
            contentRegistry = content,
            presentationContentRegistry = presentation,
            readQueries = queries,
            worldReadQueries = DefaultWorldReadQueries(content)
        )
        val regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        val base = factory.newGame(9_501L)
        val selected = base.copy(run = base.run.copy(world = WorldState(
            activeRegionId = regionId,
            unlockedRegionIds = setOf(regionId)
        )))

        val encounters = worldProjector.project(selected).regions.single().encounters
        check(encounters.size == TrainingHollowWorldContent.MAX_STAGE)
        assertSector(encounters[0], 1, PresentationStringKey.OUTER_FRACTURE,
            PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BANNER)
        assertSector(encounters[9], 1, PresentationStringKey.OUTER_FRACTURE,
            PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BANNER)
        assertSector(encounters[10], 2, PresentationStringKey.RESONANT_DEPTHS,
            PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BANNER)
        assertSector(encounters[19], 2, PresentationStringKey.RESONANT_DEPTHS,
            PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BANNER)
        assertSector(encounters[20], 3, PresentationStringKey.WARDEN_CORE,
            PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BANNER)
        assertSector(encounters[29], 3, PresentationStringKey.WARDEN_CORE,
            PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BANNER)

        fun battleBackground(stage: Int): PresentationAssetKey {
            require(stage in 1..TrainingHollowWorldContent.MAX_STAGE)
            val encounterId = content.region(regionId).encounterIds[stage - 1]
            val state = selected.copy(run = selected.run.copy(world = selected.run.world.copy(
                currentEncounter = EncounterState(
                    definitionId = encounterId,
                    encounterIndex = stage.toLong(),
                    encounterSeed = 9_501L
                )
            )))
            return battleProjector.project(state).backgroundAssetKey
        }
        check(battleBackground(1) == PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BACKGROUND)
        check(battleBackground(10) == PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BACKGROUND)
        check(battleBackground(11) == PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BACKGROUND)
        check(battleBackground(20) == PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BACKGROUND)
        check(battleBackground(21) == PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BACKGROUND)
        check(battleBackground(30) == PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BACKGROUND)
        check(encounters.last().sectorIndex == 3)
        check(battleProjector.project(GameState.newGame(9_502L)).backgroundAssetKey ==
            PresentationAssetKey.TRAINING_HOLLOW_ILLUSTRATION)
    }

    private fun assertSector(
        encounter: com.idlerpg.game.presentation.model.WorldEncounterUiState,
        expectedIndex: Int,
        expectedTitle: PresentationStringKey,
        expectedBackground: PresentationAssetKey
    ) {
        check(encounter.sectorIndex == expectedIndex)
        check(encounter.sectorTitleStringKey == expectedTitle)
        check(encounter.sectorBackgroundAssetKey == expectedBackground)
    }
}
