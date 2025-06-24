import com.keyboardr.build.jdkVersion

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
      baseName = "core"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.androidx.annotation)
      implementation(libs.stately.collections)
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

