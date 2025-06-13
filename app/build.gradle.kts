plugins {
    alias(libs.plugins.android.application)
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

    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")       // Firebase Authentication
    implementation("com.google.firebase:firebase-firestore")  // Firebase Firestore
    implementation("com.google.firebase:firebase-database")   // Firebase Realtime Database
    implementation("com.google.firebase:firebase-messaging")  // Firebase Cloud Messaging

    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    // https://mvnrepository.com/artifact/com.github.bumptech.glide/glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("androidx.core:core-splashscreen:1.0.1")

    // https://mvnrepository.com/artifact/pl.droidsonroids.gif/android-gif-drawable
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.27")

    implementation("androidx.core:core-splashscreen:1.0.0-beta02")
    implementation("com.getbase:floatingactionbutton:1.10.1")
    implementation ("com.wdullaer:materialdatetimepicker:4.2.3")
}

apply(plugin = "com.google.gms.google-services")  // Apply the Google services plugin
