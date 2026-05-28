import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}


val releaseKeystoreBase64 = providers
    .environmentVariable("EDUTRACK_KEYSTORE_BASE64")
    .orNull
val releaseKeystoreFile = layout.buildDirectory
    .file("signing/edutrack-release.jks")
    .get()
    .asFile
if (!releaseKeystoreBase64.isNullOrBlank()) {
    releaseKeystoreFile.parentFile.mkdirs()
    releaseKeystoreFile.writeBytes(
        Base64.getDecoder().decode(releaseKeystoreBase64)
    )
}
val hasReleaseSigning = releaseKeystoreFile.exists() &&
    !providers.environmentVariable("EDUTRACK_KEYSTORE_PASSWORD").orNull
        .isNullOrBlank() &&
    !providers.environmentVariable("EDUTRACK_KEY_ALIAS").orNull
        .isNullOrBlank() &&
    !providers.environmentVariable("EDUTRACK_KEY_PASSWORD").orNull
        .isNullOrBlank()

android {
    namespace = "com.raju.edutrack"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.raju.edutrack"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword =
                    providers.environmentVariable("EDUTRACK_KEYSTORE_PASSWORD")
                        .get()
                keyAlias =
                    providers.environmentVariable("EDUTRACK_KEY_ALIAS")
                        .get()
                keyPassword =
                    providers.environmentVariable("EDUTRACK_KEY_PASSWORD")
                        .get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
