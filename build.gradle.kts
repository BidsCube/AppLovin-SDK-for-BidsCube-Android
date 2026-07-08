plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.android.library") version "8.9.1" apply false
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    // CI often has no gradle.properties; enable AndroidX for Android modules
    if (!project.hasProperty("android.useAndroidX")) {
        project.ext.set("android.useAndroidX", true)
    }
}

tasks.register("stageAllReleaseAars") {
    dependsOn(":sdk:stageReleaseAars", ":applovin-adapter:stageReleaseAars")
    doLast {
        val sdkVersion = project(":sdk").extra["sdkVersion"] as String
        val adapterVersion = System.getenv("BidscubeAdapterVersion") ?: "1.2.10"
        val outputDir = layout.buildDirectory.dir("staged-aars").get().asFile
        outputDir.mkdirs()

        val sdkFiles = linkedMapOf(
            "bidscube-sdk-lite-no-video-$sdkVersion.aar" to
                project(":sdk").layout.buildDirectory.file("staged-aars/bidscube-sdk-lite-no-video-$sdkVersion.aar"),
            "bidscube-sdk-webview-video-$sdkVersion.aar" to
                project(":sdk").layout.buildDirectory.file("staged-aars/bidscube-sdk-webview-video-$sdkVersion.aar"),
            "bidscube-sdk-legacy-media-video-$sdkVersion.aar" to
                project(":sdk").layout.buildDirectory.file("staged-aars/bidscube-sdk-legacy-media-video-$sdkVersion.aar"),
            "bidscube-sdk-full-video-$sdkVersion.aar" to
                project(":sdk").layout.buildDirectory.file("staged-aars/bidscube-sdk-full-video-$sdkVersion.aar")
        )
        val adapterFiles = linkedMapOf(
            "applovin-bidscube-max-adapter-lite-no-video-$adapterVersion.aar" to
                project(":applovin-adapter").layout.buildDirectory.file("staged-aars/applovin-bidscube-max-adapter-lite-no-video-$adapterVersion.aar"),
            "applovin-bidscube-max-adapter-webview-video-$adapterVersion.aar" to
                project(":applovin-adapter").layout.buildDirectory.file("staged-aars/applovin-bidscube-max-adapter-webview-video-$adapterVersion.aar"),
            "applovin-bidscube-max-adapter-legacy-media-video-$adapterVersion.aar" to
                project(":applovin-adapter").layout.buildDirectory.file("staged-aars/applovin-bidscube-max-adapter-legacy-media-video-$adapterVersion.aar"),
            "applovin-bidscube-max-adapter-full-video-$adapterVersion.aar" to
                project(":applovin-adapter").layout.buildDirectory.file("staged-aars/applovin-bidscube-max-adapter-full-video-$adapterVersion.aar")
        )

        (sdkFiles + adapterFiles).forEach { (name, provider) ->
            val src = provider.get().asFile
            if (src.exists()) {
                src.copyTo(outputDir.resolve(name), overwrite = true)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Test app (module :bidscube-testapp-android, sibling ../bidscube-testapp-android)
// Open THIS repository root in Android Studio / Cursor and run these tasks.
// See docs/test-app.md
// ---------------------------------------------------------------------------

val testAppProject = ":bidscube-testapp-android"
val hasTestApp = project.findProject(testAppProject) != null

tasks.register("assembleTestApp") {
    group = "test app"
    description = "Build debug APK for the local SDK test application"
    if (hasTestApp) {
        dependsOn("$testAppProject:assembleDebug")
    } else {
        doLast {
            throw GradleException(
                "Test app not found. Clone bidscube-testapp-android next to this repo:\n" +
                    "  android/bidscube-testapp-android\n" +
                    "  android/AppLovin-SDK-for-BidsCube-Android/"
            )
        }
    }
}

tasks.register("installTestApp") {
    group = "test app"
    description = "Install debug test application on a connected device/emulator"
    if (hasTestApp) {
        dependsOn("$testAppProject:installDebug")
    } else {
        doLast {
            throw GradleException(
                "Test app not found. See docs/test-app.md"
            )
        }
    }
}

tasks.register("runTestApp") {
    group = "test app"
    description = "Install test app and launch MainActivity (requires adb)"
    if (hasTestApp) {
        dependsOn("installTestApp")
        doLast {
            exec {
                commandLine(
                    "adb", "shell", "am", "start",
                    "-n", "com.bidscube.testapp/com.bidscubeExample.testApp.MainActivity"
                )
            }
        }
    } else {
        doLast {
            throw GradleException("Test app not found. See docs/test-app.md")
        }
    }
}

