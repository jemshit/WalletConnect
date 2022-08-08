pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

include(":walletconnect-core")
include(":walletconnect-store-file")
include(":walletconnect-store-prefs")
include(":walletconnect-adapter-gson")
include(":walletconnect-adapter-moshi")
include(":walletconnect-socket-scarlet")
include(":walletconnect-requests")
include(":walletconnect")
include(":sample")
