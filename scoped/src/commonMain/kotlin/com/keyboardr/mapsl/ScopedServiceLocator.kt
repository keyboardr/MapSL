package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.LazyKey
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey

/**
 * A [ServiceLocator] which applies to a given scope. The [scope] is used to
 * determine whether this [ServiceLocator] is appropriate for the current context.
 */
public open class ScopedServiceLocator<out S>(public val scope: S, allowReregister: Boolean = false) :
  ServiceLocator(allowReregister) {

  /**
   * Fetches the item for [key]. If the key has not been previously registered, will create a new
   * entry using [putParams]. If [allowedScopes] returns `false` for this [ServiceLocator]'s [scope],
   * the created entry will come from [onInvalidScope].
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
    threadSafetyMode: LazyThreadSafetyMode = LazyKey.PutParams.defaultThreadSafetyMode,
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
   * Called when attempting to provide a value for a disallowed scope. The default
   * behavior is to throw a [ServiceLocatorException].
   */
  public open fun <T : Any, PutParams> onInvalidScope(
    key: ServiceKey<T, *, *, PutParams>,
    putParams: PutParams
  ): ServiceEntry<T> =
    throw ServiceLocatorException("Unsupported scope $scope for key $key", key)
}

public fun <S, T : Any, PutParams> ScopedServiceLocator<S>.getOrProvide(
  key: ServiceKey<T, *, Unit, PutParams>,
  allowedScopes: (S) -> Boolean,
  putParams: PutParams
): T {
  return getOrProvide(key, allowedScopes, putParams, Unit)
}

public fun <S, T : Any> ScopedServiceLocator<S>.getOrProvide(
  key: LazyKey<T>,
  allowedScopes: (S) -> Boolean,
  threadSafetyMode: LazyThreadSafetyMode = LazyKey.PutParams.defaultThreadSafetyMode,
  provider: () -> T,
): T {
  return getOrProvide(key, allowedScopes, LazyKey.PutParams(provider, threadSafetyMode), Unit)
}