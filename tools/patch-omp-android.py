#!/usr/bin/env python3
"""Apply the minimal Android Bionic compatibility edit to pinned OMP pi-vfs."""
from pathlib import Path

source = Path("crates/pi-vfs/src/fs.rs")
text = source.read_text()
old = """\t// SAFETY: `path` is a NUL-terminated string alive for the call.
\tunsafe { libc::faccessat(libc::AT_FDCWD, path.as_ptr(), mode, libc::AT_EACCESS) == 0 }
"""
new = """\t// SAFETY: `path` is a NUL-terminated string alive for the call.
\t// Android app processes use matching real/effective UIDs; Bionic's
\t// libc bindings do not expose AT_EACCESS, so use flags=0 there.
\t#[cfg(target_os = "android")]
\tlet flags = 0;
\t#[cfg(not(target_os = "android"))]
\tlet flags = libc::AT_EACCESS;
\tunsafe { libc::faccessat(libc::AT_FDCWD, path.as_ptr(), mode, flags) == 0 }
"""
count = text.count(old)
if count != 1:
    raise SystemExit(f"Expected exactly one may_access call to patch; found {count}")
source.write_text(text.replace(old, new))
print("Applied Android may_access compatibility edit.")
