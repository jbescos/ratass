# Race Circuit Masks

The current roguelite/race prototype uses mask-only maps. The game can still
load a decorated image beside a mask when one exists, but for now the mask is
also used as the visible surface.

- `*_mask.png`: road mask, race checkpoints, and start-grid anchors
- `*.json.gz`: compressed generated gameplay metadata for the parsed mask

The `.json.gz` file is a compressed generated cache. It stores only gameplay data
derived from the mask, not the decorated map image. If it is missing or stale,
the loader rebuilds it from the matching `*_mask.png` and writes a fresh sidecar cache
when the assets directory is writable. Keep the `.json.gz` next to the map pair
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

Regenerate the current circuit masks:

```bash
python3 tools/generate_f1_circuit_masks.py
```

`map018` is a dedicated high-speed training circuit with long straights,
hairpins in both directions, chicanes, and broad return corners. Regenerate its
mask and matching presentation image with:

```bash
python3 tools/generate_map018.py
```

The generated masks are deliberately simple and high-contrast so training can
start on road-following and checkpoint completion before decorated circuit art is
added.
