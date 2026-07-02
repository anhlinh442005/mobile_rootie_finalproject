import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.veganbeauty.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.veganbeauty.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load GEMINI_API_KEY from local.properties
        val localProperties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            FileInputStream(localPropertiesFile).use { localProperties.load(it) }
        }
        val geminiApiKey = localProperties.getProperty("gemini.api.key") ?: "YOUR_GEMINI_API_KEY_HERE"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        val waqiApiKey = localProperties.getProperty("waqi.api.key") ?: "YOUR_WAQI_API_KEY_HERE"
        buildConfigField("String", "WAQI_API_KEY", "\"$waqiApiKey\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            pickFirsts.add("META-INF/NOTICE.md")
            pickFirsts.add("META-INF/LICENSE.md")
            pickFirsts.add("META-INF/DEPENDENCIES")
            pickFirsts.add("META-INF/LICENSE")
            pickFirsts.add("META-INF/NOTICE")
            pickFirsts.add("META-INF/license.txt")
            pickFirsts.add("META-INF/notice.txt")
            pickFirsts.add("META-INF/ASL2.0")
            pickFirsts.add("META-INF/*.kotlin_module")
        }
    }
}

kotlin {
    jvmToolchain(17)
}


ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.viewpager2)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Coil
    implementation(libs.coil)
    implementation(libs.coil.svg)
    
    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.guava)

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Chart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.sun.mail:android-mail:1.6.7")

    implementation("com.google.android.flexbox:flexbox:3.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
dependencies { implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21") }
