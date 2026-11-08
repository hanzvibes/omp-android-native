# OMP Terminal Compatibility Spike

This document is the terminal acceptance checklist for the embedded OMP TUI MVP.

## Why a normal text console is not enough

OMP's TUI is a full-screen terminal application. The Android view must behave like a terminal emulator connected to a PTY, not like a scrolling log window or a WebView.

The upstream TUI implementation uses terminal control sequences for alternate-screen lifecycle and Kitty keyboard protocol negotiation. Its renderer also uses synchronized output to avoid partially painted frames. The selected emulator must implement these behaviors or explicitly avoid advertising unsupported capabilities.

Upstream references:
- https://github.com/can1357/oh-my-pi/blob/24439066040e006eb08da7b204f0ac41c79d2de9/packages/tui/src/terminal.ts
- https://github.com/can1357/oh-my-pi/blob/24439066040e006eb08da7b204f0ac41c79d2de9/packages/tui/src/terminal-capabilities.ts
- https://github.com/can1357/oh-my-pi/blob/24439066040e006eb08da7b204f0ac41c79d2de9/packages/tui/README.md
- https://github.com/can1357/oh-my-pi/blob/24439066040e006eb08da7b204f0ac41c79d2de9/packages/tui/src/keys.ts

## Required behavior

| Capability | Required behavior | Acceptance test |
|---|---|---|
| PTY | Real pseudo-terminal with stdin/stdout/stderr and process exit | Start a shell child, type a command, receive output and exit status |
| Alternate screen | Save/restore normal screen without corrupting scrollback | Enter and leave full-screen mode repeatedly |
| Cursor control | Position, hide/show, save/restore cursor | Render a fixed grid and update one cell |
| Colors | 256-color and 24-bit RGB | Render foreground/background color swatches |
| Unicode | UTF-8, combining marks, East Asian wide characters | Render mixed Indonesian, emoji, and CJK sample text |
| Resize | Update PTY rows/columns when view size changes | Rotate device and resize split/full-screen layout |
| Kitty keyboard | Correctly negotiate and decode enhanced key events | Verify Enter, Shift+Enter, Ctrl+Enter, Alt, arrows, Escape, and modified keys |
| Paste | Preserve pasted multiline text and bracketed-paste boundaries | Paste multiline source text into prompt |
| Interrupt | Deliver Ctrl-C to foreground process | Interrupt a running command and start another |
| Lifecycle | Stop process and release PTY on explicit close | Open/close/reopen without leaked child processes |
| Output synchronization | Handle DEC private mode 2026 begin/end safely | Update many rows without tearing or stale partial frames |

## Compatibility notes

- Do not claim Kitty keyboard support merely because a library supports ANSI/VT100 or xterm.
- Do not advertise terminal capabilities that the emulator does not actually implement.
- OMP must run in a real PTY. Capturing a non-interactive process's stdout is not a substitute.
- A local TCP/HTTP bridge is not needed and is not part of this design.
- The Android IME must not silently consume Enter, Escape, Ctrl, Alt, or arrow key events needed by the TUI.
- The terminal must report accurate dimensions and propagate resize events to the PTY.
- A scrolling Compose Text component is not an acceptable terminal implementation.

## Candidate assessment

### libtermux-android

The upstream README documents a private app runtime, JNI PTY support, session APIs, and a terminal view. It also explicitly labels the project experimental and not production-ready. Treat it as a candidate for a throwaway compatibility spike only; do not make it a production dependency until the exact artifact, source revision, API surface, ABI packaging, and lifecycle behavior are verified.

Reference: https://github.com/libtermux/libtermux-android

### Official Termux terminal libraries\n\nThe official Termux App repository publishes reusable `terminal-view` and `terminal-emulator` libraries. The inspected `TerminalSession` uses JNI to create a PTY process and supports terminal resize. Source inspection found xterm `modifyOtherKeys`, but did not find the Kitty keyboard query/stack protocol or DEC synchronized output mode 2026 required by OMP. It is therefore a strong PTY/rendering base candidate only after a compatibility adapter or maintained fork is implemented. The terminal view is an Android `View`, so Compose integration uses `AndroidView`. Check GPL-3.0-or-later obligations before distribution.\n\nReference and details: [Termux terminal library spike](TERMUX_TERMINAL_LIBRARY_SPIKE.md)\n\n### ConnectBot termlib

ConnectBot's terminal emulator is a separate candidate. Its public README describes libvterm-based rendering and says Kitty/iTerm2 escape-code support is planned. That makes it insufficient as the OMP terminal unless the missing protocol support is implemented and tested first.

Reference: https://github.com/connectbot/termlib

## Test execution record

No Android device test has been run from this repository yet.

| Test | Status |
|---|---|
| Android ARM64 PTY smoke test | Not run |
| ANSI / alternate-screen rendering | Not run |
| Kitty keyboard negotiation | Not run |
| IME and hardware keyboard behavior | Not run |
| OMP TUI launch | Not run |
| Device rotation / PTY resize | Not run |

A green Gradle build does not satisfy these runtime tests.
