package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ExperimentalKeyType
import kotlin.reflect.KClass

/**
 * A [ServiceKey] which invokes its provider each time a value is requested.
 */
@ExperimentalKeyType
public open class FactoryKey<T : Any>(override val type: KClass<T>) :
  ServiceKey<T, FactoryKey.Entry<T>, Unit, () -> T> {
  override fun createEntry(params: () -> T): Entry<T> = Entry(params)

  override fun getValue(
    params: Unit,
    entry: ServiceEntry<T>,
  ): T {
    return (entry as Entry<T>).create()
  }

  public class Entry<T>(private val factory: () -> T) : ServiceEntry<T> {
    public fun create(): T = factory()
  }
}

/**
 * A [ServiceKey] which invokes its provider each time a value is requested.
 */
@ExperimentalKeyType
public inline fun <reified T : Any> FactoryKey(): FactoryKey<T> = FactoryKey<T>(T::class)
