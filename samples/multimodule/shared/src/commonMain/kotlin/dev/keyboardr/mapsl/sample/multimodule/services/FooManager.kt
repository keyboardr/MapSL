package dev.keyboardr.mapsl.sample.multimodule.services

import androidx.annotation.OpenForTesting
import dev.keyboardr.mapsl.sample.multimodule.locator.MainServiceLocator
import dev.keyboardr.mapsl.sample.multimodule.locator.serviceLocator
import dev.keyboardr.mapsl.sample.multimodule.platform.PlatformContext

@OpenForTesting
open class FooManager private constructor(
  private val context: PlatformContext = MainServiceLocator.applicationContext,
) {

  fun sayHello() = "Hello, ${context.applicationId} from FooManager"

  companion object {
    val instance by serviceLocator { FooManager() }
  }
}