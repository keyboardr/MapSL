package com.keyboardr.mapsl.sample.multimodule.services

import androidx.annotation.OpenForTesting
import com.keyboardr.mapsl.sample.multimodule.locator.serviceLocator

@OpenForTesting
open class BarManager private constructor() {

  fun sayHello() = "Hello, from BarManager"

  companion object {
    val instance by serviceLocator { BarManager() }
  }
}