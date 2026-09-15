# Incremental progression and combat math

This document is the tuning contract for the scalable Training Hollow route. The formulas are
implemented in `domain/system` and are reused by simulation, rewards, and presentation queries.

## Player level and XP

The default level curve is `Progressive`:

```text
XP(L) = 20 + 10 × (L - 1) + triangular(max(L - 10, 0))
triangular(n) = n × (n - 1) / 2
```

`XP(L)` is the cost to move from level `L` to `L + 1`. The first ten levels retain the original
readable linear costs; the triangular term then adds gentle acceleration without multiplying the
previous requirement. Cumulative XP is evaluated with exact `BigInteger` sums, so offline grants
can skip many levels without a level-by-level loop.

| Level | XP to next | Cumulative XP from level 1 |
| ---: | ---: | ---: |
| 1 | 20 | 0 |
| 10 | 110 | 540 |
| 25 | 365 | 3,695 |
| 50 | 1,290 | 22,620 |
| 100 | 5,015 | 167,970 |

On level-up, the current XP remainder is preserved, all eligible feature unlocks are evaluated,
and the same canonical state is used by active and offline simulation.

## Player combat baseline

Persisted `BaseStats` are the level-one contract. At level `L`, the baseline gains `L - 1` steps:

| Stat | Level-step contribution | Definition |
| --- | ---: | --- |
| Attack | +2 | Base damage before skill scaling |
| Max Health | +12 | Combat health ceiling |
| Armor | +3 | Physical damage mitigation input |
| Attack Speed | +0.40% | Shortens action interval |
| Critical Chance | +0.25% | Clamped to 100% at the roll |
| Critical Damage | +0.50% | Multiplier applied to critical hits |
| Effect Power | +0.30% | Element/status effect scaling |
| Healing Power | +0.25% | Healing output scaling |

Equipment, upgrades, affixes, and active statuses are applied by `ModifierSystem` after the level
baseline. Ratio aggregation is fixed-point and clamped at `Long.MAX_VALUE` before it becomes a
runtime ratio.

Armor uses diminishing mitigation:

```text
physical damage after armor = damage × 100 / (100 + max(Armor - Penetration, 0))
damage reduction = Armor / (100 + Armor)
```

The final action interval is bounded to at least 250 ms. Critical chance is bounded to 100%.

## Skill scaling

Skill rank is the player level capped by the skill's optional `maxRank`. For a skill with rank `R`:

```text
damage power ratio = authored ratio × (1 + 0.75% × (R - 1))
healing amount = authored amount × (1 + 0.60% × (R - 1))
```

Trait multipliers, effect power, critical state, affinity/status modifiers, and target mitigation
are then applied by the normal combat pipeline. Skills retain different authored effect lists and
therefore keep distinct identities instead of becoming reskinned copies of one formula.

## Enemy and stage scaling

Stage 1 maps to scaling tier 0. A later stage uses `tier = stage - 1`.

Each enemy role has an independent profile. For a value with role growth `g`:

```text
scaled value = base value × (1 + g × tier)
```

Armor additionally grows by a flat role-specific amount per tier. Regional health growth is added
after role health scaling. Tanks therefore gain more health and armor, assassins gain more damage
and cadence, casters gain stronger damage/reward growth, and bosses use their own stronger profile.

Enemy XP and gold use their role reward profile, then the encounter reward multiplier is applied.
The expanded Training Hollow contains 120 ordered stages, with authored role compositions, elite
and special encounters, multi-wave encounters, and boss milestones at stages 30, 45, 60, 72, 84,
96, 108, and 120. The final stage stops push automation cleanly; players can choose it as a
farm target. Each boss milestone has its own boss definition so first-clear persistence remains
unambiguous.

## Combat pipeline

The authoritative player damage path is deterministic and ordered:

1. Resolve the skill rank and authored effect.
2. Apply equipment trait multipliers.
3. Apply attack/effect-power scaling and conditional skill power.
4. Roll critical chance and apply critical damage when allowed.
5. Apply target status vulnerability.
6. Apply physical armor and penetration; elemental/arcane/shadow damage bypasses physical armor.
7. Apply formation protection and adaptive affinity rules.
8. Clamp to at least one damage when a non-zero hit reaches mitigation.
9. Subtract health, emit the damage event, then resolve statuses/death/wave progression.

Enemy attacks use the same armor mitigation boundary against the player's derived armor. Action
intervals, rewards, and projections all call the shared scaling owners; screens do not reimplement
these formulas or invent display values.

## Validation checkpoints

The `IncrementalProgressionContractTest` covers monotonic player baselines and enemy growth at
levels/tiers 1, 10, 25, 50, and 100. It also checks role differentiation, positive bounded action
intervals, armor mitigation, critical chance clamping, and stage/boss content expansion.
