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
        val adapterVersion = System.getenv("BidscubeAdapterVersion") ?: "1.2.7"
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

