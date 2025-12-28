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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // --- THIS IS THE NECESSARY CHANGE ---
        // This line adds the "JitPack" repository, which is where
        // the MPAndroidChart library is hosted.
        // --- THE TYPO 'httpsS' IS NOW FIXED ---
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "VoiceFinance"
include(":app")