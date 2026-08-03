import org.gradle.plugins.signing.SigningExtension

plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("signing")
}

// Published Maven version; also BuildConfig.SDK_VERSION_NAME at runtime.
val sdkVersionString = System.getenv("BidscubeVersion") ?: "1.2.13"
val sdkVersion by extra(sdkVersionString)

val skipSigning = (project.findProperty("skipSigning") as String?) == "true"

android {
    namespace = "com.bidscube.sdk"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "videoMode"

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION_NAME", "\"$sdkVersionString\"")
    }

    productFlavors {
        create("liteNoVideo") {
            dimension = "videoMode"
        }
        create("webViewVideo") {
            dimension = "videoMode"
        }
        create("legacyMediaVideo") {
            dimension = "videoMode"
        }
        create("fullVideo") {
            dimension = "videoMode"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    publishing {
        singleVariant("liteNoVideoRelease") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("webViewVideoRelease") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("legacyMediaVideoRelease") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("fullVideoRelease") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")

    val media3Version = "1.8.0"
    add("fullVideoImplementation", "androidx.media3:media3-common:$media3Version")
    add("fullVideoImplementation", "androidx.media3:media3-ui:$media3Version")
    add("fullVideoImplementation", "com.google.ads.interactivemedia.v3:interactivemedia:3.37.0")
}

afterEvaluate {
    val flavorArtifactIds = linkedMapOf(
        "liteNoVideoRelease" to "sdk-lite-no-video",
        "webViewVideoRelease" to "sdk-webview-video",
        "legacyMediaVideoRelease" to "sdk-legacy-media-video",
        "fullVideoRelease" to "sdk-full-video"
    )

    publishing {
        publications {
            flavorArtifactIds.forEach { (componentName, artifactIdValue) ->
                val component = components.findByName(componentName) ?: return@forEach
                create<MavenPublication>(componentName) {
                    groupId = "com.bidscube"
                    artifactId = artifactIdValue
                    version = extra["sdkVersion"] as String
                    from(component)
                    pom {
                        name.set("Bidscube SDK")
                        description.set("The official Bidscube SDK for Android advertising platform")
                        url.set("https://github.com/BidsCube/AppLovin-SDK-for-BidsCube-Android")

                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://github.com/BidsCube/AppLovin-SDK-for-BidsCube-Android/blob/main/LICENSE")
                            }
                        }

                        developers {
                            developer {
                                id.set("bidscube-team")
                                name.set("Bidscube Team")
                                email.set("dev@bidscube.com")
                                organization.set("Bidscube")
                                organizationUrl.set("https://bidscube.com")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/BidsCube/AppLovin-SDK-for-BidsCube-Android.git")
                            developerConnection.set("scm:git:ssh://github.com/BidsCube/AppLovin-SDK-for-BidsCube-Android.git")
                            url.set("https://github.com/BidsCube/AppLovin-SDK-for-BidsCube-Android")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "central"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.findProperty("mavenCentralUsername") as String? ?: ""
                    password = project.findProperty("mavenCentralPassword") as String? ?: ""
                }
            }
        }
    }

    tasks.register("stageReleaseAars") {
        dependsOn(
            "assembleLiteNoVideoRelease",
            "assembleWebViewVideoRelease",
            "assembleLegacyMediaVideoRelease",
            "assembleFullVideoRelease"
        )
        doLast {
            val outputDir = layout.buildDirectory.dir("staged-aars").get().asFile
            outputDir.mkdirs()
            val mappings = linkedMapOf(
                "liteNoVideo" to "bidscube-sdk-lite-no-video-$sdkVersionString.aar",
                "webViewVideo" to "bidscube-sdk-webview-video-$sdkVersionString.aar",
                "legacyMediaVideo" to "bidscube-sdk-legacy-media-video-$sdkVersionString.aar",
                "fullVideo" to "bidscube-sdk-full-video-$sdkVersionString.aar"
            )
            mappings.forEach { (flavor, outName) ->
                val src = layout.buildDirectory.file("outputs/aar/sdk-$flavor-release.aar").get().asFile
                if (src.exists()) {
                    src.copyTo(outputDir.resolve(outName), overwrite = true)
                }
            }
        }
    }
}

extensions.configure<SigningExtension>("signing") {
    useGpgCmd()
}

afterEvaluate {
    if (!skipSigning) {
        publishing.publications.forEach { publication ->
            extensions.getByType(SigningExtension::class.java).sign(publication)
        }
    }
    tasks.matching { it.name.startsWith("publish", ignoreCase = true) }.configureEach {
        dependsOn("stageReleaseAars")
    }
}
