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

The app began as a native Compose shell. A normal Android APK cannot assume it can install and manage an arbitrary Termux environment by copying Linux files into its private directory. The runtime integration options considered at the start of the project were:

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


## Permissioned runtime diagnostic bridge

The app now contains a narrowly scoped Termux RUN_COMMAND bridge for one fixed diagnostic:
- Executes Termux bash with a fixed script that checks `command -v omp` and, if present, runs `omp --version`.
- Requests a result through a one-shot mutable PendingIntent, then stores bounded output and exit status locally.
- Does not accept arbitrary commands from UI, deep links, or external intents.
- Does not run the OMP installer automatically.

Required user setup:
1. Install a compatible Termux build (result callback requires Termux >= 0.109).
2. In Android App Info for OMP Android, grant the additional `Run commands in Termux environment` permission.
3. In Termux, set `allow-external-apps=true` in `~/.termux/termux.properties`, then restart Termux.
4. Tap **Check OMP runtime** in OMP Android.

The Termux documentation requires both the Android permission and the Termux `allow-external-apps` setting. Result callbacks use background command output and exit code. See https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent.

The callback protocol and package/version behavior still require a physical-device test. A successful Android compile alone does not prove the bridge works on a device.


## Android workspace picker (first milestone)

The app now uses Android's Storage Access Framework folder picker:
- User chooses the project directory explicitly.
- The app requests persistable read/write URI access and stores the selected tree URI and display name.
- The saved URI is checked when the screen is reopened; the user can change or remove the selection.
- No broad storage permission is requested.

Important boundary: this URI grant belongs to OMP Android. It does **not** give Termux direct access to the same content-provider URI. The next implementation step must add an explicit, bounded file-sync bridge into a Termux-readable directory. Termux shared-storage access generally requires the user to run `termux-setup-storage` and grant Termux storage permission; Android 11+ storage restrictions can still affect access. Do not treat the folder picker as a connected OMP workspace yet.

Android reference: https://developer.android.com/training/data-storage/shared/documents-files
Termux storage reference: https://github.com/termux/termux-tools/blob/master/doc/termux.1.md.in


## Workspace sync into Termux shared storage

The workspace manager now supports a bounded one-way copy from a user-selected Android SAF folder into the root of primary shared storage:
- The user selects the source project folder.
- The user separately selects the **root of Internal storage** as the destination. The app validates the SAF tree document ID as `primary:`; it does not accept arbitrary SD-card or provider folders for this mapping.
- The app copies into `/storage/emulated/0/OMPWorkspaces/<workspace-name>`.
- In Termux, after running `termux-setup-storage`, the matching path is `~/storage/shared/OMPWorkspaces/<workspace-name>`.
- Sync overwrites matching files but does not delete stale destination files.
- The sync limit is 3,000 files and 50 MiB. It skips `.git`, dependency/build caches, `.env*`, private key files, and common secrets directories.
- The app requires read access to the source and read/write access to the shared-storage destination.

This is a one-way copy, not a live mount or two-way sync. OMP edits inside Termux will not automatically be reflected in the original source folder yet. Large repositories may exceed the current sync cap. File sync must be tested on real Android versions and document providers before being treated as production-ready.

Termux storage docs: https://github.com/termux/termux-tools/blob/master/doc/termux.1.md.in
Termux filesystem layout and cross-app access model: https://github.com/termux/termux-packages/wiki/Termux-file-system-layout
