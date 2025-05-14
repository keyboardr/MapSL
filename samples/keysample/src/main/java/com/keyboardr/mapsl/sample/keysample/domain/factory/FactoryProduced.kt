package com.keyboardr.mapsl.sample.keysample.domain.factory

import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.keys.FactoryKey
import com.keyboardr.mapsl.sample.keysample.locator.create

@OptIn(ExperimentalKeyType::class)
data class FactoryProduced(val instance: Int) {
  companion object {
    val factoryKey = FactoryKey<FactoryProduced>()
    fun create() = factoryKey.create()
  }
}