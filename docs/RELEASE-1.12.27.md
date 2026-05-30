# VitaLite 1.12.27 Release Notes

This release updates VitaLite to RuneLite `1.12.27`.

## Changes

- Updated the default RuneLite bootstrap target to `1.12.27`.
- Updated the VitaLite gamepack mappings and runtime patch bundle for RuneLite `1.12.27`.
- Updated the login call stack check for the new client revision.
- Corrected the call stack second frame for this revision.
- Added the new RuneLite `CameraFocusableEntity` API type required by this revision.
- Fixed forced target bootstrap downloads so clean installs can fetch the exact target artifacts reliably.
- Pinned API source sync to the selected RuneLite release tag instead of a moving branch.
- Refreshed developer docs for the `1.12.27_0` API version.

## Download

Use the release asset:

```text
VitaLite-1.12.27_0.zip
```

SHA-256:

```text
982e13ee78b1eb056779dfd1c393db36bd7e71b25c9751b74781bbc6a97bc403
```

The zip contains:

```text
VitaLite.jar
run-linux.sh
run-mac.sh
run-windows.bat
```

## Verification

- Gradle test and release packaging completed successfully.
- Release-mode startup applied the bundled patches and started RuneLite `1.12.27`.
- The uploaded GitHub release asset was downloaded again and its SHA-256 matched the local verified build.
