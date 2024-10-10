// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.objectbox.gradle.plugin)
    }
}

allprojects {
    extra["publishGroupId"] = "snow.player"
    extra["publishVersion"] = "1.2.13"
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}