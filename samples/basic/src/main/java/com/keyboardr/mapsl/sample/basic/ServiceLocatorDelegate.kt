package com.keyboardr.mapsl.sample.basic

import com.keyboardr.mapsl.SimpleServiceLocator
import kotlin.properties.ReadOnlyProperty

/**
 * A property delegate to access a service stored in [ProcessServiceLocator] using
 * [SimpleServiceLocator.getOrProvide].
 */
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it == ServiceLocatorScope.Production },
  threadSafetyMode: LazyThreadSafetyMode = ProcessServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T,
): ReadOnlyProperty<Any, T> = ReadOnlyProperty { _, _ ->
  ProcessServiceLocator.instance.getOrProvide(
    allowedScopes,
    threadSafetyMode,
    provider
  )
}