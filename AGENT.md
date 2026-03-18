# AGENT.md — APK Build Guide

## Build Command

```bash
flutter build apk --release --split-per-abi --target-platform android-arm64
```

## Output

```
build/app/outputs/flutter-apk/app-arm64-v8a-release.apk
```

## Flags Explained

| Flag | Reason |
|---|---|
| `--release` | Enables minification, tree-shaking, and signing |
| `--split-per-abi` | Produces a single ABI-specific APK instead of a fat APK |
| `--target-platform android-arm64` | Builds only for arm64-v8a, reducing APK size significantly |

## Signing

Signing is configured in `android/app/build.gradle.kts` and reads credentials from `android/key.properties`.

```
android/key.properties
  storeFile=/home/user/upload-keystore.jks
  storePassword=android
  keyPassword=android
  keyAlias=upload
```

The keystore file lives at `/home/user/upload-keystore.jks`. Do not move or rename it without updating `key.properties`.

## Before Building

Remove old APKs to avoid confusion:

```bash
find build -name "*.apk" -delete
```

## Full Build Script

```bash
# From project root: /home/user/myapp
find build -name "*.apk" -delete
flutter build apk --release --split-per-abi --target-platform android-arm64
```

## Notes

- Do **not** use `--target-platform android-arm,android-x64` unless testing on other architectures — it increases APK size
- Do **not** run `flutter build apk` without `--split-per-abi` — it produces a fat APK (~3x larger)
- Icons are tree-shaken automatically; do not pass `--no-tree-shake-icons`
- The app targets rooted Android devices (arm64) only, so other ABIs are unnecessary
