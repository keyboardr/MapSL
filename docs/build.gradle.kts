import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
  alias(libs.plugins.dokka)
}

dokka {
  moduleName.set("MapSL")
  dokkaSourceSets.configureEach {
    documentedVisibilities.set(setOf(VisibilityModifier.Public, VisibilityModifier.Protected))
  }
}

dependencies {
  dokka(projects.core)
  dokka(projects.lifecycle)
  dokka(projects.scoped)
  dokka(projects.scoped.testing)
  dokka(projects.simple)
  dokka(projects.simple.testing)
}