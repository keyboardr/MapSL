package dev.keyboardr.mapsl.sample.multimodule.services

import androidx.annotation.OpenForTesting
import dev.keyboardr.mapsl.sample.multimodule.locator.ServiceProvider
import dev.keyboardr.mapsl.sample.multimodule.locator.serviceProvider

@OpenForTesting
open class BarManager private constructor() {

  fun sayHello() = "Hello, from BarManager"

  // Example using interface delegation rather than property delegation
  companion object : ServiceProvider<BarManager> by serviceProvider({ BarManager() })
}