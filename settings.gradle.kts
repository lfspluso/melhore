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
include(":core:auth")
include(":core:database")
include(":core:notifications")
include(":core:scheduling")
include(":core:sync")
include(":feature:reminders")
include(":feature:auth")
include(":feature:categories")
include(":feature:settings")
include(":feature:integrations")
