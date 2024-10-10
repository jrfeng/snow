plugins {
    id("com.android.application")
    id("io.objectbox")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "snow.music"
        minSdk = 16
        targetSdk = 34
        versionCode = 5
        versionName = "1.2.8"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.android.support:multidex:1.0.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.objectbox:objectbox-linux:${project.extra["objectboxVersion"]}")
    testImplementation("io.objectbox:objectbox-macos:${project.extra["objectboxVersion"]}")
    testImplementation("io.objectbox:objectbox-windows:${project.extra["objectboxVersion"]}")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(project(path = ":player"))
    implementation(project(path = ":ui"))

    // media-helper
    implementation("com.github.jrfeng:media-helper:${project.extra["mediaHelperVersion"]}")

    // Guava
    implementation("com.google.guava:guava:${project.extra["guavaVersion"]}")

    // RxJava2
    implementation("io.reactivex.rxjava2:rxjava:${project.extra["rxjavaVersion"]}")
    implementation("io.reactivex.rxjava2:rxandroid:${project.extra["rxandroidVersion"]}")

    // MMKV
    implementation("com.tencent:mmkv-static:${project.extra["mmkvVersion"]}")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:${project.extra["glideVersion"]}")
    annotationProcessor("com.github.bumptech.glide:compiler:${project.extra["glideVersion"]}")

    // rv-helper
    implementation("com.github.jrfeng:rv-helper:1.2.2")

    // pinyin-comparator
    implementation("com.github.jrfeng:pinyin-comparator:1.0.3")
}
