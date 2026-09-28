// Top-level build file. Plugin versions live in gradle/libs.versions.toml.
// The Android plugin is applied only in :app so that :core can be built and
// tested on machines without access to Google's Maven repository.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
