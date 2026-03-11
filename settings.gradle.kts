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
//comment to ignore test app
//include(":bidscube-testapp-android")
