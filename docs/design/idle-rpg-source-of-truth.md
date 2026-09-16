# IdleRPG Source-of-Truth Gameplay Contract

Status: PR #3 implementation contract. This document is the authoritative gameplay boundary for the implemented progression, Rebirth, skills, equipment, loot, offline, persistence, and Power Score slices. Constants remain balance targets and can be tuned through the central configuration.

## Purpose

IdleRPG is a single-hero incremental RPG. Automation removes repetitive combat time; it does not make decisions on behalf of the player.

The player repeatedly:

1. Chooses whether to push or farm.
2. Builds a skill loadout and automation rules.
3. Clears stages and bosses.
4. Earns Gold, XP, equipment, materials, and progression.
5. Inspects actual stat changes.
6. Enhances, refines, equips, salvages, or retains gear.
7. Rebirths when the long-term benefit justifies the reset.

All gameplay outcomes come from canonical domain state and deterministic simulation. Compose projections display that state; they do not calculate alternate combat or reward results.

## Product boundaries

- One hero; no party system.
- Standard RPG language: Attack, HP, Armor, Defense, Critical Chance, Critical Damage, Speed, Fire, Ice, and similar readable terms.
- Random gear rolls are allowed as gameplay loot. There is no paid gear gacha, companion gacha, or pet gacha.
- No leaderboard or trading economy.
- No AI or LLM decision path in gameplay.
- Gameplay is offline-capable. The store may connect only when purchasing or restoring real-money Gems.
- No pity system for Legendary drops or enhancement.
- No gear destruction and no enhancement durability loss.

## Progression scale

### Level

- Level starts at 1.
- Maximum level is 15,000.
- The soft-cap transition begins at level 800.
- Level 15,000 is a hard cap. XP beyond the cap is not converted into automatic levels.
- XP requirements must grow exponentially, with a clearly faster late-game curve after level 800.
- The level curve must be centralized, deterministic, overflow-safe, and evaluated without simulating every individual level during large offline grants.
- The first rebirth threshold is level 1,000. Reaching level 1,000 does not force a rebirth.
- Levels beyond 1,000 remain valuable because they improve temporary combat strength, stage access, farming results, and deep-push milestones.

The initial target curve is piecewise exponential:

~~~text
XP to next level at L <= 800:
  ceil(100 * 1.008^(L - 1))

XP to next level at L > 800:
  ceil(100 * 1.008^799 * 1.012^(L - 800))

At L = 15,000:
  no next level exists
~~~

The coefficients are balance configuration, not UI constants. The implementation must use exact fixed-point or BigInteger-safe arithmetic rather than platform floating-point behavior.

### Normal stat growth

Normal level and upgrade progression is run-scoped. It is reset by rebirth. Rebirth points are then allocated into the normal-stat layer as permanent progression.

Core normal-stat candidates are:

- Attack
- Max HP
- Armor
- Defense
- Critical Chance
- Critical Damage
- Speed
- Elemental power or resistance where the stat has a clearly defined combat role

Niche effects should not be added to the allocation screen unless their mathematical role is stable and player-readable. Bounded probabilities use hard caps where required; other stats use diminishing returns where an uncapped linear investment would create a dominant strategy.

## Rebirth

Rebirth is an explicit soft reset, not the existing full Chronicle collapse.

### Eligibility and cost

- Rebirth is available at level 1,000 or higher.
- The player may continue leveling instead of rebirthing.
- The first rebirth costs 100,000,000 Gold.
- The cost increases for every later rebirth.
- The cost and reward transaction is atomic: either the Gold is consumed and the reset is committed, or neither happens.
- Rebirth is available from a safe menu state, never automatically and never in the middle of an unresolved combat action.
- The confirmation screen must show the reset list, retained list, cost, normal points, Legacy points, and any deep-level reward.

Initial rebirth-cost target:

~~~text
rebirthCost(n) = ceil(100,000,000 * 1.25^(n - 1))

n = 1 for the first rebirth
~~~

The multiplier is centrally tunable after balance testing.

### Reset on rebirth

The following reset:

- Level and current XP
- Run-scoped normal-stat purchases
- Skill unlocks
- Equipped skill loadout
- Skill rank
- Skill mastery
- Skill evolution choices
- Run-scoped skill refinement
- Quest progress
- Stage progress and current push position
- Other explicitly run-scoped progression

The following remain:

