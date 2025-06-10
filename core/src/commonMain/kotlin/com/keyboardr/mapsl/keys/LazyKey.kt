package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ServiceLocator
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

/**
 * A [ServiceKey] for services that are created and stored lazily.
 *
 * The [provider][PutParams.provider] lambda is executed only the first time a value for this key
 * is requested from the [ServiceLocator]. The resulting instance is then stored and returned for
 * all subsequent requests for the same key. The multi-thread initialization behavior is determined
 * by the [LazyThreadSafetyMode].
 */
public open class LazyKey<T : Any>(override val type: KClass<T>) :
  ServiceKey<T, LazyKey<T>.Entry<T>, Unit, LazyKey.PutParams<T>> {
  override fun createEntry(params: PutParams<T>): LazyKey<T>.Entry<T> =
    Entry(params.provider, params.threadSafetyMode)

  override fun getValue(
    params: Unit,
    entry: ServiceEntry<T>,
  ): T = (entry as ParamlessServiceEntry<T>).service

  public class PutParams<T>(
    public val provider: () -> T,
    public val threadSafetyMode: LazyThreadSafetyMode,
  )

  /**
   * The [ServiceEntry] for a [LazyKey]. It holds the provider lambda and the created service.
   *
   * It includes a check for circular dependencies. If this entry is re-entered while its
   * own `service` is being initialized, it indicates a dependency loop, which will
   * result in an [IllegalStateException].
   */
  public inner class Entry<T : Any>(
    loader: () -> T,
    threadSafetyMode: LazyThreadSafetyMode,
  ) :
    ParamlessServiceEntry<T> {

    @Volatile
    private var isLoading = false

    override val service: T by lazy(threadSafetyMode) {
      if (isLoading) throw IllegalStateException("Circular dependency detected when loading service for key: ${this@LazyKey}") else {
        isLoading = true
        val result = loader()
        isLoading = false
        result
      }
    }
  }

  override fun toString(): String {
    return "LazyKey(type=$type)"
  }

  public companion object {
    /**
     * The default [LazyThreadSafetyMode] used for lazy-initialized dependencies
     * managed by the service locator.
     *
     * Defaults to [LazyThreadSafetyMode.SYNCHRONIZED] to ensure thread-safe
     * initialization by default. This can be changed if a different trade-off
     * between thread safety and performance is desired for most lazy initializations.
     *
     * @see LazyThreadSafetyMode
     */
    public var ServiceLocator.defaultLazyKeyThreadSafetyMode
      get() = getDefaultParams().threadSafetyMode
      set(value) {
        getDefaultParams().threadSafetyMode = value
      }
  }
}

private class DefaultParams {
  var threadSafetyMode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED

  companion object {
    val key = SingletonKey<DefaultParams>()
  }
}

private fun ServiceLocator.getDefaultParams() =
  getOrProvideValue(DefaultParams.key, Unit) { DefaultParams() }

/**
 * Registers a lazy singleton provider for the given [key].
 *
 * The [provider] lambda will be invoked only the first time a value is requested for this [key].
 * The created instance will be stored and reused for all subsequent requests.
 *
 * The multi-thread behavior of the initialization is determined by [threadSafetyMode].
 *
 * @param key The [LazyKey] to associate with the provider.
 * @param threadSafetyMode The [LazyThreadSafetyMode] for the initialization.
 * @param provider A lambda that creates the service instance.
 */
public fun <T : Any> ServiceLocator.put(
  key: LazyKey<T>,
  threadSafetyMode: LazyThreadSafetyMode = getDefaultParams().threadSafetyMode,
  provider: () -> T,
) {
  put(key, LazyKey.PutParams<T>(provider, threadSafetyMode))
}

/**
 * A [ServiceKey] that loads services lazily. The [LazyKey.PutParams.provider] will be
 * invoked the first time the ServiceLocator is queried for the key's value. The multi-thread
 * behavior depends on [LazyKey.PutParams.threadSafetyMode].
 */
public inline fun <reified T : Any> LazyKey(): LazyKey<T> = LazyKey<T>(T::class)
