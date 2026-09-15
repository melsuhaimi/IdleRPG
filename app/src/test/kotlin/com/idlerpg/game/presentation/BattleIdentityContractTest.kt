package com.idlerpg.game.presentation

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.local.migration.SaveMigrationRegistry
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.ReplaceDoctrine
import com.idlerpg.game.domain.command.SetHeroName
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.presentation.intent.DoctrineUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand
import com.idlerpg.game.simulation.SimulationTestSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Executable contracts for the Battle toggle and the new player-identity gate. */
class BattleIdentityContractTest {
    @Test
    fun autoBattleToggle_mapsToCanonicalDoctrineMutation() {
        val state = GameState.newGame(randomSeed = 12_510L)
        val command = DoctrineUiIntent.SetDoctrineEnabled(false)
            .toGameCommand(CommandCorrelationId(1L), state)

        assertTrue(command is ReplaceDoctrine)
        assertEquals(false, (command as ReplaceDoctrine).enabled)
    }

    @Test
    fun heroName_isAcceptedByDomainAndSurvivesSaveRoundTrip() {
        val runtime = SimulationTestSupport.runtime(seed = 12_511L)
        val result = runtime.dispatch(SetHeroName("Aster"))

        assertEquals(CommandResult.Accepted, result.commandResult)
        assertEquals("Aster", result.state.meta.heroName)
        assertEquals(result.state, SaveData.fromGameState(result.state).toGameState())
    }

    @Test
    fun v6Save_migratesToAnExplicitUnnamedIdentityWithoutReplacingProgress() {
        val original = GameState.newGame(randomSeed = 12_512L)
        val v6Fields = SaveData.fromGameState(original).fields.filterKeys { key ->
            !key.startsWith("meta.heroName") &&
                !key.startsWith("meta.rebirth") &&
                !key.startsWith("run.progression.skillProgression")
        }
        val envelope = SaveEnvelope(
            schemaVersion = SaveVersion.V6,
            contentVersion = SimulationTestSupport.CONTENT_VERSION,
            writtenAtEpochMs = 12_512_000L,
            data = SaveData(v6Fields)
        )

        val migrated = SaveMigrationRegistry().migrate(envelope)

        assertEquals(SaveVersion.V7, migrated.schemaVersion)
        assertEquals(original, migrated.gameState())
        assertNull(migrated.gameState().meta.heroName)
    }
}
