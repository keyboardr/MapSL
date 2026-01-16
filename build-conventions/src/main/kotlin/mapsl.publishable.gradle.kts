import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  id("mapsl.documentable")
  id("com.vanniktech.maven.publish")
  kotlin("multiplatform")
}

group = "dev.keyboardr.mapsl"
version = mapSlLibs.versions.mapsl +
    if (System.getenv("IS_CI").toBoolean()) "" else "-SNAPSHOT"
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

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()

  pom {
    name.set("MapSL")
    description.set("Service Locator library")
    inceptionYear.set("2025")
    url.set("https://github.com/keyboardr/mapsl/")
    licenses {
      license {
        name.set("The MIT License")
        url.set("http://www.opensource.org/licenses/mit-license.php")
      }
    }
    developers {
      developer {
        id.set("keyboardr")
        name.set("Josh Brown")
        url.set("https://github.com/keyboardr/")
      }
    }
    scm {
      url.set("https://github.com/keyboardr/mapsl/")
      connection.set("scm:git:git://github.com/keyboardr/mapsl.git")
      developerConnection.set("scm:git:ssh://git@github.com/keyboardr/mapsl.git")
    }
  }
}

tasks.withType<PublishToMavenRepository> {
  dependsOn(rootProject.subprojects.map { subproject ->
    subproject.tasks.named { it == "checkLegacyAbi" }
  })
  doLast {
    println("Don't forget to click publish at https://central.sonatype.com/publishing/deployments")
  }
}
