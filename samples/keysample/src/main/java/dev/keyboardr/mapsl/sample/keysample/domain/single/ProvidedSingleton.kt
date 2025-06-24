package dev.keyboardr.mapsl.sample.keysample.domain.single

import androidx.annotation.VisibleForTesting
import dev.keyboardr.mapsl.sample.keysample.locator.serviceLocator

class ProvidedSingleton @VisibleForTesting constructor() {

  fun sayHello() = "Hello Singleton"

  companion object {
    val instance by serviceLocator { ProvidedSingleton() }
  }
}