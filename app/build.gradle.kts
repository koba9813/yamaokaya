plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "win.haya.yamaokaya"
    compileSdk = 35

    defaultConfig {
        applicationId = "win.haya.yamaokaya"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "V.beta1.2"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM — バージョンを一括管理
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Compose Material3
    implementation("androidx.compose.material3:material3")

    // Compose Foundation (layout, scroll, shapes, clickable, Image, etc.)
    implementation("androidx.compose.foundation:foundation")

    // Compose Animation
    implementation("androidx.compose.animation:animation")

    // Activity Compose (setContent, rememberLauncherForActivityResult)
    implementation("androidx.activity:activity-compose:1.9.0")

    // AndroidX Browser (CustomTabsIntent)
    implementation("androidx.browser:browser:1.8.0")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Material (XML views)
    implementation("com.google.android.material:material:1.12.0")
}

