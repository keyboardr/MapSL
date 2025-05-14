enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention").version("0.9.0")
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "MapSL"
include(":core")
include(":docs")
include(":lifecycle")
include(":scoped", ":scoped:testing")
include(":simple", ":simple:testing")
include(":samples:basic")
include(":samples:keysample")
include(
  ":samples:multimodule:app",
  ":samples:multimodule:desktop",
  ":samples:multimodule:preview",
  ":samples:multimodule:shared",
  ":samples:multimodule:testing",
)
