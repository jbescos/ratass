# Picture Maps

The game loads maps from this directory by pairing files named like this:

- `map000.png`: visible map art
- `map000_mask.png`: gameplay mask used for floor, voids, spawns, and facing directions
- `map000.ser`: compressed generated gameplay cache for the parsed mask

Add new maps as the next numbered pair (`map021.png` and `map021_mask.png`, then
`map022...`). No Java change is needed when the pair is present under
`assets/maps` and the game is rebuilt.

Keep the mask. The pretty image is only the texture; the mask is what makes the
map playable.

The `.ser` file is a compressed generated cache. It stores only gameplay data
derived from the mask, not the decorated map image. If it is missing or stale,
the loader rebuilds it from `map000_mask.png` and writes a fresh sidecar cache
when the assets directory is writable. Keep the `.ser` next to the map pair
before packaging the game so normal startup does not have to parse the mask
image and rebuild mask distance fields.

The mask controls the map's gameplay size and aspect ratio. The decorated image
is drawn centered over that mask while preserving its own aspect ratio. The
safest workflow is to export the decorated image and its mask at the same pixel
size. If they differ, keep the playable content centered in both images so the
aspect-fit drawing still lines up.

Mask colors:

- white or near-white: playable floor
- black or near-black: void, holes, or outside the arena
- red circles: spawn points
- blue markers: spawn facing direction, paired with the nearest red spawn
