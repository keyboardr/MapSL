package dev.keyboardr.mapsl.sample.scaffold

import androidx.annotation.VisibleForTesting
import dev.keyboardr.mapsl.simple.scaffold.serviceLocator

class MyService @VisibleForTesting constructor() {

  fun sayHello() = "Hello, MapSL!"

  companion object {
    val instance by serviceLocator { MyService() }
  }
}