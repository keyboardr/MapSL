package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.LazyKey
import com.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey

/**
 * A [ServiceLocator] that is associated with a specific [scope].
 *
 * The [scope] acts as a label to identify the environment in which the locator is operating
 * (e.g., `Production`, `Testing`). This allows for conditional service registration based
 * on the current context.
 *
 * @param S The type of the scope identifier.
 * @param scope The specific scope instance for this locator.
 * @param allowReregister If true, allows keys to be registered multiple times.
 * @see getOrProvide
 */
public open class ScopedServiceLocator<out S>(
  public val scope: S,
  allowReregister: Boolean = false,
) :
  ServiceLocator(allowReregister) {

  /**
   * Fetches an item for the given [key].
   *
   * If an entry for the [key] has not been previously registered, this function determines
   * how to create a new one. It checks if this locator's [scope] is permitted by the
   * [allowedScopes] predicate.
   * - If the scope is allowed, a new entry is created using the provided [putParams].
   * - If the scope is not allowed, it delegates to [onInvalidScope] to create a fallback entry.
   *
   * @param key The [ServiceKey] for the desired service.
   * @param allowedScopes A predicate to check if the current [scope] is valid for this provider.
   * @param putParams The parameters needed to create a new [ServiceEntry] if one doesn't exist.
   * @param getParams The parameters needed to retrieve the value from the entry.
   * @return The requested service instance.
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
   * Called to create a fallback [ServiceEntry] when `getOrProvide` is called for a service
   * in a scope that is disallowed by the `allowedScopes` predicate.
   *
   * The default behavior is to throw a [ServiceLocatorException]. Subclasses, such as a
   * testing-specific locator, may override this to provide a mock or fake entry instead.
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
 * Fetches an item for [key]. If the key has not been previously registered, will create a new
 * entry using [putParams]. If [allowedScopes] returns `false` for this [ServiceLocator]'s
 * [scope][ScopedServiceLocator.scope], the created entry will come from
 * [ScopedServiceLocator.onInvalidScope].
 */
public fun <S, T : Any, PutParams> ScopedServiceLocator<S>.getOrProvide(
  key: ServiceKey<T, *, Unit, PutParams>,
  allowedScopes: (S) -> Boolean,
  putParams: PutParams,
): T {
  return getOrProvide(key, allowedScopes, putParams, Unit)
}


/**
 * Fetches the item for [key]. If the key has not been previously registered, will create a new
 * entry using [provider]. If [allowedScopes] returns `false` for this [ServiceLocator]'s
 * [scope][ScopedServiceLocator.scope], the created entry will come from
 * [ScopedServiceLocator.onInvalidScope].
 *
 * The multi-thread behavior depends on [threadSafetyMode].
 */
public fun <S, T : Any> ScopedServiceLocator<S>.getOrProvide(
  key: LazyKey<T>,
  allowedScopes: (S) -> Boolean,
  threadSafetyMode: LazyThreadSafetyMode = defaultLazyKeyThreadSafetyMode,
  provider: () -> T,
): T {
  return getOrProvide(key, allowedScopes, LazyKey.PutParams(provider, threadSafetyMode), Unit)
}
