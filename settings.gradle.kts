pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\.android.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroupByRegex("com\\.google\\.testing.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "tasks"

// `:core` is plain Kotlin (all the app's rules: grouping, #tag parsing, Markdown,
// backups, update checks) and builds without the Android SDK.
// `./gradlew -PcoreOnly=true :core:test` runs its tests on any machine with a JDK.
include(":core")
if (providers.gradleProperty("coreOnly").orNull != "true") {
    include(":app")
}
