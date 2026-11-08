# OMP Android Native

A native Android application shell for running [Oh My Pi](https://omp.sh/) on ARM64 devices.

> Early-stage project. The first Android UI scaffold and APK CI workflow are in place. The OMP runtime is not yet connected to the app.

## Current direction

Use native Android / Termux ARM64 userspace. A community-maintained OMP Termux integration documents a source-build route using Android-native Bun and a Rust addon compiled for Android / Bionic.

- Android integration: https://github.com/PlayWithG/oh-my-pi-termux/tree/termux
- OMP upstream: https://github.com/can1357/oh-my-pi
- Runtime details and caveats: [docs/ANDROID_RUNTIME.md](docs/ANDROID_RUNTIME.md)

This is not an official upstream OMP Android binary release. Do not use Linux glibc binaries or generic Linux Bun builds as substitutes for Android-native artifacts.

## Stack

- Kotlin
- Jetpack Compose
- Android Gradle Plugin
- GitHub Actions
- Target architecture: Android ARM64

## Development status

- [x] Repository bootstrap
- [x] Native Compose application shell
- [x] Debug APK CI workflow
- [x] Document Android / Termux runtime approach
- [ ] Verify the community runtime on a physical Android device
- [x] Termux companion onboarding: package detection, launch, and copy-only setup commands
- [x] Implement a restricted Termux RUN_COMMAND diagnostic bridge
- [ ] Verify RUN_COMMAND callback on a physical Android device
- [ ] Workspace picker and process lifecycle
- [ ] Connect OMP sessions and provider authentication
- [ ] Release APK and device regression tests

## Build

The GitHub Actions workflow builds a debug APK on pushes to main and pull requests. The artifact is published as `omp-android-debug`.

For local builds, use JDK 17 and Gradle 8.9:

    gradle assembleDebug

## Important

The current APK is a Termux companion onboarding shell, not yet a functioning coding agent. It detects and opens Termux, copies setup commands, and can request one fixed read-only OMP version diagnostic through the permissioned Termux bridge. It does not run the installer or accept arbitrary shell commands. The bridge still needs physical-device verification. Workspace access, interactive OMP sessions, authentication, and device regression tests remain pending.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before redistributing third-party runtime components.
