import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  id("mapsl.documentable")
  id("maven-publish")
  kotlin("multiplatform")
}

group = "dev.keyboardr.mapsl"
version = mapSlLibs.versions.mapsl

kotlin {
  @OptIn(ExperimentalAbiValidation::class)
  abiValidation {
    enabled.set(true)

    // Only check API. ABI can't be verified for iOS on Windows machines.
    klib {
      enabled.set(false)
    }
  }
}

if (properties.containsKey("repsyUrl")) {
  publishing {
    publications {
      repositories {
        maven {
          url = uri(property("repsyUrl") as String)
          credentials {
            username = property("repsyUsername") as String
            password = property("repsyPassword") as String
          }
        }
      }
    }
  }
} else {
  logger.warn("Publishing repository not configured")
}

tasks.withType<PublishToMavenRepository> {
  dependsOn(rootProject.subprojects.map { subproject ->
    subproject.tasks.named { it == "checkLegacyAbi" }
  })
}
