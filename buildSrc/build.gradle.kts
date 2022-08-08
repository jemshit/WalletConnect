plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

// required since gradle 4.10+
repositories {
    mavenCentral()
    google()
}

dependencies {
    // Also exists in ProjectBuildPlugins
    implementation("com.android.tools.build:gradle:7.2.1")
    // Also exists in ProjectDependencies.kotlinVersion
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
}

/*
kotlinDslPluginOptions {
    experimentalWarning.set(false)
}*/
