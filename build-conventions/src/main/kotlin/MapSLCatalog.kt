import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.mapSlLibs: MapSLCatalog
  get() = MapSLCatalog(extensions.getByType<VersionCatalogsExtension>().named("libs"))

@JvmInline
value class MapSLCatalog(private val catalog: VersionCatalog) {
  val versions
    get() = Versions(catalog)

  val plugins
    get() = Plugins(catalog)

  private fun VersionCatalog.findLibraryOrThrow(name: String) =
    findLibrary(name)
      .orElseThrow { NoSuchElementException("Library $name not found in version catalog") }
}


@JvmInline
value class Versions(private val catalog: VersionCatalog) {
  val mapsl
    get() = catalog.findVersionOrThrow("mapsl")

  private fun VersionCatalog.findVersionOrThrow(name: String) =
    findVersion(name)
      .orElseThrow { NoSuchElementException("Version $name not found in version catalog") }
      .requiredVersion
}

@JvmInline
value class Plugins(private val catalog: VersionCatalog) {

  private fun VersionCatalog.findPluginOrThrow(name: String) =
    findPlugin(name)
      .orElseThrow { NoSuchElementException("Plugin $name not found in version catalog") }

}
