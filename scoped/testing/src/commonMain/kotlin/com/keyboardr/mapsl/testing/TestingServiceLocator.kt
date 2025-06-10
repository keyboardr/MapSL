package com.keyboardr.mapsl.testing

import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.Key
import com.keyboardr.mapsl.ScopedServiceLocator
import com.keyboardr.mapsl.keys.FactoryKey
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey
import kotlin.reflect.KClass

/**
 * A [ScopedServiceLocator] subclass designed specifically for use in tests.
 *
 * This locator provides two key features to simplify testing:
 * 1.  **Automatic Mocking**: If a service is requested that has not been explicitly registered,
 * this locator will automatically create and return a mock instance by calling
 * the abstract [createMock] function.
 * 2.  **Re-registration**: Unlike a production locator, this class allows a key to be
 * registered multiple times, overwriting the previous registration. This is useful for
 * setting up different fakes or mocks for different test cases.
 *
 * @param S The type of the scope identifier.
 * @param scope The specific scope instance for this locator, which should be a testing-specific scope.
 */
public abstract class TestingServiceLocator<S>(scope: S) :
  ScopedServiceLocator<S>(scope, allowReregister = true) {

  /**
   * Creates a mock instance for the given service type.
   *
   * Subclasses must implement this method to integrate their chosen mocking framework
   * (e.g., Mockito, MockK).
   *
   * @param clazz The [KClass] of the service to mock.
   * @return A mock instance of type [T].
   */
  public abstract fun <T : Any> createMock(clazz: KClass<T>): T

  /**
   * Creates a [MockEntry] for the specified type. By default, returns an entry that creates a new
   * mock each time if the [key] is a [FactoryKey], otherwise the entry gives the same mock every
   * time. Override to support other key behaviors.
   */
  protected open fun <T : Any> createMockEntry(key: Key<T>): MockEntry<T> =
    @OptIn(ExperimentalKeyType::class)
    if (key is FactoryKey)
      MockFactoryEntry { createMock(key.type) }
    else
      SingleMockEntry(createMock(key.type))

  /**
   * Overrides the standard value retrieval process to intercept requests for mock entries.
   * If the [entry] is a [MockEntry], it returns the mock; otherwise, it delegates to the
   * standard `getValue` behavior.
   */
  override fun <T : Any, GetParams> getValue(
    key: ServiceKey<T, *, GetParams, *>,
    params: GetParams,
    entry: ServiceEntry<T>,
  ): T = if (entry is MockEntry) entry.getMock() else super.getValue(key, params, entry)

  /**
   * Called when [get] is called for a key that has no registered provider.
   *
   * This implementation overrides the default behavior to create, store, and return a mock
   * instance by creating a [MockEntry] using [createMockEntry].
   */
  final override fun <T : Any, GetParams> onMiss(
    key: ServiceKey<T, *, GetParams, *>,
    params: GetParams,
  ): T {
    return getValue(key, params, getOrProvideEntry(key) { createMockEntry(key) })
  }

  /**
   * Called when [getOrProvide] is attempted for a key in a disallowed scope.
   *
   * This implementation overrides the default behavior to create and return a [MockEntry]
   * using [createMockEntry].
   */
  final override fun <T : Any, PutParams> onInvalidScope(
    key: ServiceKey<T, *, *, PutParams>,
    putParams: PutParams,
  ): ServiceEntry<T> {
    return createMockEntry(key)
  }

  private class SingleMockEntry<T : Any>(private val mock: T) : MockEntry<T> {
    override fun getMock(): T {
      return mock
    }
  }

  private class MockFactoryEntry<T : Any>(private val mockFactory: () -> T) : MockEntry<T> {
    override fun getMock(): T {
      return mockFactory()
    }
  }

  /**
   * A [ServiceEntry] whose values are mocks. The mocks should generally be created using
   * [createMock], and may be stored between invocations.
   */
  protected interface MockEntry<T : Any> : ServiceEntry<T> {
    public fun getMock(): T
  }
}
