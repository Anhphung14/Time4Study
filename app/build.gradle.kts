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
    implementation(libs.firebase.crashlytics.buildtools)
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

    // Thêm Glide dependency
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation("com.airbnb.android:lottie:6.6.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Android UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-firestore:24.10.2")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-messaging:23.4.0")

    // Gemini AI & Markdown
    implementation("com.google.ai.client.generativeai:generativeai:0.2.0")
    implementation("io.noties.markwon:core:4.6.2")


    // Kiểm thử
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Dui dui
    implementation("androidx.core:core-splashscreen:1.0.1")

    // https://mvnrepository.com/artifact/pl.droidsonroids.gif/android-gif-drawable
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.27")
    implementation("com.getbase:floatingactionbutton:1.10.1")
    implementation ("com.wdullaer:materialdatetimepicker:4.2.3")

    // Login with Google
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}

apply(plugin = "com.google.gms.google-services")  // Apply the Google services plugin
