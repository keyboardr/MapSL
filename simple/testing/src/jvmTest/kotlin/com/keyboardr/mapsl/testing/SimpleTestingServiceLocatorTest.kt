package com.keyboardr.mapsl.testing

import junit.framework.TestCase.assertFalse
import org.mockito.Mockito.mock
import org.mockito.kotlin.mockingDetails
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SimpleTestingServiceLocatorTest {

  @Test
  fun get_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()

    val first = serviceLocator.get<Any>()
    val second = serviceLocator.get<Any>()

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }


  @Test
  fun getOrProvide_disallowedScope_returnsMock_sameInstance() {
    val serviceLocator = TestServiceLocator()

    val first = serviceLocator.getOrProvide<Any>(allowedScopes = { false }) { Any() }
    val second = serviceLocator.getOrProvide<Any>(allowedScopes = { false }) { Any() }

    assertSame(first, second)
    assertTrue(mockingDetails(first).isMock)
  }

  @Test
  fun getOrProvide_allowedScope_returnsReal_sameInstance() {
    val serviceLocator = TestServiceLocator()

    val first = serviceLocator.getOrProvide<Any>(allowedScopes = { true }) { Any() }
    val second = serviceLocator.getOrProvide<Any>(allowedScopes = { true }) { Any() }

    assertSame(first, second)
    assertFalse(mockingDetails(first).isMock)
  }

  private class TestServiceLocator : SimpleTestingServiceLocator<Unit>(Unit) {
    override fun <T : Any> createMock(clazz: KClass<T>): T {
      return mock(clazz.java)
    }
  }
}