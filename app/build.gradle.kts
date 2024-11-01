plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("kotlin-android")
}

android {
    namespace = "com.phoenix.pillreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.phoenix.pillreminder"
        minSdk = 33
        //noinspection EditedTargetSdkVersion
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.phoenix.pillreminder.feature_alarms.CustomTestRunner"
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging{
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.test:core-ktx:1.5.0")
    implementation("androidx.test.ext:junit-ktx:1.1.5")
    implementation("com.android.support:cardview-v7:28.0.0")
    implementation("androidx.test:runner:1.6.2")
    implementation("com.google.dagger:hilt-android-testing:2.51.1")
    val navVersion = "2.7.5"
    val coreVersion = "1.12.0"
    val roomVersion = "2.6.1"
    val lifecycleVersion = "2.5.0-alpha02"
    val activityVersion = "1.8.2"
    val fragmentVersion = "1.6.2"
    val workVersion = "2.9.0"

    //Activity
    implementation("androidx.activity:activity-ktx:$activityVersion")

    //AppIntro
    implementation("com.github.AppIntro:AppIntro:6.3.1")

    //Fragment
    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")

    //Navigation dependencies
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    //Room database
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

    //LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    //WorkManager
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    //Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")

    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Swipe refresh layout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    //Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.2.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.9")
    testImplementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    testImplementation("androidx.work:work-testing:$workVersion")
    testImplementation("androidx.core:core-ktx:$coreVersion")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    testImplementation("org.robolectric:robolectric:4.13")
    kspTest("com.google.dagger:hilt-compiler:2.51.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.9")
    androidTestImplementation("com.google.truth:truth:1.4.2")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
}