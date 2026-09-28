// Top-level build file. Plugin versions live in gradle/libs.versions.toml and
// are declared by the modules that apply them: the Kotlin Android plugin has
// to be loaded alongside the Android Gradle plugin, and :core must stay
// buildable on machines without access to Google's Maven repository
// (pass -Pbaking.coreOnly).
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
