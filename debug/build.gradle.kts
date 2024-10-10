plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "snow.player.debug"

    compileSdk = 35

    defaultConfig {
        applicationId = "snow.player.debug"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    dataBinding {
        enable = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)

    implementation(project(path = ":player"))
    implementation(project(path = ":exo"))
    implementation(project(path = ":ui"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.multidex)

    implementation(libs.permissionx)
}
