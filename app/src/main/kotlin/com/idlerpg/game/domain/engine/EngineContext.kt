package com.idlerpg.game.domain.engine

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.config.SimulationConfig
import com.idlerpg.game.core.id.IdFactory
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.random.GameRandom
import com.idlerpg.game.core.random.RandomState
import com.idlerpg.game.core.random.SeededGameRandom
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.model.EngineState

/** Creates a controlled deterministic GameRandom from persisted RandomState. */
fun interface GameRandomFactory {
    fun create(state: RandomState): GameRandom
}

/**
 * Read-mostly deterministic execution dependencies shared by engine handlers.
 *
 * Foundation 5 adds the real validated [ContentRegistry] promised by the master
 * architecture. Wall-clock time remains absent from pure simulation.
 *
 * The engine restores RNG and instance-ID generators from EngineState at the start of
 * every public execution call and snapshots them back only on committed transitions.
 */
class EngineContext(
    val contentRegistry: ContentRegistry,
    val balanceConfig: BalanceConfig = BalanceConfig(),
    val simulationConfig: SimulationConfig = SimulationConfig(),
    val commandHandler: GameCommandHandler = RejectUnsupportedGameCommandHandler,
    val scheduledActionSource: ScheduledActionSource = EmptyScheduledActionSource,
    val scheduledActionHandler: ScheduledActionHandler = RejectUnexpectedScheduledActionHandler,
    private val randomFactory: GameRandomFactory = GameRandomFactory { state ->
        SeededGameRandom(state)
    }
) {
    private var activeRandom: GameRandom? = null
    private var activeIdFactory: IdFactory? = null

    /** Controlled gameplay RNG. Valid only while GameEngine/SimulationEngine is executing. */
    val random: GameRandom
        get() = activeRandom
            ?: error("EngineContext.random is available only during engine execution")

    /** Allocate a controlled deterministic runtime instance ID during an engine transition. */
    fun nextInstanceId(): InstanceId = activeIdFactory
        ?.next()
        ?: error("EngineContext.nextInstanceId() is available only during engine execution")

    internal fun beginExecution(engineState: EngineState) {
        require(
            simulationConfig.schedulerOrderingVersion == EnginePriority.ORDERING_VERSION
        ) {
            "Unsupported scheduler ordering version: " +
                "${simulationConfig.schedulerOrderingVersion}; " +
                "engine supports ${EnginePriority.ORDERING_VERSION}"
        }

        activeRandom = randomFactory.create(engineState.randomState)
        activeIdFactory = IdFactory(engineState.nextInstanceIdCounter)
    }

    internal fun snapshotEngineResources(base: EngineState): EngineState {
        val random = activeRandom
            ?: error("Cannot snapshot RNG before beginExecution")
        val ids = activeIdFactory
            ?: error("Cannot snapshot instance IDs before beginExecution")

        return base.copy(
            randomState = random.snapshot(),
            nextInstanceIdCounter = ids.nextValue()
        )
    }
}
