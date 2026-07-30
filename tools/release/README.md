# Release Builds

Release credentials and store identifiers stay outside the repository. The
scripts fail before packaging when required values are missing.

## Android

Install Android SDK Platform 36 and Build Tools 36.0.0. Maven remains the
top-level entry point and invokes the checked-in Android Gradle wrapper for AAR,
APK, AAB, and signing support. Keep the Play upload key separate from the
app-signing key managed by Google Play.

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
export ANDROID_KEYSTORE=/secure/path/upload.p12
export ANDROID_KEY_ALIAS=upload
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_PASSWORD=...
mvn -Pandroid,android-release package \
  -Dandroid.version.code=1 \
  -Dandroid.version.name=1.0
```

The signed bundle is written to
`android/target/ratass-android-1.0-release.aab`. The same build also creates the
debug APK used for local installation.

## Desktop

Run the package script separately on Linux, Windows, and macOS. `jpackage`
creates a native app image with a trimmed Java runtime, so Steam users do not
need to install Java.

```bash
tools/release/package-desktop.sh
```

Set `JPACKAGE_ICON` to a platform-native icon (`.png`, `.ico`, or `.icns`) once
final store artwork is available. macOS signing and notarization should be
performed on the generated app in the macOS CI job.

To upload one platform depot:

```bash
export STEAM_APP_ID=...
export STEAM_DEPOT_ID=...
export STEAM_USERNAME=...
export STEAM_CONTENT_DIR="$PWD/dist/desktop/linux-x86_64/RogueCircuit"
tools/release/publish-steam.sh
```

Steam Guard may prompt during login. Use a dedicated Steam build account in CI.

## iOS

Use macOS with the current Xcode and iOS SDK:

```bash
mvn -Pios -pl ios -am package
mvn -Pios -pl ios robovm:ipa
```

The bundle identifier and signing identity can be overridden in
`ios/robovm.properties` or by the secure macOS CI configuration. App Store
submission still requires final icons, screenshots, privacy declarations, and
an Apple distribution profile.
