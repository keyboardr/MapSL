package dev.keyboardr.mapsl

import dev.keyboardr.mapsl.keys.LazyKey
import dev.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import dev.keyboardr.mapsl.keys.ServiceEntry
import dev.keyboardr.mapsl.keys.ServiceKey
import kotlin.jvm.JvmOverloads

/**
 * A [ServiceLocator] that is associated with a specific [scope].
 *
 * The [scope] acts as a label to identify the environment in which the locator is operating
 * (e.g., `Production`, `Testing`). This allows for conditional service registration based
 * on the current context.
 *
 * @param S The type of the scope identifier.
 * @param scope The specific scope instance for this locator.
 * @param allowReregister if true, allow registering keys multiple times. The latest registration
 * will be used. This should generally only be true for tests.
 * @see getOrProvide
 */
public open class ScopedServiceLocator<out S> @JvmOverloads constructor(
  public val scope: S,
  allowReregister: Boolean = false,
) :
  ServiceLocator(allowReregister) {

  /**
   * Fetches an item for the given [key].
   *
   * If an entry for the [key] has not been previously registered, this function determines
   * how to create a new one. Creation is subject to the [allowedScopes] predicate.
   * - If the scope is allowed, a new entry is created using the provided [putParams].
   * - If the scope is not allowed, it delegates to [onInvalidScope] to create a fallback entry.
   *
   * @param key The [ServiceKey] for the desired service.
   * @param allowedScopes A predicate to check if the current [scope] is valid for this provider.
   * @param putParams The parameters needed to create a new [ServiceEntry] for [key] if one doesn't exist.
   * @param getParams The parameters needed to retrieve the value from the entry.
   */
  public fun <T : Any, GetParams, PutParams> getOrProvide(
    key: ServiceKey<T, *, GetParams, PutParams>,
    allowedScopes: (S) -> Boolean,
    putParams: PutParams,
    getParams: GetParams,
  ): T =
    getValue(
      key,
      getParams,
      getOrProvideEntry(key) {
        if (allowedScopes(scope)) key.createEntry(putParams) else onInvalidScope(key, putParams)
      })

  /**
   * Fetches an item for the given reified type [T].
   *
   * If an instance has not been previously registered, it creates and stores a new one using the
   * [provider] lambda. Creation is subject to the [allowedScopes] predicate; if the current
   * scope is not allowed, this delegates to [onInvalidScope].
   *
   * This is a convenience function equivalent to calling `getOrProvide` with `classKey<T>()`.
   *
   * @param T The service type to retrieve.
   * @param allowedScopes A predicate to check if the current [scope] is valid for this provider.
   * @param threadSafetyMode The thread safety mode for the lazy initialization.
   * @param provider A lambda that creates the service instance if one doesn't exist.
   */
  @JvmOverloads
  public inline fun <reified T : Any> getOrProvide(
    noinline allowedScopes: (S) -> Boolean,
    threadSafetyMode: LazyThreadSafetyMode = defaultLazyKeyThreadSafetyMode,
    noinline provider: () -> T,
  ): T {
    return getOrProvide(
      classKey(),
      allowedScopes,
      LazyKey.PutParams(provider, threadSafetyMode),
      Unit
    )
  }


  /**
   * Called when [getOrProvide] is attempted for a key in a disallowed scope.
   *
   * The default behavior is to throw a [ServiceLocatorException]. Subclasses can override this
   * to provide a mock or other fallback instance.
   *
   * @param key The [ServiceKey] for which the provision was attempted.
   * @param putParams The original parameters that would have been used for registration.
   * @return A [ServiceEntry] to be used as a fallback.
   */
  public open fun <T : Any, PutParams> onInvalidScope(
    key: ServiceKey<T, *, *, PutParams>,
    putParams: PutParams,
  ): ServiceEntry<T> =
    throw ServiceLocatorException("Unsupported scope $scope for key $key", key)
}

/**
 * Fetches an item for the given [key].
 *
 * If the key has not been previously registered, will create a new
 * entry using [putParams]. If [allowedScopes] returns `false` for this [ServiceLocator]'s
 * [scope][ScopedServiceLocator.scope], the created entry will come from
 * [ScopedServiceLocator.onInvalidScope].
 *
 * @param key The [ServiceKey] for the desired service.
 * @param allowedScopes A predicate to check if the current [ScopedServiceLocator.scope] is valid for this provider.
 * @param putParams The parameters needed to create a new [ServiceEntry] for [key] if one doesn't exist.
 */
public fun <S, T : Any, PutParams> ScopedServiceLocator<S>.getOrProvide(
  key: ServiceKey<T, *, Unit, PutParams>,
  allowedScopes: (S) -> Boolean,
  putParams: PutParams,
): T {
  return getOrProvide(key, allowedScopes, putParams, Unit)
}

/**
 * Fetches an item for the given [key].
 *
 * If an entry for the [key] has not been previously registered, it creates and stores a new one using the
 * [provider] lambda. Creation is subject to the [allowedScopes] predicate; if the current
 * scope is not allowed, this delegates to [ScopedServiceLocator.onInvalidScope].
 *
 * The multi-thread behavior depends on [threadSafetyMode].
 *
 * @param key The [ServiceKey] for the desired service.
 * @param allowedScopes A predicate to check if the current [ScopedServiceLocator.scope] is valid for this provider.
 * @param threadSafetyMode The thread safety mode for the lazy initialization.
 * @param provider A lambda that creates the service instance if one doesn't exist.
 */
@JvmOverloads
public fun <S, T : Any> ScopedServiceLocator<S>.getOrProvide(
  key: LazyKey<T>,
  allowedScopes: (S) -> Boolean,
  threadSafetyMode: LazyThreadSafetyMode = defaultLazyKeyThreadSafetyMode,
  provider: () -> T,
): T {
  return getOrProvide(key, allowedScopes, LazyKey.PutParams(provider, threadSafetyMode), Unit)
}
