# Source-of-truth alignment review — 18 September 2026

Branch: `design/idle-rpg-ui-v1`.
Authority: `docs/design/idle-rpg-source-of-truth.md`, read from this branch, not from an older project ZIP.
Baseline: `9d5400a7ac4ad485af70d7afd0f3f0ba4910b5b2`.
Validated implementation: `e3388f144506ee80e644ef99cb6383758d161056`.

## Verdict

The starting branch was **not fully aligned**, despite the checked implementation list in the contract. This revision corrects verified mismatches and redesigns the decision surfaces around the canonical projections. It does not claim complete combat-contract compliance: the separate Defense interaction remains unspecified and unimplemented, and some authored equipment pools cannot supply the promised number of substats.

## Verified corrections

| Contract requirement | Baseline finding | Revision |
| --- | --- | --- |
| Deep Rebirth adds a modest reward, without extra normal points | Level 15,000 replaced both ordinary point grants with materials | Retains the normal per-Rebirth Normal/Legacy grants and adds the existing material reward; confirmation copy agrees |
| Rebirth is available from a safe state | Preview could say ready during combat while the command rejected it | Preview requires idle combat, matching command eligibility |
| Legacy is primarily acquisition/economy | Legacy funded the same direct combat tree as Normal | New Legacy investments fund bounded Legendary Find; Normal funds combat stats |
| Preserve earned permanent progression | Existing saves can contain now-incompatible investments | V10 → V11 refunds those investments into their original unspent pool, retaining earned totals and valid allocations |
| Commit resources, RNG and outcome atomically | Accepted commands and loot were published before a periodic checkpoint | Production runtime persists accepted commands and loot-producing advances before publishing state/events; failed checkpoint leaves the live state unchanged |
| Armor mitigates physical damage | Incoming elemental/arcane/shadow attacks also received Armor mitigation | Incoming Armor mitigation now checks damage kind, matching outgoing physical mitigation |
| Higher rarity improves roll quality | Rarity changed line count but every value used the same range | One centralized rarity floor is used by drops, refinement and previews; upper bounds and one value roll remain deterministic |
| Refinement previews cost and outcome | Refine spent immediately; no legal outcome range shown | Selected-line confirmation shows current roll, range, cost, and possibility of a worse result |
| Power Score exposes its complete calculation | Component explanations omitted coefficients; heuristic defense score was labeled effective health | Details include coefficients, total formula and overlap warning; physical EHP separately follows the Armor ratio with assumptions shown |
| Offline awards only Gold and XP | Quest copy said progress continued while away | Player-facing copy now states active-only quest progress and the 12-hour Gold/XP policy |
| Chronicle is distinct from Rebirth | Chronicle currency was labeled Legacy, colliding with Rebirth Legacy points | Chronicle/Echo Shards and Rebirth/Legacy points have distinct labels |

Rarity quality is a new central balance choice: the minimum rises by 10% of the authored range per rarity rank, from Common 0 to Legendary 4. Existing item rolls are preserved. This is a tunable coefficient, not a claim that the markdown supplied an exact value.

## UI/UX changes

- Build selection opens a bounded, scrollable item inspector over the inventory. Closing it preserves the inventory position. Item acquisition no longer automatically opens a detail view.
- Growth leads with level/XP and concise readouts. Power formulas, combat stats and permanent allocation are intentional disclosures. Normal and Legacy use separate allocation views. Gem respec has a confirmation.
- Battle keeps its dominant battlefield and lower readout/skill scroll on ordinary layouts. Short layouts and enlarged text can scroll the full encounter surface to keep controls reachable.
- Adventure respects the explicitly selected encounter for inspection instead of always replacing it with the active encounter.
- Skills clarify the permanent Basic Attack fallback and the relationship between equipped skills and Auto Battle.
- Shared cards are quieter, repeated stretched decorative frames are removed from secondary cards, small text is enlarged, the redundant HUD tagline is removed, and Growth navigation meets a 48 dp minimum.
- Existing environment/actor artwork is retained. This is an implemented layout and interaction redesign, not a generated-image mockup or a replacement art set.

