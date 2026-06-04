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

    flavorDimensions += "videoMode"

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
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
    add("liteNoVideoApi", project(":sdk"))
    add("webViewVideoApi", project(":sdk"))
    add("legacyMediaVideoApi", project(":sdk"))
    add("fullVideoApi", project(":sdk"))
    // 13.0.x uses com.applovin.mediation.adapter (+ .parameters / .listeners). Newer SDKs use com.applovin.mediation.adapters — see BidscubeMediationAdapter imports when bumping.
    implementation("com.applovin:applovin-sdk:13.0.0@aar")
    implementation("androidx.annotation:annotation:1.8.2")
}

val adapterVersion = System.getenv("BidscubeAdapterVersion") ?: "1.2.7"

afterEvaluate {
    val flavorPublicationConfig = linkedMapOf(
        "liteNoVideoRelease" to Triple("applovin-bidscube-max-adapter-lite-no-video", "sdk-lite-no-video", "LiteNoVideo"),
        "webViewVideoRelease" to Triple("applovin-bidscube-max-adapter-webview-video", "sdk-webview-video", "WebViewVideo"),
        "legacyMediaVideoRelease" to Triple("applovin-bidscube-max-adapter-legacy-media-video", "sdk-legacy-media-video", "LegacyMediaVideo"),
        "fullVideoRelease" to Triple("applovin-bidscube-max-adapter-full-video", "sdk-full-video", "FullVideo")
    )

    publishing {
        publications {
            flavorPublicationConfig.forEach { (componentName, config) ->
                val (adapterArtifactId, sdkArtifactId, flavorTaskPrefix) = config
                create<MavenPublication>(componentName) {
                    groupId = "com.bidscube"
                    artifactId = adapterArtifactId
                    version = adapterVersion
                    artifact(tasks.named("bundle${flavorTaskPrefix}ReleaseAar"))

                    pom {
                        name.set("AppLovin Bidscube MAX Adapter")
                        description.set("AppLovin MAX mediation adapter for Bidscube SDK. Includes matching Bidscube SDK artifact transitively.")
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

                            val toRemove = mutableListOf<Node>()
                            for (c in deps.children()) {
                                val n = c as? Node ?: continue
                                if (n.name().toString() != "dependency") continue
                                val aidRaw = n.get("artifactId")
                                val artifactIdText = when (aidRaw) {
                                    is java.util.List<*> ->
                                        if (aidRaw.isNotEmpty()) (aidRaw[0] as Node).text() else null
                                    else -> null
                                }
                                if (artifactIdText?.startsWith("sdk-") == true || artifactIdText == "bidscube-sdk") {
                                    toRemove.add(n)
                                }
                            }
                            toRemove.forEach { deps.remove(it) }

                            val d = deps.appendNode("dependency")
                            d.appendNode("groupId", "com.bidscube")
                            d.appendNode("artifactId", sdkArtifactId)
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
                "liteNoVideo" to "applovin-bidscube-max-adapter-lite-no-video-$adapterVersion.aar",
                "webViewVideo" to "applovin-bidscube-max-adapter-webview-video-$adapterVersion.aar",
                "legacyMediaVideo" to "applovin-bidscube-max-adapter-legacy-media-video-$adapterVersion.aar",
                "fullVideo" to "applovin-bidscube-max-adapter-full-video-$adapterVersion.aar"
            )
            mappings.forEach { (flavor, outName) ->
                val src = layout.buildDirectory.file("outputs/aar/applovin-adapter-$flavor-release.aar").get().asFile
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
