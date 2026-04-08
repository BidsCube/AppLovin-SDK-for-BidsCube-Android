import groovy.util.Node
import org.gradle.plugins.signing.SigningExtension

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

val skipSigning = (project.findProperty("skipSigning") as String?) == "true"

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
    api(project(":sdk"))
    // 13.0.x uses com.applovin.mediation.adapter (+ .parameters / .listeners). Newer SDKs use com.applovin.mediation.adapters — see BidscubeMediationAdapter imports when bumping.
    implementation("com.applovin:applovin-sdk:13.0.0@aar")
    implementation("androidx.annotation:annotation:1.8.2")
}

val adapterVersion = System.getenv("BidscubeAdapterVersion") ?: "1.0.3"

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

                        withXml {
                            val sdkVer = project(":sdk").extra["sdkVersion"] as String
                            val root = asNode() as Node
                            val depsContainer = root.get("dependencies")
                            val deps: Node = when (depsContainer) {
                                is java.util.List<*> ->
                                    if (depsContainer.isNotEmpty()) depsContainer[0] as Node
                                    else root.appendNode("dependencies")
                                else -> root.appendNode("dependencies")
                            }
                            var hasBidscubeSdk = false
                            for (c in deps.children()) {
                                val n = c as? Node ?: continue
                                if (n.name().toString() != "dependency") continue
                                val aidRaw = n.get("artifactId")
                                val artifactIdText = when (aidRaw) {
                                    is java.util.List<*> ->
                                        if (aidRaw.isNotEmpty()) (aidRaw[0] as Node).text() else null
                                    else -> null
                                }
                                if (artifactIdText == "bidscube-sdk") {
                                    hasBidscubeSdk = true
                                    break
                                }
                            }
                            if (!hasBidscubeSdk) {
                                val d = deps.appendNode("dependency")
                                d.appendNode("groupId", "com.bidscube")
                                d.appendNode("artifactId", "bidscube-sdk")
                                d.appendNode("version", sdkVer)
                                d.appendNode("type", "aar")
                                d.appendNode("scope", "compile")
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
    }
}

extensions.configure<SigningExtension>("signing") {
    useGpgCmd()
}

afterEvaluate {
    if (!skipSigning) {
        val pub = publishing.publications.findByName("release") ?: return@afterEvaluate
        extensions.getByType(SigningExtension::class.java).sign(pub)
    }
}
