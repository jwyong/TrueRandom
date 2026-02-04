import java.io.FileInputStream
import java.util.Properties

// Apply plugins
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    kotlin("plugin.serialization")
}
android.buildFeatures.buildConfig = true

// 1. Load the properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.truerandom"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.truerandom"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["redirectSchemeName"] = "truerandom" // The schemer
        manifestPlaceholders["redirectHostName"] = "callback"          // The host

        buildConfigField(
            "String",
            "SPOTIFY_CLIENT_ID",
            "\"${localProperties.getProperty("SPOTIFY_CLIENT_ID")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_API_KEY",
            "\"${localProperties.getProperty("SUPABASE_API_KEY")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_PROJECT_URL",
            "\"${localProperties.getProperty("SUPABASE_PROJECT_URL")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.paging.common.android)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    implementation(files("libs/app-remote-lib.aar"))
    implementation(files("libs/auth.aar"))

    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    implementation(libs.androidx.browser)
    implementation(libs.google.gson)

    implementation(libs.androidx.room.runtime) // Core Room functions
    implementation(libs.androidx.room.ktx)    // Coroutines extensions for Room
    kapt(libs.androidx.room.compiler)         // Annotation Processor for Room
    implementation(libs.androidx.room.paging)

    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation)
    implementation(libs.hilt.android)

    implementation(libs.security.crypto)

    implementation("androidx.media:media:1.7.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // supabase
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.0.1")
    implementation("io.ktor:ktor-client-android:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
}

kapt {
    correctErrorTypes = true
}