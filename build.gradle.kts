// Top-level build file. All Gradle plugins are put on the root classpath here
// so that Kotlin and Android plugins share one classloader (the Kotlin plugin
// breaks when loaded separately per module). Modules apply them by id only.
// Pass -Pbaking.coreOnly to build :core on a machine without access to
// Google's Maven repository; the Android plugin is then left out.
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.kotlin.serialization.plugin)
        classpath(libs.kotlin.compose.plugin)
        if (!providers.gradleProperty("baking.coreOnly").isPresent) {
            classpath(libs.android.gradle.plugin)
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
