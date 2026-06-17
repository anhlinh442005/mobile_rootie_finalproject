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
    }

    buildFeatures {
        viewBinding = true
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
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Coil
    implementation(libs.coil)
    implementation(libs.coil.svg)
    implementation(libs.guava)

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Chart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
dependencies { implementation("com.github.yalantis:ucrop:2.2.8") }
dependencies { implementation("com.google.firebase:firebase-database-ktx") }
