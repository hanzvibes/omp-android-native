#!/usr/bin/env bash
set -euo pipefail

BUN_PATH="${1:?usage: verify-android-bun.sh <bun-binary>}"
EXPECTED_SHA256="c1e10a8c7b1cdb059bd0c55ff6f49cab502424ed4ad5df41a2ce116abd95e72b"

for tool in sha256sum readelf; do
  command -v "$tool" >/dev/null 2>&1 || {
    echo "Missing required tool: $tool" >&2
    exit 2
  }
done

[[ -f "$BUN_PATH" ]] || { echo "Bun binary not found: $BUN_PATH" >&2; exit 2; }

actual_sha256="$(sha256sum "$BUN_PATH" | awk '{print $1}')"
if [[ "$actual_sha256" != "$EXPECTED_SHA256" ]]; then
  echo "Bun checksum mismatch" >&2
  echo "Expected: $EXPECTED_SHA256" >&2
  echo "Actual:   $actual_sha256" >&2
  exit 1
fi

elf_header="$(readelf -h "$BUN_PATH")"
grep -Eq 'Machine:.*AArch64' <<<"$elf_header" || {
  echo "Bun is not an AArch64 ELF binary" >&2
  exit 1
}

program_headers="$(readelf -l "$BUN_PATH")"
grep -Fq '/system/bin/linker64' <<<"$program_headers" || {
  echo "Bun does not use Android's linker64 interpreter" >&2
  exit 1
}

dynamic_section="$(readelf -d "$BUN_PATH")"
grep -Fq 'libc++_shared.so' <<<"$dynamic_section" || {
  echo "Bun does not declare the expected libc++_shared.so dependency" >&2
  exit 1
}

echo "Verified native Android ARM64 Bun"
echo "SHA-256: $actual_sha256"
echo "ELF dependencies:"
grep 'NEEDED' <<<"$dynamic_section" || true