- Gold
- Equipment and equipment enhancement levels
- Equipment main-stat and substat rolls
- Materials
- Inventory and overflow
- Gems
- Rebirth count
- Rebirth-point allocation
- Legacy-point allocation
- Lifetime statistics and achievements where already permanent
- Other explicitly meta-scoped progression

Gear has no player-level requirement and can be equipped immediately after rebirth. Gear effects that depend on an unavailable skill remain inactive until that skill is unlocked again.

### Rebirth rewards

Normal rebirth points are permanent and freely allocated into the normal-stat layer.

Initial target:

~~~text
normalPoints(n) = 100 + 25 * (n - 1)
legacyPoints(n) = floor(normalPoints(n) / 5)
~~~

The first rebirth grants 100 normal points. Later rebirths grant incrementally more. Legacy grows more slowly and remains a separate meta-progression layer.

Rebirthing at level 15,000 grants a modest deep-level reward but does not grant additional normal rebirth points. The purpose is to preserve both strategies:

- Fast rebirth cycles for permanent allocation growth.
- Deep pushes for stronger temporary power, better farming, boss access, and milestone rewards.

Legacy should primarily improve long-term acquisition and economy, such as Legendary drop probability, enhancement protection access, or bounded efficiency. It must not become a second large direct-combat-stat tree.

## Combat math contract

Combat must have one shared authoritative pipeline used by active play, offline simulation, projections, and tests.

The target ordering is:

1. Build derived player and enemy stats.
2. Resolve the skill, coefficient, target, and conditions.
3. Apply Attack, skill-power, elemental, and conditional bonuses.
4. Roll Critical Chance using the canonical RNG.
5. Apply Critical Damage when the hit is eligible and critical.
6. Apply target vulnerability and elemental resistance.
7. Apply Armor and Penetration for physical damage.
8. Resolve Defense-driven Guard, block, stagger, or similar defensive behavior.
9. Resolve shields and HP damage.
10. Emit the authoritative event.
11. Resolve statuses, death, rewards, wave transitions, and stage state.

Armor answers physical mitigation. Defense is a separate defensive interaction and must not silently duplicate Armor's reduction.

A baseline physical mitigation shape is:

~~~text
physicalDamageTaken =
  incomingPhysicalDamage * 100 / (100 + max(Armor - Penetration, 0))
~~~

Critical Chance, Speed, resistance, penetration, status chance, and Guard behavior must have explicit caps or diminishing-return rules. Rounding occurs at defined domain boundaries, never separately inside UI projections.

## Skills

Skills are build decisions, not decorative buttons.

Every skill definition must be able to describe:

- Role
- Scaling stat
- Damage or healing coefficient
- Element
- Number and type of targets
- Cooldown or recovery
- Resource cost
- Status effects
- Conditions
- Critical eligibility
- Mastery modifiers
- Evolution modifiers
- Refinement modifiers
- Player-readable technical explanation

The player always has Basic Attack. The remaining active and passive loadout is limited so that choices matter. Equipped skills and Doctrine rules persist during the current life and reset on rebirth.

Skill evolution choices are mutually exclusive within a life. Mastery and refinement improve named parts of a skill rather than adding an opaque universal multiplier. Technical details are available through an intentional detail surface, such as a hold action or advanced view.

## Equipment, rolls, and refinement

Equipment has two separate progression layers.

### Base equipment enhancement

The enhancement ladder is:

~~~text
+1 through +15
PRI
DUO
TRI
TET
PEN
~~~

Enhancement:

- Improves only the item's base stats.
- Consumes materials on every attempt.
- Has no durability loss.
- Does not destroy the item.
- Does not alter the item's rolled substats.
- May downgrade one enhancement level after failure from PRI through PEN.
- Has no pity or guaranteed-success counter.
- Uses failstacks and protection rules defined centrally.
- Shows exact success chance and failure consequence before confirmation.

### Random equipment rolls

Equipment drops with a main stat and three or four random substat lines depending on the item's rarity. The substat values are gacha-like gameplay rolls:

- Candidate stats are selected from the item's legal pool.
- Duplicate substat lines are not allowed.
- A substat cannot duplicate the main stat.
- Each selected substat receives a deterministic value-quality roll.
- Higher rarity improves the number and quality of available substats.
- Gear refinement is a separate system from enhancement.
- Refinement may improve or reroll a selected roll, but must not silently behave like enhancement.

The exact refinement actions must be represented as explicit commands with previewable cost and outcome. Randomness advances only through actual committed gameplay rolls.

## Loot and Legendary gear

