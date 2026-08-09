# Steam Store Media

Generate the Steam capsule and library image set from the checked-in game menu
art and artwork-only library hero source:

```bash
python3 marketing/steam/build_store_assets.py
```

The generator requires Pillow. It validates every output dimension and writes
the final files under `store-assets/`.

Gameplay footage is intentionally kept outside Git. To rebuild the screenshots
from locally retained footage under `marketing/trailer/raw/`, also install
`ffmpeg` and run:

```bash
python3 marketing/steam/build_store_assets.py --screenshots
```

## Steamworks Upload Map

| Steamworks field | File |
| --- | --- |
| Header capsule | `store-assets/capsule_header.png` |
| Small capsule | `store-assets/capsule_small.png` |
| Main capsule | `store-assets/capsule_main.png` |
| Vertical capsule | `store-assets/capsule_vertical.png` |
| Library capsule | `store-assets/library_capsule.png` |
| Library hero | `store-assets/library_hero.png` |
| Library logo | `store-assets/library_logo.png` |
| Library header | `store-assets/library_header.png` |
| Screenshots | `store-assets/screenshots/*.png` |
| Shortcut icon | `../../assets/branding/steam-shortcut-icon.png` |
| Community app icon | `../../assets/branding/steam-app-icon.jpg` |
| Gameplay trailer | `../trailer/output/rogue-circuit-steam-gameplay-trailer.mp4` |

`source/library-hero-art.png` was generated specifically for the Steam library
hero. It intentionally contains no title, UI, writing, or logos.
