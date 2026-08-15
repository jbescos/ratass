# Road Map Gameplay Assets

This directory contains only the shared gameplay representation of each map.
Rendered road artwork is theme-specific and lives under
`assets/theme/<theme>/maps/`.

- `*_mask.png`: road mask, race checkpoints, and start-grid anchors
- `*.mapcache`: gzip-compressed generated gameplay metadata for the parsed mask

The rendered `mapNNN.png` files must not be placed here. Each theme supplies
its own presentation image while every theme continues to use the same mask
and metadata.

For GT3, the presentation theme is "roads of the world": each map uses scenery
from a distinct real-world region while preserving the shared route exactly.
Generated artwork must be a road-free background plate. Rebuild the visible
road from the authoritative mask with the offline compositor:

```bash
javac -d /tmp tools/AlignRenderedMapToMask.java
java -cp /tmp AlignRenderedMapToMask \
  /path/to/road-free-background.png \
  assets/maps/mapNNN_mask.png \
  assets/theme/gt3/maps/mapNNN.png
```

Pass `halloween` as a final argument when compositing a Halloween map. This
keeps the themed cyan edge and bronze shoulder while using the exact same
playable road geometry.

This process keeps road width, boundaries, and the start/finish line aligned
with gameplay. Do not ask an image generator to draw the playable road.

The `.mapcache` file is a compressed generated cache. It stores only gameplay data
derived from the mask, not the decorated map image. If it is missing or stale,
the loader rebuilds it from the matching `*_mask.png` and writes a fresh sidecar cache
when the assets directory is writable. Keep the `.mapcache` next to the map pair
before packaging the game so normal startup does not have to parse the mask
image and rebuild mask distance fields.

The loader discovers every `*_mask.png` file in this directory and sorts them by
filename, so adding a new map should only require adding a new mask image with
the marker colors below.

The mask controls the map's gameplay size and aspect ratio. Use wide roads and
ordered checkpoint gates so race training has a clear route to learn.

Mask colors:

- white or near-white: race road
- black or near-black: off-road/outside-road space
- red dots: the two side-by-side anchors for the first start-grid row
- green lines: ordered race checkpoint gates, perpendicular to the road; the
  start/finish line also defines the grid's forward direction

Race-mode car spawns are separate mask markers. The loader expands the two red
anchors behind the start/finish line into 20 positions on two parallel
F1-style grid columns.

Regenerate the current road masks:

```bash
python3 tools/generate_f1_circuit_masks.py
```

`map018` is a dedicated high-speed training route with long straights,
hairpins in both directions, chicanes, and broad return corners. Regenerate its
mask and matching presentation image with:

```bash
python3 tools/generate_map018.py
```

The generated masks are deliberately simple and high-contrast so training can
start on road-following and checkpoint completion before decorated road art is
added.
