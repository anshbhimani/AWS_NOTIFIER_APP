import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}

val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().apply {
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

// Read properties from gradle.properties
val gradleProps = Properties()
val gradlePropsFile = rootProject.file("gradle.properties")
if (gradlePropsFile.exists()) {
    gradlePropsFile.reader().use { gradleProps.load(it) }
}


android {
    namespace = "com.ansh.awsnotifier"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val storeFilePath = gradleProps.getProperty("MY_RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                val storeFile = File(storeFilePath)
                if (storeFile.exists()) {
                    this.storeFile = storeFile
                    this.storePassword = gradleProps.getProperty("MY_RELEASE_STORE_PASSWORD")
                    this.keyAlias = gradleProps.getProperty("MY_RELEASE_KEY_ALIAS")
                    this.keyPassword = gradleProps.getProperty("MY_RELEASE_KEY_PASSWORD")
                }
            }
        }
        getByName("debug") {
            val storeFilePath = gradleProps.getProperty("MY_DEBUG_STORE_FILE")
            if (storeFilePath != null) {
                val storeFile = File(storeFilePath)
                if (storeFile.exists()) {
                    this.storeFile = storeFile
                    this.storePassword = gradleProps.getProperty("MY_DEBUG_STORE_PASSWORD")
                    this.keyAlias = gradleProps.getProperty("MY_DEBUG_KEY_ALIAS")
                    this.keyPassword = gradleProps.getProperty("MY_DEBUG_KEY_PASSWORD")
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.ansh.awsnotifier"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.2.2"

        manifestPlaceholders["firebase_messaging_auto_init_enabled"] = "true"

        listOf(
            "SNS_PLATFORM_APPLICATION_ARN",
            "SNS_PLATFORM_APP_US_EAST_1",
            "SNS_PLATFORM_APP_US_EAST_2",
            "SNS_PLATFORM_APP_US_WEST_1",
            "SNS_PLATFORM_APP_US_WEST_2",
            "SNS_PLATFORM_APP_AP_SOUTH_1",
            "SNS_PLATFORM_APP_AP_SOUTHEAST_1",
            "SNS_PLATFORM_APP_AP_SOUTHEAST_2",
            "SNS_PLATFORM_APP_AP_NORTHEAST_1",
            "SNS_PLATFORM_APP_EU_WEST_1",
            "SNS_PLATFORM_APP_EU_CENTRAL_1",
            "SNS_PLATFORM_APP_SA_EAST_1"
        ).forEach { key ->
            buildConfigField("String", key, "\"${localProps.getProperty(key, "")}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply the signing configuration to the release build type
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.analytics.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.material)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.constraintlayout)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.gson)

    implementation(platform(libs.aws.sdk.kotlin.bom))
    implementation(libs.aws.sdk.kotlin.sts)
    implementation(libs.aws.sdk.kotlin.sns)
    implementation(libs.aws.sdk.kotlin.ec2)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
