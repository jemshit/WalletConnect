import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin")
    id("maven-publish")
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
            artifactId = "walletconnect"
            version = "0.0.1"

            from(components["java"])
        }
    }
}

dependencies {

    api(project(":walletconnect-core"))
    implementation(Dependencies.kotlinJDK8)
    implementation(Dependencies.kotlinCoroutines)
    testImplementation(Dependencies.kotlinCoroutinesTest)

    testImplementation(Dependencies.junit)
}
