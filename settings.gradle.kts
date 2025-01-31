pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

enableFeaturePreview("VERSION_CATALOGS")

include(
    ":app",
    ":core",
    ":customisations",
    ":sandbox",
    ":interop-ribs",
    ":interop-rx2",
    ":navmodel-addons",
    ":samples:navigation-compose",
    ":testing-junit4",
    ":testing-junit5",
    ":testing-ui",
    ":testing-unit-common",
)

includeBuild("plugins")