## Reviewed existing foundations

| Area | Source evidence | Assessment |
| --- | --- | --- |
| Level curve | DefaultGameContent player progression; GameMath exact arithmetic; IncrementalProgressionContractTest | Central 800 soft transition and 15,000 cap exist; connected to revision validation |
| Rebirth reset/retain | RebirthSystem; RebirthScenarioTest | Separate from Chronicle; revised reward and readiness behavior |
| Skills | SkillProgressionSystem; SkillScalingSystem; SkillLoadoutScreen | Rank/mastery/evolution/refinement and detail UI exist; connected progression checks |
| Enhancement | GearEnhancementSystem; EnhancementLevel | +1–+15, PRI–PEN, bounded failstacks, material spend, downgrade/protection, separate refinement |
| Loot | LootSystem; AffixRollSystem; LootTableSystem | Deterministic selection, unique affix IDs, main-ID exclusion and Legacy cap exist; quality floor corrected. Some authored pools are too small for the promised count; see below |
| Offline | OfflineSessionCoordinator; OfflineReturnScenarioTest | 12-hour cap and latest eligible non-boss target; canonical simulation result projected back to permitted reward/state partitions |
| Save integrity | LocalGameRepository; SaveMigrationRegistry; GameRuntime/GameSession | Candidate verification and backup rotation exist; transaction publication boundary corrected |
| Automation | Doctrine command path and Battle UI | Player chooses build/route/policy; Basic Attack fallback and explicit Auto toggle retained |

## Remaining contract gaps

`BaseStats` has no separate Defense stat, and the combat pipeline has no Defense-driven Guard/block/stagger step. The contract names the requirement but supplies no interaction, threshold, scaling or rounding formula. Adding an invented second mitigation stat would violate the instruction not to assume. Armor remains clearly identified as physical mitigation; no fake Defense value is displayed.

The product owner must define that interaction before full combat alignment can be claimed. This revision neither deletes that requirement nor marks it implemented.

The authored equipment pools also need a content redesign: `TrainingHollowLootContent.allowedAffixIdsFor` gives Armor three candidates and Helm two. Reserving one main roll leaves fewer than three substats; `AffixRollSystem.roll` silently takes the smaller candidate count. Several differently named affixes also map to the same underlying Attack effect. The contract requires three/four lines without duplicating the main stat. This needs a legal stat pool with distinct mechanical roles, not extra renamed Attack lines. Existing rolled items are preserved; this revision does not claim the pool problem is solved.

## Validation

- Baseline GitHub run `35312130348`: compilation, configured JUnit tests and debug APK assembly passed.
- Local Gradle: could not start compilation because downloading Gradle 9.0.0 was blocked by network access. This is an environment failure, not a compiler result.
- Standards review: no confirmed blocking defect in the reviewed candidate. Added randomized checkpoint-failure/retry coverage in response to its validation finding.
- Contract review: identified the missing Defense interaction and missing Power Score coefficients; coefficients corrected, Defense gap retained explicitly.
- First candidate: APK assembly passed; 28/30 tests passed. Two stale test expectations were corrected after independent verification: exact level-14,999 XP and elemental Cinder Bolt bypassing physical Armor.
- Final run [35347702105](https://github.com/melsuhaimi/IdleRPG/actions/runs/35347702105), on the validated implementation above: compilation, all 30 configured JUnit tests, debug APK assembly and Android API 35 emulator smoke script passed. The script covers required navigation and enlarged-text navigation; optional interaction results are recorded in its artifact.
- The runner uploaded 57 screenshot/UI-tree/log/result files in `contract-ui-screens`. Downloading that artifact into the review environment returned HTTP 403 (error 1010), so screenshots, optional interaction outcomes and the crash buffer could not be independently inspected. Passing automation is not a claim of visual approval or exhaustive feature coverage.
- The original workflows remain `.bak`. The temporary evidence workflow is also renamed `.bak` after validation; no active workflow YAML remains on this branch. The final documentation/workflow-only commit does not change the tested application source.
