package dev.keyboardr.mapsl.sample.keysample.domain.factory

import dev.keyboardr.mapsl.ExperimentalKeyType
import dev.keyboardr.mapsl.keys.FactoryKey
import dev.keyboardr.mapsl.sample.keysample.locator.create

@OptIn(ExperimentalKeyType::class)
data class FactoryProduced(val instance: Int) {
  companion object {
    val factoryKey = FactoryKey<FactoryProduced>()
    fun create() = factoryKey.create()
  }
}