import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.ut2_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ut2_app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val props = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { props.load(it) }
        }

        buildConfigField("String", "SUPABASE_URL", "\"${props.getProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${props.getProperty("SUPABASE_ANON_KEY")}\"")
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("com.google.android.material:material:1.12.0")

    // Compose
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")

    // KoalaPlot
    implementation("io.github.koalaplot:koalaplot-core:0.6.2")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.datastore.core)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)



//    implementation("io.github.jan-tennert.supabase:bom:3.1.1")
//    implementation("io.github.jan-tennert.supabase:auth-kt")
//    // Core client
//    implementation("io.github.jan-tennert.supabase:supabase-kt")
//
//    // Feature modules
//    implementation("io.github.jan-tennert.supabase:postgrest-kt")
//    implementation("io.github.jan-tennert.supabase:storage-kt")
//
//    // Ktor client and serialization (your existing versions look fine)
//    implementation("io.ktor:ktor-client-core:2.3.12")
//    implementation("io.ktor:ktor-client-android:2.3.12")
//    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
//    implementation("io.ktor:ktor-client-okhttp:3.0.3")
//
//    // Ensure compatible serialization library
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.6"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    implementation("io.coil-kt:coil:2.6.0")


    implementation("io.ktor:ktor-client-android:3.3.3")


}