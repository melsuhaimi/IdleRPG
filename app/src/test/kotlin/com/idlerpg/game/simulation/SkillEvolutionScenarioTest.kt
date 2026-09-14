package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveDataException
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.local.migration.SaveMigrationException
import com.idlerpg.game.data.local.migration.SaveMigrationRegistry
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.command.SelectSkillEvolution
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.event.SkillEvolutionSelected
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.event.StatusApplied
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.StatusEffectState
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.skill.SkillEvolutionSystem

/** Persistent, mastery-gated evolution choices and observable archetype tradeoffs. */
object SkillEvolutionScenarioTest {
    fun run() {
        val registry = SimulationTestSupport.factory().contentRegistry
        val evolvedSkillIds = setOf(
            DefaultGameContent.HEAVY_STRIKE_ID,
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.FROST_LANCE_ID,
            DefaultGameContent.UMBRAL_CUT_ID
        )
        check(registry.allSkillEvolutions().size == 8)
        check(registry.allSkillEvolutions().groupBy { it.baseSkillId }.keys == evolvedSkillIds)
        evolvedSkillIds.forEach { skillId ->
            check(registry.skillEvolutionsFor(skillId).size == 2)
        }
        registry.allSkillEvolutions().forEach { evolution ->
            val base = registry.skill(evolution.baseSkillId)
            val selected = GameState.newGame(6_099L).let { state ->
                state.copy(run = state.run.copy(player = state.run.player.copy(
                    selectedSkillEvolutionBySkillId = mapOf(base.id to evolution.id)
                )))
            }
            val effective = SkillEvolutionSystem.effectiveDefinition(selected, base, registry)
            check(effective.effects == evolution.replacementEffects)
            check(effective.copy(effects = base.effects) == base)
        }

        heavyStrikeSelectionAndReplacement()
        masteryBoundariesAndIndependentSelections()
        archetypeBranchesAlterOutcomes()
        staleSelectionAndSaveCompatibility()
    }

