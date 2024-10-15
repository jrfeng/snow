plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "snow.player"

    compileSdk = 35

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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // media
    api(libs.androidx.media)

    // ViewModel and LiveData
    api(libs.lifecycle.extensions)

    // Guava
    implementation(libs.guava)

    // MMKV
    implementation(libs.mmkv)

    // RxJava
    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    // Glide
    implementation(libs.glide)

    // channel-helper
    implementation(libs.channel.helper)
    implementation(libs.channel.helper.pip)
    annotationProcessor(libs.channel.helper.processor)

    // media-helper
    implementation(libs.mediaHelper)
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("maven") {
                groupId = project.extra["publishGroupId"] as String
                artifactId = "player"
                version = project.extra["publishVersion"] as String

                from(components["release"])
            }
        }
    }
}