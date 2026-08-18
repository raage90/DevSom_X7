plugins {
    id("com.android.application")
    // NOTE: no explicit Kotlin plugin here. AGP 9.3.1 provides Kotlin
    // support built-in -- applying org.jetbrains.kotlin.android on top of
    // that caused "Cannot add extension with name 'kotlin', as there is
    // an extension already registered" (a real conflict, not a typo).
    // The version is still declared (apply false) in the root
    // build.gradle.kts so it's available if this ever needs to change.
}

android {
    namespace = "com.galcad.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.galcad.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Request-signing secret (Layer 2 security, upgraded). Used to
        // HMAC-sign every request along with a timestamp + one-time nonce,
        // so a captured request can't just be replayed like a static key
        // could. Injected at build time from a GitHub Actions Secret --
        // never committed to source control.
        buildConfigField("String", "APP_SIGNING_SECRET", "\"${System.getenv("APP_SIGNING_SECRET")?.takeIf { it.isNotBlank() } ?: ""}\"")
        // Your real Railway backend. Swappable later via the resolver system
        // without needing a new APK build.
        buildConfigField("String", "FALLBACK_API_URL", "\"${System.getenv("FALLBACK_API_URL")?.takeIf { it.isNotBlank() } ?: "https://resourceful-peace-production.up.railway.app/"}\"")
    }

    // Signing is configured via GitHub Actions Secrets, not hardcoded here.
    // See .github/workflows/build-apk.yml
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // NOTE: the old kotlinOptions { jvmTarget = "17" } block was removed --
    // that DSL belongs to the separate Kotlin Gradle plugin we're no longer
    // applying explicitly. compileOptions above covers the Java/JVM target.

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Video/audio playback (HLS support)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-datasource:1.4.1")
    implementation("androidx.media3:media3-database:1.4.1")

    // Image loading (thumbnails, news photos)
    implementation("io.coil-kt:coil:2.6.0")

    // Local offline cache (Room database) -- not yet implemented in code,
    // so removed for now rather than carrying an untested dependency.
    // Re-add when we build the offline caching feature.

    testImplementation("junit:junit:4.13.2")
}
