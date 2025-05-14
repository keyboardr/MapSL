package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.LazyClassKey
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey
import com.keyboardr.mapsl.keys.put
import kotlin.reflect.KClass

/**
 * A simplified service locator that only uses classes as keys, and always populates services lazily.
 */
public open class SimpleServiceLocator<out S>(scope: S, allowReregister: Boolean = false) {
  private val backingServiceLocator = object : ScopedServiceLocator<S>(scope, allowReregister) {
    override fun <T : Any, GetParams> onMiss(
      key: ServiceKey<T, *, GetParams, *>,
      params: GetParams,
    ): T = getOrProvide(key.type, { true }) { onMiss(key.type) }

    override fun <T : Any, PutParams> onInvalidScope(
      key: ServiceKey<T, *, *, PutParams>,
      putParams: PutParams
    ): ServiceEntry<T> = SimpleEntry(onInvalidScope(key.type))

    override fun <T : Any, GetParams> getValue(
      key: ServiceKey<T, *, GetParams, *>,
      params: GetParams,
      entry: ServiceEntry<T>
    ): T = if (entry is SimpleEntry<T>) entry.value else super.getValue(key, params, entry)
  }

  private class SimpleEntry<T>(val value: T) : ServiceEntry<T>

  public val scope: S = backingServiceLocator.scope

  public fun <T : Any> put(
    key: KClass<T>,
    threadSafetyMode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    provider: () -> T,
  ) {
    backingServiceLocator.put<T>(LazyClassKey(key), threadSafetyMode) { provider() }
  }

  public inline fun <reified T : Any> put(
    threadSafetyMode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline provider: () -> T,
  ) {
    put(T::class, threadSafetyMode, provider)
  }

  public fun <T : Any> get(key: KClass<T>): T = backingServiceLocator.get(LazyClassKey(key))

  public inline fun <reified T : Any> get(): T = get(T::class)

  public fun <T : Any> getOrNull(key: KClass<T>): T? =
    backingServiceLocator.getOrNull(LazyClassKey(key))

  public inline fun <reified T : Any> getOrNull(): T? = getOrNull(T::class)

  public fun <T : Any> getOrProvide(
    key: KClass<T>,
    allowedScopes: (S) -> Boolean,
    threadSafetyMode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    provider: (S) -> T,
  ): T = backingServiceLocator.getOrProvide(
    LazyClassKey<T>(key),
    allowedScopes,
    threadSafetyMode,
  ) {
    provider(scope)
  }

  public inline fun <reified T : Any> getOrProvide(
    noinline allowedScopes: (S) -> Boolean,
    threadSafetyMode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline provider: (S) -> T,
  ): T = getOrProvide(T::class, allowedScopes, threadSafetyMode, provider)

  /**
   * Called when no entry is found for the specified [key] to decide what should be returned.
   * Override to customize this behavior. Throws an [IllegalArgumentException] by default.
   */
  protected open fun <T : Any> onMiss(key: KClass<T>): T =
    throw IllegalArgumentException("No value found for key: $key")


  /**
   * Called when attempting to provide a value for a disallowed scope. The default
   * behavior is to throw an [IllegalArgumentException].
   */
  protected open fun <T : Any> onInvalidScope(key: KClass<T>): T =
    throw IllegalArgumentException("Unsupported scope ${backingServiceLocator.scope} for key $key")
}