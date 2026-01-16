package dev.keyboardr.mapsl.keys

import dev.keyboardr.mapsl.ServiceLocator
import dev.keyboardr.mapsl.classKey
import dev.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/**
 * A [ServiceKey] that uses the service's own [KClass] as its identifier.
 *
 * This key kind is useful for the common case where you only need a single instance of a service
 * and can identify it by its type alone, without needing to declare an explicit key instance.
 *
 * All `ClassKey` implementations for the same service type `T` are considered equal. This allows,
 * for example, a service to be registered with a [LazyClassKey] and later retrieved using a
 * [SingletonClassKey] for the same type.
 *
 * > **Warning on Type Erasure**: Due to JVM type erasure, it is not advisable to use `ClassKey`
 * > with generic types (e.g., `List<String>`). The key's identity is based on the erased
 * > type (`List` in this case), meaning `ClassKey<List<String>>` and `ClassKey<List<Int>>`
 * > would be treated as the same key. For generic services, use explicit [LazyKey] or
 * > [SingletonKey] instances instead.
 */
public sealed interface ClassKey<T : Any> {
  public val type: KClass<T>
}

/**
 * A [ClassKey] that provides a single, eagerly-loaded value.
 *
 * It overrides [equals] and [hashCode] to be based solely on the service [type]. This ensures
 * it is considered equal to any other [ClassKey] (like [LazyClassKey]) for the same service type.
 */
public data class SingletonClassKey<T : Any>(override val type: KClass<T>) : SingletonKey<T>(type),
  ClassKey<T> {
  override fun equals(other: Any?): Boolean {
    return other is ClassKey<*> && other.type == type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }
}

/**
 * A [ClassKey] that provides a single value that is computed lazily.
 *
 * It overrides [equals] and [hashCode] to be based solely on the service [type]. This ensures
 * it is considered equal to any other [ClassKey] (like [SingletonClassKey]) for the same service type.
 */
public data class LazyClassKey<T : Any>(override val type: KClass<T>) : LazyKey<T>(type),
  ClassKey<T> {
  override fun equals(other: Any?): Boolean {
    return other is ClassKey<*> && other.type == type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }
}

/**
 * Registers a lazy singleton provider using a [LazyClassKey] for the reified type `T`.
 *
 * The [provider] will be invoked only the first time a value is requested for the type `T`.
 * The created instance is then stored and reused for all subsequent requests.
 *
 * @param T The service type to register.
 * @param threadSafetyMode The [LazyThreadSafetyMode] for the initialization.
 * @param provider A lambda that creates the service instance.
 */
@JvmOverloads
public inline fun <reified T : Any> ServiceLocator.put(
  threadSafetyMode: LazyThreadSafetyMode = defaultLazyKeyThreadSafetyMode,
  noinline provider: () -> T,
) {
  put(classKey<T>(), LazyKey.PutParams(provider, threadSafetyMode))
}

/**
 * Registers an eager singleton instance using a [SingletonClassKey] for the reified type `T`.
 *
 * The provided [value] is stored directly and will be returned for all subsequent requests for the
 * type `T`.
 *
 * @param T The service type to register.
 * @param value The service instance to store.
 */
public inline fun <reified T : Any> ServiceLocator.put(value: T) {
  put(SingletonClassKey<T>(T::class), value)
}
