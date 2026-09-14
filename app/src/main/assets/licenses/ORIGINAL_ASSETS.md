# Idle RPG Project-Original Visual Assets — FUI-11

## Skill illustration atlas — September 6, 2026

`drawable-nodpi/skill_atlas.webp` is original AI-generated artwork made for this
project: twelve dark crystalline skill illustrations, arranged in four columns
and three rows. The source is 1448 × 1086 pixels; WebP encoding quality 88.
The Android renderer shares one decoded atlas and samples authored tile regions.
No text, gameplay values, third-party artwork or external font is embedded.
The original generation PNG is not packaged in the APK. Existing semantic vectors
remain available for nonillustrated presentation surfaces.

All drawable resources introduced by FUI-11 are project-original vector geometry authored for
this Idle RPG frontend. They were not copied or traced from a third-party game, icon pack, or
font. They are presentation assets only; backend `ContentId` / `InstanceId` remain canonical
identities.

Style batch: **Obsidian Resonance Vector Set 01**

Design constraints:

- 24×24 monochrome semantic icons designed to accept Material/affinity tint.
- 320×180 multi-color hero illustrations for Training Hollow, Chronicle, and Adaptation
  Forecast; 160×120 internal viewport for the Slime illustration.
- Strong silhouette and low micro-detail so icons remain readable at phone scale.
- Affinity and rarity meanings never depend on color alone; each has distinct geometry.
- No embedded text inside artwork.

Introduced asset groups:

- system accessibility icon;
- world / Training Hollow / encounter art;
- six skill icons and two status icons;
- Slime, Forged Flame, Ash Skin, upgrade, item, affix, quest, achievement, Chronicle, Echo,
  discovery and objective icons;
- eight affinity glyphs;
- five rarity marks;
- Training Hollow, Slime, Chronicle and Adaptation Forecast illustration vectors.

The existing FUI navigation vectors remain project-local and unchanged in this phase.

---

## Training Hollow Combat Expansion + Premium UI Asset Set 01

The raster resources introduced by the Training Hollow combat-variety and premium-UI change are
project-original presentation assets created specifically for this Idle RPG project. They are not
copied from a third-party game, marketplace pack, icon set, or commercial UI kit.

### Training Hollow combat art

- five enemy illustration masters: Hollow Slime, Riftfang, Cinder Wisp, Hollow Bulwark, Arcane Seer;
- five matching enemy portrait icons;
- five matching attack/impact FX assets;
- authored encounter-frame presentation for Normal, Elite, and Anomaly encounters.

### Premium UI modular assets

- Battle background;
- World background/banner;
- primary and secondary panel frames;
- enemy showcase frame;
- separate HP fill and HP track assets;
- separate XP fill and XP track assets;
- Saved, Threat, and Affinity badges;
- premium Battle, World, Doctrine, Gear, and Progress navigation icons.

Dynamic gameplay state is never baked into these static images. HP, XP, cooldowns, labels, numbers,
and timing values remain rendered from canonical presentation state at runtime. Progress fills are
separate from their tracks so Compose can size the fill from canonical progress units.

All assets contain no embedded gameplay text. Transparent-background assets preserve alpha where
required for compositing on Android. Decorative raster assets are stored in `drawable-nodpi` so
Android does not apply density rescaling to their authored pixel geometry.

---

## Obsidian Resonance Vector Set 02 — Buildcraft Skills

Project-original 24×24 vector geometry authored for Frost Lance, Arcane Pulse, Vital Surge, and Umbral Cut. The icons use distinct shard, orbit, organic-pulse, and negative-space blade silhouettes so affinity meaning is not color-only. No text, third-party geometry, or franchise material is embedded.

---

## Hollow Strategy Vector Set 03 — Resonance and Enemy Roles

Project-original Android vector geometry authored for Glacial Ward, Resonance Shift, Blood Eclipse,
Frostbound Mite, Echo Leech, Shade Mimic, and seven new Convergence glyphs. Shield, orbit,
parasite, crystal, negative-space, speed-line, and interlocking-bastion silhouettes provide shape
identification in addition to affinity color. No third-party geometry, text, or franchise material
is included.

