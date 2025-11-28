import java.util.Properties
import java.io.File

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
    }

    defaultConfig {
        applicationId = "com.ansh.awsnotifier"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["firebase_messaging_auto_init_enabled"] = "true"

        listOf(
            "USER_POOL_ID",
            "CLIENT_ID",
            "COGNITO_REGION",
            "COGNITO_IDENTITY_POOL_ID",
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
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.androidx.constraintlayout)
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation(platform("aws.sdk.kotlin:bom:1.3.99"))
    implementation("aws.sdk.kotlin:sts")
    implementation("aws.sdk.kotlin:sns")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
