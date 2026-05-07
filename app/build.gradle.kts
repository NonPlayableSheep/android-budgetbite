plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "de.fhe.budget_bite"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.fhe.budget_bite"
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
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures{
        viewBinding = true
        compose = true
    }
}

dependencies {
    val fakerVersion = "1.0.2"
    val coroutinesVersion = "1.3.9"
    val roomVersion = "2.5.0"
    val lifecycleVersion = "2.5.0"
    val navVersion = "2.5.0"
    val viewPagerVersion = "1.1.0"
    val hiltVersion = "2.48"
    val coreKtxVersion = "1.9.0"
    val appCompatVersion = "1.6.1"
    val materialVersion = "1.11.0"
    val constraintLayoutVersion = "2.1.4"
    val profileInstallerVersion = "1.4.1"
    val junitVersion = "4.13.2"
    val junitKtxVersion = "1.2.1"
    val coroutinesTestVersion = "1.7.3"
    val androidxJunitVersion = "1.1.5"
    val espressoCoreVersion = "3.5.1"
    val coreTestingVersion = "2.1.0"

    // faker
    implementation("com.github.javafaker:javafaker:$fakerVersion")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // room database
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // NavComponent
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // viewpager2
    implementation("androidx.viewpager2:viewpager2:$viewPagerVersion")

    // hilt
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")

    implementation("androidx.tracing:tracing:1.2.0")
    implementation("com.squareup.curtains:curtains:1.2.5")

    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.appcompat:appcompat:$appCompatVersion")
    // from 1.10 to 1.9, cause build error with sdk version
    implementation("com.google.android.material:material:$materialVersion")
    implementation("androidx.constraintlayout:constraintlayout:$constraintLayoutVersion")
    implementation("androidx.profileinstaller:profileinstaller:$profileInstallerVersion")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("androidx.test.ext:junit-ktx:$junitKtxVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")
    androidTestImplementation("androidx.test.ext:junit:$androidxJunitVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoCoreVersion")
    androidTestImplementation("androidx.arch.core:core-testing:$coreTestingVersion")


    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.material3:material3")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
}