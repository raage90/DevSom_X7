// Top-level build file. Nothing project-specific goes here --
// per-module config lives in app/build.gradle.kts
//
// Versions below are confirmed real and working as of Aug 2026, pulled
// directly from a fresh Android Studio project on this same machine --
// not guessed. The previous AGP 8.5.2 was simply too old for the current
// Android SDK/build tools, which is what caused the resource-linking
// errors (AAPT2 mismatch), not a bug in the app's own code.
plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
}
