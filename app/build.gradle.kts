plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.hanzdev.susunblock"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.hanzdev.susunblock"
        minSdk = 24
        targetSdk = 34
        // Diisi otomatis oleh GitHub Actions (-PappVersionName / -PappVersionCode)
        versionCode = (findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (findProperty("appVersionName") as String?) ?: "1.0.0"
    }

    signingConfigs {
        // Keystore rilis opsional, dibaca dari environment variable (lihat README).
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (!keystorePath.isNullOrEmpty() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            // Tanpa keystore rilis, APK ditandatangani dengan debug key agar tetap bisa dipasang.
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
