package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.LazyClassKey
import com.keyboardr.mapsl.keys.LazyKey


/**
 * Provides a [Key] that can be used for instances of [T]. Multiple invocations of
 * this function using the same [T] will return keys that reference the same value.
 */
public inline fun <reified T : Any> classKey(): LazyKey<T> = LazyClassKey(T::class)


/**
 * Fetches the item previously stored using [classKey].
 */
public inline fun <reified T : Any> ServiceLocator.get(): T = get(classKey<T>())

/**
 * Fetches the item previously stored using [classKey] or `null` if no provider was registered.
 */
public inline fun <reified T : Any> ServiceLocator.getOrNull(): T? = getOrNull(classKey<T>())