// Set AndroidX before any project build script runs (CI often has no gradle.properties)
gradle.beforeProject { proj ->
    if (!proj.hasProperty("android.useAndroidX")) {
        proj.ext.set("android.useAndroidX", true)
    }
}

plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.android.library") version "8.9.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

