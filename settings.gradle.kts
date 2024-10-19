@file:Suppress("UnstableApiUsage")

rootProject.name = "timber-multiplatform"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupByRegex(".*android.*")
                includeGroupByRegex(".*google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupByRegex(".*android.*")
                includeGroupByRegex(".*google.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
