package dev.keyboardr.mapsl.sample.keysample.domain.single

import dev.keyboardr.mapsl.get
import dev.keyboardr.mapsl.sample.keysample.locator.MainServiceLocator

class LazyPreregisteredSingleton(val name: String) {

  fun sayHello() = "Hello $name"

  companion object {
    val instance: LazyPreregisteredSingleton
      get() = MainServiceLocator.instance.get()
  }
}