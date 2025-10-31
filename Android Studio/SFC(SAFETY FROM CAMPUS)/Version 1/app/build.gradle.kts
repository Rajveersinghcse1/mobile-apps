plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.imageanalysis"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.imageanalysis"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
    }
    aaptOptions {
        noCompress.add("tflite")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    
    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
    
    // CameraX dependencies
    val cameraxVersion = "1.5.1"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    
    // Guava for ListenableFuture
    implementation("com.google.guava:guava:33.5.0-android")
    
    // ML Kit for Image Labeling
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // ML Kit for Face Detection
    implementation("com.google.mlkit:face-detection:16.1.7")
    
    // iText PDF for PDF generation
    implementation("com.itextpdf:itext7-core:9.3.0")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.13.2")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}