---

## Obsidian Resonance Character Masters — 2026-08-31

Five project-original raster masters were generated specifically for this project
with OpenAI image generation and losslessly normalized to 768×768 WebP for Android:

- `hero_echo_bound.webp` — Echo-Bound Wanderer;
- `enemy_frostbound_mite.webp` — Frostbound Mite;
- `enemy_echo_leech.webp` — Echo Leech;
- `enemy_shade_mimic.webp` — Shade Mimic;
- `enemy_hollow_warden.webp` — Hollow Warden.

Prompt direction: premium dark crystalline fantasy, obsidian armor, luminous Resonance accents,
clean mobile silhouettes, no text, and no franchise-specific identity. Each subject was
generated independently, visually inspected, normalized to an obsidian matte to eliminate unsafe
transparent-pixel color bleed, padded without distortion, and stored under `drawable-nodpi`. No third-party assets were used.

---

## Training Hollow Sector Backgrounds — 2026-09-03

Three project-original 900×1600 portrait backgrounds were generated specifically for the Training
Hollow with OpenAI image generation, visually inspected, resized to the authored Android target,
and encoded as optimized WebP under `drawable-nodpi`:

- `bg_training_hollow_outer_fracture.webp` — exposed obsidian fissures with restrained Ember and Guard light;
- `bg_training_hollow_resonant_depths.webp` — Frost/Arcane crystal geometry in the deeper Hollow;
- `bg_training_hollow_warden_core.webp` — monumental fractured architecture surrounding the Warden core.

Each background also has a project-authored 600×180 center-cropped WebP derivative named
`banner_training_hollow_<sector>.webp` for the World route. These small variants prevent the World
screen from decoding three full portrait textures solely for compact banners.

Prompt direction: dark crystalline fantasy, luminous Resonance, open central combat staging,
dark overlay-safe margins, no characters, no text, no logos, and no franchise-specific identity.
No third-party assets were used.

---

## Training Hollow Arsenal Vector Set — 2026-09-04

Four project-original 24×24 Android vector silhouettes were authored for the first-region
equipment arsenal: Fracture Mail, Seer’s Helm, Riftstep Boots, and Echo Sigil. The armor fracture,
seer visor, paired rift boots, and nested Resonance sigil remain distinguishable by shape at phone
size and contain no embedded text, third-party geometry, or franchise material.

---

## Obsidian Resonance Character Masters — Production Alpha Pass — 2026-09-05

Ten project-original combat actors were generated or background-extracted with OpenAI image
generation for the production Battle Theater pass, then visually inspected, losslessly normalized
to 768×768 WebP, and stored under `drawable-nodpi`:

- `hero_echo_bound.webp` — Echo-Bound Wanderer;
- `enemy_hollow_slime.webp` — Hollow Slime;
- `enemy_riftfang.webp` — Riftfang;
- `enemy_cinder_wisp.webp` — Cinder Wisp;
- `enemy_hollow_bulwark.webp` — Hollow Bulwark;
- `enemy_arcane_seer.webp` — Arcane Seer;
- `enemy_frostbound_mite.webp` — Frostbound Mite;
- `enemy_echo_leech.webp` — Echo Leech;
- `enemy_shade_mimic.webp` — Shade Mimic;
- `enemy_hollow_warden.webp` — Hollow Warden.

Prompt direction: project-original premium dark crystalline fantasy; obsidian construction;
luminous Resonance cores; strong, distinct mobile silhouettes; no text, logos, watermarks, or
franchise-specific identity. The five legacy enemy subjects were regenerated specifically to
remove baked checkerboards, gray bands, white mattes, frame boxes, and floor planes while
preserving their gameplay identities. Every installed master uses transparent RGBA compositing,
was checked at its transparent corners, and was inspected together on the game’s near-black
battlefield color. No third-party assets were introduced.
