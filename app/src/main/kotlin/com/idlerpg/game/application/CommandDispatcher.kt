package com.idlerpg.game.application

import com.idlerpg.game.domain.command.AchievementCommand
import com.idlerpg.game.domain.command.ChronicleCommand
import com.idlerpg.game.domain.command.ClaimAchievementReward
import com.idlerpg.game.domain.command.ClaimQuestReward
import com.idlerpg.game.domain.command.ClearQueuedSkillCast
import com.idlerpg.game.domain.command.DoctrineCommand
import com.idlerpg.game.domain.command.EconomyCommand
import com.idlerpg.game.domain.command.EchoCommand
import com.idlerpg.game.domain.command.EquipSkill
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.InventoryCommand
import com.idlerpg.game.domain.command.MoveEquippedSkill
import com.idlerpg.game.domain.command.PurchaseEchoOffer
import com.idlerpg.game.domain.command.PlayerCommand
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.command.RebirthCommand
import com.idlerpg.game.domain.command.SelectSkillEvolution
import com.idlerpg.game.domain.command.QuestCommand
import com.idlerpg.game.domain.command.SkillCommand
import com.idlerpg.game.domain.command.UnequipSkill
import com.idlerpg.game.domain.command.WorldCommand
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.engine.GameEngine
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.achievement.AchievementClaimSystem
import com.idlerpg.game.domain.system.chronicle.ChronicleSystem
import com.idlerpg.game.domain.system.doctrine.DoctrineSystem
import com.idlerpg.game.domain.system.economy.EconomySystem
import com.idlerpg.game.domain.system.chronicle.EchoOfferSystem
import com.idlerpg.game.domain.system.inventory.InventorySystem
import com.idlerpg.game.domain.system.quest.QuestClaimSystem
import com.idlerpg.game.domain.system.skill.ManualSkillInputSystem
import com.idlerpg.game.domain.system.skill.SkillLoadoutSystem
import com.idlerpg.game.domain.system.skill.SkillEvolutionSystem
import com.idlerpg.game.domain.system.player.PlayerSystem
import com.idlerpg.game.domain.system.rebirth.RebirthSystem
import com.idlerpg.game.domain.system.world.WorldSystem

/** Application-layer command router; gameplay mutation stays in owning domain systems. */
object ApplicationCommandRouter : GameCommandHandler {
    override fun handle(state: GameState, command: GameCommand, context: EngineContext): CommandHandlingResult = when (command) {
        is EconomyCommand -> EconomySystem.handle(state, command, context)
        is WorldCommand -> WorldSystem.handle(state, command, context)
        is DoctrineCommand -> DoctrineSystem.handle(state, command, context)
        is InventoryCommand -> InventorySystem.handle(state, command, context)
        is ChronicleCommand -> ChronicleSystem.handle(state, command, context)
        is SkillCommand -> when (command) {
            is QueueSkillCast -> ManualSkillInputSystem.handle(state, command, context)
            is ClearQueuedSkillCast -> ManualSkillInputSystem.handle(state, command, context)
            is EquipSkill -> SkillLoadoutSystem.handle(state, command, context)
            is UnequipSkill -> SkillLoadoutSystem.handle(state, command, context)
            is MoveEquippedSkill -> SkillLoadoutSystem.handle(state, command, context)
            is SelectSkillEvolution -> SkillEvolutionSystem.handle(state, command, context)
        }
        is QuestCommand -> when (command) { is ClaimQuestReward -> QuestClaimSystem.handle(state, command, context) }
        is AchievementCommand -> when (command) { is ClaimAchievementReward -> AchievementClaimSystem.handle(state, command, context) }
        is EchoCommand -> when (command) { is PurchaseEchoOffer -> EchoOfferSystem.handle(state, command, context) }
        is PlayerCommand -> PlayerSystem.handle(state, command, context)
        is RebirthCommand -> RebirthSystem.handle(state, command, context)
    }
}

class CommandDispatcher(private val engineContext: EngineContext) {
    fun dispatch(state: GameState, command: GameCommand): EngineResult = GameEngine.handle(state, command, engineContext)
}
