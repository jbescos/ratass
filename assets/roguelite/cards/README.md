# Card Artwork Atlas

`card_art_atlas_v3.png` is the active 6 by 11 atlas. Cells are addressed in row-major order
by `RogueliteCardDefinition.artworkIndex`.

The legacy `card_art_atlas.png` and `card_art_atlas_v2.png` files are retained as
artwork references.

| Row | Column 1 | Column 2 | Column 3 | Column 4 | Column 5 | Column 6 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Club Tune | Corner Exit | Draft Hunter | Nitro Pulse | Grip Fan | Sport Tune |
| 2 | Clean Momentum | Recovery Launch | Ram Reactor | Draft Magnet | Race Tune | Drift Slingshot |
| 3 | Slipstream Slingshot | Phase Shield | Rocket Exhaust | Heavyweight Tune | Overtake Surge | Apex Slingshot |
| 4 | Gravity Well | Mirror Duo | Championship Tune | Perfect Lap | Racecraft Mastery | Hyperdrive |
| 5 | Crown Engine | Streamline Kit | Short-Ratio Gearbox | Le Mans Body | Drift Differential | Ground Effect |
| 6 | Velocity Shell | Torque Vectoring | Recovery Beacon | Draft Vendetta | Payback Shield | Repulsor Surge |
| 7 | Tar Tether | EMP Snare | Void Anchor | Ghost Cloak | Phantom Cloak | Void Cloak |
| 8 | Mirror Trio | Mirror Quartet | Underdog Instinct | Comeback Drive | Last Place Fury | Close Quarters |
| 9 | Pack Racer | Traffic Dominance | Carbon Panels | Carbon Monocoque | Graphene Chassis | Blind Hex |
| 10 | Burden Hex | Doom Hex | Lucky Spark | Chaos Relay | Wildcard Core | Loaded Grudge |
| 11 | Chaos Retort | Fate's Revenge | Unused | Unused | Unused | Unused |

Every non-driver card has a unique artwork cell.

Keep every cell square and free of text or logos.

Driver portraits are theme assets at
`assets/theme/<theme>/drivers/driver_art_atlas.png`. Each sheet is a 5 by 2
atlas mapped in row-major order from `profile00` through `profile09`.

`ability_effect_atlas.png` is the active 7 by 1 alpha atlas for the centered
powerup and revenge effects. Cells map to Nitro, Grip, Ram, Draft, Shield,
Mirror, and Cloak. The renderer tints the generated artwork by card type and
keeps projectiles, physical mirror cars, and cloak transparency on their
dedicated presentation paths.

`card_shell_atlas_v2.png` is the active 5 by 2 presentation atlas. Columns are
Driver, Tuning, Technique, Powerup, and Revenge. The first row contains filled-card
shells; the second row contains the corresponding empty-slot artwork. Every
cell reserves the same header badge sockets, a 196 by 196 square artwork socket,
information panel, and footer tab so the renderer can keep text and images inside
safe areas. The square socket matches the cells in `card_art_atlas_v3.png` without
cropping or stretching them. The legacy `card_shell_atlas.png` is retained as the
original design reference.

`card_type_icon_atlas.png` is the active 5 by 1 category icon atlas in the same
Driver, Tuning, Technique, Powerup, and Revenge order. The renderer uses these
shared icons in each card's top-left square and next to cars while the
corresponding card type is active or armed. Individual transparent 256 by 256
sources are stored under `icons/`.

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
javac -d /tmp/ratass-visual-tools tools/BuildAbilityEffectAtlas.java
java -Djava.awt.headless=true -cp /tmp/ratass-visual-tools \
  BuildAbilityEffectAtlas \
  tools/art_sources/ability_effect_source.png \
  assets/roguelite/cards/ability_effect_atlas.png \
  15 344 1629 261 7
```
