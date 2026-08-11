# Building

This tree is [VLC for Android](https://code.videolan.org/videolan/vlc-android)
upstream, taken at `cfee6d3`, with the changes needed to build it from Gradle
alone. What came before -- the 0.1.4 tree from 2013 -- is in this branch's
history.

## What is built, and what is not

libvlc and the media library are **not** built from source: the application
takes them from Maven Central, which is what upstream's `debug` and `release`
build types already do.

```
org.videolan.android:libvlc-all:3.7.5
org.videolan.android:medialibrary-all:0.13.22
```

Those artifacts ship the native libraries for `armeabi-v7a`, `arm64-v8a`,
`x86` and `x86_64`, so the APK runs on 64-bit-only devices -- a Quest among
them, which the 2013 tree could never do.

Building libvlc from source is what upstream's `dev` build type and the
separate [libvlcjni](https://code.videolan.org/videolan/libvlcjni) repository
are for. That repository is not checked out here, so `:libvlcjni:libvlc` is
dropped from `settings.gradle` and the three `dev` dependencies on it point at
the Maven artifact instead. Nothing else was changed to upstream's build.

## Building

```sh
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :application:app:assembleDebug
# => application/app/build/outputs/apk/debug/VLC-Android-<version>-debug-all.apk
```

Requirements: a JDK 21, and an Android SDK with `platforms;android-36` and
`build-tools;36.0.0`. `local.properties` is not optional: the root
`build.gradle` reads it unconditionally.

## Building for one ABI

The APK carries the four ABIs libvlc ships, which is most of its ~130 MB,
while a device loads exactly one of them:

```sh
./gradlew -PabiFilter=arm64-v8a :application:app:assembleDebug
```

`arm64-v8a` is the only ABI a 64-bit-only device -- a Quest 3 among them --
can load, and the resulting APK is a fraction of the universal one to push
over `adb`. Left unset, the build keeps every ABI.

## Installing next to an official VLC

Upstream's `debug` build type already appends `.debug` to the application id,
so the APK installs as `org.videolan.vlc.debug` and coexists with a VLC from
the store.

## CI

`.github/workflows/build-apk.yml` runs that build on every push and uploads
the APK as a build artifact (`vlc-debug-apk`).
