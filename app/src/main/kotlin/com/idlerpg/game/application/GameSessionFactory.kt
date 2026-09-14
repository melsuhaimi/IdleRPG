package com.idlerpg.game.application

import com.idlerpg.game.core.config.GameConfig
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.GameContent
import com.idlerpg.game.data.repository.ContentRepository
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.model.EngineState
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.state.RunStateFactory
import com.idlerpg.game.domain.system.world.WorldSystem
import com.idlerpg.game.domain.command.DeployStartingEncounter
import com.idlerpg.game.domain.engine.CommandResult

/** Default Kotlin-authored content repository used until content externalization is chosen. */
private object DefaultKotlinContentRepository : ContentRepository {
    override fun load(): GameContent = DefaultGameContent.create()
}

/**
 * Assembles consistent dependencies for new, loaded, and explicit-state headless sessions.
 *
 * Every created session receives its own EngineContext so controlled RNG/InstanceId
 * resources cannot leak between independently simulated sessions.
 */
class GameSessionFactory(
    private val contentRepository: ContentRepository,
    val gameConfig: GameConfig = GameConfig()
) {
    private val authoredContent: GameContent by lazy {
        contentRepository.load()
    }

    val contentRegistry: ContentRegistry by lazy {
        ContentRegistry(authoredContent)
    }

    fun newGame(
        randomSeed: Long = EngineState.DEFAULT_RANDOM_SEED
    ): GameSession =
        createSession(
            GameState.newGame(randomSeed).copy(
                run = RunStateFactory.fresh(gameConfig.balance)
            )
        )

    /** Production new game: canonical content selection and combat begin before first projection. */
    fun newPlayableGame(
        randomSeed: Long = EngineState.DEFAULT_RANDOM_SEED
    ): GameSession {
        val session = newGame(randomSeed)
        val deployment = session.applyCommand(DeployStartingEncounter())
        check(deployment.commandResult == CommandResult.Accepted) {
            "Authored starting encounter deployment failed: ${deployment.commandResult}"
        }
        return session
    }

    fun loadedGame(state: GameState): GameSession =
        createSession(state)

    fun debugSession(state: GameState): GameSession =
        createSession(state)

    fun createEngineContext(): EngineContext {
        val registry = contentRegistry
        return EngineContext(
            contentRegistry = registry,
            balanceConfig = gameConfig.balance,
            simulationConfig = gameConfig.simulation,
            commandHandler = ApplicationCommandRouter,
            scheduledActionSource = WorldSystem.scheduledActionSource(registry, gameConfig.balance),
            scheduledActionHandler = WorldSystem
        )
    }

    private fun createSession(state: GameState): GameSession =
        GameSession(
            initialState = state,
            engineContext = createEngineContext()
        )

    companion object {
        fun default(
            gameConfig: GameConfig = GameConfig()
        ): GameSessionFactory =
            GameSessionFactory(
                contentRepository = DefaultKotlinContentRepository,
                gameConfig = gameConfig
            )
    }
}
