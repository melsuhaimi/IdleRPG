package com.idlerpg.game.data.local

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.random.RandomState
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.EngineState
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.MetaState
import com.idlerpg.game.domain.model.RunState
import com.idlerpg.game.domain.model.achievement.AchievementProgressState
import com.idlerpg.game.domain.model.achievement.AchievementState
import com.idlerpg.game.domain.model.adaptation.ActiveMutationState
import com.idlerpg.game.domain.model.adaptation.AdaptationState
import com.idlerpg.game.domain.model.adaptation.AffinityExposureState
import com.idlerpg.game.domain.model.adaptation.RegionAdaptationState
import com.idlerpg.game.domain.model.chronicle.ChronicleState
import com.idlerpg.game.domain.model.chronicle.DiscoveryState
import com.idlerpg.game.domain.model.chronicle.EchoState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.CooldownState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.combat.StatusEffectState
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineRule
import com.idlerpg.game.domain.model.doctrine.DoctrineState
import com.idlerpg.game.domain.model.doctrine.DoctrineStatusTarget
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.model.economy.EconomyState
import com.idlerpg.game.domain.model.economy.UpgradeProgressState
import com.idlerpg.game.domain.model.inventory.EquipmentLoadoutState
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.ItemLockState
import com.idlerpg.game.domain.model.inventory.RolledAffix
import com.idlerpg.game.domain.model.player.BaseStats
import com.idlerpg.game.domain.model.player.PlayerState
import com.idlerpg.game.domain.model.player.ResourceState
import com.idlerpg.game.domain.model.progression.AffinityMasteryState
import com.idlerpg.game.domain.model.progression.FeatureUnlockState
import com.idlerpg.game.domain.model.progression.PlayerLevelState
import com.idlerpg.game.domain.model.progression.ProgressionState
import com.idlerpg.game.domain.model.progression.SkillProgressionState
import com.idlerpg.game.domain.model.rebirth.RebirthState
import com.idlerpg.game.domain.model.rebirth.RebirthStat
import com.idlerpg.game.domain.model.quest.QuestProgressState
import com.idlerpg.game.domain.model.quest.QuestState
import com.idlerpg.game.domain.model.resonance.ConvergenceState
import com.idlerpg.game.domain.model.resonance.ResonanceSequenceState
import com.idlerpg.game.domain.model.resonance.ResonanceState
import com.idlerpg.game.domain.model.statistics.StatisticsState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.domain.model.world.WorldState

