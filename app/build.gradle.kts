plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("com.google.gms.google-services")
}

android {
    namespace = "com.example.biyahecebu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.biyahecebu"
        minSdk = 26
        targetSdk = 34
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
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }
}

dependencies {
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.play.services.location)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.volley)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("com.google.firebase:firebase-bom:33.8.0") // Use the latest version
    implementation ("com.google.firebase:firebase-firestore-ktx") // For Firestore
    implementation ("com.google.firebase:firebase-auth-ktx") // For authentication (optional)
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.6")
    implementation ("com.google.android.gms:play-services-maps:19.0.0") // Use the latest version
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.google.firebase:firebase-auth:22.0.0")
    implementation ("com.google.maps.android:android-maps-utils:2.2.3")
    implementation ("com.github.bumptech.glide:glide:4.15.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.0")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.android.libraries.places:places:3.4.0")
}

//client ID 817631651424-ubjiro9915jg316pdiu2d78msv433vp7.apps.googleusercontent.com
// implementation ('androidx.constraintlayout:constraintlayout:2.1.4')