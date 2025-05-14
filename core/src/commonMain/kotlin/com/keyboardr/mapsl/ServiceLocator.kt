@file:Suppress("unused")

package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.*


/**
 * A Key type used in [ServiceLocator][com.keyboardr.mapsl.ServiceLocator]. Will provide
 * values of type [T].
 */
public typealias Key<T> = ServiceKey<T, *, *, *>

/**
 * A basic service locator for storing common dependencies. [ServiceLocator] acts like a
 * heterogeneous map, holding values of different types. The [ServiceKey] dictates which type will
 * be accessed.
 *
 * There are several built-in key types:
 *   - [LazyKey]: holds a fixed value that is created the first time a value is requested for the key
 *   - [SingletonKey]: holds a fixed value that is created on initialization
 *   - [ClassKey]: useful for the common case where there is only one entry of a particular type. May be either Lazy or Singleton.
 *   - [FactoryKey] (experimental): used to create a new instance of the service each time a value is requested
 *
 *  When a key is registered in the [ServiceLocator], it creates a [ServiceEntry] specific to that
 *  key type, which is stored for later access. When values are requested for the key, the
 *  [ServiceEntry] it created will be used to obtain the value. Different key types use different
 *  implementations of [ServiceEntry] to determine their respective behaviors.
 *
 *  @param allowReregister if true, allow registering keys multiple times. The latest registration
 *  will be used. This should generally only be true for tests.
 */
public abstract class ServiceLocator(private val allowReregister: Boolean = false) {

  public class ServiceLocatorException(message: String, public val key: Key<*>) : Exception(message)

  private val serviceMap = ConcurrentMap<Key<*>, ServiceEntry<*>>()

  /**
   * Registers a service in the [ServiceLocator] for the specified [key]. It is an error to register
   * a [key] more than once in the same [ServiceLocator].
   */
  public fun <T : Any, PutParams> put(
    key: ServiceKey<T, *, *, PutParams>,
    params: PutParams,
  ) {
    val newEntry = key.createEntry(params)
    val hadNoPreviousValue = serviceMap.put(key, newEntry) == null
    check(hadNoPreviousValue || allowReregister) { "Key already registered: $key" }
  }

  /**
   * Called when no entry is found for the specified [key] to decide what should be returned.
   * Override to customize this behavior. Throws a [ServiceLocatorException] by default.
   */
  protected open fun <T : Any, GetParams> onMiss(
    key: ServiceKey<T, *, GetParams, *>,
    params: GetParams
  ): T =
    throw ServiceLocatorException("No value found for key: $key", key)

  /**
   * Fetches an item for [key]. If the [key] was not previously registered, returns the result of
   * [onMiss].
   */
  public fun <T : Any, GetParams> get(key: ServiceKey<T, *, GetParams, *>, params: GetParams): T =
    getOrNull(key, params) ?: onMiss(key, params)

  /**
   * Fetches the item for [key]. If the [key] was not previously registered, returns `null`. Does
   * not invoke [onMiss].
   */
  @Suppress("UNCHECKED_CAST")
  public fun <T : Any, GetParams> getOrNull(
    key: ServiceKey<T, *, GetParams, *>,
    params: GetParams
  ): T? =
    serviceMap[key]?.let { entry -> getValue(key, params, entry as ServiceEntry<T>) }

  /**
   * Fetches the [ServiceEntry] previously stored using [key]. If no entry was registered for [key],
   * creates a new entry and stores it.
   */
  protected fun <T : Any> getOrProvideEntry(
    key: Key<T>,
    entryProvider: () -> ServiceEntry<T>,
  ): ServiceEntry<T> {
    @Suppress("UNCHECKED_CAST")
    return serviceMap.computeIfAbsent(key) { entryProvider() } as ServiceEntry<T>
  }

  /**
   * Fetches the value previously stored for [key]. If no value was registered for [key], creates a
   * new entry and stores it.
   *
   * This function is internal since users of the library should generally use the `getOrProvide`
   * from `ScopedServiceLocator`. This is only made available for library components to store
   * configuration.
   */
  internal fun <T : Any, GetParams, PutParams> getOrProvideValue(
    key: ServiceKey<T, *, GetParams, PutParams>,
    getParams: GetParams,
    putParams: () -> PutParams
  ): T {
    val entry = getOrProvideEntry(key, { key.createEntry(putParams()) })
    return getValue(key, getParams, entry)
  }

  /**
   * Fetches the value from [entry]. This should normally delegate to [key], but some subtypes may
   * perform their own inspection if they create their own [ServiceEntries][ServiceEntry].
   */
  protected open fun <T : Any, GetParams> getValue(
    key: ServiceKey<T, *, GetParams, *>,
    params: GetParams,
    entry: ServiceEntry<T>
  ): T {
    return key.getValue(params, entry)
  }

}

/**
 * Fetches an item for [key]. If the [key] was not previously registered, returns the result of
 * [ServiceLocator.onMiss].
 */
public fun <T : Any> ServiceLocator.get(key: ServiceKey<T, *, Unit, *>): T = get(key, Unit)

/**
 * Fetches the item for [key]. If the [key] was not previously registered, returns `null`. Does
 * not invoke [ServiceLocator.onMiss].
 */
public fun <T : Any> ServiceLocator.getOrNull(key: ServiceKey<T, *, Unit, *>): T? =
  getOrNull(key, Unit)

