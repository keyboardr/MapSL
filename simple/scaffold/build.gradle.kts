import com.android.build.api.dsl.androidLibrary
import dev.keyboardr.build.jdkVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.multiplatform.library)
  id("mapsl.publishable")
}

kotlin {
  jvm()
  @Suppress("UnstableApiUsage")
  androidLibrary {
    namespace = "dev.keyboardr.mapsl.simple.scaffold"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    lint {
      targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
  }
  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "simple-scaffold"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(projects.simple)
      implementation(projects.simple.simpleTesting)
      implementation(libs.androidx.annotation)
    }

    androidMain.dependencies {
      implementation(libs.androidx.startup)
    }
  }
  jvmToolchain(jdkVersion)
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
  explicitApi()
}

