# 7. Android app from the same Java

`android-app/` is a native Android application written in Java. It contains only three classes of
its own; every calculation comes from `java/civiltech-core`, the **same source files** that the
website runs (after TeaVM translation) and that the unit tests check on the JVM.

```groovy
// android-app/app/build.gradle
sourceSets {
    main {
        java.srcDirs += ['../../java/civiltech-core/src/main/java']   // shared Java
        assets.srcDirs += ['../../site/data/catchment']                // shared open data
    }
}
```

| Class | Role |
|-------|------|
| `MainActivity` | builds the screen in code (no XML layouts), loads the data from the app's assets, runs delineation and simulation on a background thread, shows the results |
| `CatchmentMapView` | draws land cover with hillshade, streams, catchment outline and outlet on a `Canvas`; tap to move the outlet |
| `HydrographView` | draws rainfall bars and the two hydrographs |

The app uses no third-party libraries, so it needs nothing beyond the Android SDK.

## Get the APK

* **From the lab website**: the *Pages* workflow builds the app and publishes
  `downloads/civiltech-catchment-debug.apk`. On the phone allow *Install unknown apps* for the
  browser, open the file, install. This is a debug build for teaching, not a Play Store release.
* **From GitHub Actions**: every CI run uploads the APK as the artifact `android-apk`.

## Build it yourself

With Android Studio: *Open* the folder `android-app`, wait for Gradle sync, press *Run*.

Command line (JDK 17 or 21 and the Android SDK installed, `ANDROID_HOME` set):

```bash
cd android-app
./gradlew assembleDebug
```

The APK appears in `app/build/outputs/apk/debug/`.

## Exercises

1. Add a third line to the hydrograph for the Clark-routed flow (`SimulationResult.flowClark`).
2. Add a `Spinner` for the computation step (5, 10, 15 minutes).
3. Save the last result to a CSV file in the app's storage and share it with an `Intent`.