    private fun heavyStrikeSelectionAndReplacement() {
        val runtime = SimulationTestSupport.runtime(seed = 6_100L)
        val locked = runtime.dispatch(
            SelectSkillEvolution(
                DefaultGameContent.HEAVY_STRIKE_ID,
                DefaultGameContent.EARTHBREAKER_EVOLUTION_ID
            )
        )
        check((locked.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.LOCKED)

        setMasteryExperience(runtime, Affinity.MIGHT, 49L)
        val belowBoundary = runtime.dispatch(
            SelectSkillEvolution(
                DefaultGameContent.HEAVY_STRIKE_ID,
                DefaultGameContent.EARTHBREAKER_EVOLUTION_ID
            )
        )
        check((belowBoundary.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.LOCKED)

        setMasteryExperience(runtime, Affinity.MIGHT, 50L)
        val earthbreaker = runtime.dispatch(
            SelectSkillEvolution(
                DefaultGameContent.HEAVY_STRIKE_ID,
                DefaultGameContent.EARTHBREAKER_EVOLUTION_ID
            )
        )
        SimulationTestSupport.checkAccepted(earthbreaker)
        check(earthbreaker.events.single().event is SkillEvolutionSelected)

        val registry = SimulationTestSupport.factory().contentRegistry
        val base = registry.skill(DefaultGameContent.HEAVY_STRIKE_ID)
        val earthbreakerDefinition = SkillEvolutionSystem.effectiveDefinition(runtime.state(), base, registry)
        check(earthbreakerDefinition.copy(effects = base.effects) == base)
        check(earthbreakerDefinition.effects.filterIsInstance<EffectSpec.DealDamage>()
            .any { it.targetPattern == EffectSpec.TargetPattern.ADJACENT_ENEMIES })

        SimulationTestSupport.checkAccepted(
            runtime.dispatch(
                SelectSkillEvolution(
                    DefaultGameContent.HEAVY_STRIKE_ID,
                    DefaultGameContent.EXECUTIONER_EVOLUTION_ID
                )
            )
        )
        val executionerDefinition = SkillEvolutionSystem.effectiveDefinition(runtime.state(), base, registry)
        check(executionerDefinition.copy(effects = base.effects) == base)
        check(executionerDefinition.effects.filterIsInstance<EffectSpec.DealDamage>()
            .flatMap { it.conditions }
            .any { it is EffectSpec.DamageCondition.TargetHealthAtOrBelow })
        check(executionerDefinition.effects.none {
            it is EffectSpec.DealDamage && it.targetPattern == EffectSpec.TargetPattern.ADJACENT_ENEMIES
        })
    }

    private fun masteryBoundariesAndIndependentSelections() {
        data class EvolutionGate(
            val skillId: ContentId,
            val evolutionId: ContentId,
            val affinity: Affinity,
            val featureId: ContentId
        )

        val gates = listOf(
            EvolutionGate(
                DefaultGameContent.FLAME_BRAND_ID,
                DefaultGameContent.WILDSPARK_EVOLUTION_ID,
                Affinity.EMBER,
                DefaultGameContent.FLAME_BRAND_FEATURE_ID
            ),
            EvolutionGate(
                DefaultGameContent.FROST_LANCE_ID,
                DefaultGameContent.PERMAFROST_EVOLUTION_ID,
                Affinity.FROST,
                DefaultGameContent.FROST_LANCE_FEATURE_ID
            ),
            EvolutionGate(
                DefaultGameContent.UMBRAL_CUT_ID,
                DefaultGameContent.REAPERS_ARC_EVOLUTION_ID,
                Affinity.SHADOW,
                DefaultGameContent.UMBRAL_CUT_FEATURE_ID
            )
        )

        val selections = linkedMapOf<ContentId, ContentId>(
            DefaultGameContent.HEAVY_STRIKE_ID to DefaultGameContent.EARTHBREAKER_EVOLUTION_ID
        )
        gates.forEachIndexed { index, gate ->
            val runtime = SimulationTestSupport.runtime(seed = 6_110L + index)
            unlockFeature(runtime, gate.featureId)
            setMasteryExperience(runtime, gate.affinity, 49L)
            val locked = runtime.dispatch(SelectSkillEvolution(gate.skillId, gate.evolutionId))
            check((locked.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.LOCKED)
            setMasteryExperience(runtime, gate.affinity, 50L)
            SimulationTestSupport.checkAccepted(
                runtime.dispatch(SelectSkillEvolution(gate.skillId, gate.evolutionId))
            )
            selections[gate.skillId] = gate.evolutionId
        }

        val combined = SimulationTestSupport.runtime(seed = 6_119L)
        gates.forEach { unlockFeature(combined, it.featureId) }
        setMasteryExperience(combined, Affinity.MIGHT, 50L)
        gates.forEach { setMasteryExperience(combined, it.affinity, 50L) }
        SimulationTestSupport.checkAccepted(combined.dispatch(SelectSkillEvolution(
            DefaultGameContent.HEAVY_STRIKE_ID,
            DefaultGameContent.EARTHBREAKER_EVOLUTION_ID
        )))
        gates.forEach { gate ->
            SimulationTestSupport.checkAccepted(
                combined.dispatch(SelectSkillEvolution(gate.skillId, gate.evolutionId))
            )
        }
        val beforeSwitch = combined.state().run.player.selectedSkillEvolutionBySkillId
        SimulationTestSupport.checkAccepted(combined.dispatch(SelectSkillEvolution(
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.SEARING_BRAND_EVOLUTION_ID
        )))
        val afterSwitch = combined.state().run.player.selectedSkillEvolutionBySkillId
        check(afterSwitch.size == 4)
        check(afterSwitch[DefaultGameContent.FLAME_BRAND_ID] ==
            DefaultGameContent.SEARING_BRAND_EVOLUTION_ID)
        check(afterSwitch.filterKeys { it != DefaultGameContent.FLAME_BRAND_ID } ==
            beforeSwitch.filterKeys { it != DefaultGameContent.FLAME_BRAND_ID })

        val state = GameState.newGame(6_120L).let { base ->
            base.copy(run = base.run.copy(player = base.run.player.copy(
                selectedSkillEvolutionBySkillId = selections
            )))
        }
        val restored = SaveData.fromGameState(state).toGameState()
        check(restored.run.player.selectedSkillEvolutionBySkillId == selections)
    }

    private fun archetypeBranchesAlterOutcomes() {
        val earthbreaker = executeEvolution(
            DefaultGameContent.HEAVY_STRIKE_ID,
            DefaultGameContent.EARTHBREAKER_EVOLUTION_ID,
            DefaultGameContent.HEAVY_STRIKE_ID,
            enemyCount = 4,
            seed = 6_201L
        )
        check(playerDamageEvents(earthbreaker).map { it.targetInstanceId }.distinct().size == 2)

        val executionerHealthy = executeEvolution(
            DefaultGameContent.HEAVY_STRIKE_ID,
            DefaultGameContent.EXECUTIONER_EVOLUTION_ID,
            DefaultGameContent.HEAVY_STRIKE_ID,
            primaryHealth = GameNumber.of(100L),
            seed = 6_202L
        )
        val executionerWeakened = executeEvolution(
            DefaultGameContent.HEAVY_STRIKE_ID,
            DefaultGameContent.EXECUTIONER_EVOLUTION_ID,
            DefaultGameContent.HEAVY_STRIKE_ID,
            primaryHealth = GameNumber.of(25L),
            seed = 6_202L
        )
        check(playerDamageEvents(executionerWeakened).single().amount >
            playerDamageEvents(executionerHealthy).single().amount)

        val wildspark = executeEvolution(
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.WILDSPARK_EVOLUTION_ID,
            DefaultGameContent.FLAME_BRAND_FEATURE_ID,
            enemyCount = 3,
            seed = 6_203L
        )
        check(playerDamageEvents(wildspark).map { it.targetInstanceId }.distinct().size == 3)
        check(playerDamageEvents(wildspark).all { it.damageKindId == DamageKind.ELEMENTAL.id })
        check(wildspark.events.map { it.event }.filterIsInstance<StatusApplied>()
            .count { it.statusDefinitionId == DefaultGameContent.BURNING_STATUS_ID } == 3)

        val searing = executeEvolution(
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.SEARING_BRAND_EVOLUTION_ID,
            DefaultGameContent.FLAME_BRAND_FEATURE_ID,
            enemyCount = 3,
            seed = 6_204L
        )
        check(playerDamageEvents(searing).map { it.targetInstanceId }.distinct().size == 1)
        check(playerDamageEvents(searing).single().damageKindId == DamageKind.ELEMENTAL.id)
        val searingTargetId = playerDamageEvents(searing).single().targetInstanceId
        check(searing.state.run.combat.enemies.single { it.instanceId == searingTargetId }
            .combatant.statusEffects.single { it.definitionId == DefaultGameContent.BURNING_STATUS_ID }
            .stackCount == 2)

        val permafrost = executeEvolution(
            DefaultGameContent.FROST_LANCE_ID,
            DefaultGameContent.PERMAFROST_EVOLUTION_ID,
            DefaultGameContent.FROST_LANCE_FEATURE_ID,
            enemyCount = 3,
            seed = 6_205L
        )
        check(playerDamageEvents(permafrost).map { it.targetInstanceId }.distinct().size == 3)
        check(playerDamageEvents(permafrost).all { it.damageKindId == DamageKind.ELEMENTAL.id })
        check(permafrost.events.map { it.event }.filterIsInstance<StatusApplied>()
            .count { it.statusDefinitionId == DefaultGameContent.CHILL_STATUS_ID } == 3)

        val shatterUnchilled = executeEvolution(
            DefaultGameContent.FROST_LANCE_ID,
            DefaultGameContent.SHATTER_SPEAR_EVOLUTION_ID,
            DefaultGameContent.FROST_LANCE_FEATURE_ID,
            seed = 6_206L
        )
        val shatterChilled = executeEvolution(
            DefaultGameContent.FROST_LANCE_ID,
            DefaultGameContent.SHATTER_SPEAR_EVOLUTION_ID,
            DefaultGameContent.FROST_LANCE_FEATURE_ID,
            chilled = true,
            seed = 6_206L
        )
        check(playerDamageEvents(shatterChilled).single().amount >
            playerDamageEvents(shatterUnchilled).single().amount)
        check(playerDamageEvents(shatterChilled).single().damageKindId == DamageKind.ELEMENTAL.id)
        check(shatterChilled.state.run.combat.enemies.single().combatant.statusEffects
            .none { it.definitionId == DefaultGameContent.CHILL_STATUS_ID })

        val reapersArc = executeEvolution(
            DefaultGameContent.UMBRAL_CUT_ID,
            DefaultGameContent.REAPERS_ARC_EVOLUTION_ID,
            DefaultGameContent.UMBRAL_CUT_FEATURE_ID,
            enemyCount = 3,
            seed = 6_207L
        )
        check(playerDamageEvents(reapersArc).map { it.targetInstanceId }.distinct().size == 3)
        check(playerDamageEvents(reapersArc).all { it.damageKindId == DamageKind.SHADOW.id })

        val sanguine = executeEvolution(
            DefaultGameContent.UMBRAL_CUT_ID,
            DefaultGameContent.SANGUINE_EDGE_EVOLUTION_ID,
            DefaultGameContent.UMBRAL_CUT_FEATURE_ID,
            enemyCount = 3,
            playerHealth = GameNumber.of(40L),
            seed = 6_208L
        )
        check(playerDamageEvents(sanguine).map { it.targetInstanceId }.distinct().size == 1)
        check(playerDamageEvents(sanguine).single().damageKindId == DamageKind.SHADOW.id)
        check(sanguine.events.map { it.event }.filterIsInstance<HealingApplied>().single().amount >
            GameNumber.ZERO)

        // A lethal hit followed by a status/removal effect must be a deterministic no-op,
        // not an attempt to mutate a combatant that is already dead.
        val lethalShatter = executeEvolution(
            DefaultGameContent.FROST_LANCE_ID,
            DefaultGameContent.SHATTER_SPEAR_EVOLUTION_ID,
            DefaultGameContent.FROST_LANCE_FEATURE_ID,
            primaryHealth = GameNumber.ONE,
            chilled = true,
            seed = 6_209L
        )
        check(playerDamageEvents(lethalShatter).single().amount > GameNumber.ZERO)

        val invalidRuntime = SimulationTestSupport.runtime(seed = 6_211L)
        SimulationTestSupport.startTraining(invalidRuntime)
        val invalidState = invalidRuntime.state()
        val invalidPlayerId = invalidState.run.combat.playerCombatant?.instanceId
            ?: error("Missing player combatant")
        val invalidEnemy = invalidState.run.combat.enemies.single()
        val deadTargetState = invalidState.copy(run = invalidState.run.copy(
            combat = invalidState.run.combat.copy(enemies = listOf(
                invalidEnemy.copy(combatant = invalidEnemy.combatant.copy(
                    currentHealth = GameNumber.ZERO
                ))
            ))
        ))
        val invalidTargetFailure = runCatching {
            ActionResolutionSystem.resolvePrimitiveEffects(
                deadTargetState,
                invalidPlayerId,
                invalidEnemy.instanceId,
                listOf(EffectSpec.DealDamage(canCritical = false)),
                SimulationTestSupport.factory().createEngineContext()
            )
        }.exceptionOrNull()
        check(invalidTargetFailure is IllegalArgumentException)

        val deterministicA = executeEvolution(
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.WILDSPARK_EVOLUTION_ID,
            DefaultGameContent.FLAME_BRAND_FEATURE_ID,
            enemyCount = 3,
            seed = 6_210L
        )
        val deterministicB = executeEvolution(
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.WILDSPARK_EVOLUTION_ID,
            DefaultGameContent.FLAME_BRAND_FEATURE_ID,
            enemyCount = 3,
            seed = 6_210L
        )
        check(deterministicA == deterministicB)
    }

    private fun staleSelectionAndSaveCompatibility() {
        val registry = SimulationTestSupport.factory().contentRegistry
        val base = registry.skill(DefaultGameContent.HEAVY_STRIKE_ID)
        val staleSelection = GameState.newGame(6_300L).let { state ->
            state.copy(run = state.run.copy(player = state.run.player.copy(
                selectedSkillEvolutionBySkillId = mapOf(
                    DefaultGameContent.HEAVY_STRIKE_ID to ContentId("evolution.removed")
                )
            )))
        }
        check(SkillEvolutionSystem.effectiveDefinition(staleSelection, base, registry) == base)

        val current = GameState.newGame(6_301L).let { state ->
            state.copy(run = state.run.copy(player = state.run.player.copy(
                selectedSkillEvolutionBySkillId = mapOf(
                    DefaultGameContent.HEAVY_STRIKE_ID to DefaultGameContent.EXECUTIONER_EVOLUTION_ID,
                    DefaultGameContent.FLAME_BRAND_ID to DefaultGameContent.WILDSPARK_EVOLUTION_ID,
                    DefaultGameContent.FROST_LANCE_ID to DefaultGameContent.PERMAFROST_EVOLUTION_ID,
                    DefaultGameContent.UMBRAL_CUT_ID to DefaultGameContent.SANGUINE_EDGE_EVOLUTION_ID
                )
            )))
        }
        val persisted = SaveData.fromGameState(current).toGameState()
        check(persisted.run.player.selectedSkillEvolutionBySkillId ==
            current.run.player.selectedSkillEvolutionBySkillId)

        val v5Fields = SaveData.fromGameState(current).fields
            .filterKeys {
                !it.startsWith("run.player.selectedSkillEvolutionBySkillId") &&
                    !it.startsWith("meta.heroName")
            }
        val hostileV5Fields = v5Fields + mapOf(
            "run.player.selectedSkillEvolutionBySkillId.count" to "1",
            "run.player.selectedSkillEvolutionBySkillId.0.key" to DefaultGameContent.HEAVY_STRIKE_ID.value,
            "run.player.selectedSkillEvolutionBySkillId.0.value" to "evolution.removed"
        )
        val collisionFailure = runCatching {
            SaveMigrationRegistry().migrate(
                SaveEnvelope(
                    SaveVersion.V5,
                    SimulationTestSupport.CONTENT_VERSION,
                    6_301_000L,
                    SaveData(hostileV5Fields)
                )
            )
        }.exceptionOrNull()
        check(collisionFailure is SaveMigrationException)
        check(collisionFailure.cause is SaveDataException)

        val migrated = SaveMigrationRegistry().migrate(
            SaveEnvelope(
                SaveVersion.V5,
                SimulationTestSupport.CONTENT_VERSION,
                6_301_000L,
                SaveData(v5Fields)
            ),
            targetVersion = SaveVersion.V6
        )
        check(migrated.schemaVersion == SaveVersion.V6)
        check(migrated.data.toGameState().run.player.selectedSkillEvolutionBySkillId.isEmpty())
    }

    private fun executeEvolution(
        skillId: ContentId,
        evolutionId: ContentId,
        featureId: ContentId,
        enemyCount: Int = 1,
        primaryHealth: GameNumber = GameNumber.of(10_000L),
        playerHealth: GameNumber = GameNumber.of(100L),
        chilled: Boolean = false,
        seed: Long
    ): EngineResult {
        val runtime = SimulationTestSupport.runtime(seed = seed)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        val original = started.run.combat.enemies.single()
        val playerId = started.run.combat.playerCombatant?.instanceId
            ?: error("Missing player combatant")
        val enemies = (0 until enemyCount).map { index ->
            val instanceId = if (index == 0) {
                original.instanceId
            } else {
                InstanceId(original.instanceId.value + index * 10_000L)
            }
            val statusEffects = if (index == 0 && chilled) {
                listOf(
                    StatusEffectState(
                        instanceId = InstanceId(original.instanceId.value + 90_000L),
                        definitionId = DefaultGameContent.CHILL_STATUS_ID,
                        sourceInstanceId = playerId,
                        appliedAt = started.engine.simulationTime,
                        expiresAt = started.engine.simulationTime + GameDuration.ofSeconds(10L)
                    )
                )
            } else emptyList()
            original.copy(
                instanceId = instanceId,
                combatant = original.combatant.copy(
                    instanceId = instanceId,
                    currentHealth = if (index == 0) primaryHealth else GameNumber.of(10_000L),
                    statusEffects = statusEffects
                )
            )
        }
        val playerCombatant = started.run.combat.playerCombatant
            ?: error("Missing player combatant")
        runtime.replaceLoadedState(
            started.copy(
                run = started.run.copy(
                    player = started.run.player.copy(
                        currentHealth = playerHealth,
                        equippedSkillIds = listOf(skillId),
                        selectedSkillEvolutionBySkillId = mapOf(skillId to evolutionId)
                    ),
                    progression = started.run.progression.copy(
                        featureUnlocks = started.run.progression.featureUnlocks.copy(
                            unlockedFeatureIds = started.run.progression.featureUnlocks.unlockedFeatureIds + featureId
                        )
                    ),
                    combat = started.run.combat.copy(
                        playerCombatant = playerCombatant.copy(currentHealth = playerHealth),
                        enemies = enemies
                    )
                )
            )
        )
        SimulationTestSupport.checkAccepted(runtime.dispatch(QueueSkillCast(skillId)))
        return runtime.advance(GameDuration.ofSeconds(1L))
    }

    private fun playerDamageEvents(result: EngineResult): List<DamageDealt> {
        val playerId = result.events.map { it.event }.filterIsInstance<SkillUsed>()
            .single().actorInstanceId
        return result.events.map { it.event }.filterIsInstance<DamageDealt>()
            .filter { it.sourceInstanceId == playerId }
    }

    private fun unlockFeature(runtime: GameRuntime, featureId: ContentId) {
        val state = runtime.state()
        runtime.replaceLoadedState(
            state.copy(run = state.run.copy(progression = state.run.progression.copy(
                featureUnlocks = state.run.progression.featureUnlocks.copy(
                    unlockedFeatureIds = state.run.progression.featureUnlocks.unlockedFeatureIds + featureId
                )
            )))
        )
    }

    private fun setMasteryExperience(
        runtime: GameRuntime,
        affinity: Affinity,
        experience: Long
    ) {
        val state = runtime.state()
        runtime.replaceLoadedState(
            state.copy(run = state.run.copy(progression = state.run.progression.copy(
                affinityMastery = state.run.progression.affinityMastery.copy(
                    experienceByAffinityId = state.run.progression.affinityMastery.experienceByAffinityId +
                        (affinity.id to GameNumber.of(experience))
                )
            )))
        )
    }
}
