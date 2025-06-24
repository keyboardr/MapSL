package dev.keyboardr.mapsl.sample.basic

import androidx.annotation.VisibleForTesting

class MyService @VisibleForTesting constructor() {

  fun sayHello() = "Hello, MapSL!"

  companion object {
    val instance by serviceLocator { MyService() }
  }
}