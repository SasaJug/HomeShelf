import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.jugurdzija.homeshelf"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "com.jugurdzija.homeshelf.HiltTestRunner"
        buildConfigField("String", "DEFAULT_WEB_CLIENT_ID", "\"${project.findProperty("DEFAULT_WEB_CLIENT_ID")}\"")

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("studentTest") {
            storeFile = localProperties.getProperty("STUDENT_TEST_KEYSTORE")?.let { file(it) }
            storePassword = localProperties.getProperty("STUDENT_TEST_KEYSTORE_PASSWORD")
            keyAlias = localProperties.getProperty("STUDENT_TEST_KEY_ALIAS")
            keyPassword = localProperties.getProperty("STUDENT_TEST_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("studentTest")
        }
    }

    packaging {
        resources.excludes += setOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
    }

    flavorDimensions += "variant"

    productFlavors {
        create("production") {
            dimension = "variant"
            resValue("string", "app_name", "HomeShelf")
        }
        create("capture") {
            dimension = "variant"
            applicationIdSuffix = ".capture"
            versionNameSuffix = "-capture"
            resValue("string", "app_name", "Capture")
        }
    }
}

dependencies {
    implementation(libs.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)

    implementation(libs.google.mediapipe.vision)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    implementation(libs.opencv)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ui.auth)
    implementation(libs.firebase.ai)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.hilt.android.testing)
}
