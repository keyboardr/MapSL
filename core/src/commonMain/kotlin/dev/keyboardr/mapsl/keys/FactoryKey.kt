package dev.keyboardr.mapsl.keys

import dev.keyboardr.mapsl.ExperimentalKeyType
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * A [ServiceKey] that creates a new instance of a service every time one is requested.
 *
 * This key is useful for services that are lightweight and should not be shared, or for services
 * whose creation depends on parameters that are only available at runtime. When registering a
 * `FactoryKey`, you provide a factory lambda. This lambda is executed on every `get()` call.
 *
 * @param T The type of object to be produced by the factory.
 * @param GetParams The type of parameters to be passed to the factory lambda during retrieval.
 * Use [Unit] if no parameters are needed.
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
 * Creates a [FactoryKey] for services that do not require parameters for creation.
 */
@ExperimentalKeyType
public fun <T : Any> FactoryKey(type: KClass<T>): FactoryKey<T, Unit> = FactoryKey<T, Unit>(type)

/**
 * Creates a [FactoryKey] for the reified type `T` which may take parameters of type `GetParams`.
 */
@ExperimentalKeyType
public inline fun <reified T : Any, GetParams> FactoryKey(): FactoryKey<T, GetParams> =
  FactoryKey<T, GetParams>(T::class)

/**
 * Creates a [FactoryKey] for the reified type `T` that does not require parameters for creation.
 */
@ExperimentalKeyType
@JvmName("FactoryKeyUnit")
public inline fun <reified T : Any> FactoryKey(): FactoryKey<T, Unit> = FactoryKey<T>(T::class)
