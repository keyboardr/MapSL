package com.keyboardr.mapsl

import java.util.concurrent.ConcurrentHashMap

internal actual class ConcurrentMap<K, V>(private val backingMap: ConcurrentHashMap<K, V>) :
  MutableMap<K, V> by backingMap {
  actual constructor() : this(ConcurrentHashMap())

  actual fun computeIfAbsent(key: K, default: () -> V): V =
    backingMap.computeIfAbsent(key) { default() }
}