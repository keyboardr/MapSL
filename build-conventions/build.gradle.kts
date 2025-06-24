plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

//noinspection UseTomlInstead
dependencies {
  implementation(libs.kotlin.gradlePlugin)
  implementation(libs.vanniktech.mavenPublish)
  implementation(libs.dokka)
}