- Normal stages, elite encounters, and bosses use authored loot tables.
- Bosses have better Legendary weighting than ordinary encounters.
- Legacy bonuses are small additive improvements with an upper bound.
- Legendary gear cannot be created by promoting lower rarities.
- Legendary gear cannot be crafted into existence.
- There is no Legendary pity system.
- Extreme rarity is acceptable if long-term play makes acquisition realistically obtainable.
- Duplicate Legendary items must have a meaningful salvage, collection, or refinement value.

The player controls equip, lock, salvage, and capacity decisions. Automatic salvage is an explicit player policy and must never delete protected or build-defining equipment.

## Stages and automation

The current authored route is a baseline, not the final endgame.

The long-term route must support:

- Normal encounters
- Elite encounters
- Multi-wave encounters
- Special or anomaly encounters
- Boss encounters with authored mechanics
- Stage scaling beyond the initial authored route
- Repeatable farming
- Meaningful push milestones

Push remains active until the player fails or reaches the available boundary. After failure, the player may resume from the latest clear. Farming target selection remains a player decision during active play.

Auto Battle is a helper:

- Basic Attack remains available.
- Auto Battle executes the player's equipped skills and Doctrine rules.
- Auto does not choose the player's build, stage, salvage policy, or rebirth timing.
- Manual queues and player commands remain authoritative.

## Offline progression

Offline progression is bounded and deliberately narrower than active play.

- Gameplay simulation remains available without network access.
- The default offline claim window is 12 hours.
- Offline farming uses the latest cleared non-boss stage.
- If the latest clear is a boss, the simulation moves backward to the nearest eligible non-boss stage.
- Offline rewards are Gold and XP only.
- Offline progression does not award gear, materials, loot, quests, mastery, achievements, stage progress, Legacy points, or Gems.
- Offline XP can advance the level toward 15,000.
- Rebirth is never performed automatically.
- The return result reports the simulated duration, Gold, XP, level changes, and stopping reason.
- Offline simulation uses canonical saved state and deterministic arithmetic; it must not invent a second combat formula.

Real-money Gem purchase and restoration may require a platform connection when the store is used. Once validated and saved, the gameplay loop remains usable offline.

## Persistence and RNG

- Canonical GameState remains the gameplay source of truth.
- The existing EngineState remains responsible for simulation time, deterministic RNG continuation, event sequencing, and instance IDs.
- Rebirth is a new explicit reset boundary and must not be implemented by renaming or weakening Chronicle.
- Rebirth commits must be atomic and crash-safe.
- Enhancement and loot rolls must commit their consumed materials, RNG advancement, and result together.
- Save restoration of an older local copy cannot be technically prevented in a fully offline game. The design therefore uses committed RNG state and atomic persistence, while treating save restoration as outside the honest-play contract.
- Background reseeding every ten seconds is not an anti-cheat mechanism and must not replace canonical deterministic RNG.

## Power Score

Power Score is informational, not a hidden combat multiplier and not the sole stage gate.

It must provide:

- A headline score.
- Offense breakdown.
- Defense and survivability breakdown.
- Gear contribution.
- Skill and mastery contribution.
- Rebirth and Legacy contribution.
- A hold or detail action that exposes the complete calculation.

Expected DPS, Effective HP, and other build projections should be shown separately because different skill rotations and defensive builds cannot be represented honestly by one number.

## PR #3 implementation coverage

The current branch implements the core contract in vertical slices:

- [x] Exact level progression through the 15,000 hard cap with the level-800 soft transition.
- [x] Atomic Rebirth preview, cost, reset/retain boundary, incremental normal points, Legacy points, and allocation/respec commands.
- [x] Independent bounded skill rank, mastery, evolution, and refinement progression with technical explanations.
- [x] Separate gear enhancement and rolled-affix refinement, including +1 through +15, PRI through PEN, failstacks, protection, downgrade behavior, and persistence migration.
- [x] Deterministic three- or four-line affix rolls, boss-weighted Legendary loot, and a capped additive Legendary Find bonus without pity.
- [x] Offline simulation constrained to Gold and XP from the latest eligible non-boss stage.
- [x] Explanatory Power Score projections and player-facing Progress, Gear, and Skill detail surfaces.
- [x] Deterministic scenario, presentation, persistence, and active/offline regression coverage.

Further authored content and balance tuning can extend this contract without changing its reset, retention, randomness, or offline boundaries.

Any implementation that conflicts with this document must either update the contract first or be treated as a defect.
