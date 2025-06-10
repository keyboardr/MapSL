package com.keyboardr.mapsl.simple.scaffold

import com.keyboardr.mapsl.SimpleServiceLocator
import com.keyboardr.mapsl.simple.scaffold.MainServiceLocator.register

/**
 * Holds the pre-configured, application-wide [SimpleServiceLocator] instance.
 *
 * This object is the central access point for all services when using the `simple-scaffold`.
 *
 * On Android, this locator is initialized automatically on app startup. If you need to customize
 * the initialization process, you must first disable the automatic initializer.
 * @see [register]
 */
public object MainServiceLocator {
  public lateinit var instance: SimpleServiceLocator<ServiceLocatorScope>
    private set

  /**
   * Initializes or replaces the `MainServiceLocator` instance.
   *
   * This should typically only be called once at application startup. On Android, this is handled
   * automatically by [ServiceLocatorInitializer]. If you need to perform manual registration
   * (e.g., to pre-register services at startup), you must first disable the automatic
   * initializer to avoid conflicts.
   *
   * @see [Disable automatic initialization](https://developer.android.com/topic/libraries/app-startup#disable-individual)
   */
  public fun register(
    serviceLocator: SimpleServiceLocator<ServiceLocatorScope>,
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {},
  ) {
    if (serviceLocator.scope == ServiceLocatorScope.Production) {
      check(!::instance.isInitialized) {
        if (instance is PreRegistered) {
          """MainServiceLocator was registered automatically. If you wish to register manually,
            | disable com.keyboardr.mapsl.simple.scaffold.ServiceLocatorInitializer.
            | See https://developer.android.com/topic/libraries/app-startup#disable-individual."""
            .trimMargin()
        } else {
          "MainServiceLocator is already initialized"
        }
      }
    }
    instance = serviceLocator.apply {
      registrationBlock()
    }
  }

  public fun register(
    scope: ServiceLocatorScope,
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit,
  ) {
    register(SimpleServiceLocator(scope), registrationBlock)
  }
}

internal interface PreRegistered

/**
 * Defines the environments for the `simple-scaffold`.
 */
public enum class ServiceLocatorScope {
  /** The scope for a standard application runtime environment. */
  Production,
  /** The scope for a test runtime environment. */
  Testing
}


/**
 * Fetches a service from the locator, creating and storing it if it doesn't already exist.
 * This provider is only active in the `Production` scope.
 *
 * @see SimpleServiceLocator.getOrProvide
 */
public inline fun <reified T : Any> SimpleServiceLocator<ServiceLocatorScope>.getOrProvide(
  threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T,
) = getOrProvide({ it == ServiceLocatorScope.Production }, threadSafetyMode, provider)
