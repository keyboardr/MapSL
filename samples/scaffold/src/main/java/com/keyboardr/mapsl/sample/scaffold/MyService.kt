package com.keyboardr.mapsl.sample.scaffold

import androidx.annotation.VisibleForTesting
import com.keyboardr.mapsl.simple.scaffold.serviceLocator

class MyService @VisibleForTesting constructor() {

  fun sayHello() = "Hello, MapSL!"

  companion object {
    val instance by serviceLocator { MyService() }
  }
}