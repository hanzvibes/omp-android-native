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

def patch(path: str, before: str, after: str) -> None:
    target = Path(path)
    contents = target.read_text()
    count = contents.count(before)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}; found {count}")
    target.write_text(contents.replace(before, after))


# Bionic exposes mknodat but not mkfifoat. Create the FIFO with S_IFIFO
# through the same dirfd-relative operation.
patch(
    "crates/pi-builtins/src/rsync/tree/sys.rs",
    """\tcheck(retry(|| {
\t\t// SAFETY: valid C string and dirfd.
\t\tunsafe { libc::mkfifoat(dirfd(dir), name.as_ptr(), mode as libc::mode_t) }
\t}))
""",
    """\tcheck(retry(|| {
\t\t// SAFETY: valid C string and dirfd. mknodat is available in Bionic.
\t\tunsafe {
\t\t\tlibc::mknodat(
\t\t\t\tdirfd(dir),
\t\t\t\tname.as_ptr(),
\t\t\t\t(libc::S_IFIFO | mode) as libc::mode_t,
\t\t\t\t0,
\t\t\t)
\t\t}
\t}))
"""
)

# Bionic declares getnameinfo's address length as usize; other libc targets
# use socklen_t. The inferred cast follows the target ABI.
patch(
    "crates/pi-builtins/src/rsync/transport/tcp/sock_unix.rs",
    """\t\tlibc::getnameinfo(
\t\t\taddr.as_ptr(),
\t\t\taddr.len,
""",
    """\t\tlibc::getnameinfo(
\t\t\taddr.as_ptr(),
\t\t\taddr.len as _,
"""
)

print("Applied Android Bionic compatibility edits.")
