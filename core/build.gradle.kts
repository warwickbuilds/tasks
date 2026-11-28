import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api("org.commonmark:commonmark:0.24.0")
    api("org.commonmark:commonmark-ext-task-list-items:0.24.0")
    api("org.commonmark:commonmark-ext-gfm-strikethrough:0.24.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    // NetworkGuardTest reads the app's sources to prove there is only one network call.
    systemProperty("tasks.appSources", rootProject.file("app/src/main").absolutePath)
}
