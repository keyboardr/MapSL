package com.keyboardr.mapsl.sample.multimodule.locator

// Rather than using property delegation, you can use interface delegation on companion objects to
// provide services.

/**
 * Provides an instance of a service. Intended to be used for interface delegation with [serviceProvider].
 *
 * @see com.keyboardr.mapsl.sample.multimodule.services.BarManager
 */
interface ServiceProvider<T : Any> {
  val instance: T
}

/**
 * Provides a service from [ProcessServiceLocator]. Intended to be used for interface delegation.
 */
inline fun <reified T : Any> serviceProvider(
  noinline provider: (ServiceLocatorScope) -> T,
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it is ServiceLocatorScope.ProdScope },
  threadSafetyMode: LazyThreadSafetyMode = ProcessServiceLocator.instance.defaultThreadSafetyMode,
): ServiceProvider<T> = object : ServiceProvider<T> {
  override val instance by serviceLocator(allowedScopes, threadSafetyMode, provider)
}