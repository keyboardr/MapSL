package com.keyboardr.mapsl.sample.multimodule.services

import androidx.annotation.OpenForTesting
import com.keyboardr.mapsl.sample.multimodule.locator.ProcessServiceLocator
import com.keyboardr.mapsl.sample.multimodule.locator.serviceLocator
import com.keyboardr.mapsl.sample.multimodule.platform.PlatformContext

@OpenForTesting
open class FooManager private constructor(
  private val context: PlatformContext = ProcessServiceLocator.applicationContext,
) {

  fun sayHello() = "Hello, ${context.applicationId} from FooManager"

  companion object {
    val instance by serviceLocator { FooManager() }
  }
}