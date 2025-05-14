import com.keyboardr.build.jdkVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.dokka)
}

kotlin {
  jvm()
  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "testing"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(projects.core)
      api(projects.scoped)
      implementation(libs.androidx.annotation)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.mockito.kotlin)
    }
  }
  jvmToolchain(jdkVersion)
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
  explicitApi()
}

dokka {
  moduleName = "scoped-testing"
}
