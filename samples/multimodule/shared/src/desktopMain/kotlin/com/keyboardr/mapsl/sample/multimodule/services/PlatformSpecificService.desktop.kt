package com.keyboardr.mapsl.sample.multimodule.services

import com.keyboardr.mapsl.sample.multimodule.locator.serviceLocator

actual class PlatformSpecificService: PlatformSpecificServiceBase() {
  actual fun sayHello() = "Hello desktop"
  fun sayHelloDesktopOnly() = "Hello to desktop-specific code"

  actual companion object {
    actual val instance: PlatformSpecificService by serviceLocator { PlatformSpecificService() }
  }
}