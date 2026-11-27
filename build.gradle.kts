buildscript {
    // The Android Gradle plugin sits on the root classpath so the Kotlin plugins
    // below can see it. It is skipped with -PcoreOnly=true (no Android SDK needed).
    if (project.findProperty("coreOnly") != "true") {
        dependencies {
            classpath("com.android.tools.build:gradle:8.13.0")
        }
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
}
