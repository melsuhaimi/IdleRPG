package com.idlerpg.game.domain.event

/** Completed player-identity facts. */
sealed interface PlayerEvent : GameEvent

data class HeroNamed(
    val name: String
) : PlayerEvent {
    init {
        require(name.isNotBlank()) { "Hero name cannot be blank" }
    }
}
