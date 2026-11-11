/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.openaudio.player"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndkVersion.get()

    defaultConfig {
        applicationId = "org.openaudio.player"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0-alpha01"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    // Android 16 (API 36) 16 KB page-size uncompressed native shared library alignment
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:preferences"))

    implementation(project(":audio:decoder"))
    implementation(project(":audio:engine"))
    implementation(project(":audio:dsp"))
    implementation(project(":audio:verification"))

    implementation(project(":usb:detection"))
    implementation(project(":usb:descriptors"))
    implementation(project(":usb:uac"))
    implementation(project(":usb:transfer"))

    implementation(project(":player:session"))
    implementation(project(":player:queue"))
    implementation(project(":player:library"))

    implementation(project(":data:local"))
    implementation(project(":data:repository"))

    implementation(project(":feature:home"))
    implementation(project(":feature:library"))
    implementation(project(":feature:player"))
    implementation(project(":feature:dac"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:equalizer"))

    implementation(project(":native:usb"))
    implementation(project(":native:audio"))
    implementation(project(":native:decoder"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
}
