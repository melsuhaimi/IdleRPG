package com.idlerpg.game.domain.event

/**
 * Root contract for completed domain facts.
 *
 * Events describe what happened. Canonical GameState remains the durable source of truth;
 * an unbounded event history is not stored inside GameState.
 */
sealed interface GameEvent
