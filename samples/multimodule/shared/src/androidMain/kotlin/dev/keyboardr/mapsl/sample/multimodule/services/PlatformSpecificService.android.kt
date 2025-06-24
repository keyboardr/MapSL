package dev.keyboardr.mapsl.sample.multimodule.services

import dev.keyboardr.mapsl.sample.multimodule.locator.serviceLocator

actual class PlatformSpecificService: PlatformSpecificServiceBase() {
  actual fun sayHello() = "Hello Android"

  fun sayHelloAndroidOnly() = "Hello to Android-specific code"

  actual companion object {
    actual val instance: PlatformSpecificService by serviceLocator { PlatformSpecificService() }
  }
}