plugins {
  alias(libs.plugins.dokka)
}

dokka {
  moduleName.set("MapSL")
}

dependencies {
  dokka(projects.core)
  dokka(projects.lifecycle)
  dokka(projects.scoped)
  dokka(projects.scoped.testing)
  dokka(projects.simple)
  dokka(projects.simple.testing)
}