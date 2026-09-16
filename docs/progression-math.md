# Incremental progression and combat math

Status: PR #3 implementation index.

The authoritative contract is [IdleRPG Source of Truth](design/idle-rpg-source-of-truth.md). This file summarizes the formulas and migration boundaries; runtime systems and projections consume the same domain-owned definitions.

## PR #2 baseline

The previous branch baseline used:

- A Progressive XP curve with linear and triangular growth.
- Player-level skill rank with an optional skill maximum.
- Level-step combat stat contributions.
- 120 authored Training Hollow stages.
- A shared Armor mitigation boundary.
- Deterministic active and offline simulation.

Those values remain useful as implementation evidence for the existing runtime, but they are not the new design target where they conflict with the PR #3 contract.

## PR #3 target changes

- Maximum level is 15,000.
- The level soft-cap transition begins at 800.
- XP growth becomes piecewise exponential and remains exact-arithmetic safe.
- Skill rank, mastery, evolution, and refinement become independent bounded progression systems rather than a direct mirror of player level.
- Rebirth becomes a separate soft-reset boundary from Chronicle.
- Rebirth points and Legacy points become permanent allocation layers.
- Gear enhancement changes base stats only.
- Gear receives three or four random substat lines with separate roll/refinement behavior.
- Enhancement uses the +1 to +15 and PRI through PEN ladder without durability loss or item destruction.
- Offline progression awards only Gold and XP.
- Legendary acquisition remains random, boss-weighted, and without pity.

## Migration rule

Before changing a formula or system, update the authoritative source-of-truth contract and its tests first. Runtime projections, active simulation, offline simulation, and UI explanations must all consume the same domain-owned formulas.
