plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    val ephemerisBaseUrl = project.findProperty("EPHEMERIS_BASE_URL")?.toString() ?: ""

    buildFeatures {
        buildConfig = true
    }

    namespace = "com.example.ghansarvatobhadra"
    compileSdk = 35

    defaultConfig {
        buildConfigField(
            "String",
            "EPHEMERIS_BASE_URL",
            "\"${ephemerisBaseUrl.replace("\"", "\\\"")}\""
        )
        applicationId = "com.example.ghansarvatobhadra"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // PDF export (iText7 — free for non-commercial, AGPL for commercial)
    implementation("com.itextpdf:itext7-core:7.2.5")
}
