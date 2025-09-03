plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "snow.player.exo"

    compileSdk = 36

    defaultConfig {
        minSdk = 21

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

    implementation(project(path = ":player"))

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // media3 exoplayer
    api(libs.media3.exoplayer)
    api(libs.media3.exoplayer.dash)
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("maven") {
                groupId = project.extra["publishGroupId"] as String
                artifactId = "exo"
                version = project.extra["publishVersion"] as String

                from(components["release"])
            }
        }
    }
}