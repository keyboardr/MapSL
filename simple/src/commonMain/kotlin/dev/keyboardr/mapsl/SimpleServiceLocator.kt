package dev.keyboardr.mapsl

import dev.keyboardr.mapsl.keys.LazyClassKey
import dev.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import dev.keyboardr.mapsl.keys.ServiceEntry
import dev.keyboardr.mapsl.keys.ServiceKey
import dev.keyboardr.mapsl.keys.put
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/**
 * A service locator designed for the common use case of managing lazy, class-keyed singletons.
 *
 * This class allows services to be registered and retrieved using their class type as an identifier,
 * providing a straightforward way to manage application-wide dependencies.
 *
 * @param S The type of the scope identifier.
 * @param scope The specific scope instance for this locator.
 * @param allowReregister if true, allow registering keys multiple times. The latest registration
 * will be used. This should generally only be true for tests.
 */
public open class SimpleServiceLocator<out S> @JvmOverloads constructor(
  scope: S,
  allowReregister: Boolean = false
) {
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
   * By default, it is an error to register the same `key` more than once. This behavior can be
   * changed by setting `allowReregister` to `true` in the constructor, in which case subsequent
   * registrations will overwrite previous ones.
   *
   * The multi-thread behavior depends on [threadSafetyMode].
   *
   * @param key The [KClass] to associate with the provider.
   * @param threadSafetyMode The thread safety mode for the lazy initialization.
   * @param provider A lambda that creates the service instance.
   */
  @JvmOverloads
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
   * The [provider] lambda will be invoked only the first time a value is requested for this `key`.
   * The resulting instance is then stored and returned for all subsequent requests.
   *
   * By default, it is an error to register the same `key` more than once. This behavior can be
   * changed by setting `allowReregister` to `true` in the constructor, in which case subsequent
   * registrations will overwrite previous ones.
   *
   * The multi-thread behavior depends on [threadSafetyMode].
   *
   * This is a convenience function that is equivalent to calling [put] with `T::class` as the key.
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
   * Fetches the singleton instance for the given class [key]. If no provider has been registered,
   * this will delegate to [onMiss].
   */
  public fun <T : Any> get(key: KClass<T>): T = backingServiceLocator.get(LazyClassKey(key))

  /**
   * Fetches the singleton instance for the reified type [T]. If no provider has been registered,
   * this will delegate to [onMiss].
   */
  public inline fun <reified T : Any> get(): T = get(T::class)

  /**
   * Fetches the singleton instance for the given class [key]. If no provider has been registered,
   * returns `null`. Does not invoke `onMiss`.
   */
  public fun <T : Any> getOrNull(key: KClass<T>): T? =
    backingServiceLocator.getOrNull(LazyClassKey(key))

  /**
   * Fetches the singleton instance for the reified type [T]. If no provider has been registered,
   * returns `null`. Does not invoke `onMiss`.
   */
  public inline fun <reified T : Any> getOrNull(): T? = getOrNull(T::class)


  /**
   * Fetches the singleton instance for the given class [key].
   *
   * If an instance has not been previously registered, it creates and stores a new one using the
   * [provider] lambda. Creation is subject to the [allowedScopes] predicate; if the current
   * scope is not allowed, this delegates to [onInvalidScope].
   *
   * The multi-thread behavior depends on [threadSafetyMode].
   *
   * @param key The class of the service to retrieve.
   * @param allowedScopes A predicate to check if the current [scope] is valid for this provider.
   * @param threadSafetyMode The thread safety mode for the lazy initialization.
   * @param provider A lambda that creates the service instance if one doesn't exist.
   */
  @JvmOverloads
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
   * [provider] lambda. Creation is subject to the [allowedScopes] predicate; if the current
   * scope is not allowed, this delegates to [onInvalidScope].
   *
   * The multi-thread behavior depends on [threadSafetyMode].
   *
   * This is a convenience function equivalent to calling `getOrProvide` with `T::class`.
   *
   * @param T The service type to retrieve.
   * @param allowedScopes A predicate to check if the current [scope] is valid for this provider.
   * @param threadSafetyMode The thread safety mode for the lazy initialization.
   * @param provider A lambda that creates the service instance if one doesn't exist.
   */
  @JvmOverloads
  public inline fun <reified T : Any> getOrProvide(
    noinline allowedScopes: (S) -> Boolean,
    threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
    noinline provider: (S) -> T,
  ): T = getOrProvide(T::class, allowedScopes, threadSafetyMode, provider)

  /**
   * Called when [get] is called for a class [key] that has no registered provider.
   *
   * The default behavior is to throw an [IllegalArgumentException]. Subclasses can override this
   * to provide a different fallback mechanism.
   */
  protected open fun <T : Any> onMiss(key: KClass<T>): T =
    throw IllegalArgumentException("No value found for key: $key")


  /**
   * Called when [getOrProvide] is attempted for a class [key] in a disallowed scope.
   *
   * The default behavior is to throw an [IllegalArgumentException]. Subclasses can override this
   * to provide a mock or other fallback instance.
   */
  protected open fun <T : Any> onInvalidScope(key: KClass<T>): T =
    throw IllegalArgumentException("Unsupported scope ${backingServiceLocator.scope} for key $key")
}
