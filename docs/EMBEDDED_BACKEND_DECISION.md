# Embedded terminal backend decision

**Status:** Investigation required; no backend approved for production.

## Product constraints

The MVP must run entirely inside one Android app:
- no separate Termux installation or app launch;
- no localhost or loopback server bridge;
- Android ARM64 / Bionic process runtime in app-private storage;
- real PTY and embedded terminal UI;
- OMP TUI keyboard, alternate-screen, resize, and output behavior must be proven on a physical device.

## Candidate findings

### libtermux-android

The project advertises an app-private Termux-like runtime, JNI PTY, sessions, and a terminal view. Its README also explicitly states it is experimental and not production-ready. The documentation has multiple API/dependency examples and includes a separate Linux-distro/PRoot module, so the exact supported artifact, API surface, runtime prefix, and terminal protocol behavior must be verified from source before integration.

Decision: **spike only; do not add as a production dependency yet.**

Source: https://github.com/libtermux/libtermux-android

### Official Termux terminal libraries

Termux documents separately published `terminal-view`, `terminal-emulator`, and `termux-shared` libraries from Termux App 0.116 onward. The terminal view can provide a reusable Android terminal UI, but importing those libraries alone does not provide a standalone OMP runtime in this app's private sandbox. The package prefix, native process launcher, and runtime packaging still need a separate solution. The library's exact OMP/Kitty keyboard compatibility also remains unverified.

Decision: **useful terminal-renderer reference; not a complete embedded-runtime solution.**

Source: https://github.com/termux/termux-app/wiki/Termux-Libraries

### ConnectBot termlib

This is a separate terminal-rendering candidate, but its documented Kitty/iTerm2 protocol support is not established as complete for OMP's needs.

Decision: **do not select until protocol tests pass.**

Source: https://github.com/connectbot/termlib

## Next engineering gate

Do not add an unverified AAR or write a fake terminal screen. First produce a minimal Android ARM64 vertical slice that:
1. creates a PTY in the app sandbox;
2. starts a known Android/Bionic test executable;
3. renders ANSI alternate-screen and synchronized-output fixtures;
4. receives Kitty keyboard negotiation and modified key input correctly;
5. propagates terminal resize and Ctrl-C;
6. then runs the pinned Bun runtime and loads OMP's Android native addon.

A candidate passes only with a reproducible APK and physical-device evidence. CI build success by itself is insufficient.
