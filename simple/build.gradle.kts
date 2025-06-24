import dev.keyboardr.build.jdkVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  id("mapsl.publishable")
}

kotlin {
  jvm()
  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "simple"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(projects.scoped)
      implementation(libs.androidx.annotation)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
    }
  }
  jvmToolchain(jdkVersion)
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
  explicitApi()
}

