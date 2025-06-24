package dev.keyboardr.mapsl.testing

import dev.keyboardr.mapsl.ExperimentalKeyType
import dev.keyboardr.mapsl.get
import dev.keyboardr.mapsl.getOrProvide
import dev.keyboardr.mapsl.keys.FactoryKey
import dev.keyboardr.mapsl.keys.LazyKey
import dev.keyboardr.mapsl.keys.SingletonKey
import junit.framework.TestCase.assertFalse
import org.mockito.Mockito.mock
import org.mockito.kotlin.mockingDetails
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestingServiceLocatorTest {

  @Test
  fun get_lazyKey_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()
    val key = LazyKey<Any>()

    val first = serviceLocator.get(key)
    val second = serviceLocator.get(key)

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }

  @Test
  fun get_singletonKey_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()
    val key = SingletonKey<Any>()

    val first = serviceLocator.get(key)
    val second = serviceLocator.get(key)

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }

  @Test
  fun get_classKey_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()

    val first = serviceLocator.get<Any>()
    val second = serviceLocator.get<Any>()

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }

  @OptIn(ExperimentalKeyType::class)
  @Test
  fun get_factoryKey_returnsMock_differentInstances() {
    val serviceLocator = TestServiceLocator()
    val key = FactoryKey<Any>()

    val first = serviceLocator.get(key)
    val second = serviceLocator.get(key)

    assertNotSame(first, second)
    assertTrue(mockingDetails(first).isMock)
    assertTrue(mockingDetails(second).isMock)
  }

  @Test
  fun getOrProvide_disallowedScope_lazyKey_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()
    val key = LazyKey<Any>()

    val first = serviceLocator.getOrProvide(key, { false }) { Any() }
    val second = serviceLocator.getOrProvide(key, { false }) { Any() }

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }

  @Test
  fun getOrProvide_disallowedScope_singletonKey_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()
    val key = SingletonKey<Any>()

    val first = serviceLocator.getOrProvide(key, { false }) { Any() }
    val second = serviceLocator.getOrProvide(key, { false }) { Any() }

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }

  @Test
  fun getOrProvide_disallowedScope_classKey_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()

    val first = serviceLocator.getOrProvide<Any>(allowedScopes = { false }) { Any() }
    val second = serviceLocator.getOrProvide<Any>(allowedScopes = { false }) { Any() }

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }

  @OptIn(ExperimentalKeyType::class)
  @Test
  fun getOrProvide_disallowedScope_factoryKey_returnsMock_differentInstances() {
    val serviceLocator = TestServiceLocator()
    val key = FactoryKey<Any>()

    val first = serviceLocator.getOrProvide(key, { false }) { Any() }
    val second = serviceLocator.getOrProvide(key, { false }) { Any() }

    assertNotSame(first, second)
    assertTrue(mockingDetails(first).isMock)
    assertTrue(mockingDetails(second).isMock)
  }

  @Test
  fun getOrProvide_allowedScope_lazyKey_returnsReal_sameInstance() {
    val serviceLocator = TestServiceLocator()
    val key = LazyKey<Any>()

    val first = serviceLocator.getOrProvide(key, { true }) { Any() }
    val second = serviceLocator.getOrProvide(key, { true }) { Any() }

    assertSame(first, second)
    assertFalse(mockingDetails(first).isMock)
  }

  @Test
  fun getOrProvide_allowedScope_singletonKey_returnsReal_sameInstance() {
    val serviceLocator = TestServiceLocator()
    val key = SingletonKey<Any>()

    val first = serviceLocator.getOrProvide(key, { true }) { Any() }
    val second = serviceLocator.getOrProvide(key, { true }) { Any() }

    assertSame(first, second)
    assertFalse(mockingDetails(first).isMock)
  }

  @Test
  fun getOrProvide_allowedScope_classKey_returnsReal_sameInstance() {
    val serviceLocator = TestServiceLocator()

    val first = serviceLocator.getOrProvide<Any>(allowedScopes = { true }) { Any() }
    val second = serviceLocator.getOrProvide<Any>(allowedScopes = { true }) { Any() }

    assertSame(first, second)
    assertFalse(mockingDetails(first).isMock)
  }

  @OptIn(ExperimentalKeyType::class)
  @Test
  fun getOrProvide_allowedScope_factoryKey_returnsReal_differentInstances() {
    val serviceLocator = TestServiceLocator()
    val key = FactoryKey<Any>()

    val first = serviceLocator.getOrProvide(key, { true }) { Any() }
    val second = serviceLocator.getOrProvide(key, { true }) { Any() }

    assertNotSame(first, second)
    assertFalse(mockingDetails(first).isMock)
    assertFalse(mockingDetails(second).isMock)
  }

  private class TestServiceLocator : TestingServiceLocator<Unit>(Unit) {
    override fun <T : Any> createMock(clazz: KClass<T>): T {
      return mock(clazz.java)
    }
  }
}