plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

//noinspection UseTomlInstead
dependencies {
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
}