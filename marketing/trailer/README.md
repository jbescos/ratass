# Steam Trailer

The raw recordings, extracted stills, intermediate contact sheets, and rendered
trailer are local production artifacts and are intentionally excluded from Git.
Keep the final video in Steamworks, YouTube, or release storage.

The checked-in filter and build script document how to rebuild the trailer when
the current GT3 and Halloween recordings are available under `raw/current/`:

```bash
marketing/trailer/build_trailer.sh
```

The final poster remains in `output/rogue-circuit-trailer-poster.png` because it
is small and useful as a checked-in release asset.
