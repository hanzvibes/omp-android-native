# Embedded OMP TUI MVP

## Product requirement

The MVP must open the real interactive OMP TUI inside OMP Android. Installing or opening the separate Termux app is not an accepted runtime dependency.

Target user flow:

1. Install and open OMP Android.
2. On first launch, the app prepares its private Android ARM64 runtime and OMP payload, with visible progress and cancellation/retry.
3. The app opens an embedded terminal screen and starts the real `omp` process in a selected workspace.
4. Keyboard input, ANSI rendering, terminal resize, process exit, and relaunch are handled by OMP Android.
5. On later launches, an already-installed runtime is reused and the app goes straight to the OMP home/TUI flow.

## MVP acceptance criteria

- [ ] No Termux package, Termux permission, Termux app launch, or RUN_COMMAND bridge is required.
- [ ] Runtime is stored under this app's private files directory; no root and no localhost bridge.
- [ ] Runtime target is Android ARM64 / Bionic. Linux glibc binaries and Ubuntu PRoot are not accepted substitutes.
- [ ] The exact Android-compatible Bun runtime and OMP native addon are pinned to immutable versions/commits and verified by SHA-256 before extraction.
- [ ] OMP is installed from a pinned source revision using an Android-compatible build path. No mutable `curl | sh` installer is executed by the app.
- [ ] The real `omp` interactive TUI starts inside an embedded terminal backed by a PTY.
- [ ] Terminal handles ANSI alternate screen, cursor movement, colors, resize, UTF-8, Ctrl-C, Enter, backspace, and the keyboard protocol required by OMP.
- [ ] The app can stop/restart the process and show useful bootstrap/runtime errors.
- [ ] A physical ARM64 Android device test proves the TUI opens and accepts input. CI compilation alone is not proof.

## Runtime and terminal investigation

### OMP runtime

The community Android/Termux integration documents that OMP needs Android/Bionic-native Bun and a native Rust addon built for Android ARM64. Its source-build route currently uses Termux packages and a Termux-native Bun distribution. A Termux binary must not be assumed portable to another app's private directory: package prefixes, linker paths, and runtime assumptions must be audited before bundling.

Reference: https://github.com/PlayWithG/oh-my-pi-termux/tree/termux

### Embedded runtime candidate

`libtermux-android` advertises a standalone userspace, PTY, and terminal widget, but its own README explicitly labels it experimental and not production-ready. It is therefore a research candidate only, not an approved production dependency.

Reference: https://github.com/libtermux/libtermux-android

### Terminal emulator candidate

ConnectBot's `termlib` is a Compose terminal component using libvterm and is Apache-2.0 licensed. Its README lists Kitty/iTerm2 protocol support as planned, while OMP's terminal experience may rely on Kitty keyboard protocol behavior. It must pass a compatibility spike before being selected as the OMP terminal.

References:
- https://github.com/connectbot/termlib
- https://github.com/can1357/oh-my-pi/blob/main/docs/tui.md

## Implementation sequence

### Milestone A — prove the vertical slice

- [x] Select the official Termux `terminal-view` / `terminal-emulator` libraries as the initial PTY + rendering base; dependency is pinned to `0.118.0`.
- [x] Add an embedded `TerminalSession` backed by the library JNI PTY and launch `/system/bin/sh` from app-private storage in the app UI.
- [ ] Build and run the shell prototype on an Android device; rendering, keyboard input, and resize are not yet device-verified.
- [x] Define the OMP terminal compatibility matrix and add a manual PTY smoke script (`docs/TERMINAL_COMPATIBILITY.md`, `tools/terminal-compat-smoke.sh`).
- [ ] Run the terminal smoke script on a real Android device using the selected embedded terminal backend.
- [ ] Prove an Android-compatible Bun binary can run from the app sandbox.
- [ ] Prove the OMP Rust addon loads in that exact runtime.
- [ ] Launch `omp` and capture a real on-device TUI session.

Do not replace the current main-branch workflow or claim the MVP is functional until this vertical slice passes.

### Milestone B — product integration

- [ ] Add first-run runtime bootstrap with pinned artifacts, hashes, progress, cancellation, and recovery.
- [ ] Add embedded terminal screen and lifecycle-aware PTY session owner.
- [ ] Connect workspace picker to the app-private runtime path without the Termux shared-storage copy.
- [ ] Add provider configuration and secure credential storage.
- [ ] Remove the old Termux companion setup from the primary onboarding flow.

## Current implementation status

The current `main` branch is still a Termux companion prototype. Its workspace picker and RUN_COMMAND launch path do not satisfy the one-app product requirement. They remain reference code until the embedded runtime vertical slice is proven.

The compatibility requirements and manual smoke fixture are in this spike branch. The app now contains an embedded Android shell prototype using the official Termux terminal libraries (`terminal-view` + its transitive `terminal-emulator`) and JNI PTY. The library's `terminal-view` and `terminal-emulator` modules are explicitly license-exempt from the Termux app's GPL-3.0-only license and retain their Apache-2.0 licensing; do not add `termux-shared` without a separate license review. The prototype has not yet passed CI or a physical-device test. It does not bundle Bun, install OMP, implement the Kitty keyboard adapter, or support DEC synchronized output mode 2026. No embedded OMP TUI is claimed.
