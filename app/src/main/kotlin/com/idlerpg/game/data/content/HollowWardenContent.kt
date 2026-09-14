package com.idlerpg.game.data.content
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.enemy.EnemyAttackDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.definition.world.BossDefinition
/** The first of four scalable Training Hollow boss milestones. */
object HollowWardenContent {
    val BOSS_ID = ContentId("boss.hollow_warden"); val ENEMY_ID = ContentId("enemy.hollow_warden")
    val ATTACK_ID = ContentId("enemy_attack.hollow_warden.fracture_lance")
    val attack = EnemyAttackDefinition(ATTACK_ID, "Fracture Lance", GameDuration.ofMillis(1_850L), GameNumber.of(17L), affinity = Affinity.GUARD, damageKind = DamageKind.ARCANE, resonanceDrain = GameNumber.ONE)
    val enemy = EnemyDefinition(ENEMY_ID, "The Hollow Warden", GameNumber.of(520L), GameNumber.of(180L), GameNumber.of(180L), TrainingHollowLootContent.BOSS_LOOT_TABLE_ID, ATTACK_ID, role = EnemyRole.BOSS)
    val boss = BossDefinition(BOSS_ID, DefaultGameContent.TRAINING_HOLLOW_REGION_ID, TrainingHollowWorldContent.stageId(30), 15L, true)
}
