package com.keyboardr.mapsl.sample.basic

import com.keyboardr.mapsl.SimpleServiceLocator
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A property delegate to access a service stored in [ProcessServiceLocator] using
 * [SimpleServiceLocator.getOrProvide].
 */
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it == ServiceLocatorScope.Production },
  threadSafetyMode: LazyThreadSafetyMode = ProcessServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T,
): ReadOnlyProperty<Any, T> = object : ReadOnlyProperty<Any, T> {
  override fun getValue(thisRef: Any, property: KProperty<*>): T =
    ProcessServiceLocator.instance.getOrProvide(
      allowedScopes,
      threadSafetyMode,
      provider
    )
}