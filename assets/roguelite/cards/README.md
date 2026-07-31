# Card Artwork Atlas

`card_art_atlas.png` is a 5 by 5 atlas. Cells are addressed in row-major order
by `RogueliteCardDefinition.artworkIndex`.

| Row | Column 1 | Column 2 | Column 3 | Column 4 | Column 5 |
| --- | --- | --- | --- | --- | --- |
| 1 | Club Tune | Corner Exit | Draft Hunter | Nitro Pulse | Grip Fan |
| 2 | Sport Tune | Clean Momentum | Recovery Launch | Ram Reactor | Draft Magnet |
| 3 | Race Tune | Drift Slingshot | Slipstream Slingshot | Phase Shield | Rocket Exhaust |
| 4 | Heavyweight Tune | Overtake Surge | Apex Slingshot | Gravity Well | Overdrive Coil |
| 5 | Championship Tune | Perfect Lap | Racecraft Mastery | Hyperdrive | Crown Engine |

Keep every cell square and free of text or logos.

`driver_art_atlas.png` is a 5 by 2 atlas of driver portraits. Cells map in
row-major order from `profile00` through `profile09`.

`card_shell_atlas_v2.png` is the active 4 by 2 presentation atlas. Columns are
Driver, Tuning, Technique, and Gadget. The first row contains filled-card
shells; the second row contains the corresponding empty-slot artwork. Every
cell reserves the same header badge sockets, artwork window, information panel,
and footer tab so the renderer can keep text and images inside safe areas. The
legacy `card_shell_atlas.png` is retained as the original design reference.
These assets are loaded only by the rendered game UI and are not used by the RL
simulation path.
