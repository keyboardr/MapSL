package com.keyboardr.mapsl.sample.keysample.domain.single

import com.keyboardr.mapsl.get
import com.keyboardr.mapsl.sample.keysample.locator.MainServiceLocator

class PreregisteredSingleton(val name: String) {

  fun sayHello() = "Hello $name"

  companion object {
    val instance: PreregisteredSingleton
      get() = MainServiceLocator.instance.get()
  }
}