plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.omesh.glyphtoys"

    // 37, not 36: the androidx artifacts refuse to be consumed below it. targetSdk stays at 36,
    // which is what Play requires — compiling against newer APIs is independent of opting in to
    // newer runtime behaviour.
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.omesh.glyphtoys"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Nothing Glyph Matrix SDK 2.0 — vendored; Nothing publishes no Maven artifact.
    //
    // That is the whole runtime dependency list. The toys are framework classes and the drawing
    // core is pure Kotlin, so there is no androidx here at all.
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}
