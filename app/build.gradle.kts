plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "win.haya.doko"
    compileSdk = 35

    defaultConfig {
        applicationId = "win.haya.doko"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "V.beta3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing secrets are read from environment variables so they are never committed.
    // Set these before building a release APK:
    //   RELEASE_KEYSTORE_PATH
    //   RELEASE_KEYSTORE_PASSWORD
    //   RELEASE_KEY_ALIAS
    //   RELEASE_KEY_PASSWORD
    val releaseKeystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
    val releaseStorePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
    val canSignRelease = releaseKeystorePath != null &&
            releaseStorePassword != null &&
            releaseKeyAlias != null &&
            releaseKeyPassword != null

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    testOptions {
        // Robolectric（ユニットテスト）から Android のリソースとマージ済みマニフェストを参照できるようにする。
        // Compose のスクリーンショット確認（UpdateFlowScreenshotTest）で必要。
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Compose BOM — バージョンを一括管理
    val composeBomVersion = "2024.05.00"
    val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Compose Material3
    implementation("androidx.compose.material3:material3")

    // Compose Material Icons Extended（AddAPhoto 等）
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Foundation (layout, scroll, shapes, clickable, Image, etc.)
    implementation("androidx.compose.foundation:foundation")

    // Compose Animation
    implementation("androidx.compose.animation:animation")

    // CameraX
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Coil (image loading for gallery thumbnails)
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Activity Compose (setContent, rememberLauncherForActivityResult)
    implementation("androidx.activity:activity-compose:1.9.0")

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // AndroidX Browser (CustomTabsIntent)
    implementation("androidx.browser:browser:1.8.0")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Material (XML views)
    implementation("com.google.android.material:material:1.12.0")

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // ダイアログのスクリーンショット確認用（UpdateFlowScreenshotTest）
    testImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

