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

The native libraries are **not** built by Gradle. Anything found in
`vlc-android/libs/<abi>/` is packaged into the APK, so a build made by the
Makefile (or a set of prebuilt `.so`) can be dropped there before running
`assembleDebug`. Without them the APK builds and installs, but libvlc fails to
load at startup.

## CI

`.github/workflows/build-apk.yml` runs `assembleDebug` on every push and
uploads the APK as a build artifact (`vlc-debug-apk`).
