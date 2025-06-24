package dev.keyboardr.mapsl.sample.keysample.locator

import android.content.Context
import dev.keyboardr.mapsl.ExperimentalKeyType
import dev.keyboardr.mapsl.ScopedServiceLocator
import dev.keyboardr.mapsl.classKey
import dev.keyboardr.mapsl.get
import dev.keyboardr.mapsl.getOrProvide
import dev.keyboardr.mapsl.keys.FactoryKey
import dev.keyboardr.mapsl.keys.LazyKey
import dev.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import dev.keyboardr.mapsl.keys.SingletonKey
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object MainServiceLocator {
  lateinit var instance: ScopedServiceLocator<ServiceLocatorScope>
    private set

  fun register(
    serviceLocator: ScopedServiceLocator<ServiceLocatorScope>,
    applicationContext: Context,
    registrationBlock: ScopedServiceLocator<ServiceLocatorScope>.() -> Unit = {},
  ) {
    if (serviceLocator.scope is ServiceLocatorScope.ProdScope) {
      check(!::instance.isInitialized) { "MainServiceLocator is already initialized" }
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
 * A property delegate to access a service stored in [MainServiceLocator] using [getOrProvide].
 */
inline fun <reified T : Any> serviceLocator(
  key: LazyKey<T> = classKey<T>(),
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it is ServiceLocatorScope.ProdScope },
  threadSafetyMode: LazyThreadSafetyMode = MainServiceLocator.instance.defaultLazyKeyThreadSafetyMode,
  noinline provider: () -> T,
): ReadOnlyProperty<Any, T> = object : ReadOnlyProperty<Any, T> {
  override fun getValue(thisRef: Any, property: KProperty<*>): T =
    MainServiceLocator.instance.getOrProvide(
      key,
      allowedScopes,
      threadSafetyMode,
      provider
    )
}

@OptIn(ExperimentalKeyType::class)
fun <T : Any, GetParams> FactoryKey<T, GetParams>.create(params: GetParams): T =
  MainServiceLocator.instance.get(this, params)

@OptIn(ExperimentalKeyType::class)
fun <T : Any> FactoryKey<T, Unit>.create(): T = MainServiceLocator.instance.get(this)