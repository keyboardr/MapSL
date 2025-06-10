plugins {
  id("mapsl.documentable")
  id("maven-publish")
}

group = "com.keyboardr.mapsl"
version = "0.2.0"

if(properties.containsKey("repsyUrl")) {
  publishing {
    publications {
      repositories {
        maven {
          url = uri(property("repsyUrl") as String)
          credentials {
            username = property("repsyUsername") as String
            password = property("repsyPassword") as String
          }
        }
      }
    }
  }
} else {
  logger.warn("Publishing repository not configured")
}