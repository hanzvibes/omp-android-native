# OMP Android Native

Native Android ARM64 runtime and mobile interface for [Oh My Pi](https://omp.sh/).

> Early-stage project. The first milestone is proving the upstream OMP runtime on Android/Termux ARM64 before integrating it into the APK.

## Project goals

- Run OMP on-device on Android ARM64.
- Bootstrap a compatible Bun runtime and OMP dependencies.
- Provide a Kotlin/Jetpack Compose mobile shell.
- Manage workspaces, sessions, and runtime lifecycle.
- Build reproducible APK artifacts with GitHub Actions.

## Architecture

```text
Android app (Kotlin + Compose)
  └── Runtime manager / foreground service
       └── Android ARM64 userspace
            ├── Bun runtime
            ├── Oh My Pi
            └── Native addon(s), if supported
```

The runtime is intentionally isolated behind an adapter. Do not assume Linux x86_64 binaries or generic Linux Bun builds work on Android. Validate upstream compatibility before packaging binaries.

## Development status

- [x] Repository bootstrap
- [ ] Verify upstream OMP Android/Termux compatibility
- [ ] Runtime bootstrap prototype
- [ ] Android shell and service
- [ ] Workspace and process integration
- [ ] CI APK build and device testing

## References

- [Oh My Pi](https://omp.sh/)
- [OMP source](https://github.com/can1357/oh-my-pi)
- [Termux](https://termux.dev/)

## License

The Android integration code will be licensed separately. Oh My Pi and bundled dependencies retain their respective upstream licenses. See `THIRD_PARTY_NOTICES.md` before distributing a build.
