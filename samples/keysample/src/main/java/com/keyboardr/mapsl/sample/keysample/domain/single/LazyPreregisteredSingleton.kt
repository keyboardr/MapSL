package com.keyboardr.mapsl.sample.keysample.domain.single

import com.keyboardr.mapsl.get
import com.keyboardr.mapsl.sample.keysample.locator.ProcessServiceLocator

class LazyPreregisteredSingleton(val name: String) {

  fun sayHello() = "Hello $name"

  companion object {
    val instance: LazyPreregisteredSingleton
      get() = ProcessServiceLocator.instance.get()
  }
}