package com.keyboardr.mapsl.sample.keysample.domain.single

import com.keyboardr.mapsl.sample.keysample.locator.ProcessServiceLocator
import com.keyboardr.mapsl.get

class PreregisteredSingleton(val name: String) {

  fun sayHello() = "Hello $name"

  companion object {
    val instance: PreregisteredSingleton
      get() = ProcessServiceLocator.instance.get()
  }
}