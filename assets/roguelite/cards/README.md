# Card Artwork Atlas

Each theme provides a 6 by 23 artwork atlas at
`assets/theme/<theme>/roguelite/cards/card_art_atlas_v3.png`. Cells are addressed
in row-major order by `RogueliteCardDefinition.artworkIndex`.

| Row | Column 1 | Column 2 | Column 3 | Column 4 | Column 5 | Column 6 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Club Tune | Corner Focus | Draft Focus | Nitro Pulse | Grip Fan | Sport Tune |
| 2 | Straight Focus | Rally Focus | Time Ripple | Draft Magnet | Race Tune | Drift Focus |
| 3 | Draft Expert | Phase Shield | Rocket Exhaust | Heavyweight Tune | Apex Focus | Corner Expert |
| 4 | Gravity Well | Quantum Duo | Championship Tune | Straight Expert | Drift Expert | Hyperdrive |
| 5 | Crown Engine | Streamline Kit | Short-Ratio Gearbox | Le Mans Body | Drift Differential | Ground Effect |
| 6 | Velocity Shell | Torque Vectoring | Recovery Beacon | Draft Vendetta | Payback Shield | Repulsor Surge |
| 7 | Tar Tether | EMP Snare | Void Anchor | Ghost Cloak | Phantom Cloak | Void Cloak |
| 8 | Quantum Trio | Quantum Quartet | Underdog Instinct | Comeback Drive | Last Place Fury | Close Quarters |
| 9 | Pack Racer | Traffic Dominance | Carbon Panels | Carbon Monocoque | Graphene Chassis | Blind Hex |
| 10 | Burden Hex | Doom Hex | Lucky Spark | Chaos Relay | Wildcard Core | Loaded Grudge |
| 11 | Chaos Retort | Fate's Revenge | Featherweight Drive | Track Wing | Grounded Aero | Light Compound |
| 12 | Agile Chassis | Streamlined Chassis | Aero Featherweight | Titanium Drive | Downforce Package | Grounded Downforce |
| 13 | Magnesium Suspension | Aero-Agile Chassis | Carbon Longtail | Venturi Monocoque | Titanium Skeleton | Hypercar Core |
| 14 | Active Aero Shell | Carbon Prototype | Track Vacuum | Wing Car | Feather Ground | Triad Coup |
| 15 | Rally Expert | Sprint Focus | Corner Master | Draft Master | Straight Master | Drift Master |
| 16 | Rally Master | Slide Focus | Repulsor Wave | Hunter Barrage | Grudge Spark | Vengeance Core |
| 17 | Nemesis Engine | Apex Expert | Sprint Expert | Slide Expert | Apex Master | Sprint Master |
| 18 | Slide Master | Hunter Storm | Ace Hotline | Priority Hotline | Chrono Shift | Temporal Dominion |
| 19 | Traction Focus | Traction Expert | Traction Master | Agility Focus | Agility Expert | Agility Master |
| 20 | Technique Coupler | Technique Matrix | Technique Singularity | Powerup Link | Powerup Matrix | Powerup Nexus |
| 21 | Bulk Field | Titan Field | Colossus Field | Tune Link | Dual Link | Grid Link |
| 22 | Apex Key | Lap Dividend | Lap Booster | Lap Doubler | Telemetry Theft | Build Heist |
| 23 | Apex Plunder | Final Reckoning | Reserved | Reserved | Reserved | Reserved |

Every non-driver card has a unique artwork cell. Artwork must depict the card's
actual mechanic in that theme; do not reuse another theme's cell with a tint or
decorative overlay.

Keep every cell square and free of text or logos.

Driver portraits are theme assets at
`assets/theme/<theme>/drivers/driver_art_atlas.png`. Each sheet is a 5 by 2
atlas mapped in row-major order from `profile00` through `profile09`.

`ability_effect_atlas.png` is the active 23 by 1 alpha atlas for the centered
powerup and revenge effects. Cells map to Nitro T1, Grip T1, Ram, Draft, Shield,
Mirror, Cloak, Grudge Spark, Vengeance Core, Nemesis Engine, Nitro T2, Nitro T3,
Grip T2, Grip T3, Time T1, Time T2, Time T3, Ace Hotline, Priority Hotline,
Antenna T1, Antenna T2, Antenna T3, and the Apex Key Tier 4 unlock.
Nitro is anchored behind the exhaust. Every other under-car sprite reserves a
transparent center and carries its readable symbols around the car as an aura.
The renderer tints the generated artwork by card type and keeps projectiles,
physical mirror cars, and cloak transparency on their dedicated presentation
paths. All effect sprites are loaded and updated only by the presentation path,
so they do not affect RL training.

