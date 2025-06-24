import com.android.build.api.dsl.androidLibrary
import dev.keyboardr.build.jdkVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.multiplatform.library)
}

val applicationId = "dev.keyboardr.mapsl.sample.multimodule.testing"

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
      api(projects.simple.simpleTesting)
      api(projects.samples.multimodule.shared)
      implementation(libs.androidx.annotation)
      implementation(libs.mockito.kotlin)
    }
  }

  jvmToolchain(jdkVersion)
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
}
