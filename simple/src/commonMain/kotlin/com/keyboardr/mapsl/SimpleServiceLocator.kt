package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.LazyClassKey
import com.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey
import com.keyboardr.mapsl.keys.put
import kotlin.reflect.KClass

/**
 * A service locator designed for the common use case of managing lazy, class-keyed singletons.
 *
 * This class allows services to be registered and retrieved using their class type as an identifier,
 * providing a straightforward way to manage application-wide dependencies.
 *
 * @param S The type of the scope identifier.
 * @param scope The specific scope instance for this locator.
 * @param allowReregister If true, allows keys to be registered multiple times.
 */
public open class SimpleServiceLocator<out S>(scope: S, allowReregister: Boolean = false) {
  private val backingServiceLocator = object : ScopedServiceLocator<S>(scope, allowReregister) {
    override fun <T : Any, GetParams> onMiss(
      key: ServiceKey<T, *, GetParams, *>,
      params: GetParams,
    ): T = getOrProvide(key.type, { true }) { onMiss(key.type) }

    override fun <T : Any, PutParams> onInvalidScope(
      key: ServiceKey<T, *, *, PutParams>,
      putParams: PutParams,
    ): ServiceEntry<T> = SimpleEntry(onInvalidScope(key.type))

    override fun <T : Any, GetParams> getValue(
      key: ServiceKey<T, *, GetParams, *>,
      params: GetParams,
      entry: ServiceEntry<T>,
    ): T = if (entry is SimpleEntry<T>) entry.value else super.getValue(key, params, entry)
  }

  private class SimpleEntry<T>(val value: T) : ServiceEntry<T>

  public val scope: S = backingServiceLocator.scope

  /**
   * The default [LazyThreadSafetyMode] used for lazy-initialized dependencies
   * managed by this service locator.
   *
   * Defaults to [LazyThreadSafetyMode.SYNCHRONIZED] to ensure thread-safe
   * initialization by default.
   *
   * @see LazyThreadSafetyMode
   */
  public var defaultThreadSafetyMode: LazyThreadSafetyMode
    get() = backingServiceLocator.defaultLazyKeyThreadSafetyMode
    set(value) {
      backingServiceLocator.defaultLazyKeyThreadSafetyMode = value
    }

  /**
   * Registers a lazy singleton provider for the given class [key].
   *
   * The [provider] lambda will be invoked only the first time a value is requested for this `key`.
   * The resulting instance is then stored and returned for all subsequent requests.
   *
   * By default, it is an error to register the same `key` more than once.
   *
   * @param key The [KClass] to associate with the provider.
   * @param threadSafetyMode The thread safety mode for the lazy initialization.
   * @param provider A lambda that creates the service instance.
   */
  public fun <T : Any> put(
    key: KClass<T>,
    threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
    provider: () -> T,
  ) {
    backingServiceLocator.put<T>(LazyClassKey(key), threadSafetyMode) { provider() }
  }

  /**
   * Registers a lazy singleton provider for the reified type [T].
   *
   * This is a convenience function that is equivalent to calling [put] with `T::class` as the key.
   * By default, it is an error to register the same type `T` more than once.
   *
   * @param T The service type to register.
   * @param threadSafetyMode The thread safety mode for the lazy initialization.
   * @param provider A lambda that creates the service instance.
   */
  public inline fun <reified T : Any> put(
    threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
    noinline provider: () -> T,
  ) {
    put(T::class, threadSafetyMode, provider)
  }

  /**
   * Fetches the singleton instance for the given class [key]. If the service was not previously
   * registered, this will delegate to [onMiss].
   */
  public fun <T : Any> get(key: KClass<T>): T = backingServiceLocator.get(LazyClassKey(key))

  /**
   * Fetches the singleton instance for the reified type [T]. If the service was not previously
   * registered, this will delegate to [onMiss].
   */
  public inline fun <reified T : Any> get(): T = get(T::class)

  /**
   * Fetches the singleton instance for the given class [key]. If the service was not previously
   * registered, this returns `null` and does not invoke [onMiss].
   */
  public fun <T : Any> getOrNull(key: KClass<T>): T? =
    backingServiceLocator.getOrNull(LazyClassKey(key))

  /**
   * Fetches the singleton instance for the reified type [T]. If the service was not previously
   * registered, this returns `null` and does not invoke [onMiss].
   */
  public inline fun <reified T : Any> getOrNull(): T? = getOrNull(T::class)


  /**
   * Fetches the singleton instance for the given class [key].
   *
   * If an instance has not been previously registered, it creates and stores a new one using the
   * [provider] lambda. Creation is subject to the [allowedScopes] predicate; if the current
   * scope is not allowed, this delegates to [onInvalidScope].
   *
   * @param key The class of the service to retrieve.
   * @param allowedScopes A predicate to check if the current [scope] is valid for this provider.
   * @param threadSafetyMode The thread safety mode for the lazy initialization.
   * @param provider A lambda that creates the service instance if one doesn't exist.
   */
  public fun <T : Any> getOrProvide(
    key: KClass<T>,
    allowedScopes: (S) -> Boolean,
    threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
    provider: (S) -> T,
  ): T = backingServiceLocator.getOrProvide(
    LazyClassKey<T>(key),
    allowedScopes,
    threadSafetyMode,
  ) {
    provider(scope)
  }

  /**
   * Fetches the singleton instance for the reified type [T].
   *
   * If an instance has not been previously registered, it creates and stores a new one using the
   * [provider] lambda. This is a convenience function equivalent to calling `getOrProvide` with
   * `T::class`.
   */
  public inline fun <reified T : Any> getOrProvide(
    noinline allowedScopes: (S) -> Boolean,
    threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
    noinline provider: (S) -> T,
  ): T = getOrProvide(T::class, allowedScopes, threadSafetyMode, provider)

  /**
   * Called when [get] is called for a class [key] that has no registered provider.
   *
   * The default behavior is to throw an [IllegalArgumentException]. Subclasses can override this
   * to provide a different fallback mechanism, such as creating a mock.
   */
  protected open fun <T : Any> onMiss(key: KClass<T>): T =
    throw IllegalArgumentException("No value found for key: $key")


  /**
   * Called when [getOrProvide] is attempted for a class [key] in a disallowed scope.
   *
   * The default behavior is to throw an [IllegalArgumentException]. Subclasses, such as a
   * testing locator, can override this to provide a mock or other fallback instance.
   */
  protected open fun <T : Any> onInvalidScope(key: KClass<T>): T =
    throw IllegalArgumentException("Unsupported scope ${backingServiceLocator.scope} for key $key")
}
