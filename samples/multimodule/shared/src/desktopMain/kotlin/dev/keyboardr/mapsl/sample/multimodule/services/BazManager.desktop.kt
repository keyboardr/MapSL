package dev.keyboardr.mapsl.sample.multimodule.services

actual class BazManagerImpl actual constructor() : BazManager {
  override fun sayHello() = "Hello from BazManager on desktop"
}