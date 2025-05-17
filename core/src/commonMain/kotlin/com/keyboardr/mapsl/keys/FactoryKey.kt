package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ExperimentalKeyType
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * A [ServiceKey] which invokes its provider each time a value is requested. May take additional
 * parameters when creating a new value.
 *
 * @param T The type of items to be produced.
 * @param GetParams The type of parameters to be passed to the provider.
 */
@ExperimentalKeyType
public open class FactoryKey<T : Any, GetParams>(override val type: KClass<T>) :
  ServiceKey<T, FactoryKey.Entry<T, GetParams>, GetParams, (GetParams) -> T> {
  override fun createEntry(params: (GetParams) -> T): Entry<T, GetParams> = Entry(params)

  override fun getValue(
    params: GetParams,
    entry: ServiceEntry<T>,
  ): T {
    @Suppress("UNCHECKED_CAST")
    return (entry as Entry<T, GetParams>).create(params)
  }

  public class Entry<T, GetParams>(private val factory: (GetParams) -> T) : ServiceEntry<T> {
    public fun create(params: GetParams): T = factory(params)
  }
}

/**
 * A [ServiceKey] which invokes its provider each time a value is requested.
 */
@ExperimentalKeyType
public fun <T : Any> FactoryKey(type: KClass<T>): FactoryKey<T, Unit> = FactoryKey<T, Unit>(type)

/**
 * A [ServiceKey] which invokes its provider each time a value is requested. May take additional
 * parameters when creating a new value.
 */
@ExperimentalKeyType
public inline fun <reified T : Any, GetParams> FactoryKey(): FactoryKey<T, GetParams> =
  FactoryKey<T, GetParams>(T::class)

/**
 * A [ServiceKey] which invokes its provider each time a value is requested.
 */
@ExperimentalKeyType
@JvmName("FactoryKeyUnit")
public inline fun <reified T : Any> FactoryKey(): FactoryKey<T, Unit> = FactoryKey<T>(T::class)


