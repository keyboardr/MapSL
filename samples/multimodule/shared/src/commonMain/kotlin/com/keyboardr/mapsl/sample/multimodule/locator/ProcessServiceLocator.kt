package com.keyboardr.mapsl.sample.multimodule.locator

import com.keyboardr.mapsl.SimpleServiceLocator
import com.keyboardr.mapsl.sample.multimodule.platform.PlatformContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object ProcessServiceLocator {
  lateinit var instance: SimpleServiceLocator<ServiceLocatorScope>
    private set

  fun register(
    serviceLocator: SimpleServiceLocator<ServiceLocatorScope>,
    applicationContext: PlatformContext,
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {},
  ) {
    if (serviceLocator.scope is ServiceLocatorScope.ProdScope) {
      check(!::instance.isInitialized) { "ProcessServiceLocator is already initialized" }
    }
    instance = serviceLocator.apply {
      put<PlatformContext> { applicationContext }
      registrationBlock()
    }
  }

  val applicationContext
    get() = instance.get<PlatformContext>()
}

sealed interface ServiceLocatorScope {
  sealed interface ProdScope : ServiceLocatorScope

  data class Process(val processName: String) : ProdScope
  object Preview : ServiceLocatorScope
  object Testing : ServiceLocatorScope
}


/**
 * A property delegate to access a service stored in [ProcessServiceLocator] using
 * [SimpleServiceLocator.getOrProvide].
 */
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it is ServiceLocatorScope.ProdScope },
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
