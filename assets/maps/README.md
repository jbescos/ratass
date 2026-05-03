# Picture Maps

The game loads maps from this directory by pairing files named like this:

- `map000.png`: visible map art
- `map000_mask.png`: gameplay mask used for floor, voids, spawns, and facing directions

Add new maps as the next numbered pair (`map006.png` and `map006_mask.png`, then
`map007...`). No Java change is needed when the pair is present under
`assets/maps` and the game is rebuilt.

Keep the mask. The pretty image is only the texture; the mask is what makes the
map playable.

The mask controls the map's gameplay size and aspect ratio. The decorated image
is drawn centered over that mask while preserving its own aspect ratio. The
safest workflow is to export the decorated image and its mask at the same pixel
size. If they differ, keep the playable content centered in both images so the
aspect-fit drawing still lines up.

Mask colors:

- white or near-white: playable floor
- black or near-black: void, holes, or outside the arena
- red circles: spawn points
- blue arrows or triangles: spawn facing direction, paired with the nearest red spawn
