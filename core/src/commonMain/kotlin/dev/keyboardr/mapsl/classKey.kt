package dev.keyboardr.mapsl

import dev.keyboardr.mapsl.keys.LazyClassKey
import dev.keyboardr.mapsl.keys.LazyKey


/**
 * Creates a [LazyClassKey] for the reified type `T`, which can be used for class-based lookups.
 *
 * This is the standard way to create a key for registering and retrieving a service based on its
 * type, ensuring lazy singleton behavior by default. Multiple invocations of this function for the
 * same type `T` will produce keys that are considered equal.
 *
 * @return A [LazyKey] that uses the class of `T` as its identifier.
 */
public inline fun <reified T : Any> classKey(): LazyKey<T> = LazyClassKey(T::class)


/**
 * A convenience extension to fetch a singleton instance from the [ServiceLocator]
 * using its reified type `T` as the key.
 *
 * This is equivalent to calling `get(classKey<T>())`. If no provider has been registered for `T`,
 * this will delegate to the locator's `onMiss` behavior.
 *
 * @receiver The [ServiceLocator] to retrieve the service from.
 * @return The singleton instance of type `T`.
 */
public inline fun <reified T : Any> ServiceLocator.get(): T = get(classKey<T>())

/**
 * A convenience extension to fetch a singleton instance from the [ServiceLocator]
 * using its reified type `T` as the key.
 *
 * This is equivalent to calling `getOrNull(classKey<T>())`. If no provider has been registered
 * for `T`, this will return `null` and will not invoke `onMiss`.
 *
 * @receiver The [ServiceLocator] to retrieve the service from.
 * @return The singleton instance of type `T`, or `null` if not found.
 */
public inline fun <reified T : Any> ServiceLocator.getOrNull(): T? = getOrNull(classKey<T>())
