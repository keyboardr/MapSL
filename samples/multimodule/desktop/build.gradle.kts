plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.compose)
}

kotlin {
  jvmToolchain(jdkVersion)
}

dependencies {
  implementation(projects.samples.multimodule.shared)
  implementation(projects.simple)

  implementation(compose.components.resources)
  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
}

compose {
  desktop {
    application {
      nativeDistributions {
        appResourcesRootDir.set(project.layout.projectDirectory.dir("assets"))
        packageName = "multimodule"
      }
      mainClass = "dev.keyboardr.mapsl.sample.multimodule.MainApplication"
    }
  }

  resources {
    packageOfResClass = "dev.keyboardr.mapsl.sample.multimodule"
    generateResClass = always
  }
}