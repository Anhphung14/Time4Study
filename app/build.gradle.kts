plugins {
    alias(libs.plugins.android.application)
   /* id("com.android.application")*/
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.time4study"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.time4study"
        minSdk = 24
        targetSdk = 35
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.google.android.gms:play-services-auth:21.0.0")
    implementation ("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    /*kapt("com.github.bumptech.glide:compiler:4.16.0")*/

    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")       // Firebase Authentication
    implementation("com.google.firebase:firebase-firestore")  // Firebase Firestore
    implementation("com.google.firebase:firebase-database")   // Firebase Realtime Database
    implementation("com.google.firebase:firebase-messaging")  // Firebase Cloud Messaging

    implementation("com.google.firebase:firebase-storage:20.3.0")
}

apply(plugin = "com.google.gms.google-services")  // Apply the Google services plugin
