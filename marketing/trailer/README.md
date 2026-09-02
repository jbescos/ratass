# Steam Trailer

The raw recordings, extracted stills, intermediate contact sheets, and rendered
trailer are local production artifacts and are intentionally excluded from Git.
Keep the final video in Steamworks, YouTube, or release storage.

The checked-in scripts generate the card slates and rebuild the trailer when the
current GT3, Halloween, and card-screen captures are available under
`raw/current/`:

```bash
marketing/trailer/build_trailer.sh
```

Required local inputs:

- `raw/current/gt3-current.mp4`
- `raw/current/halloween-current.mp4`
- `raw/current/hunter-storm-current.mkv`
- `raw/current/card-types-current.png`

The rendered 1920x1080 screenshots are written to `output/screenshots/` by
`capture_screenshots.sh`.

The final poster remains in `output/rogue-circuit-trailer-poster.png` because it
is small and useful as a checked-in release asset.
