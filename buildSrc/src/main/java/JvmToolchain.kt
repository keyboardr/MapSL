import org.gradle.api.JavaVersion
import org.gradle.api.Project

val Project.javaVersion
  get() = JavaVersion.VERSION_17
val Project.jdkVersion
  get() = javaVersion.ordinal + 1