import com.keyboardr.build.javaVersion
import com.keyboardr.build.jdkVersion

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.keyboardr.mapsl.sample.basic"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.keyboardr.mapsl.sample.basic"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
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
  // Change these to library references
  implementation(projects.simple)

  implementation(libs.androidx.core)
  implementation(libs.androidx.activity.compose)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)

  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)

  testImplementation(projects.simple.simpleTesting)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.androidx.test)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.robolectric)
}