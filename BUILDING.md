# Building

## The historical build (application + libvlc)

`compile.sh` / the top level `Makefile` build libvlc, its plugins and the JNI
glue with the Android NDK, then package everything with `ant`. This is the only
way to get a *working* APK, since the player is useless without the native
libraries. It requires the old SDK tools (`ant` support was removed from the
Android SDK after r25.2.5).

## The Gradle build (application only)

A Gradle build is provided next to it so that the Java sources and the
resources can be compiled and packaged with current tooling -- this is what the
CI uses. It reuses the legacy directory layout as is (`vlc-android/src`,
`vlc-android/res`, `java-libs/*`), no source file has been moved.

```sh
./gradlew assembleDebug
# => vlc-android/build/outputs/apk/debug/vlc-android-debug.apk
```

Requirements: a JDK 17 and an Android SDK with `platforms;android-33` and
`build-tools;33.0.2` (`ANDROID_HOME` or `sdk.dir` in `local.properties`).

`minSdkVersion` is raised from 7 to 14, the floor of current build tools, and
`targetSdkVersion` from 18 to 23: Android 14 refuses to install a package
targeting less than 23 ("the package appears to be invalid"). 23 is the lowest
value that installs, and the highest one that keeps the legacy storage access
this code base is written against -- it only costs the runtime storage
permission asked for on startup.

The native libraries are **not** built by Gradle. Anything found in
`vlc-android/libs/<abi>/` is packaged into the APK, so a build made by the
Makefile (or a set of prebuilt `.so`) can be dropped there before running
`assembleDebug`. Without them the APK builds and installs, but libvlc fails to
load at startup.

## Application id

This build installs as `org.videolan.vlc.dev`, under the name `VLC (dev)`, so
that it can sit next to an official VLC on the same device. Only the
application id differs: the Java package is still `org.videolan.vlc`, which is
what the JNI symbol names are built from, so libvlcjni does not have to be
rebuilt (this is what `rename_package.sh` was for).

To rename it again, change `applicationId` in `vlc-android/build.gradle` and
`app_name` / `widget_name` in `vlc-android/res/values/strings.xml`. Nothing
else refers to the package by name: the internal broadcasts and the widget are
addressed through `getPackageName()`.

## Headsets (Meta Quest 3)

The package installs and shows up as a 2D application: `READ_PHONE_STATE`
implies `android.hardware.telephony` and every application implicitly requires
a touchscreen, so both are declared optional in the manifest, and the code no
longer assumes a telephony service exists.

Playing anything is another matter. Quest 3 runs 64-bit only, so it needs
libvlc built for `arm64-v8a`, an ABI that did not exist when this tree was
written (the NDK build here produces `armeabi-v7a` / `x86`). Without a
loadable `libvlcjni.so` the application exits at startup -- `LibVLC`'s static
initializer calls `System.exit(1)` when `System.loadLibrary("vlcjni")` fails.
An arm64 libvlc from a current VLC build, dropped in
`vlc-android/libs/arm64-v8a/`, is what this build is missing.

## CI

`.github/workflows/build-apk.yml` runs `assembleDebug` on every push and
uploads the APK as a build artifact (`vlc-debug-apk`).
