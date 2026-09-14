# Game UI visual system

## Verdict

The project should use a game-first presentation layer over the existing deterministic runtime:

- Keep the dark blue-black game canvas and use violet, teal, gold, green, and red as semantic accents.
- Reserve decorative artwork and ornate frames for focal surfaces such as the battle theater, world banner, and primary status panels.
- Use outlined, lightly elevated cards for supporting information. One card should represent one readable topic.
- Use a consistent 4/8/12/16/24 dp spacing rhythm, with 16 dp as the default content inset and 48 dp as the minimum interactive target.
- Use a single horizontally scrollable secondary destination row instead of compressing eight tabs into multiple dense rows.
- Preserve stable backend IDs and save data; player-facing labels are presentation strings only.

## Source-backed findings

### Cards

Material 3 recommends cards for content and actions that belong to a single topic and should be easy to scan. This supports using one shared `GameCard` for secondary information and a framed `PremiumPanel` only for focal content.

Sources: [Material 3 card guidelines](https://m3.material.io/components/cards/guidelines), [Material 3 card overview](https://m3.material.io/components/cards/overview)

### Spacing and density

Material 3 describes spacing as a tool for grouping content, directing attention, and controlling product personality. The implementation therefore uses deliberate 8 dp gaps and 16 dp panel insets instead of letting every screen choose unrelated values.

Source: [Material 3 grids and spacing](https://m3.material.io/foundations/layout/grids-spacing/spacing)

### Touch targets

Android Compose accessibility guidance recommends a minimum 48 dp size for interactive elements. Buttons, chips, navigation items, and custom action surfaces retain or exceed that target.

Source: [Compose accessibility API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)

### Navigation and adaptive layout

Material 3 positions navigation bars for switching between views on handheld screens. Android's adaptive-layout guidance recommends making layout decisions from window size classes and changing the number of content panes instead of only stretching elements. The current compact phone layout remains a bottom navigation layout, while the spacing and card system are kept adaptable for later larger-window work.

Sources: [Material 3 navigation bar](https://m3.material.io/components/navigation-bar), [Android adaptive Compose apps](https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps)

## Player-facing terminology

The runtime keeps its stable IDs and domain names, but the UI uses short, familiar RPG language. Stat labels favor the compact forms players expect in a combat HUD:

| Runtime concept | Player-facing label |
| --- | --- |
| Might | ATK |
| Tempo | SPD |
| Ember | FIRE |
| Frost | ICE |
| Arcane | MAGIC |
| Vitality | HP |
| Shadow | DARK |
| Guard | DEF |
| Armor value | ARMOR |
| Critical chance | CRIT |
| Critical multiplier | CRIT DMG |
| Skill power | SKILL PWR |
| Healing power | HEAL |

System vocabulary is also normalized across every player-facing surface:

| Internal/system term | Player-facing label |
| --- | --- |
| Doctrine | Auto Battle |
| Resonance sequence / charge | Combo / charge |
| Convergence | Combo effect |
| Adaptation | Enemy Resistance |
| Mutation | Enemy Modifier |
| Echo | Legacy |
| Chronicle / collapse | Prestige |
| Overflow | Stash |
| Expedition | Adventure |
| Discoveries | Codex |

Only the presentation layer changes. Save data, command names, stable content IDs, and simulation logic remain intact.
