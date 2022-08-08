import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin")
    id("java-test-fixtures")
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

publishing {
    publications {
        create<MavenPublication>("maven"){
            groupId = "com.jemshit.walletconnect"
            artifactId = "walletconnect-core"
            version = "0.0.1"

            from(components["java"])
        }
    }
}

dependencies {
    dokkaJavadocPlugin(Dependencies.kotlinDokka)

    implementation(Dependencies.kotlinJDK8)
    implementation(Dependencies.kotlinCoroutines)
    testImplementation(Dependencies.kotlinCoroutinesTest)
    testFixturesImplementation(Dependencies.kotlinCoroutinesTest)

    testImplementation(Dependencies.junit)
    testFixturesImplementation(Dependencies.junit)
}
