package com.keyboardr.mapsl.keys

import kotlin.reflect.KClass

/**
 * A [ServiceKey] that returns a stored service value.
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
 * A [ServiceKey] that returns a stored service value.
 */
public inline fun <reified T : Any> SingletonKey(): SingletonKey<T> = SingletonKey<T>(T::class)
