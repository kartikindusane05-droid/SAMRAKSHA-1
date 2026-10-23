import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}
val geoapifyApiKey: String = localProperties.getProperty("GEOAPIFY_API_KEY")
    ?: System.getenv("GEOAPIFY_API_KEY")
    ?: ""

val devBaseUrl: String = localProperties.getProperty("DEV_BASE_URL")
    ?: System.getenv("DEV_BASE_URL")
    ?: "http://10.122.145.245:5000/"

val prodBaseUrl: String = localProperties.getProperty("PROD_BASE_URL")
    ?: System.getenv("PROD_BASE_URL")
    ?: "https://samraksha-backend.onrender.com/"

android {
    namespace = "com.samraksha.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.samraksha.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "GEOAPIFY_API_KEY", "\"$geoapifyApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keyStorePath = localProperties.getProperty("RELEASE_KEYSTORE_PATH")
                ?: System.getenv("RELEASE_KEYSTORE_PATH")
            if (!keyStorePath.isNullOrBlank() && file(keyStorePath).exists()) {
                storeFile = file(keyStorePath)
                storePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
                    ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: ""
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                    ?: ""
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
                    ?: ""
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
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")

            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            } else {
                // Fall back to debug signing for testing the release build APK on physical devices
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM — manages all Compose library versions (aligned with maps-compose:6.4.1)
    val composeBom = platform("androidx.compose:compose-bom:2024.11.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Jetpack Compose — Material 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")

    // Compose — Activity integration
    implementation("androidx.activity:activity-compose:1.9.1")

    // Core & Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Navigation — Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room Database
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Networking — Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Location Services (Fused Location Provider)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // MapLibre Maps SDK for Android
    implementation("org.maplibre.gl:android-sdk:11.8.2")
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.2")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

tasks.withType<Test>().configureEach {
    val javaHome = project.findProperty("org.gradle.java.home") as? String ?: "C:/Users/karti/.jdks/jbr-21.0.11"
    val javaBin = file("$javaHome/bin/java.exe")
    if (javaBin.exists()) {
        executable = javaBin.absolutePath
    }
}

