# Contract-driven UI redesign

Authority: `idle-rpg-source-of-truth.md`. This companion defines presentation decisions without changing the gameplay contract.

## Screen hierarchy

- Battle: compact account HUD, stage and Auto Battle controls, dominant battlefield, then scrollable combat readout and skills. At enlarged font sizes or very short heights, allow the complete encounter surface to scroll so controls cannot disappear below a fixed header.
- Build: equipment silhouette and inventory remain visible; selecting an item opens an inspection dialog. Equip, lock, salvage, enhancement and refinement stay attached to the selected item. Close returns to the same inventory position. Refinement explicitly previews the selected line, material cost, legal range and possibility of a worse roll before spending.
- Skills: loadout slots lead; selected skill details and mutually exclusive evolution choices remain explicit. Technical details stay expandable.
- Adventure: Push/Farm is a player choice, with the selected encounter and its requirements visible. Existing authored route and mechanics are retained.
- Growth: level and XP first, then an optional Power Score calculation and combat-stat inspection. Rebirth is a separate decision card. Permanent allocation expands on request, with Normal combat growth and Legacy acquisition in separate views. Gem respec requires confirmation.

## Visual language

Use the existing dark-fantasy environment and character art. Secondary information uses quiet slate surfaces; reserve gold for selected progression and rewards. Do not stretch ornate frame images across every card. Use readable body copy, restrained monospace metrics, consistent 12/16/24 dp spacing and at least 48 dp interactive controls. Allow text wrapping instead of clipping large values or confirmation labels.

## Gameplay corrections required by audit

- Deep Rebirth retains the ordinary Normal/Legacy grant and adds the existing material reward; it does not award extra Normal points.
- Legacy allocations fund bounded Legendary Find, not a parallel combat tree. Refund old incompatible allocations into their original pool during an explicit save migration; preserve earned totals and valid investments.
- Rebirth readiness includes the safe combat state.
- Persist accepted commands before publishing their result. Persist committed loot transitions before showing their result. A failed checkpoint leaves the live state unchanged.
- Show refinement range/cost through projections. Never calculate rolls or advance RNG in Compose.
- Separate heuristic Power Score defense points from physical effective HP; label assumptions.

## Evidence

Record compilation, unit tests, device launch and screenshot inspection separately. A passing build alone cannot establish visual quality or complete gameplay alignment.
