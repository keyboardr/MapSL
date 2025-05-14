package com.keyboardr.mapsl.keys

import kotlin.reflect.KClass

/**
 * A Key type used in [ServiceLocator][com.keyboardr.mapsl.ServiceLocator]. Will provide
 * values of type [T].
 *
 * @param T the type of values provided by this key
 * @param Entry the [ServiceEntry] type used by this key
 * @param GetParams the type used by the key when requesting a value. Commonly this is [Unit] for
 * keys that do not require parameters when requesting a value.
 * @param PutParams the type used by the key when creating an [Entry]. This includes the necessary
 * information for obtaining values.
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
 * An entry to be stored in a [ServiceLocator][com.keyboardr.mapsl.ServiceLocator] to provide values
 * of type [T]
 */
public interface ServiceEntry<T>

/**
 * Used by [LazyKey] and [SingletonKey] so that they can get values from each other's
 * [ServiceEntry], which is a trait needed by [ClassKey].
 */
internal interface ParamlessServiceEntry<T> : ServiceEntry<T> {
  val service: T
}

