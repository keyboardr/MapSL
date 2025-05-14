package com.keyboardr.mapsl.sample.basic

import android.content.Context
import com.keyboardr.mapsl.SimpleServiceLocator

object ProcessServiceLocator {
  lateinit var instance: SimpleServiceLocator<ServiceLocatorScope>
    private set

  fun register(
    serviceLocator: SimpleServiceLocator<ServiceLocatorScope>,
    applicationContext: Context,
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {},
  ) {
    if (serviceLocator.scope == ServiceLocatorScope.Production) {
      check(!::instance.isInitialized) { "ProcessServiceLocator is already initialized" }
    }
    instance = serviceLocator.apply {
      put<ApplicationContextHolder> { ApplicationContextHolder(applicationContext) }
      registrationBlock()
    }
  }

  private class ApplicationContextHolder(val context: Context)

  val applicationContext
    get() = instance.get<ApplicationContextHolder>().context
}

enum class ServiceLocatorScope { Production, Testing }

/**
 * Provides services in production environments
 * @see SimpleServiceLocator.getOrProvide
 */
inline fun <reified T : Any> SimpleServiceLocator<ServiceLocatorScope>.getOrProvide(
  threadSafetyMode: LazyThreadSafetyMode = defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T,
) = getOrProvide({ it == ServiceLocatorScope.Production }, threadSafetyMode, provider)
