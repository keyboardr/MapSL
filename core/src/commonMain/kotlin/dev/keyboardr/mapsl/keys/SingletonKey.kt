package dev.keyboardr.mapsl.keys

import dev.keyboardr.mapsl.ServiceLocator
import kotlin.reflect.KClass

/**
 * A [ServiceKey] for a pre-instantiated, eagerly-loaded service.
 *
 * This key is intended for services that you want to create and register at the same time,
 * typically during the initial setup of your [ServiceLocator]. When you register a
 * `SingletonKey` using `put()`, you provide the actual service instance directly.
 * That instance is then stored and returned for all subsequent requests.
 *
 * While it is possible to use this key with a late-registration function like `getOrProvide()`,
 * doing so negates the eager-loading aspect of this key. For lazy-loading behavior,
 * [LazyKey] is the more appropriate choice.
 */
public open class SingletonKey<T : Any>(override val type: KClass<T>) :
  ServiceKey<T, SingletonKey.Entry<T>, Unit, T> {
  override fun createEntry(params: T): Entry<T> {
    return Entry(params)
  }

  override fun getValue(
    params: Unit,
    entry: ServiceEntry<T>,
  ): T = (entry as ParamlessServiceEntry<T>).service

  public class Entry<T>(override val service: T) : ParamlessServiceEntry<T>
}

/**
 * Creates a [SingletonKey] for the reified type `T`.
 */
public inline fun <reified T : Any> SingletonKey(): SingletonKey<T> = SingletonKey<T>(T::class)
