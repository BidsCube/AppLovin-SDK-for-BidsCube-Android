plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.applovin.mediation.adapters.bidscube"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation(project(":sdk"))
    implementation("com.applovin:applovin-sdk:13.6.0@aar")
}

val adapterVersion = System.getenv("BidscubeAdapterVersion") ?: "1.0.0"

afterEvaluate {
    val releaseComponent = components.findByName("release")
    if (releaseComponent != null) {
        publishing {
            publications {
                create<MavenPublication>("release") {
                    groupId = "com.bidscube"
                    artifactId = "applovin-bidscube-adapter"
                    version = adapterVersion

                    from(releaseComponent)

                pom {
                    name.set("AppLovin Bidscube Adapter")
                    description.set("AppLovin MAX mediation adapter for Bidscube SDK. Includes Bidscube SDK transitively.")
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
}

signing {
    useGpgCmd()
}

afterEvaluate {
    publishing.publications.findByName("release")?.let { signing.sign(it) }
}
