# Release Builds

Release credentials and store identifiers stay outside the repository. The
scripts fail before packaging when required values are missing.

## Android

Install Android SDK Platform 36 and Build Tools 36.0.0. Maven remains the
top-level entry point and invokes the checked-in Android Gradle wrapper for AAR,
APK, AAB, and signing support. Keep the Play upload key separate from the
app-signing key managed by Google Play.

Create the permanent upload key once. The helper creates a private PKCS12
keystore valid for 100 years and prompts for its password and certificate
identity:

```bash
tools/release/create-android-upload-key.sh
```

The default location is
`~/.config/rogue-circuit/android-upload.p12`. Keep secure backups of the
keystore and password; never add either to Git. You do not need Google's
downloadable public certificates to sign an upload.

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
export ANDROID_KEYSTORE="$HOME/.config/rogue-circuit/android-upload.p12"
export ANDROID_KEY_ALIAS=upload
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_PASSWORD=...
mvn -Pandroid,android-release package \
  -Dandroid.version.name=1.0
```

The signed bundle is written to
`android/target/ratass-android-1.0-release.aab`. The same build also creates the
debug APK used for local installation. Release builds generate a monotonically
increasing Google Play `versionCode` automatically and retain the last value in
`~/.local/state/rogue-circuit/android-version-code`. Use
`-Dandroid.release.version.code=123` only when an explicit code is required.
Before packaging, the release script checks that the selected certificate
remains valid after 22 October 2033, as required by Google Play.

## Desktop

Run the package script separately on Linux, Windows, and macOS. `jpackage`
creates a native app image with a trimmed Java runtime, so Steam users do not
need to install Java.

```bash
tools/release/package-desktop.sh
```

The package script selects the checked-in platform icon automatically. Set
`JPACKAGE_ICON` only to override it. macOS signing and notarization should be
performed on the generated app in the macOS CI job.

The Steam-specific release and store checklist is in
[`STEAM_RELEASE_CHECKLIST.md`](STEAM_RELEASE_CHECKLIST.md).

To upload one platform depot:

```bash
export STEAM_APP_ID=...
export STEAM_DEPOT_ID=...
export STEAM_USERNAME=...
export STEAM_CONTENT_DIR="$PWD/dist/desktop/linux-x86_64/RogueCircuit"
tools/release/publish-steam.sh
```

Steam Guard may prompt during login. Use a dedicated Steam build account in CI.

### Windows And Steam

Install a 64-bit JDK 21 and Maven, set `JAVA_HOME`, and run from Command Prompt
or PowerShell at the repository root:

```bat
tools\release\release-steam-windows.cmd -LaunchSmokeTest
```

This builds a fresh Windows jar, creates the bundled app image, verifies the
expected executable/runtime layout, and briefly launches it. The Steam content
root is:

```text
dist\desktop\windows-x86_64\RogueCircuit
```

Configure its Steam launch option as `RogueCircuit.exe`. To upload the Windows
depot, install SteamCMD and provide the account-specific values:

```powershell
$env:STEAM_APP_ID = "..."
$env:STEAM_DEPOT_ID = "..." # Windows depot ID
$env:STEAM_USERNAME = "..."
$env:STEAMCMD_BIN = "C:\SteamCMD\steamcmd.exe"
tools\release\release-steam-windows.cmd -SkipBuild -Upload
```

Omit `-SkipBuild` to rebuild immediately before uploading. Set
`STEAM_BRANCH` to upload and make the build live on a named test branch;
otherwise the build is uploaded without changing a live branch.

## iOS

Use macOS with the current Xcode and iOS SDK:

```bash
mvn -Pios -pl ios -am package
mvn -Pios -pl ios robovm:ipa
```

The bundle identifier and signing identity can be overridden in
`ios/robovm.properties` or by the secure macOS CI configuration. The project
includes its app icon and privacy manifest; App Store submission still requires
screenshots, completed privacy declarations, and an Apple distribution profile.
