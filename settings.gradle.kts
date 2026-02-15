pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MelhoreApp"
include(":app")
include(":core:common")
include(":core:database")
include(":core:notifications")
include(":core:scheduling")
include(":feature:reminders")
include(":feature:categories")
include(":feature:settings")
