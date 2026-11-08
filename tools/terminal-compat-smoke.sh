#!/usr/bin/env bash
# Manual terminal compatibility probe for the future embedded OMP terminal.
# Run only inside a real PTY: bash tools/terminal-compat-smoke.sh

set -u

if [[ ! -t 0 || ! -t 1 ]]; then
  printf 'ERROR: this probe requires an interactive PTY.\n' >&2
  exit 2
fi

saved_stty="$(stty -g 2>/dev/null || true)"
cleanup() {
  printf '\033[<u\033[?2026l\033[?1049l\033[0m\033[?25h' || true
  if [[ -n "$saved_stty" ]]; then
    stty "$saved_stty" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

printf '\033[?1049h\033[2J\033[H'
printf '\033[1;36mOMP Android terminal compatibility probe\033[0m\n\n'

printf '\033[1m1. True color\033[0m\n'
for i in {0..23}; do
  r=$((i * 10))
  g=$((255 - i * 10))
  printf '\033[48;2;%d;%d;120m  \033[0m' "$r" "$g"
done
printf '\n\n'

printf '\033[1m2. Unicode and wide characters\033[0m\n'
printf 'Indonesia: Selamat datang · café · é · 日本語 · 한글 · مرحبا · 🚀\n\n'

printf '\033[1m3. Cursor addressing\033[0m\n'
printf 'This line should remain stable.\n'
printf '\033[1A\033[12G\033[33m[CURSOR UPDATE]\033[0m\n\n'

printf '\033[1m4. Synchronized output\033[0m\n'
printf '\033[?2026h'
for i in {1..8}; do
  printf 'Frame row %02d: OMP terminal paint test\n' "$i"
done
printf '\033[?2026l\n'

printf '\033[1m5. Kitty keyboard protocol\033[0m\n'
printf 'The next prompt enables Kitty keyboard disambiguation.\n'
printf 'Press: a, Shift+A, Ctrl+A, Alt+A, Enter, Shift+Enter, Up, Escape.\n'
printf 'Press q to finish. Raw key bytes are shown as hex.\n\n'

printf '\033[>1u'
stty -echo -icanon min 1 time 0
while true; do
  key=''
  IFS= read -r -n 1 key || true
  [[ -z "$key" ]] && continue
  hex="$(printf '%s' "$key" | od -An -tx1 | tr -d ' \n')"
  printf 'Received byte(s): %s\r\n' "${hex:-<none>}"
  [[ "$key" == "q" ]] && break
done
printf '\033[<u'
stty "$saved_stty"

printf '\n\033[1;32mProbe complete.\033[0m\n'
printf 'Review the screen, key bytes, resize behavior, and cleanup before marking compatibility passed.\n'
