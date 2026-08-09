# Steam Release Checklist

This checklist covers the repository and the manual Steamworks work needed to
release Rogue Circuit. Steamworks portal state cannot be verified from this
repository.

## Ready In The Repository

- Linux x86_64 app image with a bundled Java runtime.
- Platform-native desktop icons: PNG for Linux, ICO for Windows, and ICNS for
  macOS.
- Steam shortcut icon (`assets/branding/steam-shortcut-icon.png`, 512x512) and
  app icon (`assets/branding/steam-app-icon.jpg`, 184x184).
- Steam depot upload script (`tools/release/publish-steam.sh`).
- Native Windows package, verification, and Steam upload scripts under
  `tools/release/*windows*`.
- Checked-in trailer build recipe and poster under `marketing/trailer`; the
  rendered 1920x1080, 60 fps H.264/AAC video is retained locally for upload and
  intentionally excluded from Git.
- Complete Steam capsule/library artwork and six 1920x1080 gameplay screenshots
  under `marketing/steam/store-assets`.
- Paste-ready store copy, tags, languages, feature declarations, and Windows and
  Linux system requirements in `marketing/steam/STORE_PAGE.md`.
- Android launcher icons and a locally installable debug APK.
- iOS app icons, launch-screen branding, and a privacy manifest.

## Store Assets Ready

- Header, small, main, and vertical capsules at their required dimensions.
- Library capsule, artwork-only hero, transparent logo, and library header.
- Six 1920x1080 screenshots extracted from actual game footage.
- Steam shortcut icon and community app icon.

## Steamworks Configuration To Verify

- App ID, platform depot IDs, and install directory.
- Linux launch option: `bin/RogueCircuit`.
- Windows launch option: `RogueCircuit.exe`.
- Store description, tags, languages, pricing, release date, system
  requirements, content survey, and supported features.
- Verify ownership, licenses, and required attribution for every font, image,
  music track, sound effect, and bundled library.
- Advertise Windows and Linux only after each depot passes Steam-client
  validation on that operating system. Advertise macOS only after native
  packaging and testing.
- Upload the store trailer and graphical assets.
- Complete both the store-page and build review checklists. Allow at least one
  week for review and corrections.

## Build Validation Before Release

- Upload to a private Steam branch and install it through the Steam client.
- Test launch, new game, save/continue, quit, update, uninstall/reinstall, audio,
  fullscreen/windowed modes, and common 720p/1080p resolutions.
- Test on a clean supported Linux system and SteamOS/Steam Deck if claiming
  support.
- Verify save-file locations before enabling Steam Cloud.
- Re-run the full automated test suite for the exact uploaded build.

## Optional Steam Features

Steam achievements, Cloud saves, overlay/API integration, leaderboards,
workshop, and Steam Deck verification are optional and are not implemented by
the current repository. Add only features that can be tested before they are
advertised on the store page.

## Official References

- [Steam graphical asset requirements](https://partner.steamgames.com/doc/store/assets)
- [Steam trailer requirements](https://partner.steamgames.com/doc/store/trailer)
- [Steam review process](https://partner.steamgames.com/doc/store/review_process)
- [Steam release process](https://partner.steamgames.com/doc/store/releasing)
- [Uploading builds to Steam](https://partner.steamgames.com/doc/sdk/uploading)
