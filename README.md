# OMP Android Native

A native Android application shell for running [Oh My Pi](https://omp.sh/) on ARM64 devices.

> Early-stage project. Android UI, workspace onboarding, and an embedded terminal prototype are under development. The full OMP runtime and Remotion workflow are not yet verified on a physical device.

## Current direction

Use native Android / Termux ARM64 userspace. A community-maintained OMP Termux integration documents a source-build route using Android-native Bun and a Rust addon compiled for Android / Bionic.

- Android integration: https://github.com/PlayWithG/oh-my-pi-termux/tree/termux
- OMP upstream: https://github.com/can1357/oh-my-pi
- Runtime details and caveats: [docs/ANDROID_RUNTIME.md](docs/ANDROID_RUNTIME.md)

This is not an official upstream OMP Android binary release. Do not use Linux glibc binaries or generic Linux Bun builds as substitutes for Android-native artifacts.

## Remotion Specialist Mode

The planned Remotion workflow makes OMP an Android authoring/control surface for motion-graphics projects:

- Specialized Remotion agent profile: [prompts/remotion-specialist.md](prompts/remotion-specialist.md)
- Product and technical specification: [docs/REMOTION_SPECIALIST_MODE.md](docs/REMOTION_SPECIALIST_MODE.md)
- Android authoring and project inspection; final video rendering runs in the cloud.
- Embedded preview is planned as a WebView-hosted HTTPS preview with a strict origin allowlist.
- Cloud rendering is planned behind a provider interface and authenticated broker; no long-lived GitHub token will be shipped in the APK.

The specification and agent profile are committed as the foundation. Project detection, embedded preview, and cloud rendering are not implemented or device-verified yet.

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
- [x] Persistent Android workspace folder picker
- [x] Bounded project sync into Termux shared storage
- [x] Launch interactive OMP session in a new Termux terminal
- [ ] Session lifecycle, exit status, and reconnect handling
- [ ] Connect OMP sessions and provider authentication
- [ ] Release APK and device regression tests
- [x] Remotion Specialist product/technical specification and agent profile
- [ ] Remotion project detection and metadata inspection
- [ ] Embedded Remotion preview in Compose
- [ ] Cloud render broker and durable job tracking
- [ ] Physical-device Remotion preview and render regression

## Build

The GitHub Actions workflow builds a debug APK on pushes to main and pull requests. The artifact is published as `omp-android-debug`.

For local builds, use JDK 17 and Gradle 8.9:

    gradle assembleDebug

## Important

The current APK is not yet a complete coding agent. Existing workspace and terminal integration paths still need physical-device verification. Workspace sync is limited to 3,000 files / 50 MB, skips dependency/build folders and secret env/key files, and never deletes destination files. Do not treat a CI build as proof of runtime compatibility.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before redistributing third-party runtime components.
