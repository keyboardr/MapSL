import com.android.build.api.dsl.androidLibrary
import com.keyboardr.build.jdkVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.compose)
}

val applicationId = "com.keyboardr.mapsl.sample.multimodule.shared"

kotlin {
  jvm("desktop")
  @Suppress("UnstableApiUsage")
  androidLibrary {
    namespace = applicationId
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    lint {
      targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(projects.simple)

      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.preview)
      implementation(compose.runtime)

      implementation(libs.androidx.annotation)
    }
  }

  jvmToolchain(jdkVersion)
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
}