import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
}

val keystorePropertiesFile = rootProject.file("sample/keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))


android {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    compileSdk = 31
    defaultConfig {
        applicationId = "com.jemshit.walletconnect.sample"
        minSdk = 23
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            applicationIdSuffix = ".release"
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
            )
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }

    packagingOptions {
        resources {
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("META-INF/services/javax.annotation.processing.Processor")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.FlowPreview"
}

dependencies {
    implementation(project(":walletconnect"))
    implementation(project(":walletconnect-store-prefs"))
    implementation(project(":walletconnect-store-file"))
    implementation(project(":walletconnect-adapter-gson"))
    implementation(project(":walletconnect-adapter-moshi"))
    implementation(project(":walletconnect-socket-scarlet"))
    implementation(project(":walletconnect-requests"))

    implementation(Dependencies.kotlinJDK8)
    implementation(Dependencies.kotlinCoroutines)
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.activity:activity-ktx:1.5.1")
    implementation("androidx.fragment:fragment-ktx:1.5.2")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.6.1")

    implementation(Dependencies.okHttp)
    implementation(Dependencies.okHttpInterceptor)
    implementation(Dependencies.moshi)
    implementation(Dependencies.gson)
    implementation(Dependencies.coil)
    implementation(Dependencies.coilSvg)
    implementation(Dependencies.retrofit)
    implementation(Dependencies.retrofit_gson)
}
