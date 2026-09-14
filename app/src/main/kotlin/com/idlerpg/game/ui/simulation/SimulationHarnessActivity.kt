package com.idlerpg.game.ui.simulation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider

/**
 * Separate developer activity for observing Foundation 18 simulations on-device.
 *
 * This activity does not replace MainActivity and does not migrate the legacy game screen.
 */
class SimulationHarnessActivity : ComponentActivity() {

    private val simulationViewModel: SimulationHarnessViewModel by lazy {
        ViewModelProvider(this)[SimulationHarnessViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SimulationHarnessScreen(simulationViewModel)
            }
        }
    }
}
