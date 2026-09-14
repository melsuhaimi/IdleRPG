package com.idlerpg.game.ui.app

import android.content.Context
import com.idlerpg.game.application.AutosaveCoordinator
import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.GameSessionFactory
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.config.GameConfig
import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.core.time.SystemGameClock
import com.idlerpg.game.data.local.LocalGameRepository
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.event.GameEventPresenter
import com.idlerpg.game.presentation.projection.BattleProjector
import com.idlerpg.game.presentation.projection.GlobalHudProjector
import com.idlerpg.game.presentation.projection.OfflineProgressProjector
import com.idlerpg.game.presentation.projection.GearProjector
import com.idlerpg.game.presentation.projection.ProgressProjector
import com.idlerpg.game.presentation.projection.DoctrineProjector
import com.idlerpg.game.presentation.projection.SkillLoadoutProjector
import com.idlerpg.game.presentation.projection.WorldProjector
import com.idlerpg.game.presentation.query.DefaultGameReadQueries
import com.idlerpg.game.presentation.query.DefaultDoctrineReadQueries
import com.idlerpg.game.presentation.query.GameReadQueries
import com.idlerpg.game.presentation.query.DefaultGearReadQueries
import com.idlerpg.game.presentation.query.GearReadQueries
import com.idlerpg.game.presentation.query.DefaultProgressReadQueries
import com.idlerpg.game.presentation.query.ProgressReadQueries
import com.idlerpg.game.presentation.query.DoctrineReadQueries
import com.idlerpg.game.presentation.query.DefaultWorldReadQueries
import com.idlerpg.game.presentation.query.WorldReadQueries
import com.idlerpg.game.presentation.runtime.GameRuntimeController
import java.io.File

/**
 * Application-scoped production dependency owner.
 *
 * Activity recreation must never construct another gameplay runtime or RNG stream.
 */
class GameAppContainer(
    context: Context
) {
    val gameConfig: GameConfig = GameConfig()
    val gameClock: GameClock = SystemGameClock()

    val gameRepository: GameRepository = LocalGameRepository(
        directory = File(context.filesDir, SAVE_DIRECTORY_NAME)
    )

    val sessionFactory: GameSessionFactory =
        GameSessionFactory.default(gameConfig)

    val contentRegistry = sessionFactory.contentRegistry

    val presentationContentRegistry: PresentationContentRegistry =
        PresentationContentRegistry.default()

    val gameReadQueries: GameReadQueries = DefaultGameReadQueries(
        contentRegistry = contentRegistry,
        balanceConfig = gameConfig.balance,
        readEngineContext = sessionFactory.createEngineContext()
    )

    val globalHudProjector: GlobalHudProjector = GlobalHudProjector(
        presentationContentRegistry = presentationContentRegistry,
        readQueries = gameReadQueries
    )

    val battleProjector: BattleProjector = BattleProjector(
        contentRegistry = contentRegistry,
        presentationContentRegistry = presentationContentRegistry,
        readQueries = gameReadQueries
    )

    val skillLoadoutProjector: SkillLoadoutProjector = SkillLoadoutProjector(
        contentRegistry = contentRegistry,
        presentationContentRegistry = presentationContentRegistry,
        readQueries = gameReadQueries
    )

    val worldReadQueries: WorldReadQueries = DefaultWorldReadQueries(
        contentRegistry = contentRegistry
    )

    val doctrineReadQueries: DoctrineReadQueries = DefaultDoctrineReadQueries(
        contentRegistry = contentRegistry,
        balanceConfig = gameConfig.balance
    )

    val worldProjector: WorldProjector = WorldProjector(
        contentRegistry = contentRegistry,
        presentationContentRegistry = presentationContentRegistry,
        readQueries = gameReadQueries,
        worldReadQueries = worldReadQueries
    )

    val doctrineProjector: DoctrineProjector = DoctrineProjector(
        contentRegistry = contentRegistry,
        presentationContentRegistry = presentationContentRegistry,
        readQueries = gameReadQueries,
        doctrineReadQueries = doctrineReadQueries
    )


    val gearReadQueries: GearReadQueries = DefaultGearReadQueries(
        balanceConfig = gameConfig.balance
    )

    val gearProjector: GearProjector = GearProjector(
        contentRegistry = contentRegistry,
        presentationContentRegistry = presentationContentRegistry,
        readQueries = gearReadQueries
    )

    val progressReadQueries: ProgressReadQueries = DefaultProgressReadQueries(
        contentRegistry = contentRegistry
    )

    val progressProjector: ProgressProjector = ProgressProjector(
        contentRegistry = contentRegistry,
        presentationContentRegistry = presentationContentRegistry,
        readQueries = gameReadQueries,
        progressReadQueries = progressReadQueries
    )

    val gameEventPresenter: GameEventPresenter = GameEventPresenter()

    val offlineProgressProjector: OfflineProgressProjector = OfflineProgressProjector()

    private val autosaveCoordinator = AutosaveCoordinator(
        repository = gameRepository,
        clock = gameClock,
        contentVersion = gameConfig.contentVersion
    )

    private val offlineSessionCoordinator = OfflineSessionCoordinator(
        repository = gameRepository,
        clock = gameClock,
        engineContext = sessionFactory.createEngineContext()
    )

    val runtime: GameRuntime = GameRuntime(
        initialSession = sessionFactory.newPlayableGame(),
        sessionFactory = sessionFactory,
        autosaveCoordinator = autosaveCoordinator,
        offlineSessionCoordinator = offlineSessionCoordinator
    )

    val runtimeController: GameRuntimeController = GameRuntimeController(
        runtime = runtime,
        repository = gameRepository
    )

    init {
        val coverage = presentationContentRegistry.coverage(contentRegistry)
        require(coverage.isComplete) {
            "Presentation content coverage incomplete: missing=${coverage.missingVisibleIds}, " +
                "unclassified=${coverage.unclassifiedRegistryIds}"
        }
    }

    companion object {
        private const val SAVE_DIRECTORY_NAME: String = "idle_rpg_save"
    }
}
