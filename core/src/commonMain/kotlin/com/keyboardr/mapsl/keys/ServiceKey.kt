package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ServiceLocator
import kotlin.reflect.KClass

/**
 * A Key type used in a [ServiceLocator]. Will provide values of type [T].
 *
 * @param T The type of value provided by this key.
 * @param Entry The [ServiceEntry] type this key creates and uses.
 * @param GetParams The type of parameters required when retrieving a value with this key (e.g., via `get()`).
 * This is often [Unit] for keys that do not require parameters.
 * @param PutParams The type of parameters required when registering a value with this key (e.g., via `put()`).
 * This typically includes the service instance itself or a provider lambda.
 */
public interface ServiceKey<T : Any, Entry : ServiceEntry<T>, GetParams, PutParams> {
  /**
   * Creates an [Entry] to provide values.
   */
  public fun createEntry(params: PutParams): Entry

  /**
   * Retrieve the value from [entry]. The entry will have been created by a [ServiceKey] that is
   * [equals] to this key. If this [ServiceKey] is only [equals] to other keys of the same class,
   * then [entry] will be guaranteed to be of type [Entry].
   */
  public fun getValue(params: GetParams, entry: ServiceEntry<T>): T

  /**
   * A reference to the type provided by this key.
   */
  public val type: KClass<T>
}

/**
 * An entry stored in a [ServiceLocator] to provide a value of type [T].
 *
 * The specific implementation of `ServiceEntry` is what defines a key's behavior
 * (e.g., whether it holds a direct instance, a lazy provider, or a factory function).
 */
public interface ServiceEntry<T>

/**
 * An internal interface that provides a common contract for entries that do not require
 * parameters for value retrieval, such as those created by [LazyKey] and [SingletonKey].
 *
 * This abstraction is what allows a [ClassKey] to seamlessly resolve an entry created
 * by either a `LazyKey` or a `SingletonKey`, as it can access the underlying `service`
 * property on both.
 */
internal interface ParamlessServiceEntry<T> : ServiceEntry<T> {
  val service: T
}
