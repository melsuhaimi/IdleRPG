package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.model.EngineState
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.CooldownState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.model.economy.EconomyState
import com.idlerpg.game.domain.model.player.PlayerState
import com.idlerpg.game.domain.model.resonance.ResonanceSequenceState
import com.idlerpg.game.domain.model.resonance.ResonanceState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.WorldState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.BattleSkillReadinessUi
import com.idlerpg.game.presentation.projection.BattleProjector
import com.idlerpg.game.presentation.query.GameReadQueries

/** Dependency-free FUI-03 canonical-state -> Battle projection regression. */
object BattleProjectionTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val content = DefaultGameContent.registry()
        val presentation = PresentationContentRegistry.default()
        val queries = ProjectionReadQueries()
        val projector = BattleProjector(
            contentRegistry = content,
            presentationContentRegistry = presentation,
            readQueries = queries
        )

        val fresh = projector.project(GameState.newGame(randomSeed = 803L))
        check(fresh.equippedSkills.isEmpty()) {
            "FUI-03 must not invent a default skill loadout"
        }
        check(fresh.player.resources.isEmpty())
        check(fresh.resonance.size == 8)
        check(fresh.enemy == null)

        val playerId = InstanceId(1L)
        val enemyId = InstanceId(2L)
        val active = activeState(
            playerId = playerId,
            enemyId = enemyId,
            simulationTimeMillis = 0L,
            heavyCooldownReadyAtMillis = null,
            queuedHeavy = false
        )
        val activeUi = projector.project(active)
        val activeEnemy = activeUi.enemy ?: error("Expected projected active enemy")
        check(activeEnemy.currentHealthDisplay == "100")
        check(activeEnemy.maximumHealthDisplay == "100")
        check(activeUi.equippedSkills.map { it.skillId } == listOf(
            DefaultGameContent.HEAVY_STRIKE_ID,
            DefaultGameContent.QUICK_SLASH_ID
        ))
        check(activeUi.player.nextDecisionRemainingMillis == 1_000L)
        check(activeUi.player.basicAttackDpsDisplay == "10") {
            "GameRate DPS must use the compact rate formatter"
        }
        check(GameNumberFormatter.compact(GameRate.parse("1398.190045")) == "1.39K") {
            "Fractional DPS must use a compact, readable display"
        }
        check(GameNumberFormatter.compact(GameRate.parse("0.001")) == "0.001") {
            "Small nonzero rates must remain visible"
        }
        check(GameNumberFormatter.compact(GameRate.parse("0.000001")) == "<0.001") {
            "Tiny nonzero rates must not be displayed as zero"
        }
        check(activeUi.equippedSkills.all { it.queueAllowed })
        check(activeUi.equippedSkills.all { it.resourceCosts.isEmpty() })
        check(activeUi.equippedSkills.all { it.readiness == BattleSkillReadinessUi.READY })

        val cooldownQueued = activeState(
            playerId = playerId,
            enemyId = enemyId,
            simulationTimeMillis = 1_000L,
            heavyCooldownReadyAtMillis = 3_000L,
            queuedHeavy = true
        )
        val cooldownUi = projector.project(cooldownQueued)
        val heavy = cooldownUi.equippedSkills.single {
            it.skillId == DefaultGameContent.HEAVY_STRIKE_ID
        }
        check(cooldownUi.queuedSkillId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(heavy.queued)
        check(heavy.queueAllowed) {
            "Cooldown is transient and must not disable QueueSkillCast"
        }
        check(heavy.readiness == BattleSkillReadinessUi.COOLDOWN_WAIT)
        check(heavy.cooldownRemainingMillis == 2_000L)
        check(cooldownUi.resonance.single { it.affinityId == Affinity.MIGHT.id }.chargeDisplay == "2")
        check(cooldownUi.resonanceSequence == listOf(Affinity.MIGHT.id))
        check(cooldownUi.basicAttackUpgrade.affordable)

        println("FUI03_BATTLE_PROJECTION_PASS")
    }

    private fun activeState(
        playerId: InstanceId,
        enemyId: InstanceId,
        simulationTimeMillis: Long,
        heavyCooldownReadyAtMillis: Long?,
        queuedHeavy: Boolean
    ): GameState {
        val cooldowns = if (heavyCooldownReadyAtMillis == null) {
            CooldownState()
        } else {
            CooldownState(
                readyAtByActionId = mapOf(
                    DefaultGameContent.HEAVY_STRIKE_ID to
                        GameTime.ofMillis(heavyCooldownReadyAtMillis)
                )
            )
        }
        val playerCombatant = CombatantState(
            instanceId = playerId,
            currentHealth = GameNumber.of(100L),
            cooldowns = cooldowns
        )
        val enemy = EnemyState(
            instanceId = enemyId,
            definitionId = DefaultGameContent.SLIME_ID,
            combatant = CombatantState(
                instanceId = enemyId,
                currentHealth = GameNumber.of(100L)
            )
        )
        val world = WorldState(
            activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
            currentEncounter = EncounterState(
                definitionId = DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID,
                encounterIndex = 1L,
                encounterSeed = 803L,
                spawnedEnemyIds = listOf(enemyId)
            )
        )
        val combat = CombatState(
            status = CombatStatus.ACTIVE,
            playerCombatant = playerCombatant,
            enemies = listOf(enemy),
            nextPlayerDecisionAt = GameTime.ofMillis(simulationTimeMillis + 1_000L),
            combatSequenceId = 1L,
            encounterStartedAt = GameTime.ZERO,
            queuedPlayerAction = if (queuedHeavy) {
                QueuedPlayerAction.Skill(DefaultGameContent.HEAVY_STRIKE_ID)
            } else {
                null
            }
        )
        return GameState.newGame(randomSeed = 803L).copy(
            engine = EngineState(
                simulationTime = GameTime.ofMillis(simulationTimeMillis)
            ),
            run = GameState.newGame(randomSeed = 803L).run.copy(
                player = PlayerState(
                    equippedSkillIds = listOf(
                        DefaultGameContent.HEAVY_STRIKE_ID,
                        DefaultGameContent.QUICK_SLASH_ID
                    )
                ),
                combat = combat,
                world = world,
                economy = EconomyState(
                    wallet = CurrencyWallet(
                        amountsByCurrencyId = mapOf(CurrencyId.GOLD to GameNumber.of(100L))
                    )
                ),
                resonance = ResonanceState(
                    chargeByAffinityId = mapOf(Affinity.MIGHT.id to GameNumber.of(2L)),
                    sequence = ResonanceSequenceState(listOf(Affinity.MIGHT.id))
                )
            )
        )
    }

    private class ProjectionReadQueries : GameReadQueries {
        override fun attackPower(state: GameState): GameNumber = GameNumber.of(10L)
        override fun armor(state: GameState): GameNumber = GameNumber.ZERO
        override fun basicAttackInterval(state: GameState): GameDuration =
            GameDuration.ofSeconds(1L)
        override fun basicAttackDps(state: GameState): GameRate = GameRate.of(10L)
        override fun enemyMaximumHealth(
            state: GameState,
            enemy: EnemyState
        ): GameNumber = GameNumber.of(100L)
        override fun skillQueueRejection(
            state: GameState,
            skillId: ContentId
        ): CommandRejectionReason? = null
        override fun skillExecutionRejection(
            state: GameState,
            skillId: ContentId
        ): CommandRejectionReason? {
            val player = state.run.combat.playerCombatant ?: return CommandRejectionReason(
                CommandRejectionCode.INVALID_STATE,
                skillId
            )
            val readyAt = player.cooldowns.readyAtByActionId[skillId] ?: return null
            return if (readyAt > state.engine.simulationTime) {
                CommandRejectionReason(CommandRejectionCode.COOLDOWN_ACTIVE, skillId)
            } else {
                null
            }
        }
        override fun upgradeLevel(state: GameState, upgradeId: ContentId): Long = 0L
        override fun upgradeCurrentCost(state: GameState, upgradeId: ContentId): GameNumber =
            GameNumber.of(20L)
        override fun playerExperienceToNextLevel(state: GameState): GameNumber =
            GameNumber.of(100L)
        override fun masteryLevel(state: GameState, affinityId: ContentId): Long = 0L
        override fun masteryExperienceToNextLevel(
            state: GameState,
            affinityId: ContentId
        ): GameNumber = GameNumber.of(100L)
        override fun isFeatureUnlocked(state: GameState, featureId: ContentId): Boolean = false
        override fun chronicleEligible(state: GameState): Boolean = false
        override fun doctrineCapacity(state: GameState): Int = 8
        override fun doctrineConditionMaxDepth(): Int = 4
        override fun resonanceChargeCap(): GameNumber = GameNumber.of(100L)
        override fun skillLoadoutCapacity(): Int = 4
        override fun effectiveInventoryCapacity(state: GameState): Long = 60L
        override fun inventoryOverflowCapacity(): Long = 20L
        override fun nextInventoryExpansionCost(state: GameState): GameNumber = GameNumber.of(100L)
        override fun inventoryProgressionBlocked(state: GameState): Boolean = false
    }
}