/** Logical persistence-DTO decoding failure. */
class SaveDataException(
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

/**
 * Schema-versioned logical save payload.
 *
 * Foundation 15 deliberately uses an explicit flat DTO field map instead of serializing
 * domain data classes directly. The field keys are the save schema. Migrations operate on
 * this representation before it is reconstructed as current [GameState].
 *
 * This means ordinary Kotlin class refactors do not automatically become save-format
 * changes. A deliberate field-key/schema change requires a SaveMigration.
 */
class SaveData(
    fields: Map<String, String>
) {
    val fields: Map<String, String> = fields.toSortedMap()

    init {
        require(this.fields.keys.all { it.isNotBlank() }) {
            "SaveData field keys cannot be blank"
        }
    }

    fun toGameState(): GameState =
        SaveDataMapper.decode(this)

    override fun equals(other: Any?): Boolean =
        other is SaveData && fields == other.fields

    override fun hashCode(): Int =
        fields.hashCode()

    override fun toString(): String =
        "SaveData(fields=${fields.size})"

    companion object {
        fun fromGameState(state: GameState): SaveData =
            SaveDataMapper.encode(state)
    }
}

private object SaveDataMapper {

    fun encode(state: GameState): SaveData {
        val writer = FieldWriter()

        writeEngineState(writer, "engine", state.engine)
        writeRunState(writer, "run", state.run)
        writeMetaState(writer, "meta", state.meta)

        return writer.build()
    }

    fun decode(data: SaveData): GameState {
        val reader = FieldReader(data.fields)

        val state = GameState(
            engine = readEngineState(reader, "engine"),
            run = readRunState(reader, "run"),
            meta = readMetaState(reader, "meta")
        )

        reader.requireFullyConsumed()
        return state
    }

    // -------------------------------------------------------------------------
    // Root state
    // -------------------------------------------------------------------------

    private fun writeEngineState(
        writer: FieldWriter,
        path: String,
        state: EngineState
    ) {
        writer.gameTime("$path.simulationTime", state.simulationTime)
        writer.long("$path.randomState.state", state.randomState.state)
        writer.int(
            "$path.randomState.algorithmVersion",
            state.randomState.algorithmVersion
        )
        writer.long(
            "$path.nextEventSequenceNumber",
            state.nextEventSequenceNumber
        )
        writer.long(
            "$path.nextInstanceIdCounter",
            state.nextInstanceIdCounter
        )
    }

    private fun readEngineState(
        reader: FieldReader,
        path: String
    ): EngineState =
        EngineState(
            simulationTime = reader.gameTime("$path.simulationTime"),
            randomState = RandomState(
                state = reader.long("$path.randomState.state"),
                algorithmVersion = reader.int(
                    "$path.randomState.algorithmVersion"
                )
            ),
            nextEventSequenceNumber = reader.long(
                "$path.nextEventSequenceNumber"
            ),
            nextInstanceIdCounter = reader.long(
                "$path.nextInstanceIdCounter"
            )
        )

    private fun writeRunState(
        writer: FieldWriter,
        path: String,
        state: RunState
    ) {
        writePlayerState(writer, "$path.player", state.player)
        writeCombatState(writer, "$path.combat", state.combat)
        writeWorldState(writer, "$path.world", state.world)
        writeEconomyState(writer, "$path.economy", state.economy)
        writeResonanceState(writer, "$path.resonance", state.resonance)
        writeDoctrineState(writer, "$path.doctrine", state.doctrine)
        writeAdaptationState(writer, "$path.adaptation", state.adaptation)
        writeInventoryState(writer, "$path.inventory", state.inventory)
        writeProgressionState(writer, "$path.progression", state.progression)
        writeQuestState(writer, "$path.quests", state.quests)
        writeStatisticsState(writer, "$path.statistics", state.statistics)
    }

    private fun readRunState(
        reader: FieldReader,
        path: String
    ): RunState =
        RunState(
            player = readPlayerState(reader, "$path.player"),
            combat = readCombatState(reader, "$path.combat"),
            world = readWorldState(reader, "$path.world"),
            economy = readEconomyState(reader, "$path.economy"),
            resonance = readResonanceState(reader, "$path.resonance"),
            doctrine = readDoctrineState(reader, "$path.doctrine"),
            adaptation = readAdaptationState(reader, "$path.adaptation"),
            inventory = readInventoryState(reader, "$path.inventory"),
            progression = readProgressionState(reader, "$path.progression"),
            quests = readQuestState(reader, "$path.quests"),
            statistics = readStatisticsState(reader, "$path.statistics")
        )

    private fun writeMetaState(
        writer: FieldWriter,
        path: String,
        state: MetaState
    ) {
        writer.optionalString("$path.heroName", state.heroName)
        writeChronicleState(writer, "$path.chronicle", state.chronicle)
        writeEchoState(writer, "$path.echoes", state.echoes)
        writeDiscoveryState(writer, "$path.discoveries", state.discoveries)
        writeFeatureUnlockState(
            writer,
            "$path.persistentFeatureUnlocks",
            state.persistentFeatureUnlocks
        )
        writeAchievementState(
            writer,
            "$path.achievements",
            state.achievements
        )
        writeStatisticsState(
            writer,
            "$path.lifetimeStatistics",
            state.lifetimeStatistics
        )
        writeRebirthState(writer, "$path.rebirth", state.rebirth)
    }

    private fun readMetaState(
        reader: FieldReader,
        path: String
    ): MetaState =
        MetaState(
            heroName = reader.optionalString("$path.heroName"),
            chronicle = readChronicleState(reader, "$path.chronicle"),
            echoes = readEchoState(reader, "$path.echoes"),
            discoveries = readDiscoveryState(reader, "$path.discoveries"),
            persistentFeatureUnlocks = readFeatureUnlockState(
                reader,
                "$path.persistentFeatureUnlocks"
            ),
            achievements = readAchievementState(
                reader,
                "$path.achievements"
            ),
            lifetimeStatistics = readStatisticsState(
                reader,
                "$path.lifetimeStatistics"
            ),
            rebirth = readRebirthState(reader, "$path.rebirth")
        )

    private fun writeRebirthState(
        writer: FieldWriter,
        path: String,
        state: RebirthState
    ) {
        writer.long("$path.completedRebirths", state.completedRebirths)
        writer.long("$path.normalPointsEarned", state.normalPointsEarned)
        writer.long("$path.legacyPointsEarned", state.legacyPointsEarned)
        writeRebirthAllocations(writer, "$path.normalAllocations", state.normalAllocations)
        writeRebirthAllocations(writer, "$path.legacyAllocations", state.legacyAllocations)
    }

    private fun readRebirthState(
        reader: FieldReader,
        path: String
    ): RebirthState =
        RebirthState(
            completedRebirths = reader.long("$path.completedRebirths"),
            normalPointsEarned = reader.long("$path.normalPointsEarned"),
            legacyPointsEarned = reader.long("$path.legacyPointsEarned"),
            normalAllocations = readRebirthAllocations(
                reader,
                "$path.normalAllocations"
            ),
            legacyAllocations = readRebirthAllocations(
                reader,
                "$path.legacyAllocations"
            )
        )

    private fun writeRebirthAllocations(
        writer: FieldWriter,
        path: String,
        allocations: Map<RebirthStat, Long>
    ) {
        val entries = allocations.entries.sortedBy { it.key.name }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.string("$entryPath.stat", entry.key.name)
            writer.long("$entryPath.points", entry.value)
        }
    }

    private fun readRebirthAllocations(
        reader: FieldReader,
        path: String
    ): Map<RebirthStat, Long> {
        val result = linkedMapOf<RebirthStat, Long>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val stat = try {
                RebirthStat.valueOf(reader.string("$entryPath.stat"))
            } catch (error: SaveDataException) {
                throw error
            } catch (error: Throwable) {
                throw SaveDataException(
                    "Unknown RebirthStat at $entryPath",
                    error
                )
            }
            val previous = result.put(stat, reader.long("$entryPath.points"))
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate RebirthStat allocation at $entryPath: $stat"
                )
            }
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Player
    // -------------------------------------------------------------------------

    private fun writePlayerState(
        writer: FieldWriter,
        path: String,
        state: PlayerState
    ) {
        writeBaseStats(writer, "$path.baseStats", state.baseStats)
        writer.gameNumber("$path.currentHealth", state.currentHealth)
        writeResourceState(writer, "$path.resources", state.resources)
        writeContentIdList(
            writer,
            "$path.equippedSkillIds",
            state.equippedSkillIds
        )
        writeContentIdMap(
            writer,
            "$path.selectedSkillEvolutionBySkillId",
            state.selectedSkillEvolutionBySkillId
        )
    }

    private fun readPlayerState(
        reader: FieldReader,
        path: String
    ): PlayerState =
        PlayerState(
            baseStats = readBaseStats(reader, "$path.baseStats"),
            currentHealth = reader.gameNumber("$path.currentHealth"),
            resources = readResourceState(reader, "$path.resources"),
            equippedSkillIds = readContentIdList(
                reader,
                "$path.equippedSkillIds"
            ),
            selectedSkillEvolutionBySkillId = readContentIdMap(
                reader,
                "$path.selectedSkillEvolutionBySkillId"
            )
        )

    private fun writeBaseStats(
        writer: FieldWriter,
        path: String,
        state: BaseStats
    ) {
        writer.gameNumber("$path.attackPower", state.attackPower)
        writer.gameNumber("$path.maxHealth", state.maxHealth)
        writer.gameNumber("$path.armor", state.armor)
        writer.ratio("$path.actionSpeed", state.actionSpeed)
        writer.ratio("$path.criticalChance", state.criticalChance)
        writer.ratio(
            "$path.criticalMultiplier",
            state.criticalMultiplier
        )
        writer.ratio("$path.effectPower", state.effectPower)
        writer.ratio("$path.healingPower", state.healingPower)
    }

    private fun readBaseStats(
        reader: FieldReader,
        path: String
    ): BaseStats =
        BaseStats(
            attackPower = reader.gameNumber("$path.attackPower"),
            maxHealth = reader.gameNumber("$path.maxHealth"),
            armor = reader.gameNumber("$path.armor"),
            actionSpeed = reader.ratio("$path.actionSpeed"),
            criticalChance = reader.ratio("$path.criticalChance"),
            criticalMultiplier = reader.ratio(
                "$path.criticalMultiplier"
            ),
            effectPower = reader.ratio("$path.effectPower"),
            healingPower = reader.ratio("$path.healingPower")
        )

    private fun writeResourceState(
        writer: FieldWriter,
        path: String,
        state: ResourceState
    ) {
        val entries = state.amounts.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.id", entry.key)
            writer.gameNumber("$entryPath.amount", entry.value)
        }
    }

    private fun readResourceState(
        reader: FieldReader,
        path: String
    ): ResourceState {
        val result = linkedMapOf<ContentId, GameNumber>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val id = reader.contentId("$entryPath.id")
            val previous = result.put(
                id,
                reader.gameNumber("$entryPath.amount")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate ResourceState key at $entryPath: $id"
                )
            }
        }
        return ResourceState(result)
    }

    // -------------------------------------------------------------------------
    // Combat
    // -------------------------------------------------------------------------

    private fun writeCombatState(
        writer: FieldWriter,
        path: String,
        state: CombatState
    ) {
        writer.string("$path.status", state.status.name)

        writer.boolean(
            "$path.playerCombatant.present",
            state.playerCombatant != null
        )
        state.playerCombatant?.let {
            writeCombatantState(writer, "$path.playerCombatant", it)
        }

        writer.count("$path.enemies", state.enemies.size)
        state.enemies.forEachIndexed { index, enemy ->
            writeEnemyState(writer, "$path.enemies.$index", enemy)
        }

        writer.optionalGameTime(
            "$path.nextPlayerDecisionAt",
            state.nextPlayerDecisionAt
        )

        val enemyDecisionEntries =
            state.nextEnemyDecisionAt.entries.sortedBy { it.key }
        writer.count(
            "$path.nextEnemyDecisionAt",
            enemyDecisionEntries.size
        )
        enemyDecisionEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.nextEnemyDecisionAt.$index"
            writer.instanceId("$entryPath.enemyId", entry.key)
            writer.gameTime("$entryPath.readyAt", entry.value)
        }

        writer.long("$path.combatSequenceId", state.combatSequenceId)
        writer.optionalGameTime(
            "$path.encounterStartedAt",
            state.encounterStartedAt
        )
        writeQueuedPlayerAction(
            writer,
            "$path.queuedPlayerAction",
            state.queuedPlayerAction
        )
    }

    private fun readCombatState(
        reader: FieldReader,
        path: String
    ): CombatState {
        val status = reader.combatStatus("$path.status")

        val playerPresent =
            reader.boolean("$path.playerCombatant.present")
        val playerCombatant =
            if (playerPresent) {
                readCombatantState(reader, "$path.playerCombatant")
            } else {
                null
            }

        val enemies = buildList {
            repeat(reader.count("$path.enemies")) { index ->
                add(
                    readEnemyState(
                        reader,
                        "$path.enemies.$index"
                    )
                )
            }
        }

        val nextEnemyDecisionAt =
            linkedMapOf<InstanceId, GameTime>()
        repeat(reader.count("$path.nextEnemyDecisionAt")) { index ->
            val entryPath = "$path.nextEnemyDecisionAt.$index"
            val id = reader.instanceId("$entryPath.enemyId")
            val previous = nextEnemyDecisionAt.put(
                id,
                reader.gameTime("$entryPath.readyAt")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate combat enemy decision ID: $id"
                )
            }
        }

        return CombatState(
            status = status,
            playerCombatant = playerCombatant,
            enemies = enemies,
            nextPlayerDecisionAt = reader.optionalGameTime(
                "$path.nextPlayerDecisionAt"
            ),
            nextEnemyDecisionAt = nextEnemyDecisionAt,
            combatSequenceId = reader.long("$path.combatSequenceId"),
            encounterStartedAt = reader.optionalGameTime(
                "$path.encounterStartedAt"
            ),
            queuedPlayerAction = readQueuedPlayerAction(
                reader,
                "$path.queuedPlayerAction"
            )
        )
    }

    private fun writeQueuedPlayerAction(
        writer: FieldWriter,
        path: String,
        action: QueuedPlayerAction?
    ) {
        writer.boolean("$path.present", action != null)
        when (action) {
            null -> Unit
            is QueuedPlayerAction.Skill -> {
                writer.string("$path.type", "skill")
                writer.contentId("$path.skillId", action.skillId)
            }
        }
    }

    private fun readQueuedPlayerAction(
        reader: FieldReader,
        path: String
    ): QueuedPlayerAction? {
        if (!reader.boolean("$path.present")) {
            return null
        }

        return when (val type = reader.string("$path.type")) {
            "skill" ->
                QueuedPlayerAction.Skill(
                    skillId = reader.contentId("$path.skillId")
                )

            else ->
                throw SaveDataException(
                    "Unknown QueuedPlayerAction type '$type' at $path"
                )
        }
    }

    private fun writeCombatantState(
        writer: FieldWriter,
        path: String,
        state: CombatantState
    ) {
        writer.instanceId("$path.instanceId", state.instanceId)
        writer.gameNumber("$path.currentHealth", state.currentHealth)
        writeCooldownState(writer, "$path.cooldowns", state.cooldowns)

        writer.count("$path.statusEffects", state.statusEffects.size)
        state.statusEffects.forEachIndexed { index, status ->
            writeStatusEffectState(
                writer,
                "$path.statusEffects.$index",
                status
            )
        }
    }

    private fun readCombatantState(
        reader: FieldReader,
        path: String
    ): CombatantState =
        CombatantState(
            instanceId = reader.instanceId("$path.instanceId"),
            currentHealth = reader.gameNumber("$path.currentHealth"),
            cooldowns = readCooldownState(
                reader,
                "$path.cooldowns"
            ),
            statusEffects = buildList {
                repeat(reader.count("$path.statusEffects")) { index ->
                    add(
                        readStatusEffectState(
                            reader,
                            "$path.statusEffects.$index"
                        )
                    )
                }
            }
        )

    private fun writeCooldownState(
        writer: FieldWriter,
        path: String,
        state: CooldownState
    ) {
        val entries =
            state.readyAtByActionId.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.actionId", entry.key)
            writer.gameTime("$entryPath.readyAt", entry.value)
        }
    }

    private fun readCooldownState(
        reader: FieldReader,
        path: String
    ): CooldownState {
        val result = linkedMapOf<ContentId, GameTime>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val id = reader.contentId("$entryPath.actionId")
            val previous = result.put(
                id,
                reader.gameTime("$entryPath.readyAt")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate cooldown action ID: $id"
                )
            }
        }
        return CooldownState(result)
    }

    private fun writeEnemyState(
        writer: FieldWriter,
        path: String,
        state: EnemyState
    ) {
        writer.instanceId("$path.instanceId", state.instanceId)
        writer.contentId("$path.definitionId", state.definitionId)
        writeCombatantState(writer, "$path.combatant", state.combatant)
        writer.long("$path.scalingTier", state.scalingTier)
        writeContentIdSet(
            writer,
            "$path.activeTraitIds",
            state.activeTraitIds
        )

        writer.count(
            "$path.activeMutations",
            state.activeMutations.size
        )
        state.activeMutations.forEachIndexed { index, mutation ->
            writeActiveMutationState(
                writer,
                "$path.activeMutations.$index",
                mutation
            )
        }
    }

    private fun readEnemyState(
        reader: FieldReader,
        path: String
    ): EnemyState =
        EnemyState(
            instanceId = reader.instanceId("$path.instanceId"),
            definitionId = reader.contentId("$path.definitionId"),
            combatant = readCombatantState(
                reader,
                "$path.combatant"
            ),
            scalingTier = reader.long("$path.scalingTier"),
            activeTraitIds = readContentIdSet(
                reader,
                "$path.activeTraitIds"
            ),
            activeMutations = buildList {
                repeat(reader.count("$path.activeMutations")) { index ->
                    add(
                        readActiveMutationState(
                            reader,
                            "$path.activeMutations.$index"
                        )
                    )
                }
            }
        )

    private fun writeStatusEffectState(
        writer: FieldWriter,
        path: String,
        state: StatusEffectState
    ) {
        writer.instanceId("$path.instanceId", state.instanceId)
        writer.contentId("$path.definitionId", state.definitionId)
        writer.optionalInstanceId(
            "$path.sourceInstanceId",
            state.sourceInstanceId
        )
        writer.int("$path.stackCount", state.stackCount)
        writer.ratio("$path.potency", state.potency)
        writer.gameTime("$path.appliedAt", state.appliedAt)
        writer.gameTime("$path.expiresAt", state.expiresAt)
        writer.optionalGameTime(
            "$path.nextPeriodicTickAt",
            state.nextPeriodicTickAt
        )
    }

    private fun readStatusEffectState(
        reader: FieldReader,
        path: String
    ): StatusEffectState =
        StatusEffectState(
            instanceId = reader.instanceId("$path.instanceId"),
            definitionId = reader.contentId("$path.definitionId"),
            sourceInstanceId = reader.optionalInstanceId(
                "$path.sourceInstanceId"
            ),
            stackCount = reader.int("$path.stackCount"),
            potency = reader.ratio("$path.potency"),
            appliedAt = reader.gameTime("$path.appliedAt"),
            expiresAt = reader.gameTime("$path.expiresAt"),
            nextPeriodicTickAt = reader.optionalGameTime(
                "$path.nextPeriodicTickAt"
            )
        )

    // -------------------------------------------------------------------------
    // World
    // -------------------------------------------------------------------------

    private fun writeWorldState(
        writer: FieldWriter,
        path: String,
        state: WorldState
    ) {
        writer.optionalContentId(
            "$path.activeRegionId",
            state.activeRegionId
        )
        writeContentIdSet(
            writer,
            "$path.unlockedRegionIds",
            state.unlockedRegionIds
        )

        val progressEntries =
            state.regionProgressById.entries.sortedBy { it.key }
        writer.count(
            "$path.regionProgressById",
            progressEntries.size
        )
        progressEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.regionProgressById.$index"
            writer.contentId("$entryPath.regionId", entry.key)
            writeRegionProgressState(
                writer,
                "$entryPath.progress",
                entry.value
            )
        }

        writer.boolean(
            "$path.currentEncounter.present",
            state.currentEncounter != null
        )
        state.currentEncounter?.let {
            writeEncounterState(writer, "$path.currentEncounter", it)
        }

        writeContentIdSet(
            writer,
            "$path.milestoneFlagIds",
            state.milestoneFlagIds
        )
        writer.string("$path.automationMode", state.automationMode.name)
        writer.optionalContentId("$path.selectedFarmEncounterId", state.selectedFarmEncounterId)
        writer.string("$path.pushFailurePolicy", state.pushFailurePolicy.name)
        writeContentIdSet(writer, "$path.clearedEncounterIds", state.clearedEncounterIds)
    }

    private fun readWorldState(
        reader: FieldReader,
        path: String
    ): WorldState {
        val progress = linkedMapOf<ContentId, RegionProgressState>()
        repeat(reader.count("$path.regionProgressById")) { index ->
            val entryPath = "$path.regionProgressById.$index"
            val regionId = reader.contentId("$entryPath.regionId")
            val previous = progress.put(
                regionId,
                readRegionProgressState(
                    reader,
                    "$entryPath.progress"
                )
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate WorldState region progress: $regionId"
                )
            }
        }

        val encounterPresent =
            reader.boolean("$path.currentEncounter.present")

        return WorldState(
            activeRegionId = reader.optionalContentId(
                "$path.activeRegionId"
            ),
            unlockedRegionIds = readContentIdSet(
                reader,
                "$path.unlockedRegionIds"
            ),
            regionProgressById = progress,
            currentEncounter =
                if (encounterPresent) {
                    readEncounterState(
                        reader,
                        "$path.currentEncounter"
                    )
                } else {
                    null
                },
            milestoneFlagIds = readContentIdSet(
                reader,
                "$path.milestoneFlagIds"
            ),
            automationMode = com.idlerpg.game.domain.model.world.WorldAutomationMode.valueOf(
                reader.string("$path.automationMode")
            ),
            selectedFarmEncounterId = reader.optionalContentId("$path.selectedFarmEncounterId"),
            pushFailurePolicy = com.idlerpg.game.domain.model.world.PushFailurePolicy.valueOf(
                reader.string("$path.pushFailurePolicy")
            ),
            clearedEncounterIds = readContentIdSet(reader, "$path.clearedEncounterIds")
        )
    }

    private fun writeRegionProgressState(
        writer: FieldWriter,
        path: String,
        state: RegionProgressState
    ) {
        writer.long(
            "$path.highestClearedEncounterTier",
            state.highestClearedEncounterTier
        )
        writer.gameNumber("$path.normalClears", state.normalClears)
        writer.gameNumber("$path.eliteClears", state.eliteClears)
        writeContentIdSet(
            writer,
            "$path.clearedBossIds",
            state.clearedBossIds
        )
        writeContentIdSet(
            writer,
            "$path.discoveryFlagIds",
            state.discoveryFlagIds
        )
    }

    private fun readRegionProgressState(
        reader: FieldReader,
        path: String
    ): RegionProgressState =
        RegionProgressState(
            highestClearedEncounterTier = reader.long(
                "$path.highestClearedEncounterTier"
            ),
            normalClears = reader.gameNumber("$path.normalClears"),
            eliteClears = reader.gameNumber("$path.eliteClears"),
            clearedBossIds = readContentIdSet(
                reader,
                "$path.clearedBossIds"
            ),
            discoveryFlagIds = readContentIdSet(
                reader,
                "$path.discoveryFlagIds"
            )
        )

    private fun writeEncounterState(
        writer: FieldWriter,
        path: String,
        state: EncounterState
    ) {
        writer.contentId("$path.definitionId", state.definitionId)
        writer.long("$path.encounterIndex", state.encounterIndex)
        writer.long("$path.encounterSeed", state.encounterSeed)
        writer.int("$path.currentWave", state.currentWave)
        writeInstanceIdList(
            writer,
            "$path.spawnedEnemyIds",
            state.spawnedEnemyIds
        )
        writer.string("$path.status", state.status.name)
        writer.boolean("$path.rewardEligible", state.rewardEligible)
    }

    private fun readEncounterState(
        reader: FieldReader,
        path: String
    ): EncounterState =
        EncounterState(
            definitionId = reader.contentId("$path.definitionId"),
            encounterIndex = reader.long("$path.encounterIndex"),
            encounterSeed = reader.long("$path.encounterSeed"),
            currentWave = reader.int("$path.currentWave"),
            spawnedEnemyIds = readInstanceIdList(
                reader,
                "$path.spawnedEnemyIds"
            ),
            status = reader.encounterStatus("$path.status"),
            rewardEligible = reader.boolean("$path.rewardEligible")
        )

    // -------------------------------------------------------------------------
    // Economy
    // -------------------------------------------------------------------------

    private fun writeEconomyState(
        writer: FieldWriter,
        path: String,
        state: EconomyState
    ) {
        writeCurrencyWallet(writer, "$path.wallet", state.wallet)
        writeUpgradeProgressState(
            writer,
            "$path.upgrades",
            state.upgrades
        )
    }

    private fun readEconomyState(
        reader: FieldReader,
        path: String
    ): EconomyState =
        EconomyState(
            wallet = readCurrencyWallet(reader, "$path.wallet"),
            upgrades = readUpgradeProgressState(
                reader,
                "$path.upgrades"
            )
        )

    private fun writeCurrencyWallet(
        writer: FieldWriter,
        path: String,
        state: CurrencyWallet
    ) {
        val entries =
            state.amountsByCurrencyId.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.currencyId("$entryPath.currencyId", entry.key)
            writer.gameNumber("$entryPath.amount", entry.value)
        }
    }

    private fun readCurrencyWallet(
        reader: FieldReader,
        path: String
    ): CurrencyWallet {
        val result = linkedMapOf<CurrencyId, GameNumber>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val currencyId =
                reader.currencyId("$entryPath.currencyId")
            val previous = result.put(
                currencyId,
                reader.gameNumber("$entryPath.amount")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate currency ID: $currencyId"
                )
            }
        }
        return CurrencyWallet(result)
    }

    private fun writeUpgradeProgressState(
        writer: FieldWriter,
        path: String,
        state: UpgradeProgressState
    ) {
        val entries =
            state.levelByUpgradeId.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.upgradeId", entry.key)
            writer.long("$entryPath.level", entry.value)
        }
    }

    private fun readUpgradeProgressState(
        reader: FieldReader,
        path: String
    ): UpgradeProgressState {
        val result = linkedMapOf<ContentId, Long>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val id = reader.contentId("$entryPath.upgradeId")
            val previous = result.put(
                id,
                reader.long("$entryPath.level")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate upgrade progress ID: $id"
                )
            }
        }
        return UpgradeProgressState(result)
    }

    // -------------------------------------------------------------------------
    // Resonance
    // -------------------------------------------------------------------------

    private fun writeResonanceState(
        writer: FieldWriter,
        path: String,
        state: ResonanceState
    ) {
        val chargeEntries =
            state.chargeByAffinityId.entries.sortedBy { it.key }
        writer.count(
            "$path.chargeByAffinityId",
            chargeEntries.size
        )
        chargeEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.chargeByAffinityId.$index"
            writer.contentId("$entryPath.affinityId", entry.key)
            writer.gameNumber("$entryPath.charge", entry.value)
        }

        writeResonanceSequenceState(
            writer,
            "$path.sequence",
            state.sequence
        )
        writeConvergenceState(
            writer,
            "$path.convergence",
            state.convergence
        )
    }

    private fun readResonanceState(
        reader: FieldReader,
        path: String
    ): ResonanceState {
        val charge = linkedMapOf<ContentId, GameNumber>()
        repeat(reader.count("$path.chargeByAffinityId")) { index ->
            val entryPath = "$path.chargeByAffinityId.$index"
            val id = reader.contentId("$entryPath.affinityId")
            val previous = charge.put(
                id,
                reader.gameNumber("$entryPath.charge")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate Resonance affinity ID: $id"
                )
            }
        }

        return ResonanceState(
            chargeByAffinityId = charge,
            sequence = readResonanceSequenceState(
                reader,
                "$path.sequence"
            ),
            convergence = readConvergenceState(
                reader,
                "$path.convergence"
            )
        )
    }

    private fun writeResonanceSequenceState(
        writer: FieldWriter,
        path: String,
        state: ResonanceSequenceState
    ) {
        writeContentIdList(writer, path, state.affinityIds)
    }

    private fun readResonanceSequenceState(
        reader: FieldReader,
        path: String
    ): ResonanceSequenceState =
        ResonanceSequenceState(
            affinityIds = readContentIdList(reader, path)
        )

    private fun writeConvergenceState(
        writer: FieldWriter,
        path: String,
        state: ConvergenceState
    ) {
        val readyEntries =
            state.readyAtById.entries.sortedBy { it.key }
        writer.count("$path.readyAtById", readyEntries.size)
        readyEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.readyAtById.$index"
            writer.contentId("$entryPath.convergenceId", entry.key)
            writer.gameTime("$entryPath.readyAt", entry.value)
        }

        val triggerEntries =
            state.triggerCountById.entries.sortedBy { it.key }
        writer.count("$path.triggerCountById", triggerEntries.size)
        triggerEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.triggerCountById.$index"
            writer.contentId("$entryPath.convergenceId", entry.key)
            writer.gameNumber("$entryPath.count", entry.value)
        }

        val encounterEntries =
            state.encounterTriggerCountById.entries.sortedBy { it.key }
        writer.count(
            "$path.encounterTriggerCountById",
            encounterEntries.size
        )
        encounterEntries.forEachIndexed { index, entry ->
            val entryPath =
                "$path.encounterTriggerCountById.$index"
            writer.contentId("$entryPath.convergenceId", entry.key)
            writer.long("$entryPath.count", entry.value)
        }
    }

    private fun readConvergenceState(
        reader: FieldReader,
        path: String
    ): ConvergenceState {
        val readyAt = linkedMapOf<ContentId, GameTime>()
        repeat(reader.count("$path.readyAtById")) { index ->
            val entryPath = "$path.readyAtById.$index"
            val id = reader.contentId("$entryPath.convergenceId")
            val previous = readyAt.put(
                id,
                reader.gameTime("$entryPath.readyAt")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate Convergence readyAt ID: $id"
                )
            }
        }

        val triggerCount =
            linkedMapOf<ContentId, GameNumber>()
        repeat(reader.count("$path.triggerCountById")) { index ->
            val entryPath = "$path.triggerCountById.$index"
            val id = reader.contentId("$entryPath.convergenceId")
            val previous = triggerCount.put(
                id,
                reader.gameNumber("$entryPath.count")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate Convergence trigger-count ID: $id"
                )
            }
        }

        val encounterCount = linkedMapOf<ContentId, Long>()
        repeat(
            reader.count("$path.encounterTriggerCountById")
        ) { index ->
            val entryPath =
                "$path.encounterTriggerCountById.$index"
            val id = reader.contentId("$entryPath.convergenceId")
            val previous = encounterCount.put(
                id,
                reader.long("$entryPath.count")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate Convergence encounter-count ID: $id"
                )
            }
        }

        return ConvergenceState(
            readyAtById = readyAt,
            triggerCountById = triggerCount,
            encounterTriggerCountById = encounterCount
        )
    }

    // -------------------------------------------------------------------------
    // Doctrine
    // -------------------------------------------------------------------------

    private fun writeDoctrineState(
        writer: FieldWriter,
        path: String,
        state: DoctrineState
    ) {
        writer.boolean("$path.enabled", state.enabled)
        writer.count("$path.rules", state.rules.size)
        state.rules.forEachIndexed { index, rule ->
            writeDoctrineRule(writer, "$path.rules.$index", rule)
        }
    }

    private fun readDoctrineState(
        reader: FieldReader,
        path: String
    ): DoctrineState =
        DoctrineState(
            enabled = reader.boolean("$path.enabled"),
            rules = buildList {
                repeat(reader.count("$path.rules")) { index ->
                    add(
                        readDoctrineRule(
                            reader,
                            "$path.rules.$index"
                        )
                    )
                }
            }
        )

    private fun writeDoctrineRule(
        writer: FieldWriter,
        path: String,
        rule: DoctrineRule
    ) {
        writer.instanceId("$path.instanceId", rule.instanceId)
        writer.boolean("$path.enabled", rule.enabled)
        writeDoctrineCondition(
            writer,
            "$path.condition",
            rule.condition
        )
        writeDoctrineAction(writer, "$path.action", rule.action)
    }

    private fun readDoctrineRule(
        reader: FieldReader,
        path: String
    ): DoctrineRule =
        DoctrineRule(
            instanceId = reader.instanceId("$path.instanceId"),
            enabled = reader.boolean("$path.enabled"),
            condition = readDoctrineCondition(
                reader,
                "$path.condition"
            ),
            action = readDoctrineAction(reader, "$path.action")
        )

    private fun writeDoctrineCondition(
        writer: FieldWriter,
        path: String,
        condition: DoctrineCondition
    ) {
        when (condition) {
            is DoctrineCondition.All -> {
                writer.string("$path.type", "all")
                writer.count(
                    "$path.conditions",
                    condition.conditions.size
                )
                condition.conditions.forEachIndexed { index, child ->
                    writeDoctrineCondition(
                        writer,
                        "$path.conditions.$index",
                        child
                    )
                }
            }

            is DoctrineCondition.Any -> {
                writer.string("$path.type", "any")
                writer.count(
                    "$path.conditions",
                    condition.conditions.size
                )
                condition.conditions.forEachIndexed { index, child ->
                    writeDoctrineCondition(
                        writer,
                        "$path.conditions.$index",
                        child
                    )
                }
            }

            is DoctrineCondition.Not -> {
                writer.string("$path.type", "not")
                writeDoctrineCondition(
                    writer,
                    "$path.condition",
                    condition.condition
                )
            }

            is DoctrineCondition.Predicate -> {
                writer.string("$path.type", "predicate")
                writeDoctrinePredicate(
                    writer,
                    "$path.predicate",
                    condition.predicate
                )
            }
        }
    }

    private fun readDoctrineCondition(
        reader: FieldReader,
        path: String
    ): DoctrineCondition =
        when (val type = reader.string("$path.type")) {
            "all" ->
                DoctrineCondition.All(
                    conditions = buildList {
                        repeat(
                            reader.count("$path.conditions")
                        ) { index ->
                            add(
                                readDoctrineCondition(
                                    reader,
                                    "$path.conditions.$index"
                                )
                            )
                        }
                    }
                )

            "any" ->
                DoctrineCondition.Any(
                    conditions = buildList {
                        repeat(
                            reader.count("$path.conditions")
                        ) { index ->
                            add(
                                readDoctrineCondition(
                                    reader,
                                    "$path.conditions.$index"
                                )
                            )
                        }
                    }
                )

            "not" ->
                DoctrineCondition.Not(
                    condition = readDoctrineCondition(
                        reader,
                        "$path.condition"
                    )
                )

            "predicate" ->
                DoctrineCondition.Predicate(
                    predicate = readDoctrinePredicate(
                        reader,
                        "$path.predicate"
                    )
                )

            else ->
                throw SaveDataException(
                    "Unknown DoctrineCondition type '$type' at $path"
                )
        }

    private fun writeDoctrinePredicate(
        writer: FieldWriter,
        path: String,
        predicate: DoctrinePredicate
    ) {
        when (predicate) {
            is DoctrinePredicate.PlayerHealthPercent -> { writer.string("$path.type", "player_health_percent"); writer.string("$path.comparison", predicate.comparison.name); writer.ratio("$path.threshold", predicate.threshold) }
            is DoctrinePredicate.EnemyCount -> { writer.string("$path.type", "enemy_count"); writer.string("$path.comparison", predicate.comparison.name); writer.gameNumber("$path.amount", predicate.amount) }
            is DoctrinePredicate.EnemyRolePresent -> { writer.string("$path.type", "enemy_role_present"); writer.string("$path.role", predicate.role.name) }
            DoctrinePredicate.ElitePresent -> writer.string("$path.type", "elite_present")
            DoctrinePredicate.BossPresent -> writer.string("$path.type", "boss_present")
            is DoctrinePredicate.EnemyHealthPercent -> {
                writer.string(
                    "$path.type",
                    "enemy_health_percent"
                )
                writer.string(
                    "$path.comparison",
                    predicate.comparison.name
                )
                writer.ratio("$path.threshold", predicate.threshold)
            }

            is DoctrinePredicate.SkillReady -> {
                writer.string("$path.type", "skill_ready")
                writer.contentId("$path.skillId", predicate.skillId)
            }

            is DoctrinePredicate.ResonanceCharge -> {
                writer.string("$path.type", "resonance_charge")
                writer.affinity("$path.affinity", predicate.affinity)
                writer.string(
                    "$path.comparison",
                    predicate.comparison.name
                )
                writer.gameNumber("$path.amount", predicate.amount)
            }

            is DoctrinePredicate.SequenceSuffix -> {
                writer.string("$path.type", "sequence_suffix")
                writer.count(
                    "$path.affinities",
                    predicate.affinities.size
                )
                predicate.affinities.forEachIndexed { index, affinity ->
                    writer.affinity(
                        "$path.affinities.$index",
                        affinity
                    )
                }
            }

            is DoctrinePredicate.StatusPresent -> {
                writer.string("$path.type", "status_present")
                writer.string("$path.target", predicate.target.name)
                writer.contentId(
                    "$path.statusDefinitionId",
                    predicate.statusDefinitionId
                )
            }

            is DoctrinePredicate.StatusAbsent -> {
                writer.string("$path.type", "status_absent")
                writer.string("$path.target", predicate.target.name)
                writer.contentId(
                    "$path.statusDefinitionId",
                    predicate.statusDefinitionId
                )
            }
        }
    }

    private fun readDoctrinePredicate(
        reader: FieldReader,
        path: String
    ): DoctrinePredicate =
        when (val type = reader.string("$path.type")) {
            "player_health_percent" -> DoctrinePredicate.PlayerHealthPercent(reader.doctrineComparison("$path.comparison"), reader.ratio("$path.threshold"))
            "enemy_count" -> DoctrinePredicate.EnemyCount(reader.doctrineComparison("$path.comparison"), reader.gameNumber("$path.amount"))
            "enemy_role_present" -> DoctrinePredicate.EnemyRolePresent(enumValueOf(reader.string("$path.role")))
            "elite_present" -> DoctrinePredicate.ElitePresent
            "boss_present" -> DoctrinePredicate.BossPresent
            "enemy_health_percent" ->
                DoctrinePredicate.EnemyHealthPercent(
                    comparison = reader.doctrineComparison(
                        "$path.comparison"
                    ),
                    threshold = reader.ratio("$path.threshold")
                )

            "skill_ready" ->
                DoctrinePredicate.SkillReady(
                    skillId = reader.contentId("$path.skillId")
                )

            "resonance_charge" ->
                DoctrinePredicate.ResonanceCharge(
                    affinity = reader.affinity("$path.affinity"),
                    comparison = reader.doctrineComparison(
                        "$path.comparison"
                    ),
                    amount = reader.gameNumber("$path.amount")
                )

            "sequence_suffix" ->
                DoctrinePredicate.SequenceSuffix(
                    affinities = buildList {
                        repeat(
                            reader.count("$path.affinities")
                        ) { index ->
                            add(
                                reader.affinity(
                                    "$path.affinities.$index"
                                )
                            )
                        }
                    }
                )

            "status_present" ->
                DoctrinePredicate.StatusPresent(
                    target = reader.doctrineStatusTarget(
                        "$path.target"
                    ),
                    statusDefinitionId = reader.contentId(
                        "$path.statusDefinitionId"
                    )
                )

            "status_absent" ->
                DoctrinePredicate.StatusAbsent(
                    target = reader.doctrineStatusTarget(
                        "$path.target"
                    ),
                    statusDefinitionId = reader.contentId(
                        "$path.statusDefinitionId"
                    )
                )

            else ->
                throw SaveDataException(
                    "Unknown DoctrinePredicate type '$type' at $path"
                )
        }

    private fun writeDoctrineAction(
        writer: FieldWriter,
        path: String,
        action: DoctrineAction
    ) {
        when (action) {
            DoctrineAction.UseBasicAttack ->
                writer.string("$path.type", "basic_attack")

            is DoctrineAction.UseSkill -> {
                writer.string("$path.type", "use_skill")
                writer.contentId("$path.skillId", action.skillId)
            }
        }
    }

    private fun readDoctrineAction(
        reader: FieldReader,
        path: String
    ): DoctrineAction =
        when (val type = reader.string("$path.type")) {
            "basic_attack" ->
                DoctrineAction.UseBasicAttack

            "use_skill" ->
                DoctrineAction.UseSkill(
                    skillId = reader.contentId("$path.skillId")
                )

            else ->
                throw SaveDataException(
                    "Unknown DoctrineAction type '$type' at $path"
                )
        }

    // -------------------------------------------------------------------------
    // Adaptation
    // -------------------------------------------------------------------------

    private fun writeAdaptationState(
        writer: FieldWriter,
        path: String,
        state: AdaptationState
    ) {
        val entries =
            state.regionStateById.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.regionId", entry.key)
            writeRegionAdaptationState(
                writer,
                "$entryPath.state",
                entry.value
            )
        }
    }

    private fun readAdaptationState(
        reader: FieldReader,
        path: String
    ): AdaptationState {
        val result =
            linkedMapOf<ContentId, RegionAdaptationState>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val regionId = reader.contentId("$entryPath.regionId")
            val previous = result.put(
                regionId,
                readRegionAdaptationState(
                    reader,
                    "$entryPath.state"
                )
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate Adaptation region ID: $regionId"
                )
            }
        }
        return AdaptationState(result)
    }

    private fun writeRegionAdaptationState(
        writer: FieldWriter,
        path: String,
        state: RegionAdaptationState
    ) {
        val exposureEntries =
            state.exposureByAffinityId.entries.sortedBy { it.key }
        writer.count(
            "$path.exposureByAffinityId",
            exposureEntries.size
        )
        exposureEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.exposureByAffinityId.$index"
            writer.contentId("$entryPath.affinityId", entry.key)
            writeAffinityExposureState(
                writer,
                "$entryPath.exposure",
                entry.value
            )
        }

        val tierEntries =
            state.tierByAffinityId.entries.sortedBy { it.key }
        writer.count("$path.tierByAffinityId", tierEntries.size)
        tierEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.tierByAffinityId.$index"
            writer.contentId("$entryPath.affinityId", entry.key)
            writer.int("$entryPath.tier", entry.value)
        }

        writeContentIdSet(
            writer,
            "$path.unlockedMutationIds",
            state.unlockedMutationIds
        )
    }

    private fun readRegionAdaptationState(
        reader: FieldReader,
        path: String
    ): RegionAdaptationState {
        val exposure =
            linkedMapOf<ContentId, AffinityExposureState>()
        repeat(
            reader.count("$path.exposureByAffinityId")
        ) { index ->
            val entryPath = "$path.exposureByAffinityId.$index"
            val id = reader.contentId("$entryPath.affinityId")
            val previous = exposure.put(
                id,
                readAffinityExposureState(
                    reader,
                    "$entryPath.exposure"
                )
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate Adaptation exposure affinity: $id"
                )
            }
        }

        val tiers = linkedMapOf<ContentId, Int>()
        repeat(reader.count("$path.tierByAffinityId")) { index ->
            val entryPath = "$path.tierByAffinityId.$index"
            val id = reader.contentId("$entryPath.affinityId")
            val previous = tiers.put(
                id,
                reader.int("$entryPath.tier")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate Adaptation tier affinity: $id"
                )
            }
        }

        return RegionAdaptationState(
            exposureByAffinityId = exposure,
            tierByAffinityId = tiers,
            unlockedMutationIds = readContentIdSet(
                reader,
                "$path.unlockedMutationIds"
            )
        )
    }

    private fun writeAffinityExposureState(
        writer: FieldWriter,
        path: String,
        state: AffinityExposureState
    ) {
        writer.gameNumber("$path.pressure", state.pressure)
        writer.gameNumber(
            "$path.currentEncounterContribution",
            state.currentEncounterContribution
        )
        writer.gameNumber(
            "$path.recentEncounterContribution",
            state.recentEncounterContribution
        )
    }

    private fun readAffinityExposureState(
        reader: FieldReader,
        path: String
    ): AffinityExposureState =
        AffinityExposureState(
            pressure = reader.gameNumber("$path.pressure"),
            currentEncounterContribution = reader.gameNumber(
                "$path.currentEncounterContribution"
            ),
            recentEncounterContribution = reader.gameNumber(
                "$path.recentEncounterContribution"
            )
        )

    private fun writeActiveMutationState(
        writer: FieldWriter,
        path: String,
        state: ActiveMutationState
    ) {
        writer.contentId("$path.mutationId", state.mutationId)
        writer.contentId(
            "$path.sourceAffinityId",
            state.sourceAffinityId
        )
        writer.int("$path.adaptationTier", state.adaptationTier)
    }

    private fun readActiveMutationState(
        reader: FieldReader,
        path: String
    ): ActiveMutationState =
        ActiveMutationState(
            mutationId = reader.contentId("$path.mutationId"),
            sourceAffinityId = reader.contentId(
                "$path.sourceAffinityId"
            ),
            adaptationTier = reader.int("$path.adaptationTier")
        )

    // -------------------------------------------------------------------------
    // Inventory
    // -------------------------------------------------------------------------

    private fun writeInventoryState(
        writer: FieldWriter,
        path: String,
        state: InventoryState
    ) {
        val itemEntries = state.itemsById.entries.sortedBy { it.key }
        writer.count("$path.itemsById", itemEntries.size)
        itemEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.itemsById.$index"
            writer.instanceId("$entryPath.key", entry.key)
            writeItemInstance(
                writer,
                "$entryPath.item",
                entry.value
            )
        }

        writeEquipmentLoadoutState(
            writer,
            "$path.equipment",
            state.equipment
        )
        writeItemLockState(writer, "$path.locks", state.locks)

        writer.long("$path.slotCapacity", state.slotCapacity)
        writer.long(
            "$path.capacityUpgradePurchases",
            state.capacityUpgradePurchases
        )

        val overflowEntries =
            state.overflowItemsById.entries.sortedBy { it.key }
        writer.count("$path.overflowItemsById", overflowEntries.size)
        overflowEntries.forEachIndexed { index, entry ->
            val entryPath = "$path.overflowItemsById.$index"
            writer.instanceId("$entryPath.key", entry.key)
            writeItemInstance(
                writer,
                "$entryPath.item",
                entry.value
            )
        }
        writer.boolean("$path.lootFilter.autoSalvageEnabled", state.lootFilter.autoSalvageEnabled)
        writer.rarity("$path.lootFilter.minimumKeepRarity", state.lootFilter.minimumKeepRarity)
    }

    private fun readInventoryState(
        reader: FieldReader,
        path: String
    ): InventoryState {
        val items = linkedMapOf<InstanceId, ItemInstance>()
        repeat(reader.count("$path.itemsById")) { index ->
            val entryPath = "$path.itemsById.$index"
            val key = reader.instanceId("$entryPath.key")
            val item = readItemInstance(
                reader,
                "$entryPath.item"
            )
            val previous = items.put(key, item)
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate inventory InstanceId: $key"
                )
            }
        }

        val equipment = readEquipmentLoadoutState(
            reader,
            "$path.equipment"
        )
        val locks = readItemLockState(reader, "$path.locks")
        val slotCapacity = reader.long("$path.slotCapacity")
        val capacityUpgradePurchases =
            reader.long("$path.capacityUpgradePurchases")

        val overflow = linkedMapOf<InstanceId, ItemInstance>()
        repeat(reader.count("$path.overflowItemsById")) { index ->
            val entryPath = "$path.overflowItemsById.$index"
            val key = reader.instanceId("$entryPath.key")
            val item = readItemInstance(
                reader,
                "$entryPath.item"
            )
            val previous = overflow.put(key, item)
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate overflow inventory InstanceId: $key"
                )
            }
        }

        return InventoryState(
            itemsById = items,
            equipment = equipment,
            locks = locks,
            slotCapacity = slotCapacity,
            capacityUpgradePurchases = capacityUpgradePurchases,
            overflowItemsById = overflow,
            lootFilter = com.idlerpg.game.domain.model.inventory.LootFilterState(
                autoSalvageEnabled = reader.boolean("$path.lootFilter.autoSalvageEnabled"),
                minimumKeepRarity = reader.rarity("$path.lootFilter.minimumKeepRarity")
            )
        )
    }

    private fun writeEquipmentLoadoutState(
        writer: FieldWriter,
        path: String,
        state: EquipmentLoadoutState
    ) {
        val entries =
            state.itemBySlot.entries.sortedBy { it.key.id.value }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.equipmentSlot("$entryPath.slot", entry.key)
            writer.instanceId("$entryPath.itemId", entry.value)
        }
    }

    private fun readEquipmentLoadoutState(
        reader: FieldReader,
        path: String
    ): EquipmentLoadoutState {
        val result =
            linkedMapOf<EquipmentSlot, InstanceId>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val slot = reader.equipmentSlot("$entryPath.slot")
            val previous = result.put(
                slot,
                reader.instanceId("$entryPath.itemId")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate equipment slot: $slot"
                )
            }
        }
        return EquipmentLoadoutState(result)
    }

    private fun writeItemLockState(
        writer: FieldWriter,
        path: String,
        state: ItemLockState
    ) {
        writeInstanceIdSet(
            writer,
            "$path.lockedItemInstanceIds",
            state.lockedItemInstanceIds
        )
    }

    private fun readItemLockState(
        reader: FieldReader,
        path: String
    ): ItemLockState =
        ItemLockState(
            lockedItemInstanceIds = readInstanceIdSet(
                reader,
                "$path.lockedItemInstanceIds"
            )
        )

    private fun writeItemInstance(
        writer: FieldWriter,
        path: String,
        item: ItemInstance
    ) {
        writer.instanceId("$path.instanceId", item.instanceId)
        writer.contentId("$path.definitionId", item.definitionId)
        writer.rarity("$path.rarity", item.rarity)

        writer.count("$path.affixes", item.affixes.size)
        item.affixes.forEachIndexed { index, affix ->
            val affixPath = "$path.affixes.$index"
            writer.contentId("$affixPath.affixId", affix.affixId)
            writer.long("$affixPath.value", affix.value)
        }

        writer.boolean("$path.mainStat.present", item.mainStat != null)
        item.mainStat?.let { mainStat ->
            writer.contentId("$path.mainStat.affixId", mainStat.affixId)
            writer.long("$path.mainStat.value", mainStat.value)
        }
        writer.long("$path.enhancementLevel", item.enhancementLevel)
        writer.optionalContentId(
            "$path.sourceDefinitionId",
            item.sourceDefinitionId
        )
    }

    private fun readItemInstance(
        reader: FieldReader,
        path: String
    ): ItemInstance =
        ItemInstance(
            instanceId = reader.instanceId("$path.instanceId"),
            definitionId = reader.contentId("$path.definitionId"),
            rarity = reader.rarity("$path.rarity"),
            affixes = buildList {
                repeat(reader.count("$path.affixes")) { index ->
                    val affixPath = "$path.affixes.$index"
                    add(
                        RolledAffix(
                            affixId = reader.contentId(
                                "$affixPath.affixId"
                            ),
                            value = reader.long("$affixPath.value")
                        )
                    )
                }
            },
            mainStat = if (reader.boolean("$path.mainStat.present")) {
                RolledAffix(
                    affixId = reader.contentId("$path.mainStat.affixId"),
                    value = reader.long("$path.mainStat.value")
                )
            } else {
                null
            },
            enhancementLevel = reader.long("$path.enhancementLevel"),
            sourceDefinitionId = reader.optionalContentId(
                "$path.sourceDefinitionId"
            )
        )

    // -------------------------------------------------------------------------
    // Progression
    // -------------------------------------------------------------------------

    private fun writeProgressionState(
        writer: FieldWriter,
        path: String,
        state: ProgressionState
    ) {
        writePlayerLevelState(
            writer,
            "$path.playerLevel",
            state.playerLevel
        )
        writeFeatureUnlockState(
            writer,
            "$path.featureUnlocks",
            state.featureUnlocks
        )
        writeAffinityMasteryState(
            writer,
            "$path.affinityMastery",
            state.affinityMastery
        )
        writeSkillProgressionState(
            writer,
            "$path.skillProgression",
            state.skillProgression
        )
    }

    private fun readProgressionState(
        reader: FieldReader,
        path: String
    ): ProgressionState =
        ProgressionState(
            playerLevel = readPlayerLevelState(
                reader,
                "$path.playerLevel"
            ),
            featureUnlocks = readFeatureUnlockState(
                reader,
                "$path.featureUnlocks"
            ),
            affinityMastery = readAffinityMasteryState(
                reader,
                "$path.affinityMastery"
            ),
            skillProgression = readSkillProgressionState(
                reader,
                "$path.skillProgression"
            )
        )

    private fun writePlayerLevelState(
        writer: FieldWriter,
        path: String,
        state: PlayerLevelState
    ) {
        writer.long("$path.level", state.level)
        writer.gameNumber(
            "$path.currentExperience",
            state.currentExperience
        )
    }

    private fun readPlayerLevelState(
        reader: FieldReader,
        path: String
    ): PlayerLevelState =
        PlayerLevelState(
            level = reader.long("$path.level"),
            currentExperience = reader.gameNumber(
                "$path.currentExperience"
            )
        )

    private fun writeFeatureUnlockState(
        writer: FieldWriter,
        path: String,
        state: FeatureUnlockState
    ) {
        writeContentIdSet(
            writer,
            "$path.unlockedFeatureIds",
            state.unlockedFeatureIds
        )
    }

    private fun readFeatureUnlockState(
        reader: FieldReader,
        path: String
    ): FeatureUnlockState =
        FeatureUnlockState(
            unlockedFeatureIds = readContentIdSet(
                reader,
                "$path.unlockedFeatureIds"
            )
        )

    private fun writeAffinityMasteryState(
        writer: FieldWriter,
        path: String,
        state: AffinityMasteryState
    ) {
        val entries =
            state.experienceByAffinityId.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.affinityId", entry.key)
            writer.gameNumber("$entryPath.experience", entry.value)
        }
    }

    private fun readAffinityMasteryState(
        reader: FieldReader,
        path: String
    ): AffinityMasteryState {
        val result = linkedMapOf<ContentId, GameNumber>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val id = reader.contentId("$entryPath.affinityId")
            val previous = result.put(
                id,
                reader.gameNumber("$entryPath.experience")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate affinity mastery ID: $id"
                )
            }
        }
        return AffinityMasteryState(result)
    }

    // -------------------------------------------------------------------------
    // Quests / achievements
    // -------------------------------------------------------------------------

    private fun writeQuestState(
        writer: FieldWriter,
        path: String,
        state: QuestState
    ) {
        val entries =
            state.progressByQuestId.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.questId", entry.key)
            writeQuestProgressState(
                writer,
                "$entryPath.progress",
                entry.value
            )
        }
    }

    private fun readQuestState(
        reader: FieldReader,
        path: String
    ): QuestState {
        val result =
            linkedMapOf<ContentId, QuestProgressState>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val questId = reader.contentId("$entryPath.questId")
            val previous = result.put(
                questId,
                readQuestProgressState(
                    reader,
                    "$entryPath.progress"
                )
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate QuestState quest ID: $questId"
                )
            }
        }
        return QuestState(result)
    }

    private fun writeQuestProgressState(
        writer: FieldWriter,
        path: String,
        state: QuestProgressState
    ) {
        val entries =
            state.progressByObjectiveId.entries.sortedBy { it.key }
        writer.count(
            "$path.progressByObjectiveId",
            entries.size
        )
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.progressByObjectiveId.$index"
            writer.contentId("$entryPath.objectiveId", entry.key)
            writer.gameNumber("$entryPath.progress", entry.value)
        }
        writer.gameNumber(
            "$path.completionCount",
            state.completionCount
        )
        writer.gameNumber(
            "$path.claimedCount",
            state.claimedCount
        )
    }

    private fun readQuestProgressState(
        reader: FieldReader,
        path: String
    ): QuestProgressState {
        val progress = linkedMapOf<ContentId, GameNumber>()
        repeat(
            reader.count("$path.progressByObjectiveId")
        ) { index ->
            val entryPath = "$path.progressByObjectiveId.$index"
            val id = reader.contentId("$entryPath.objectiveId")
            val previous = progress.put(
                id,
                reader.gameNumber("$entryPath.progress")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate quest objective ID: $id"
                )
            }
        }
        return QuestProgressState(
            progressByObjectiveId = progress,
            completionCount = reader.gameNumber(
                "$path.completionCount"
            ),
            claimedCount = reader.gameNumber(
                "$path.claimedCount"
            )
        )
    }

    private fun writeAchievementState(
        writer: FieldWriter,
        path: String,
        state: AchievementState
    ) {
        val entries =
            state.progressByAchievementId.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId(
                "$entryPath.achievementId",
                entry.key
            )
            writeAchievementProgressState(
                writer,
                "$entryPath.progress",
                entry.value
            )
        }
    }

    private fun readAchievementState(
        reader: FieldReader,
        path: String
    ): AchievementState {
        val result =
            linkedMapOf<ContentId, AchievementProgressState>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val id = reader.contentId(
                "$entryPath.achievementId"
            )
            val previous = result.put(
                id,
                readAchievementProgressState(
                    reader,
                    "$entryPath.progress"
                )
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate achievement ID: $id"
                )
            }
        }
        return AchievementState(result)
    }

    private fun writeAchievementProgressState(
        writer: FieldWriter,
        path: String,
        state: AchievementProgressState
    ) {
        val entries =
            state.progressByObjectiveId.entries.sortedBy { it.key }
        writer.count(
            "$path.progressByObjectiveId",
            entries.size
        )
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.progressByObjectiveId.$index"
            writer.contentId("$entryPath.objectiveId", entry.key)
            writer.gameNumber("$entryPath.progress", entry.value)
        }
        writer.boolean("$path.completed", state.completed)
        writer.boolean("$path.rewardClaimed", state.rewardClaimed)
    }

    private fun readAchievementProgressState(
        reader: FieldReader,
        path: String
    ): AchievementProgressState {
        val progress = linkedMapOf<ContentId, GameNumber>()
        repeat(
            reader.count("$path.progressByObjectiveId")
        ) { index ->
            val entryPath = "$path.progressByObjectiveId.$index"
            val id = reader.contentId("$entryPath.objectiveId")
            val previous = progress.put(
                id,
                reader.gameNumber("$entryPath.progress")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate achievement objective ID: $id"
                )
            }
        }
        return AchievementProgressState(
            progressByObjectiveId = progress,
            completed = reader.boolean("$path.completed"),
            rewardClaimed = reader.boolean("$path.rewardClaimed")
        )
    }


    private fun writeSkillProgressionState(
        writer: FieldWriter,
        path: String,
        state: SkillProgressionState
    ) {
        writeSkillLongMap(writer, "$path.rankBySkillId", state.rankBySkillId)
        writeSkillLongMap(writer, "$path.masteryBySkillId", state.masteryBySkillId)
        writeSkillLongMap(writer, "$path.refinementBySkillId", state.refinementBySkillId)
    }

    private fun readSkillProgressionState(
        reader: FieldReader,
        path: String
    ): SkillProgressionState =
        SkillProgressionState(
            rankBySkillId = readSkillLongMap(reader, "$path.rankBySkillId"),
            masteryBySkillId = readSkillLongMap(reader, "$path.masteryBySkillId"),
            refinementBySkillId = readSkillLongMap(reader, "$path.refinementBySkillId")
        )

    private fun writeSkillLongMap(
        writer: FieldWriter,
        path: String,
        values: Map<ContentId, Long>
    ) {
        val entries = values.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.skillId", entry.key)
            writer.long("$entryPath.value", entry.value)
        }
    }

    private fun readSkillLongMap(
        reader: FieldReader,
        path: String
    ): Map<ContentId, Long> {
        val result = linkedMapOf<ContentId, Long>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val skillId = reader.contentId("$entryPath.skillId")
            val previous = result.put(skillId, reader.long("$entryPath.value"))
            if (previous != null) {
                throw SaveDataException("Duplicate skill progression ID: $skillId")
            }
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Chronicle / meta
    // -------------------------------------------------------------------------

    private fun writeChronicleState(
        writer: FieldWriter,
        path: String,
        state: ChronicleState
    ) {
        writer.long(
            "$path.currentChronicleNumber",
            state.currentChronicleNumber
        )
        writer.gameNumber(
            "$path.completedChronicles",
            state.completedChronicles
        )
        writeContentIdSet(
            writer,
            "$path.bestMilestoneIds",
            state.bestMilestoneIds
        )
    }

    private fun readChronicleState(
        reader: FieldReader,
        path: String
    ): ChronicleState =
        ChronicleState(
            currentChronicleNumber = reader.long(
                "$path.currentChronicleNumber"
            ),
            completedChronicles = reader.gameNumber(
                "$path.completedChronicles"
            ),
            bestMilestoneIds = readContentIdSet(
                reader,
                "$path.bestMilestoneIds"
            )
        )

    private fun writeEchoState(
        writer: FieldWriter,
        path: String,
        state: EchoState
    ) {
        writer.gameNumber("$path.available", state.available)
        writer.gameNumber("$path.spent", state.spent)
        writeContentIdSet(
            writer,
            "$path.purchasedOfferIds",
            state.purchasedOfferIds
        )
    }

    private fun readEchoState(
        reader: FieldReader,
        path: String
    ): EchoState =
        EchoState(
            available = reader.gameNumber("$path.available"),
            spent = reader.gameNumber("$path.spent"),
            purchasedOfferIds = readContentIdSet(
                reader,
                "$path.purchasedOfferIds"
            )
        )

    private fun writeDiscoveryState(
        writer: FieldWriter,
        path: String,
        state: DiscoveryState
    ) {
        writeContentIdSet(
            writer,
            "$path.discoveredConvergenceIds",
            state.discoveredConvergenceIds
        )
        writeContentIdSet(
            writer,
            "$path.discoveredMutationIds",
            state.discoveredMutationIds
        )
        writeContentIdSet(
            writer,
            "$path.discoveredEnemyKnowledgeIds",
            state.discoveredEnemyKnowledgeIds
        )
        writeContentIdSet(
            writer,
            "$path.unlockedHiddenContentIds",
            state.unlockedHiddenContentIds
        )
    }

    private fun readDiscoveryState(
        reader: FieldReader,
        path: String
    ): DiscoveryState =
        DiscoveryState(
            discoveredConvergenceIds = readContentIdSet(
                reader,
                "$path.discoveredConvergenceIds"
            ),
            discoveredMutationIds = readContentIdSet(
                reader,
                "$path.discoveredMutationIds"
            ),
            discoveredEnemyKnowledgeIds = readContentIdSet(
                reader,
                "$path.discoveredEnemyKnowledgeIds"
            ),
            unlockedHiddenContentIds = readContentIdSet(
                reader,
                "$path.unlockedHiddenContentIds"
            )
        )

    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------

    private fun writeStatisticsState(
        writer: FieldWriter,
        path: String,
        state: StatisticsState
    ) {
        val entries =
            state.countersByStatisticId.entries.sortedBy { it.key }
        writer.count(path, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPath = "$path.$index"
            writer.contentId("$entryPath.statisticId", entry.key)
            writer.gameNumber("$entryPath.value", entry.value)
        }
    }

    private fun readStatisticsState(
        reader: FieldReader,
        path: String
    ): StatisticsState {
        val result = linkedMapOf<ContentId, GameNumber>()
        repeat(reader.count(path)) { index ->
            val entryPath = "$path.$index"
            val id = reader.contentId("$entryPath.statisticId")
            val previous = result.put(
                id,
                reader.gameNumber("$entryPath.value")
            )
            if (previous != null) {
                throw SaveDataException(
                    "Duplicate statistic ID: $id"
                )
            }
        }
        return StatisticsState(result)
    }

    // -------------------------------------------------------------------------
    // Shared collection helpers
    // -------------------------------------------------------------------------

    private fun writeContentIdList(
        writer: FieldWriter,
        path: String,
        values: List<ContentId>
    ) {
        writer.count(path, values.size)
        values.forEachIndexed { index, value ->
            writer.contentId("$path.$index", value)
        }
    }

    private fun readContentIdList(
        reader: FieldReader,
        path: String
    ): List<ContentId> =
        buildList {
            repeat(reader.count(path)) { index ->
                add(reader.contentId("$path.$index"))
            }
        }

    private fun writeContentIdSet(
        writer: FieldWriter,
        path: String,
        values: Set<ContentId>
    ) {
        val ordered = values.sorted()
        writer.count(path, ordered.size)
        ordered.forEachIndexed { index, value ->
            writer.contentId("$path.$index", value)
        }
    }

    private fun writeContentIdMap(
        writer: FieldWriter,
        path: String,
        values: Map<ContentId, ContentId>
    ) {
        val ordered = values.entries.sortedBy { it.key }
        writer.count(path, ordered.size)
        ordered.forEachIndexed { index, entry ->
            writer.contentId("$path.$index.key", entry.key)
            writer.contentId("$path.$index.value", entry.value)
        }
    }

    private fun readContentIdMap(
        reader: FieldReader,
        path: String
    ): Map<ContentId, ContentId> {
        val result = linkedMapOf<ContentId, ContentId>()
        repeat(reader.count(path)) { index ->
            val key = reader.contentId("$path.$index.key")
            val value = reader.contentId("$path.$index.value")
            if (result.put(key, value) != null) {
                throw SaveDataException("Duplicate ContentId map key in $path: $key")
            }
        }
        return result
    }

    private fun readContentIdSet(
        reader: FieldReader,
        path: String
    ): Set<ContentId> {
        val result = linkedSetOf<ContentId>()
        repeat(reader.count(path)) { index ->
            val value = reader.contentId("$path.$index")
            if (!result.add(value)) {
                throw SaveDataException(
                    "Duplicate ContentId in set $path: $value"
                )
            }
        }
        return result
    }

    private fun writeInstanceIdList(
        writer: FieldWriter,
        path: String,
        values: List<InstanceId>
    ) {
        writer.count(path, values.size)
        values.forEachIndexed { index, value ->
            writer.instanceId("$path.$index", value)
        }
    }

    private fun readInstanceIdList(
        reader: FieldReader,
        path: String
    ): List<InstanceId> =
        buildList {
            repeat(reader.count(path)) { index ->
                add(reader.instanceId("$path.$index"))
            }
        }

    private fun writeInstanceIdSet(
        writer: FieldWriter,
        path: String,
        values: Set<InstanceId>
    ) {
        val ordered = values.sorted()
        writer.count(path, ordered.size)
        ordered.forEachIndexed { index, value ->
            writer.instanceId("$path.$index", value)
        }
    }

    private fun readInstanceIdSet(
        reader: FieldReader,
        path: String
    ): Set<InstanceId> {
        val result = linkedSetOf<InstanceId>()
        repeat(reader.count(path)) { index ->
            val value = reader.instanceId("$path.$index")
            if (!result.add(value)) {
                throw SaveDataException(
                    "Duplicate InstanceId in set $path: $value"
                )
            }
        }
        return result
    }
}

