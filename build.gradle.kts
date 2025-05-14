plugins {
  // Loop through plugins defined in the version catalog and resolve them here
  extensions.getByType<VersionCatalogsExtension>().named("libs").apply {
    pluginAliases.forEach {
      alias(findPlugin(it).get()) apply false
    }
  }
}

buildscript {
  extra // lookup extras to force loading of extensions so above plugins block succeeds
}