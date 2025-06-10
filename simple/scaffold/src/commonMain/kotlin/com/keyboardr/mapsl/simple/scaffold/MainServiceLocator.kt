package com.keyboardr.mapsl.simple.scaffold

import com.keyboardr.mapsl.SimpleServiceLocator
import com.keyboardr.mapsl.simple.scaffold.MainServiceLocator.register

/**
 * Holds the main [SimpleServiceLocator] for the application.
 *
 * [register] should be called exactly once at the start of the application. On Android, this is
 * already done for you via ServiceLocatorInitializer. You can disable this by following
 * [these instructions](https://developer.android.com/topic/libraries/app-startup#disable-individual).
 */
public object MainServiceLocator {
  public lateinit var instance: SimpleServiceLocator<ServiceLocatorScope>
    private set

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
 * Indicates which scope the [MainServiceLocator] was registered in.
 */
public enum class ServiceLocatorScope {
  Production, Testing
}


/**
 * Provides services in production environments
 * @see SimpleServiceLocator.getOrProvide
 */
public inline fun <reified T : Any> SimpleServiceLocator<ServiceLocatorScope>.getOrProvide(
  threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T,
) = getOrProvide({ it == ServiceLocatorScope.Production }, threadSafetyMode, provider)