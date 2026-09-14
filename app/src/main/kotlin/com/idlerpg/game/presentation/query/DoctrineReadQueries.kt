package com.idlerpg.game.presentation.query

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.resonance.ConvergenceSystem

/** FUI-06 read-only facade for backend-owned Doctrine/Resonance limits and eligibility. */
interface DoctrineReadQueries {
    fun resonanceSequenceCapacity(): Int
    fun convergenceEligible(state: GameState, convergenceId: ContentId): Boolean
}

class DefaultDoctrineReadQueries(
    private val contentRegistry: ContentRegistry,
    private val balanceConfig: BalanceConfig
) : DoctrineReadQueries {
    override fun resonanceSequenceCapacity(): Int = balanceConfig.resonanceSequenceBufferSize

    override fun convergenceEligible(
        state: GameState,
        convergenceId: ContentId
    ): Boolean {
        val definition = contentRegistry.convergenceOrNull(convergenceId) ?: return false
        return ConvergenceSystem.isEligible(state, definition)
    }
}
