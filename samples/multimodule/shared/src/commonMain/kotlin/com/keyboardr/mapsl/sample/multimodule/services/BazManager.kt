package com.keyboardr.mapsl.sample.multimodule.services

import com.keyboardr.mapsl.sample.multimodule.locator.serviceLocator

interface BazManager {

  fun sayHello(): String

  companion object {
    val instance by serviceLocator<BazManager> { BazManagerImpl() }
  }
}

expect class BazManagerImpl() : BazManager