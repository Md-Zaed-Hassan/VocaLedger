plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.voicefinance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.voicefinance"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // --- DATABASE LIBRARIES ---
    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // To run database operations off the main thread
    implementation(libs.lifecycle.viewmodel)

    // This allows the UI to observe changes in the database
    implementation(libs.lifecycle.livedata)

    // --- *** NEW CHART LIBRARY *** ---
    implementation(libs.mpandroidchart)
}