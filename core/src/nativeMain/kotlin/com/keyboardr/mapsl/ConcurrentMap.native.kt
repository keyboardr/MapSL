package com.keyboardr.mapsl

import co.touchlab.stately.collections.ConcurrentMutableMap

internal actual class ConcurrentMap<K, V>(private val backingMap: ConcurrentMutableMap<K, V>) :
  MutableMap<K, V> by backingMap {
  actual constructor() : this(ConcurrentMutableMap())

  actual fun computeIfAbsent(key: K, default: () -> V): V =
    backingMap.block {
      if (contains(key)) {
        @Suppress("UNCHECKED_CAST")
        get(key) as V
      } else {
        val value = default()
        put(key, value)
        value
      }
    }
}