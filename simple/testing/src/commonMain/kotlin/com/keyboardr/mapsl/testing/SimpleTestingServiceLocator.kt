package com.keyboardr.mapsl.testing

import com.keyboardr.mapsl.SimpleServiceLocator
import kotlin.reflect.KClass

/**
 * A [SimpleServiceLocator] that provides mocks for testing if no entry is registered for that
 * service.
 *
 * The [scope] should be one that marks the environment as a testing environment.
 *
 * Unlike other service locators, it is not an error to register a key more than once in the same
 * instance. Doing so will overwrite previous providers.
 */
public abstract class SimpleTestingServiceLocator<S>(scope: S) :
  SimpleServiceLocator<S>(scope, allowReregister = true) {

  /**
   * Returns a mock instance for type [T]
   */
  protected abstract fun <T : Any> createMock(clazz: KClass<T>): T

  /**
   * Called when no entry is found for the specified [key] to decide what should be returned. By
   * default this creates and stores a mock.
   */
  override fun <T : Any> onMiss(key: KClass<T>): T {
    return createMock(key)
  }

  /**
   * Called when attempting to provide a value for a disallowed scope. By default this creates and
   * stores a mock.
   */
  override fun <T : Any> onInvalidScope(key: KClass<T>): T {
    return createMock(key)
  }
}
