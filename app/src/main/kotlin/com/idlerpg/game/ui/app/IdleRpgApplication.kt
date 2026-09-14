package com.idlerpg.game.ui.app

import android.app.Application

/** Android process owner for the one production [GameAppContainer]. */
class IdleRpgApplication : Application() {
    lateinit var container: GameAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = GameAppContainer(this)
        // One process boot request. GameAppContainer only constructs dependencies.
        container.runtimeController.initialize()
    }
}
