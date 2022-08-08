import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
}

publishing {
    publications {
        create<MavenPublication>("maven"){
            groupId = "com.jemshit.walletconnect"
            artifactId = "walletconnect-socket-scarlet"
            version = "0.0.1"

            from(components["java"])
        }
    }
}

dependencies {
    api(project(":walletconnect-core"))
    testImplementation(testFixtures(project(":walletconnect-core")))
    dokkaJavadocPlugin(Dependencies.kotlinDokka)

    implementation(Dependencies.kotlinJDK8)
    implementation(Dependencies.kotlinCoroutines)
    testImplementation(Dependencies.kotlinCoroutinesTest)

    api(Dependencies.scarlet)
    api(Dependencies.scarletOkHttp)
    api(Dependencies.scarletGson)
    testImplementation(Dependencies.scarletMockWebServer)

    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.androidxJunitKtx)
}
