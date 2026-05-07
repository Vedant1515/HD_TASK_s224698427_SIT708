plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.nutriai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nutriai"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        jniLibs {
            // Workaround: libexecutorch.so is a pre-built library without 16 KB ELF LOAD
            // alignment. extractNativeLibs=true makes the installer copy .so files to disk
            // rather than memory-mapping them from the APK, satisfying Android 15+ compatibility.
            // Remove once ExecuTorch ships a 16 KB-aligned AAR.
            useLegacyPackaging = true
        }
    }

}

dependencies {
    // ExecuTorch runtime AAR (app/libs/executorch.aar)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    // Required transitive deps for ExecuTorch JNI layer
    implementation("com.facebook.soloader:soloader:0.10.5")
    implementation("com.facebook.fbjni:fbjni:0.7.0")

    // UI
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Navigation (2.7.x — last fully Java-compatible release)
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // ViewModel + LiveData (2.7.x — pre-KMP, works with pure Java)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    // Room DB (2.6.x — stable Java-compatible release)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Activity (1.9.x — predictive back, Java compatible)
    implementation("androidx.activity:activity:1.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
