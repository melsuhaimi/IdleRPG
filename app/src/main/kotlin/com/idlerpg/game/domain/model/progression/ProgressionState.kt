package com.idlerpg.game.domain.model.progression

/** Foundation 13 run-progression aggregate. */
data class ProgressionState(
    val playerLevel: PlayerLevelState = PlayerLevelState(),
    val featureUnlocks: FeatureUnlockState = FeatureUnlockState(),
    val affinityMastery: AffinityMasteryState = AffinityMasteryState(),
    val skillProgression: SkillProgressionState = SkillProgressionState()
)
