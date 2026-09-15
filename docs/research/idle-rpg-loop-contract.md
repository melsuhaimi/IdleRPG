# Idle-RPG loop contract

## Summary

IdleRPG should feel like a conventional single-hero idle RPG: the player chooses a route,
watches an authoritative battle resolve, returns to a clear reward and upgrade decision, and
can leave the game without losing the value of time away. Automation should remove repetition,
not remove the player's meaningful choices.

This note uses first-party game/store descriptions as directional evidence. It does not copy
names, art, content, or proprietary formulas.

## Verified source findings

### Melvor Idle

The official Steam description presents a broad skill-driven idle RPG with more than 20 skills,
individual skill mechanics that interact with one another, combat skills, monsters, dungeons,
bosses, offline progression, and an integrated bank/inventory system.

Source: [Melvor Idle — official Steam page](https://store.steampowered.com/app/1267910/Melvor_Idle/)

### Idle Slayer

The official Steam description presents active and offline progression together, multiple
dimensions with different enemies/resources/challenges, a skill tree, craftable and upgradable
equipment, bosses, minigames, achievements, and continued progression after the early game.

Source: [Idle Slayer — official Steam page](https://store.steampowered.com/app/1353300/Idle_Slayer/)

## Design decisions for IdleRPG

1. **The core loop stays visible.**
   Battle -> Gold/XP/gear -> compare/equip/salvage or upgrade -> choose Push/Farm -> repeat.

2. **Offline progress must be a real simulation, not a fake reward screen.**
   The return summary should report the same kinds of results the active simulation can produce:
   time simulated, encounters cleared, Gold, XP, gear, deaths, and the point where progress
   stopped.

3. **Stages are the long-term backbone.**
   The route needs authored milestones, ordinary repeatable farming, elite encounters, anomalies,
   and boss gates. Beyond authored milestones, deterministic stage generation must continue the
   grind without inventing a new currency or silently looping the last boss.

4. **Gear needs a reason to exist.**
   Drops must have readable slot, rarity, affixes, comparison deltas, equip/lock/salvage actions,
   and a safe stash path. Auto-salvage is a player-selected policy, never a hard-coded rarity
   assumption.

5. **Skills should create build decisions.**
   Unlocks, cooldowns, target rules, statuses, and evolutions should make skills meaningfully
   different. The game should expose familiar terms such as Attack, HP, Armor, Speed, Crit,
   Fire, Ice, Magic, and Defense.

6. **Automation should be understandable.**
   Auto Battle rules are ordered player-authored conditions and actions. Basic Attack remains a
   reliable fallback. The UI should explain what will happen without displaying internal evaluator
   or developer terminology.

7. **Prestige should be earned, not spammed.**
   A reset should follow a clear milestone and grant permanent upgrades that change the next run.
   The player must see what resets and what remains before confirming.

## Non-goals

- No party, gacha, companion, or pet systems.
- No random AI/LLM decision-making in gameplay.
- No UI-only combat formulas or fabricated rewards.
- No third-party artwork or fonts without a recorded license.
