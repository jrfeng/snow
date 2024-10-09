// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra["objectboxVersion"] = "3.1.2"
    
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.1.2")

        // ObjectBox
        classpath("io.objectbox:objectbox-gradle-plugin:${project.extra["objectboxVersion"]}")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://jitpack.io")
        }
    }

    // Guava
    extra["guavaVersion"] = "29.0-android"

    // RxJava
    extra["rxjavaVersion"] = "2.2.19"
    extra["rxandroidVersion"] = "2.1.1"

    // media-helper
    extra["mediaHelperVersion"] = "1.1"

    // MMKV
    extra["mmkvVersion"] = "1.2.12"

    // Glide
    extra["glideVersion"] = "4.11.0"

    // Maven Gradle Publish
    extra["publishGroupId"] = "snow.player"
    extra["publishVersion"] = "1.1"

    extra["objectboxVersion"] = "3.1.2"
}