import org.gradle.plugins.signing.SigningExtension

plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("signing")
}

// Published Maven version; also BuildConfig.SDK_VERSION_NAME at runtime.
val sdkVersionString = System.getenv("BidscubeVersion") ?: "1.0.3.1"
val sdkVersion by extra(sdkVersionString)

val skipSigning = (project.findProperty("skipSigning") as String?) == "true"

android {
    namespace = "com.bidscube.sdk"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION_NAME", "\"$sdkVersionString\"")
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
        isCoreLibraryDesugaringEnabled = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    val media3Version = "1.8.0"
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.ads.interactivemedia.v3:interactivemedia:3.37.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.bidscube"
                artifactId = "bidscube-sdk"
                version = extra["sdkVersion"] as String
                from(components["release"])
                pom {
                    name.set("Bidscube SDK")
                    description.set("The official Bidscube SDK for Android advertising platform")
                    url.set("https://github.com/BidsCube/bidscube-sdk")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/BidsCube/bidscube-sdk/blob/main/LICENSE")
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
                        connection.set("scm:git:git://github.com/BidsCube/bidscube-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/BidsCube/bidscube-sdk.git")
                        url.set("https://github.com/BidsCube/bidscube-sdk")
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
}

extensions.configure<SigningExtension>("signing") {
    useGpgCmd()
}

afterEvaluate {
    if (!skipSigning) {
        val pub = publishing.publications.findByName("release")
        if (pub != null) {
            extensions.getByType(SigningExtension::class.java).sign(pub)
        }
    }
    tasks.matching { it.name.startsWith("publish", ignoreCase = true) }.configureEach {
        dependsOn("assembleRelease")
    }
}
