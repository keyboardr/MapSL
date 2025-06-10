package com.keyboardr.mapsl.sample.basic

import com.keyboardr.mapsl.SimpleServiceLocator
import kotlin.properties.ReadOnlyProperty

/**
 * A property delegate to access a service stored in [MainServiceLocator] using
 * [SimpleServiceLocator.getOrProvide].
 */
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it == ServiceLocatorScope.Production },
  threadSafetyMode: LazyThreadSafetyMode = MainServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T,
): ReadOnlyProperty<Any, T> = ReadOnlyProperty { _, _ ->
  MainServiceLocator.instance.getOrProvide(
    allowedScopes,
    threadSafetyMode,
    provider
  )
}