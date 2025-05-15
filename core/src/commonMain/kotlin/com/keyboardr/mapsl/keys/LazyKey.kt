package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ServiceLocator
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

/**
 * A [ServiceKey] that loads services lazily. The [PutParams.provider] will be
 * invoked the first time the ServiceLocator is queried for the key's value. The multi-thread
 * behavior depends on [PutParams.threadSafetyMode].
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
 * Registers a provider for [key]. [provider] will be invoked the first time a value is
 * requested for [key]. The multi-thread behavior depends on [threadSafetyMode].
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