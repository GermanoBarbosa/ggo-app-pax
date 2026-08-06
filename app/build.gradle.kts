plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "br.com.paxuniao.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.paxuniao.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 23
        versionName = "26.8.$versionCode"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "version"
    productFlavors {
        create("paxuniao") {
            dimension = "version"
            applicationIdSuffix = ""
            resValue("string", "app_name", "PAX União")
        }
        create("nacionalpax") {
            dimension = "version"
            applicationIdSuffix = ".nacionalpax"
            resValue("string", "app_name", "Nacional PAX")
        }
        create("unipax") {
            dimension = "version"
            applicationIdSuffix = ".unipax"
            resValue("string", "app_name", "Unipax")
        }
        create("jardim") {
            dimension = "version"
            applicationIdSuffix = ".jardim"
            resValue("string", "app_name", "Jardim da Ressurreição")
        }
        create("jardim2") {
            dimension = "version"
            applicationIdSuffix = ".jardim.ii"
            resValue("string", "app_name", "Jardim 2")
        }
        create("jardimtimon") {
            dimension = "version"
            applicationIdSuffix = ".jardim.timon"
            resValue("string", "app_name", "Jardim Timon")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            val versionName = variant.versionName
            val flavorName = variant.flavorName
            output.outputFileName = "app_${flavorName}_$versionName.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    implementation(libs.okhttp)

    implementation(libs.androidx.biometric)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.2")

    implementation(libs.play.app.update)

    implementation(platform("com.google.firebase:firebase-bom:34.14.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
}
