package dev.keyboardr.mapsl.simple.scaffold

import kotlin.properties.ReadOnlyProperty


/**
 * Creates a property delegate for lazily retrieving a service from the [MainServiceLocator].
 *
 * This is the recommended pattern for defining and accessing singleton services. The service
 * will be created via the [provider] lambda only on its first access and then stored in the
 * locator for subsequent retrievals.
 *
 * By default, the provider is only executed in the [ServiceLocatorScope.Production] scope. In
 * a `Testing` scope, accessing the property will delegate to the registered `TestServiceLocator`,
 * which will typically provide a mock.
 *
 * @param allowedScopes A predicate to determine if the service can be created in the current scope.
 * @param threadSafetyMode The thread safety mode for the lazy initialization.
 * @param provider A lambda that creates the service instance.
 */
public inline fun <reified T : Any> serviceLocator(
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
