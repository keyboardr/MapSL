package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ServiceLocator
import com.keyboardr.mapsl.classKey
import com.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import kotlin.reflect.KClass

/**
 * A key type based on a particular class. These keys always provide values of their corresponding
 * class, [T]. [ClassKey]s are considered equal if their [type] is equal.
 *
 * Due to type erasure, it is not advisable to use [ClassKey] with generic types. Only their
 * reifiable type will be considered for equality.
 */
public sealed interface ClassKey<T : Any> {
  public val type: KClass<T>
}

/**
 * A [ClassKey] which provides a single value.
 */
public data class SingletonClassKey<T : Any>(override val type: KClass<T>) : SingletonKey<T>(type),
  ClassKey<T> {
  override fun equals(other: Any?): Boolean {
    return other is ClassKey<*> && other is ServiceKey<*, *, *, *> && other.type == type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }
}

/**
 * A [ClassKey] which provides a single value that is computed lazily.
 */
public data class LazyClassKey<T : Any>(override val type: KClass<T>) : LazyKey<T>(type),
  ClassKey<T> {
  override fun equals(other: Any?): Boolean {
    return other is ClassKey<*> && other is ServiceKey<*, *, *, *> && other.type == type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }
}

/**
 * Registers a provider using a [ClassKey]. [provider] will be invoked the first time a value is
 * requested for [T]. The multi-thread behavior depends on [threadSafetyMode].
 */
public inline fun <reified T : Any> ServiceLocator.put(
  threadSafetyMode: LazyThreadSafetyMode = defaultLazyKeyThreadSafetyMode,
  noinline provider: () -> T,
) {
  put(classKey<T>(), LazyKey.PutParams(provider, threadSafetyMode))
}

/**
 * Registers a value using a [ClassKey].
 */
public inline fun <reified T : Any> ServiceLocator.put(value: T) {
  put(SingletonClassKey<T>(T::class), value)
}