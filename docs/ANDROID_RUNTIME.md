# Android runtime strategy

## Verified direction

Use native Termux / Android ARM64 userspace. Do not use the Linux glibc build inside Ubuntu PRoot as the primary strategy, and do not package a standard Linux Bun binary.

The community Android route documents:
- Android ARM64 / Termux as the target.
- A Termux-native Bun build (Bun >= 1.4.0) from the community Bun distribution.
- Source installation of OMP so its Rust native addon is compiled for Android / Bionic.
- Build prerequisites including libc++, Rust, Clang, CMake, Python and Git.
- First setup can take several minutes.

Reference implementation:
https://github.com/PlayWithG/oh-my-pi-termux/tree/termux

Documented source installer:
curl -fsSL https://raw.githubusercontent.com/PlayWithG/oh-my-pi-termux/termux/scripts/install.sh | sh -s -- --source

This is a community-maintained integration, not an official Android binary distribution from upstream OMP. Pin and review the source revision before using it in a release build.

## Bun limitation

The Termux integration documents a failure in Bun 1.3.14 where bun build --compile creates a file that fails at runtime on Android. Use a regular Bun runtime and source launcher; do not rely on a compiled standalone executable without re-validating a newer Bun version.

## APK integration decision

The current app is only a native Compose shell. A normal Android APK cannot assume it can install and manage an arbitrary Termux environment by copying Linux files into its private directory. The next milestone must validate one of these integration models:

1. Termux companion integration: use the Termux app and a documented, permissioned bridge. Lowest-risk way to prove OMP on-device.
2. Bundled Android userspace: package a legally distributable Android userspace and native Bun / addon artifacts, then implement extraction, updates, process lifecycle and storage permissions. Larger engineering effort requiring device tests.

Do not silently download and execute mutable shell scripts in production. Pin revisions, verify checksums or signatures where available, surface licenses, and provide cancellation and recovery.

## Runtime smoke checks

Run on a real ARM64 Termux device:

    printf 'PREFIX=%s\\n' "$PREFIX"
    command -v bun
    bun --version
    bun -e 'console.log({ platform: process.platform, arch: process.arch })'
    uname -a
    omp --version
    omp --help

A successful GitHub Actions cross-build is not a substitute for loading the native addon on a physical Android device.


## App-to-Termux integration: first safe milestone

The APK now detects whether the Termux package is installed, can launch Termux, and provides copy-only setup commands. It deliberately does not execute shell commands, install packages, or read Termux's private app data.

This is a companion workflow, not yet a process bridge:
- Android package visibility is declared for `com.termux`.
- The user opens Termux explicitly.
- Setup commands are copied to the clipboard and pasted by the user.
- OMP runtime status remains "Not verified" until a future, explicit bridge can return a trustworthy command result.

The app must not label Termux presence as proof that OMP is installed. A later bridge should use Termux's documented RUN_COMMAND integration only after the user enables the required permission in Termux, and should return bounded stdout/stderr plus an exit code. Never execute installer commands silently from the APK.
