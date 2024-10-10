plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "snow.player.ui"

    compileSdk = 35

    defaultConfig {
        minSdk = 16

        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    dataBinding {
        enable = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.lifecycle.extensions)
    implementation(libs.lifecycle.runtime)

    api(project(path = ":player"))

    // MPAndroidChart
    implementation(libs.mpAndroidChart)

    // android-verticalseekbar
    implementation(libs.verticalseekbar)

    // Croller
    implementation(libs.croller)
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("maven") {
                groupId = project.extra["publishGroupId"] as String
                artifactId = "ui"
                version = project.extra["publishVersion"] as String

                from(components["release"])
            }
        }
    }
}