private class FieldWriter {
    private val values = linkedMapOf<String, String>()

    fun build(): SaveData =
        SaveData(values)

    fun string(path: String, value: String) {
        if (values.put(path, value) != null) {
            throw SaveDataException(
                "SaveData field written more than once: $path"
            )
        }
    }

    fun boolean(path: String, value: Boolean) =
        string(path, if (value) "1" else "0")

    fun int(path: String, value: Int) =
        string(path, value.toString())

    fun long(path: String, value: Long) =
        string(path, value.toString())

    fun count(path: String, value: Int) {
        require(value >= 0) {
            "Collection count cannot be negative at $path"
        }
        int("$path.count", value)
    }

    fun contentId(path: String, value: ContentId) =
        string(path, value.value)

    fun instanceId(path: String, value: InstanceId) =
        long(path, value.value)

    fun currencyId(path: String, value: CurrencyId) =
        contentId(path, value.id)

    fun gameNumber(path: String, value: GameNumber) =
        string(path, value.toPlainString())

    fun ratio(path: String, value: Ratio) =
        long(path, value.units)

    fun gameTime(path: String, value: GameTime) =
        long(path, value.millis)

    fun affinity(path: String, value: Affinity) =
        contentId(path, value.id)

    fun equipmentSlot(path: String, value: EquipmentSlot) =
        contentId(path, value.id)

