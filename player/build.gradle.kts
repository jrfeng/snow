plugins {
    id("com.android.library")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 16

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.6.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // media
    api("androidx.media:media:1.6.0")

    // ViewModel and LiveData
    api("androidx.lifecycle:lifecycle-extensions:2.2.0")

    // Guava
    implementation("com.google.guava:guava:${project.extra["guavaVersion"]}")

    // MMKV
    implementation("com.tencent:mmkv-static:${project.extra["mmkvVersion"]}")

    // RxJava
    implementation("io.reactivex.rxjava2:rxjava:${project.extra["rxjavaVersion"]}")
    implementation("io.reactivex.rxjava2:rxandroid:${project.extra["rxandroidVersion"]}")

    // Glide
    implementation("com.github.bumptech.glide:glide:${project.extra["glideVersion"]}")

    // channel-helper
    implementation("com.github.jrfeng.channel-helper:helper:1.2.8")
    implementation("com.github.jrfeng.channel-helper:pipe:1.2.8")
    annotationProcessor("com.github.jrfeng.channel-helper:processor:1.2.8")

    // media-helper
    implementation("com.github.jrfeng:media-helper:${project.extra["mediaHelperVersion"]}")
}
