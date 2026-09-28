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
    }
}

rootProject.name = "Baking"
include(":core")
// Pass -Pbaking.coreOnly to skip the Android module (e.g. CI without the Android SDK).
if (!providers.gradleProperty("baking.coreOnly").isPresent) {
    include(":app")
}
