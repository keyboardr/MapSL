import dev.keyboardr.build.javaVersion
import dev.keyboardr.build.jdkVersion

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "dev.keyboardr.mapsl.sample.multimodule.preview"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "dev.keyboardr.mapsl.sample.multimodule.preview"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }
  buildFeatures {
    compose = true
  }
}

kotlin {
  jvmToolchain(jdkVersion)
}

dependencies {
  implementation(projects.samples.multimodule.shared)
  implementation(projects.simple.simpleTesting)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)

  implementation(libs.mockito.kotlin)
}