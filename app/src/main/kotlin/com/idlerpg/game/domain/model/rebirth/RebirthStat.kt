package com.idlerpg.game.domain.model.rebirth

/** Base-stat channels that can receive permanent Rebirth or Legacy points. */
enum class RebirthStat {
    ATTACK_POWER,
    MAX_HEALTH,
    ARMOR,
    ACTION_SPEED,
    CRITICAL_CHANCE,
    CRITICAL_MULTIPLIER,
    EFFECT_POWER,
    HEALING_POWER,
    LEGENDARY_FIND
}

/** Permanent point pools earned by the Rebirth system. */
enum class RebirthPointPool {
    NORMAL,
    LEGACY
}
