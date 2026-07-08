pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "bidscube-sdk"
include(":sdk")
include(":applovin-adapter")
val testAppDir = file("../bidscube-testapp-android")
if (testAppDir.exists()) {
    include(":bidscube-testapp-android")
    project(":bidscube-testapp-android").projectDir = testAppDir
}