`card_shell_atlas_v2.png` is the 5 by 2 presentation atlas. Columns are
Driver, Tuning, Technique, Powerup, and Revenge. The first row contains filled-card
shells; the second row contains the corresponding empty-slot artwork. Every
cell reserves the same header badge sockets, a 196 by 196 square artwork socket,
information panel, and footer tab so the renderer can keep text and images inside
safe areas. The square socket matches the cells in each themed
`card_art_atlas_v3.png` without cropping or stretching them.

`card_type_icon_atlas.png` is the active 6 by 1 icon atlas containing Driver,
Tuning, Technique, Powerup, Revenge, and Warning in that order. The renderer
uses the category icons in each card's top-left square and next to cars while
the corresponding card type is active or armed. Technique and Revenge use a
solid category-colored interior so their small in-race indicators remain
legible over the road. Individual transparent 256 by 256 sources are stored
under `icons/`.

`card_tier_icon_atlas.png` is the active 3 by 1 rank atlas containing the bronze
`T1`, silver `T2`, and gold `T3` badges. These replace tier text in the card's
top-right square. Individual tier icons are also stored under `icons/`.

Regenerate the shell and icon atlas from their source artwork with:

```bash
javac -d /tmp tools/BuildRogueliteCardVisuals.java
java -Djava.awt.headless=true -cp /tmp BuildRogueliteCardVisuals \
  tools/art_sources/card_shell_source_v3.png \
  assets/roguelite/cards/icons \
  assets/roguelite/cards/card_shell_atlas_v2.png \
  assets/roguelite/cards/card_type_icon_atlas.png \
  assets/roguelite/cards/card_tier_icon_atlas.png
```

These assets are loaded only by the rendered game UI and are not used by the RL
simulation path.

Regenerate the ability-effect alpha atlas from its generated chroma-key source
with:

```bash
javac -d /tmp/ratass-visual-tools \
  tools/BuildAbilityEffectAtlas.java \
  tools/BuildOrbitAura.java \
  tools/ReplaceImageAtlasCells.java
java -Djava.awt.headless=true -cp /tmp/ratass-visual-tools \
  BuildAbilityEffectAtlas \
  tools/art_sources/ability_effect_source.png \
  assets/roguelite/cards/ability_effect_atlas.png \
  15 344 1629 261 7 \
  --replace-column 1 tools/art_sources/ability_effects/grip_t1.png \
  tools/art_sources/ability_effects/revenge_boost_t1.png \
  tools/art_sources/ability_effects/revenge_boost_t2.png \
  tools/art_sources/ability_effects/revenge_boost_t3.png \
  tools/art_sources/ability_effects/nitro_t2.png \
  tools/art_sources/ability_effects/nitro_t3.png \
  tools/art_sources/ability_effects/grip_t2.png \
  tools/art_sources/ability_effects/grip_t3.png \
  tools/art_sources/ability_effects/time_t1.png \
  tools/art_sources/ability_effects/time_t2.png \
  tools/art_sources/ability_effects/time_t3.png \
  tools/art_sources/ability_effects/hotline_aura_t1.png \
  tools/art_sources/ability_effects/hotline_aura_t2.png \
  tools/art_sources/ability_effects/antenna_t1.png \
  tools/art_sources/ability_effects/antenna_t2.png \
  tools/art_sources/ability_effects/antenna_t3.png \
  tools/art_sources/ability_effects/tier_four_unlock.png \
  --hollow-column 1 --hollow-column 2 --hollow-column 3 \
  --hollow-column 4 --hollow-column 7 --hollow-column 8 \
  --hollow-column 9 --hollow-column 12 --hollow-column 13 \
  --hollow-column 14 --hollow-column 15 --hollow-column 16 \
  --hollow-column 17 --hollow-column 18 --hollow-column 19 \
  --hollow-column 20 --hollow-column 21 --hollow-column 22
```

`BuildOrbitAura` places a family icon around the car before the center mask is
applied. Replace themed card artwork without rebuilding unrelated cells with:

```bash
java -Djava.awt.headless=true -cp /tmp/ratass-visual-tools \
  ReplaceImageAtlasCells \
  assets/theme/gt3/roguelite/cards/card_art_atlas_v3.png \
  assets/theme/gt3/roguelite/cards/card_art_atlas_v3.png \
  6 250 126 tools/art_sources/card_art/gt3/apex_key.png
java -Djava.awt.headless=true -cp /tmp/ratass-visual-tools \
  ReplaceImageAtlasCells \
  assets/theme/halloween/roguelite/cards/card_art_atlas_v3.png \
  assets/theme/halloween/roguelite/cards/card_art_atlas_v3.png \
  6 250 126 tools/art_sources/card_art/halloween/apex_key.png
```
