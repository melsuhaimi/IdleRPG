package com.idlerpg.game.ui.accessibility

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Presentation-only accessibility preferences; this store never contains gameplay state. */
@Stable
class PresentationPreferencesState internal constructor(
    private val preferences: SharedPreferences,
    initialReducedMotion: Boolean,
    initialHapticsEnabled: Boolean
) {
    var reducedMotion: Boolean by mutableStateOf(initialReducedMotion)
        private set

    var hapticsEnabled: Boolean by mutableStateOf(initialHapticsEnabled)
        private set

    fun updateReducedMotion(enabled: Boolean) {
        if (reducedMotion == enabled) return
        reducedMotion = enabled
        preferences.edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        if (hapticsEnabled == enabled) return
        hapticsEnabled = enabled
        preferences.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    companion object {
        internal const val KEY_REDUCED_MOTION: String = "reduced_motion"
        internal const val KEY_HAPTICS_ENABLED: String = "haptics_enabled"
    }
}

@Composable
fun rememberPresentationPreferences(): PresentationPreferencesState {
    val applicationContext = LocalContext.current.applicationContext
    val preferences = remember(applicationContext) {
        applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    }
    return remember(preferences) {
        PresentationPreferencesState(
            preferences = preferences,
            initialReducedMotion = preferences.getBoolean(
                PresentationPreferencesState.KEY_REDUCED_MOTION,
                false
            ),
            initialHapticsEnabled = preferences.getBoolean(
                PresentationPreferencesState.KEY_HAPTICS_ENABLED,
                true
            )
        )
    }
}

private const val PREFERENCES_NAME: String = "idle_rpg_presentation_preferences"
