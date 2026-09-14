package com.idlerpg.game.data.repository

import com.idlerpg.game.data.content.GameContent

/**
 * Authored-content repository boundary.
 *
 * Foundation 15 introduces the contract only. DefaultGameContent remains the current
 * Kotlin-authored production content source; external content persistence is not added.
 */
interface ContentRepository {
    fun load(): GameContent
}
