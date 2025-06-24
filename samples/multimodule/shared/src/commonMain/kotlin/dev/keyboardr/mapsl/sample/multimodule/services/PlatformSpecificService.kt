package dev.keyboardr.mapsl.sample.multimodule.services

/**
 * Demonstrates how to include platform-specific functionality in
 */
expect class PlatformSpecificService: PlatformSpecificServiceBase {
  fun sayHello(): String

  companion object {
    val instance: PlatformSpecificService
  }
}

abstract class PlatformSpecificServiceBase {
  fun sayHelloCommon() = "Hello common"
}

