object Dependencies {
    const val kotlinVersion = "1.6.21"
    const val kotlinCoroutinesVersion = "1.6.3"
    const val kotlinJDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}"
    const val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinCoroutinesVersion}"
    const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${kotlinCoroutinesVersion}"

    const val okHttp = "com.squareup.okhttp3:okhttp:4.9.3"
    const val okHttpInterceptor = "com.squareup.okhttp3:logging-interceptor:4.9.3"
    const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:4.9.3"
    const val moshi = "com.squareup.moshi:moshi:1.13.0"
    const val moshiKotlinReflection = "com.squareup.moshi:moshi-kotlin:1.13.0"
    const val gson = "com.google.code.gson:gson:2.9.0"
    const val coil = "io.coil-kt:coil:2.1.0"
    const val coilSvg = "io.coil-kt:coil-svg:2.1.0"
    const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
    const val retrofit_gson = "com.squareup.retrofit2:converter-gson:2.9.0"

    const val scarletVersion = "0.1.12"
    const val scarlet = "com.tinder.scarlet:scarlet:${scarletVersion}"
    const val scarletOkHttp = "com.tinder.scarlet:websocket-okhttp:${scarletVersion}"
    const val scarletGson = "com.tinder.scarlet:message-adapter-gson:${scarletVersion}"
    const val scarletMockWebServer = "com.tinder.scarlet:websocket-mockwebserver:${scarletVersion}"

    const val junit = "junit:junit:4.13.2"
    const val roboelectric = "org.robolectric:robolectric:4.8"
    const val androidxJunitKtx = "androidx.test.ext:junit-ktx:1.1.3"
    const val androidxTestRunner = "androidx.test:runner:1.4.0"
    const val androidxTestRules = "androidx.test:rules:1.4.0"
    const val androidxTestOrchestrator = "androidx.test:orchestrator:1.4.1"

}