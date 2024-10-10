plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "snow.player.exo.api16"

    compileSdk = 34

    defaultConfig {
        minSdk = 16

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
    implementation(libs.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(path = ":player"))

    // exoplayer-core
    api(libs.exoplayer.core.legacy)

    // extension-okhttp
    api(libs.exoplayer.extension.okhttp.legacy)
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("maven") {
                groupId = project.extra["publishGroupId"] as String
                artifactId = "exo-api16"
                version = project.extra["publishVersion"] as String

                from(components["release"])
            }
        }
    }
}