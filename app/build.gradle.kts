plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.staples.trampolinepoc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.staples.trampolinepoc"
        // minSdk 23 to cover the Android 6.0+ range called out in the approach doc
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-poc"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<Test> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    // Chrome Custom Tabs — required for the Auth bucket
    implementation("androidx.browser:browser:1.8.0")

    // Unit tests for PathClassifier. Robolectric is used (not plain JUnit)
    // because PathClassifier parses android.net.Uri, which is a stub in the
    // plain Android framework jar and throws "not mocked" without it.
    // This still runs on your local JVM — no emulator required.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