    fun rarity(path: String, value: Rarity) =
        contentId(path, value.id)

    fun optionalContentId(path: String, value: ContentId?) {
        boolean("$path.present", value != null)
        if (value != null) {
            contentId("$path.value", value)
        }
    }

    fun optionalInstanceId(path: String, value: InstanceId?) {
        boolean("$path.present", value != null)
        if (value != null) {
            instanceId("$path.value", value)
        }
    }

    fun optionalGameTime(path: String, value: GameTime?) {
        boolean("$path.present", value != null)
        if (value != null) {
            gameTime("$path.value", value)
        }
    }

    fun optionalString(path: String, value: String?) {
        boolean("$path.present", value != null)
        if (value != null) {
            string("$path.value", value)
        }
    }
}

private class FieldReader(
    private val values: Map<String, String>
) {
    private val consumed = linkedSetOf<String>()

    fun string(path: String): String {
        val value = values[path]
            ?: throw SaveDataException(
                "Missing required SaveData field: $path"
            )
        consumed += path
        return value
    }

    fun boolean(path: String): Boolean =
        when (val value = string(path)) {
            "0" -> false
            "1" -> true
            else ->
                throw SaveDataException(
                    "Invalid boolean '$value' at $path"
                )
        }

    fun int(path: String): Int =
        parse(path) { it.toInt() }

    fun long(path: String): Long =
        parse(path) { it.toLong() }

    fun count(path: String): Int {
        val value = int("$path.count")
        if (value < 0) {
            throw SaveDataException(
                "Negative collection count at $path: $value"
            )
        }
        if (value > MAX_COLLECTION_ENTRIES) {
            throw SaveDataException(
                "Collection count exceeds safety limit at $path: $value"
            )
        }
        return value
    }

    fun contentId(path: String): ContentId =
        parse(path) { ContentId.parse(it) }

    fun instanceId(path: String): InstanceId =
        InstanceId(long(path))

    fun currencyId(path: String): CurrencyId =
        CurrencyId(contentId(path))

    fun gameNumber(path: String): GameNumber =
        parse(path) { GameNumber.parse(it) }

    fun ratio(path: String): Ratio =
        Ratio.ofUnits(long(path))

    fun gameTime(path: String): GameTime =
        GameTime.ofMillis(long(path))

    fun affinity(path: String): Affinity {
        val id = contentId(path)
        return Affinity.values().firstOrNull { it.id == id }
            ?: throw SaveDataException(
                "Unknown Affinity ContentId '$id' at $path"
            )
    }

    fun equipmentSlot(path: String): EquipmentSlot {
        val id = contentId(path)
        return EquipmentSlot.fromId(id)
            ?: throw SaveDataException(
                "Unknown EquipmentSlot ContentId '$id' at $path"
            )
    }

    fun rarity(path: String): Rarity {
        val id = contentId(path)
        return Rarity.values().firstOrNull { it.id == id }
            ?: throw SaveDataException(
                "Unknown Rarity ContentId '$id' at $path"
            )
    }

    fun combatStatus(path: String): CombatStatus =
        parseEnum(path) { CombatStatus.valueOf(it) }

    fun encounterStatus(path: String): EncounterStatus =
        parseEnum(path) { EncounterStatus.valueOf(it) }

    fun doctrineComparison(path: String): DoctrineComparison =
        parseEnum(path) { DoctrineComparison.valueOf(it) }

    fun doctrineStatusTarget(path: String): DoctrineStatusTarget =
        parseEnum(path) { DoctrineStatusTarget.valueOf(it) }

    fun optionalContentId(path: String): ContentId? =
        if (boolean("$path.present")) {
            contentId("$path.value")
        } else {
            null
        }

    fun optionalInstanceId(path: String): InstanceId? =
        if (boolean("$path.present")) {
            instanceId("$path.value")
        } else {
            null
        }

    fun optionalGameTime(path: String): GameTime? =
        if (boolean("$path.present")) {
            gameTime("$path.value")
        } else {
            null
        }

    fun optionalString(path: String): String? {
        // Optional fields may be absent in a predecessor schema that predates the
        // presence/value convention. Treat that as the optional default instead of
        // invalidating an otherwise decodable save.
        if ("$path.present" !in values) return null
        return if (boolean("$path.present")) {
            string("$path.value")
        } else {
            null
        }
    }

    fun requireFullyConsumed() {
        val unknown = values.keys - consumed
        if (unknown.isNotEmpty()) {
            throw SaveDataException(
                "Unknown/unconsumed SaveData fields for current schema: " +
                    unknown.sorted().take(MAX_REPORTED_UNKNOWN_FIELDS)
                        .joinToString()
            )
        }
    }

    private fun <T> parse(
        path: String,
        parser: (String) -> T
    ): T {
        val value = string(path)
        return try {
            parser(value)
        } catch (error: SaveDataException) {
            throw error
        } catch (error: Throwable) {
            throw SaveDataException(
                "Invalid value '$value' at $path",
                error
            )
        }
    }

    private fun <T> parseEnum(
        path: String,
        parser: (String) -> T
    ): T =
        parse(path, parser)

    companion object {
        private const val MAX_COLLECTION_ENTRIES: Int = 100_000
        private const val MAX_REPORTED_UNKNOWN_FIELDS: Int = 20
    }
}
