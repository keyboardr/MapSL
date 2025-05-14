package com.keyboardr.mapsl.sample.basic

class MyService private constructor() {

  fun sayHello() = "Hello, MapSL!"

  companion object {
    val instance by serviceLocator { MyService() }
  }
}