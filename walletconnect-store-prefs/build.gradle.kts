plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

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
        minSdk = 21
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        consumerProguardFiles("consumer-rules.pro")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.jemshit.walletconnect"
                artifactId = "walletconnect-store-prefs"
                version = "0.0.1"

                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}

dependencies {
    api(project(":walletconnect-core"))
    testImplementation(testFixtures(project(":walletconnect-core")))

    implementation(Dependencies.kotlinJDK8)
    implementation(Dependencies.kotlinCoroutines)
    testImplementation(Dependencies.kotlinCoroutinesTest)
    androidTestImplementation(Dependencies.kotlinCoroutinesTest)
    implementation(Dependencies.gson)

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.roboelectric)
    testImplementation(Dependencies.androidxJunitKtx)
    androidTestImplementation(Dependencies.androidxJunitKtx)
    androidTestImplementation(Dependencies.androidxTestRunner)
    androidTestImplementation(Dependencies.androidxTestRules)
    androidTestUtil(Dependencies.androidxTestOrchestrator)
}
