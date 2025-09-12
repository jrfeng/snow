plugins {
    alias(libs.plugins.android.application)
    id("io.objectbox")
}

android {
    namespace = "snow.music"

    compileSdk = 36

    defaultConfig {
        applicationId = "snow.music"
        minSdk = 21
        targetSdk = 36
        versionCode = 8
        versionName = "1.2.16"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

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
    implementation(libs.multidex)

    testImplementation(libs.junit)
    testImplementation(libs.objectbox.test.linux)
    testImplementation(libs.objectbox.test.macos)
    testImplementation(libs.objectbox.test.windows)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(path = ":player"))
    implementation(project(path = ":ui"))

    // media-helper
    implementation(libs.mediaHelper)

    // Guava
    implementation(libs.guava)

    // RxJava2
    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    // MMKV
    implementation(libs.mmkv)

    // RecyclerView
    implementation(libs.recyclerview)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // rv-helper
    implementation(libs.rvhelper)

    // pinyin-comparator
    implementation(libs.pinyinComparator)
}
