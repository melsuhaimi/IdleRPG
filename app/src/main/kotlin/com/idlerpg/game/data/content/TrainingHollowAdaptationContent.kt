package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.adaptation.MutationDefinition
import com.idlerpg.game.domain.definition.adaptation.MutationEffectDefinition

/** Eight affinity reactions; four also change combat behavior beyond resistance. */
object TrainingHollowAdaptationContent {
    val REFLEX_CARAPACE_ID = ContentId("mutation.reflex_carapace")
    val NULL_VEIL_ID = ContentId("mutation.null_veil")
    val BLIGHTBLOOD_ID = ContentId("mutation.blightblood")
    val SIEGE_HUNGER_ID = ContentId("mutation.siege_hunger")

    val mutations = listOf(
        mutation("ash_skin", Affinity.EMBER),
        mutation("stonehide", Affinity.MIGHT),
        mutation(
            "reflex_carapace",
            Affinity.TEMPO,
            MutationEffectDefinition.EnemyActionIntervalMultiplier(Ratio.ofUnits(8_500L))
        ),
        mutation("icebound_core", Affinity.FROST),
        mutation(
            "null_veil",
            Affinity.ARCANE,
            MutationEffectDefinition.AdditionalResonanceDrain(GameNumber.ONE)
        ),
        mutation(
            "blightblood",
            Affinity.VITALITY,
            MutationEffectDefinition.PlayerHealingMultiplier(Ratio.ofUnits(7_000L))
        ),
        mutation("gloom_ward", Affinity.SHADOW),
        mutation(
            "siege_hunger",
            Affinity.GUARD,
            MutationEffectDefinition.EnemyDamageMultiplierWhilePlayerHasAffinityStatus(
                affinity = Affinity.GUARD,
                multiplier = Ratio.ofUnits(11_500L)
            )
        )
    )

    private fun mutation(
        name: String,
        affinity: Affinity,
        additionalEffect: MutationEffectDefinition? = null
    ): MutationDefinition = MutationDefinition(
        id = ContentId("mutation.$name"),
        displayName = name.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
        triggerAffinity = affinity,
        minimumAdaptationTier = 1,
        selectionWeight = 100L,
        effect = MutationEffectDefinition.DamageTakenMultiplierForAffinity(
            affinity = affinity,
            multiplier = Ratio.ofUnits(if (affinity == Affinity.TEMPO) 7_800L else 7_500L)
        ),
        additionalEffects = listOfNotNull(additionalEffect)
    )
}
