plugins {
  id("mapsl.documentable")
}

dokka {
  moduleName.set("MapSL")
}

dependencies {
  dokka(projects.core)
  dokka(projects.lifecycle)
  dokka(projects.scoped)
  dokka(projects.scoped.scopedTesting)
  dokka(projects.simple)
  dokka(projects.simple.simpleTesting)
  dokka(projects.simple.simpleScaffold)
}