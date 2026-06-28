import java.util.Properties

// App plugins
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Config loader
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun cfg(key: String, default: String): String =
    "\"${localProps.getProperty(key) ?: System.getenv(key) ?: default}\""

android {
    namespace = "com.orbit.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.orbit.mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Backend config
        buildConfigField("String", "BASE_URL", cfg("BASE_URL", "http://10.0.2.2:8000/api/v1/"))
        buildConfigField("String", "WS_BASE_URL", cfg("WS_BASE_URL", "ws://10.0.2.2:8000"))
        // SSL pin
        buildConfigField("String", "PIN_HOST", cfg("PIN_HOST", "10.0.2.2"))
        buildConfigField("String", "PIN_SHA256", cfg("PIN_SHA256", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="))
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Emulator loopback — overrides local.properties for Debug only
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/api/v1/\"")
            buildConfigField("String", "WS_BASE_URL", "\"ws://10.0.2.2:8000\"")
            buildConfigField("String", "PIN_HOST", "\"10.0.2.2\"")
            // Dev cleartext
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            // Obfuscate + shrink
            isMinifyEnabled = true
            isShrinkResources = true
            // Enforce HTTPS
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Lint config
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Storage
    implementation(libs.androidx.datastore.preferences)

    // Images
    implementation(libs.coil.compose)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
