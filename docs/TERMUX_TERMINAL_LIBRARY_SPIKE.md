# Termux terminal library compatibility spike

**Status:** Source inspected; not integrated into the app.

## Candidate

The official Termux App repository publishes reusable Android libraries:
- `terminal-view`
- `terminal-emulator`
- `termux-shared`

The official library guide documents JitPack coordinates for `terminal-view` version `0.118.0` and states that `terminal-emulator` is pulled in transitively. The source for the inspected version includes a JNI-backed `TerminalSession`, which creates a real PTY process, exposes process input/output, and updates the PTY window size when the view changes.

References:
- https://github.com/termux/termux-app/wiki/Termux-Libraries
- https://github.com/termux/termux-app/blob/8629e632fcb95da272221be327db653fb24befe9/terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java
- https://github.com/termux/termux-app/blob/8629e632fcb95da272221be327db653fb24befe9/terminal-view/src/main/java/com/termux/view/TerminalView.java

## OMP compatibility gap

The inspected Termux terminal emulator is a credible PTY + VT/xterm rendering base, but it is **not OMP-compatible without changes**.

Source inspection of `TerminalEmulator.java` at commit `8629e632fcb95da272221be327db653fb24befe9` found:
- xterm `modifyOtherKeys` handling exists.
- Kitty graphics/underline-related strings occur, but this is not evidence of Kitty keyboard protocol support.
- No implementation of Kitty keyboard query/stack operations (`CSI ? u`, `CSI > flags u`, `CSI < u`) was found.
- No implementation of DEC synchronized output mode 2026 was found.
- The official terminal view uses Android's View system rather than Compose, so integration requires an AndroidView wrapper.

OMP's TUI explicitly probes Kitty keyboard support and DEC private mode 2026. It enables Kitty keyboard flags when the terminal replies, and uses mode 2026 to bracket atomic screen updates. Merely rendering ANSI colors or alternate screen is insufficient.

OMP source references:
- https://github.com/can1357/oh-my-pi/blob/main/packages/tui/src/terminal.ts
- https://github.com/can1357/oh-my-pi/blob/main/packages/tui/README.md

## Decision

Use the Termux libraries as the leading **PTY + terminal rendering base candidate**, not as a finished OMP terminal.

Before production integration, create a small isolated adapter/fork that:
1. embeds `TerminalView` inside Compose using `AndroidView`;
2. launches a harmless app-private Android/Bionic test process through `TerminalSession`;
3. correctly handles OMP's Kitty keyboard query and stack protocol;
4. translates Android soft/hardware key events into the selected Kitty keyboard level;
5. handles synchronized output 2026 without tearing;
6. propagates resize, paste, Ctrl-C, and process exit;
7. passes the terminal compatibility matrix on a physical ARM64 Android device.

The Termux app repository is GPLv3-only, but its LICENSE.md explicitly exempts the terminal-view and terminal-emulator libraries: those terminal components derive from Android Terminal Emulator code released under Apache-2.0. Do not pull in termux-shared without separately reviewing its mixed licensing. Preserve upstream notices and verify the exact source/artifact license files before shipping.

Do not add the dependency to the production app until the licensing review and isolated spike pass. Do not substitute the old Termux app bridge or localhost.
