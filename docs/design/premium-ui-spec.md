# Idle RPG — Premium Expedition UI Specification

Status: implementation companion for the Compose redesign. The Android runtime and canonical simulation remain the source of truth.

## Product promise

An atmospheric dark-fantasy expedition RPG about one hero making deliberate build choices while an automatic, event-driven battle unfolds.

The repeatable loop is:

`Battle → earn Gold / XP / items → inspect the real trade-off → equip or buy an existing upgrade → see the changed stat → return to Battle.`

The source already contains the combat, waves, bosses, equipment, skills, deterministic Doctrine automation, Push/Farm behavior, offline simulation, saves, migrations, and long-term progression. This pass makes those systems legible; it does not add currencies, crafting, shops, gacha, companions, or unrelated tasks.

## Information hierarchy

1. What is happening now: hero, enemy, health, wave, incoming attack, combat state.
2. What changed: factual hit/heal/skill/status/defeat/reward feedback from canonical events.
3. What the player can do next: queue a skill, inspect the loadout, change equipment, buy an affordable existing upgrade, retry, farm, or push.
4. What is useful to inspect: detailed stats, affixes, automation rules, mastery, quests, discoveries, and Chronicle.

Secondary explanation belongs behind an intentional detail surface. Player-facing copy uses Attack, Health, Armor, Speed, Critical Chance, Fire, Ice, Equipment, Skills, and Upgrades. Internal evaluator reasoning and diagnostic text stay out of normal play.

## Destinations

| Destination | Primary job | Must remain factual |
| --- | --- | --- |
| Battle | Watch the current encounter and act on queued skills | Damage, critical state, healing, status, defeat, victory, wave, boss phase, and rewards come from events/state |
| Build | Make equipment, skill-loadout, and automation decisions | Affixes, rarity, equipped state, conditions, order, cooldowns, and unlocks reflect projections |
| Adventure | Choose the next authored route node | Stage lock, encounter type, Push/Farm, requirements, expected rewards, and only canonically authored guarantees |
| Growth | See level, XP, core upgrades, mastery, milestones, Legacy/Chronicle | Next goal carries its real target and remaining requirement |

Supporting surfaces cover offline return, save/recovery failure, settings, empty/locked/unavailable states, destructive confirmations, and background/resume.

## Battle composition

- Full-bleed authored environment with a readable slate overlay.
- Compact encounter header: region, stage, wave, boss phase when present.
- Hero and enemy silhouettes are the visual anchor, with health bars and status indicators attached to the actor.
- The threat strip names the enemy and attack intent when the projection provides it. It does not predict unsupported outcomes.
- The skill bar is always reachable on supported compact layouts. Queue state and readiness are visually distinct; tapping queues the existing command rather than simulating an instant cast.
- Ordinary event feedback is light and non-blocking. Loot, first clears, boss phase changes, and defeat use stronger but concise emphasis.

No blocked-damage label is shown unless the canonical runtime supplies a blocking event. Armor mitigation remains a stat/result detail, not a fabricated combat event.

## Build composition

- Hero-and-slot composition first; inventory second.
- Each item comparison shows the affected stat deltas and meaningful affixes. Rarity is a category, not a verdict that an item is better.
- Equipment swap, lock, salvage, overflow, filters, and capacity state remain available when the source supports them; destructive actions require confirmation.
- Skills show role, effect, cooldown, active loadout, ordering, unlock condition, and evolution choice from projections.
- Auto Battle presents existing presets before advanced rule editing. Rules read as conditions and actions, for example: `Health below 40% → Use Guard Mend`.

## Adventure and Growth composition

Adventure uses a route that adapts to available width. Nodes retain at least 48 dp touch targets; smaller screens use fewer columns or horizontal continuation rather than shrinking nodes.

Growth leads with level/XP, core stats, the next actual milestone, and the most relevant remaining requirement. Mastery, quests, achievements, discoveries, Echo, and Chronicle remain accessible without competing with the overview.

## Visual tokens

- Canvas: charcoal / blue-black.
- Surfaces: slate with restrained elevation and texture from existing artwork.
- Text: warm ivory for primary information; muted cool-gray for secondary explanation.
- Highlight: antique gold for rewards, milestones, and selected progression—not every border.
- Semantic accents: ember/fire, frost/ice, arcane, guard, vitality, shadow, and danger colors remain distinct from rarity colors.
- Spacing: 4 / 8 / 12 / 16 / 24 dp rhythm.
- Interactive controls: 48 dp minimum; visible selected, disabled, locked, loading, empty, error, and long-text states.
- Typography: readable body text and restrained decorative display headings; no critical text baked into artwork.

## Acceptance gates

- A fresh save can reach Skills/Build from Battle without a dead route.
- Battle → actual reward → inspect/equip or existing Gold upgrade → authoritative stat change → Battle works without mock values.
- Back closes the topmost detail/dialog first and preserves the actual origin.
- Push/Farm, Auto Battle, equipment capacity/salvage, offline return, save recovery, and Chronicle confirmations remain available where unlocked.
- Rapid taps are rejected or idempotent; rewards do not duplicate.
- Compact screens, large numbers, long names, enlarged text, reduced motion, haptics, and contrast remain usable.
- Compilation, unit-test execution, installation, and device playtest are reported separately; static inspection is never presented as a runtime result.
