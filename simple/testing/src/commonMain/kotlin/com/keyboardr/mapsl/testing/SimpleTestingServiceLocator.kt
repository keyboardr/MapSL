package com.keyboardr.mapsl.testing

import com.keyboardr.mapsl.SimpleServiceLocator
import kotlin.reflect.KClass

/**
 * A [SimpleServiceLocator] subclass designed specifically for use in tests.
 *
 * This locator provides two key features to simplify testing:
 * 1.  **Automatic Mocking**: If a service is requested that has not been explicitly registered,
 * this locator will automatically create and return a mock instance for it by calling
 * the abstract [createMock] function.
 * 2.  **Re-registration**: Unlike the production locator, this class allows a key to be
 * registered multiple times, overwriting the previous registration. This is useful for
 * setting up different fakes or mocks for different test cases within the same test suite.
 *
 * @param S The type of the scope identifier.
 * @param scope The specific scope instance for this locator, which should be a testing-specific scope.
 */
public abstract class SimpleTestingServiceLocator<S>(scope: S) :
  SimpleServiceLocator<S>(scope, allowReregister = true) {

  /**
   * Creates a mock instance for the given service type.
   *
   * Subclasses must implement this method to integrate their chosen mocking framework
   * (e.g., Mockito, MockK).
   *
   * @param clazz The [KClass] of the service to mock.
   * @return A mock instance of type [T].
   */
  protected abstract fun <T : Any> createMock(clazz: KClass<T>): T

  /**
   * Called when `get()` is called for a class that has no registered provider.
   *
   * This implementation overrides the default behavior to create, store, and return a mock
   * instance by calling [createMock].
   */
  override fun <T : Any> onMiss(key: KClass<T>): T {
    return createMock(key)
  }

  /**
   * Called when `getOrProvide()` is attempted for a class in a disallowed scope.
   *
   * This implementation overrides the default behavior to create, store, and return a mock
   * instance by calling [createMock].
   */
  override fun <T : Any> onInvalidScope(key: KClass<T>): T {
    return createMock(key)
  }
}
