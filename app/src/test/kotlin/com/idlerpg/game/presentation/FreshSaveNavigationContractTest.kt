package com.idlerpg.game.presentation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.event.CombatEndReason
import com.idlerpg.game.domain.event.CombatEnded
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.ItemDropped
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.event.GameEventPresenter
import com.idlerpg.game.presentation.projection.SkillLoadoutProjector
import com.idlerpg.game.presentation.query.DefaultGameReadQueries
import com.idlerpg.game.presentation.runtime.RuntimeTransition
import com.idlerpg.game.simulation.SimulationTestSupport
import com.idlerpg.game.ui.navigation.GameDestination
import com.idlerpg.game.ui.navigation.ProgressDestination
import com.idlerpg.game.ui.navigation.ProgressiveDisclosurePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Executable public contracts for the first-session Battle -> Skills path.
 *
 * The navigation assertion intentionally describes the player-facing contract:
 * a fresh save must be able to resolve the Battle Skills action to Build. The
 * test does not call ViewModel internals or duplicate route implementation.
 */
class FreshSaveNavigationContractTest {
    @Test
    fun freshSaveSkillsAction_resolvesToAnActionableBuildRoute() {
        val freshSave = GameState.newGame(randomSeed = 12_401L)
        val availability = ProgressiveDisclosurePolicy.project(freshSave)

        assertTrue(
            "Fresh-save Battle Skills action must be allowed to enter Build",
            availability.allows(GameDestination.GEAR)
        )
        assertEquals(
            GameDestination.GEAR,
            availability.resolve(
                destination = GameDestination.GEAR,
                progressDestination = ProgressDestination.OVERVIEW
            ).destination
        )
    }

    @Test
    fun freshSaveSkillProjection_exposesAnUnlockedEquipAction() {
        val factory = SimulationTestSupport.factory()
        val projector = SkillLoadoutProjector(
            contentRegistry = factory.contentRegistry,
            presentationContentRegistry = PresentationContentRegistry.default(),
            readQueries = DefaultGameReadQueries(
                contentRegistry = factory.contentRegistry,
                balanceConfig = factory.gameConfig.balance,
                readEngineContext = factory.createEngineContext()
            )
        )

        val loadout = projector.project(GameState.newGame(randomSeed = 12_402L))
        assertEquals(4, loadout.capacity)
        assertEquals(0, loadout.equippedCount)
        assertTrue(loadout.slots.all { it.skill == null })

        val heavyStrike = loadout.availableSkills.single {
            it.skillId == DefaultGameContent.HEAVY_STRIKE_ID
        }
        assertTrue("Heavy Strike must be available on a fresh save", heavyStrike.unlock.unlocked)
        assertTrue("Heavy Strike must expose an actionable Equip control", heavyStrike.canEquip)
    }

    @Test
    fun battleRewardFeedback_preservesCanonicalGoldXpAndOverflowItemFacts() {
        val itemInstanceId = InstanceId(44L)
        val feedback = GameEventPresenter().presentBattle(
            RuntimeTransition(
                state = GameState.newGame(randomSeed = 12_403L),
                events = listOf(
                    GameEventEnvelope(
                        sequenceNumber = 1L,
                        simulationTime = GameTime.ZERO,
                        event = CurrencyGranted(CurrencyId.GOLD, GameNumber.of(37L))
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 2L,
                        simulationTime = GameTime.ZERO,
                        event = ExperienceGranted(GameNumber.of(19L))
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 3L,
                        simulationTime = GameTime.ZERO,
                        event = ItemDropped(
                            itemInstanceId = itemInstanceId,
                            itemDefinitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID,
                            rarity = Rarity.RARE
                        )
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 4L,
                        simulationTime = GameTime.ZERO,
                        event = ItemSentToOverflow(
                            itemInstanceId = itemInstanceId,
                            itemDefinitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID
                        )
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 5L,
                        simulationTime = GameTime.ZERO,
                        event = CombatEnded(1L, CombatEndReason.VICTORY)
                    )
                ),
                commandResult = null
            )
        ) ?: error("Victory transition should produce battle feedback")

        val reward = feedback.reward ?: error("Victory transition should expose reward facts")
        assertEquals("37", reward.goldDisplay)
        assertEquals("19", reward.experienceDisplay)
        assertEquals(1, reward.items.size)
        assertTrue(reward.items.single().sentToOverflow)
        assertEquals(null, feedback.killReward)
    }

    @Test
    fun enemyKillFeedback_linksAllKilledInstancesWithoutFabricatingRewardOwnership() {
        val firstEnemy = InstanceId(51L)
        val secondEnemy = InstanceId(52L)
        val feedback = GameEventPresenter().presentBattle(
            RuntimeTransition(
                state = GameState.newGame(randomSeed = 12_404L),
                events = listOf(
                    GameEventEnvelope(
                        sequenceNumber = 1L,
                        simulationTime = GameTime.ZERO,
                        event = EnemyKilled(firstEnemy, DefaultGameContent.SLIME_ID)
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 2L,
                        simulationTime = GameTime.ZERO,
                        event = EnemyKilled(secondEnemy, DefaultGameContent.RIFTFANG_ID)
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 3L,
                        simulationTime = GameTime.ZERO,
                        event = CurrencyGranted(CurrencyId.GOLD, GameNumber.of(9L))
                    ),
                    GameEventEnvelope(
                        sequenceNumber = 4L,
                        simulationTime = GameTime.ZERO,
                        event = ExperienceGranted(GameNumber.of(4L))
                    )
                ),
                commandResult = null
            )
        ) ?: error("Enemy kill transition should produce battle feedback")

        val killReward = feedback.killReward ?: error("Enemy kills should be exposed to Battle")
        assertEquals(listOf(firstEnemy, secondEnemy), killReward.defeatedEnemyInstanceIds)
        assertEquals("9", killReward.reward?.goldDisplay)
        assertEquals("4", killReward.reward?.experienceDisplay)
    }
}
