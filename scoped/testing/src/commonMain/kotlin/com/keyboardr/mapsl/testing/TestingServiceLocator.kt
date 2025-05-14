package com.keyboardr.mapsl.testing

import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.Key
import com.keyboardr.mapsl.ScopedServiceLocator
import com.keyboardr.mapsl.keys.FactoryKey
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey
import kotlin.reflect.KClass

public abstract class TestingServiceLocator<S>(scope: S) :
  ScopedServiceLocator<S>(scope, allowReregister = true) {

  /**
   * Returns a mock instance for type [T]
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


  override fun <T : Any, GetParams> getValue(
    key: ServiceKey<T, *, GetParams, *>,
    params: GetParams,
    entry: ServiceEntry<T>
  ): T = if (entry is MockEntry) entry.getMock() else super.getValue(key, params, entry)

  /**
   * Registers a [MockEntry] for the specified [key] and returns a mock.
   */
  final override fun <T : Any, GetParams> onMiss(
    key: ServiceKey<T, *, GetParams, *>,
    params: GetParams
  ): T {
    return getValue(key, params, getOrProvideEntry(key) { createMockEntry(key) })
  }

  /**
   * Returns a [MockEntry] for the specified [key].
   */
  final override fun <T : Any, PutParams> onInvalidScope(
    key: ServiceKey<T, *, *, PutParams>,
    putParams: PutParams
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
  public interface MockEntry<T : Any> : ServiceEntry<T> {
    public fun getMock(): T
  }
}
