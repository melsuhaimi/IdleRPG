package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.chronicle.EchoOfferDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoUnlockEffect
import com.idlerpg.game.domain.definition.progression.FeatureUnlockDefinition
import com.idlerpg.game.domain.definition.progression.FeatureUnlockScope

/** One permanent starting investment per existing growth track; no additional currency. */
object EchoTrainingContent {
    const val STARTING_LEVEL = 5L
    val upgradeIds = listOf(
        DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, DefaultGameContent.ENDURANCE_UPGRADE_ID,
        DefaultGameContent.ARMOR_TRAINING_UPGRADE_ID, DefaultGameContent.TEMPO_TRAINING_UPGRADE_ID,
        DefaultGameContent.PRECISION_UPGRADE_ID, DefaultGameContent.LETHALITY_UPGRADE_ID,
        DefaultGameContent.CHANNELING_UPGRADE_ID, DefaultGameContent.RESTORATION_UPGRADE_ID
    )
    fun offerId(upgradeId: ContentId) = ContentId("echo_unlock.training.${upgradeId.value.substringAfterLast('.')}")
    fun featureId(upgradeId: ContentId) = ContentId("feature.echo.training.${upgradeId.value.substringAfterLast('.')}")
    val features = upgradeIds.map { FeatureUnlockDefinition(featureId(it), FeatureUnlockScope.META) }
    val offers = upgradeIds.map {
        EchoOfferDefinition(offerId(it), GameNumber.of(8L), listOf(EchoUnlockEffect.UnlockPersistentFeature(featureId(it))))
    }
}
