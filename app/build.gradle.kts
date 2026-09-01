plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.atlas.semaforo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.atlas.semaforo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1900
        versionName = "0.19"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
