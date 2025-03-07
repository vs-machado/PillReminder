plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("kotlin-android")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.phoenix.remedi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.phoenix.remedi"
        minSdk = 32
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 4
        versionName = "1.4"

        testInstrumentationRunner = "com.phoenix.remedi.CustomTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging{
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.test.ext:junit-ktx:1.1.5")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.navigation:navigation-testing:2.8.3")
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("androidx.activity:activity:1.10.0")

    val navVersion = "2.7.5"
    val coreVersion = "1.12.0"
    val roomVersion = "2.6.1"
    val lifecycleVersion = "2.5.0-alpha02"
    val activityVersion = "1.8.2"
    val fragmentVersion = "1.6.2"
    val workVersion = "2.9.0"
    val androidxTestVersion = "1.6.0"
    val espressoVersion = "3.6.1"

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

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.test:runner:$androidxTestVersion")
    implementation("androidx.test:core-ktx:$androidxTestVersion")

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

    androidTestImplementation("androidx.work:work-testing:$workVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
    androidTestImplementation("androidx.test:core:$androidxTestVersion")
    androidTestImplementation("androidx.test:core-ktx:$androidxTestVersion")
    androidTestImplementation("androidx.test:runner:$androidxTestVersion")
    androidTestImplementation("androidx.test.ext:junit:" + rootProject.version)
    androidTestImplementation("androidx.test.ext:junit-ktx:" + rootProject.version)
    androidTestImplementation("androidx.test:rules:$androidxTestVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.9")
    androidTestImplementation("com.google.truth:truth:1.4.2")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    androidTestImplementation("org.mockito:mockito-inline:5.2.0")
    androidTestImplementation("org.hamcrest:hamcrest:2.2")
    androidTestImplementation("androidx.fragment:fragment-testing:1.8.5")
    androidTestImplementation("androidx.test:monitor:1.7.0")
    androidTestImplementation("androidx.preference:preference-ktx:1.2.1")

//    debugImplementation("androidx.fragment:fragment-testing:1.8.5"){
//        exclude(group = "androidx.test", module = "monitor")
//        exclude(group = "android.test", module = "core")
//    }

    debugImplementation("androidx.fragment:fragment-testing-manifest:1.8.5")

    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
