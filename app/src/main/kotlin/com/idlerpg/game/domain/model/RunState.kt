package com.idlerpg.game.domain.model

import com.idlerpg.game.domain.model.adaptation.AdaptationState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.doctrine.DoctrineState
import com.idlerpg.game.domain.model.economy.EconomyState
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.model.player.PlayerState
import com.idlerpg.game.domain.model.progression.ProgressionState
import com.idlerpg.game.domain.model.quest.QuestState
import com.idlerpg.game.domain.model.resonance.ResonanceState
import com.idlerpg.game.domain.model.statistics.StatisticsState
import com.idlerpg.game.domain.model.world.WorldState

/**
 * Chronicle-resettable gameplay state.
 *
 * Foundation 14 adds [QuestState] here because quests are Chronicle-resettable run state.
 */
data class RunState(
    val player: PlayerState = PlayerState(),
    val combat: CombatState = CombatState(),
    val world: WorldState = WorldState(),
    val economy: EconomyState = EconomyState(),
    val resonance: ResonanceState = ResonanceState(),
    val doctrine: DoctrineState = DoctrineState(),
    val adaptation: AdaptationState = AdaptationState(),
    val inventory: InventoryState = InventoryState(),
    val progression: ProgressionState = ProgressionState(),
    val quests: QuestState = QuestState(),
    val statistics: StatisticsState = StatisticsState()
)
