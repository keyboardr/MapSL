package com.keyboardr.mapsl.sample.keysample.locator

import android.content.Context
import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.ScopedServiceLocator
import com.keyboardr.mapsl.classKey
import com.keyboardr.mapsl.get
import com.keyboardr.mapsl.getOrProvide
import com.keyboardr.mapsl.keys.FactoryKey
import com.keyboardr.mapsl.keys.LazyKey
import com.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import com.keyboardr.mapsl.keys.SingletonKey
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object ProcessServiceLocator {
  lateinit var instance: ScopedServiceLocator<ServiceLocatorScope>
    private set

  fun register(
    serviceLocator: ScopedServiceLocator<ServiceLocatorScope>,
    applicationContext: Context,
    registrationBlock: ScopedServiceLocator<ServiceLocatorScope>.() -> Unit = {},
  ) {
    if (serviceLocator.scope is ServiceLocatorScope.ProdScope) {
      check(!::instance.isInitialized) { "ProcessServiceLocator is already initialized" }
    }
    instance = serviceLocator.apply {
      put(applicationContextKey, applicationContext)
      registrationBlock()
    }
  }

  val applicationContextKey = SingletonKey<Context>()
}

sealed interface ServiceLocatorScope {
  sealed interface ProdScope : ServiceLocatorScope

  data class Process(val processName: String) : ProdScope
  object Preview : ServiceLocatorScope
  object Testing : ServiceLocatorScope
}


/**
 * A property delegate to access a service stored in [ProcessServiceLocator] using [getOrProvide].
 */
inline fun <reified T : Any> serviceLocator(
  key: LazyKey<T> = classKey<T>(),
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it is ServiceLocatorScope.ProdScope },
  threadSafetyMode: LazyThreadSafetyMode = ProcessServiceLocator.instance.defaultLazyKeyThreadSafetyMode,
  noinline provider: () -> T,
): ReadOnlyProperty<Any, T> = object : ReadOnlyProperty<Any, T> {
  override fun getValue(thisRef: Any, property: KProperty<*>): T =
    ProcessServiceLocator.instance.getOrProvide(
      key,
      allowedScopes,
      threadSafetyMode,
      provider
    )
}

@OptIn(ExperimentalKeyType::class)
fun <T : Any> FactoryKey<T>.create(): T = ProcessServiceLocator.instance.get(this)