package com.keyboardr.mapsl

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SimpleServiceLocatorTest {

  @Test
  fun registerWithKey_accessWithSameKey_returnsSameInstance() {
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any> { item }

    assertSame(item, serviceLocator.get<Any>())
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun registerWithKey_accessWithDifferentKey_callsOnMiss() {
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any> { item }

    assertFails { serviceLocator.get<Int>() }
    assertTrue(serviceLocator.missed)
  }

  @Test
  fun registerWithDifferentKeys_returnRespectiveItems() {
    val firstItem = Any()
    val secondItem = 4
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any> { firstItem }
    serviceLocator.put<Int> { secondItem }

    assertSame(firstItem, serviceLocator.get<Any>())
    assertSame(secondItem, serviceLocator.get<Int>())
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun duplicateRegisterSameKey_throws() {
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any>() { item }
    assertFails {
      serviceLocator.put<Any>() { Any() }
    }
  }

  @Test
  fun hasReturnInOnMiss_returnsMissValue() {
    val missItem = Any()
    @Suppress("UNCHECKED_CAST") val serviceLocator = object : TestServiceLocator() {
      override fun <T : Any> onMiss(key: KClass<T>): T {
        return missItem as? T ?: throw AssertionError()
      }
    }

    assertSame(missItem, serviceLocator.get<Any>())
  }

  @Test
  fun getOrProvide_providesValue() {
    var initialized = false
    val item = Any()
    val serviceLocator = TestServiceLocator()

    fun getOrProvide() = serviceLocator.getOrProvide<Any>(allowedScopes = { true }) {
      if (initialized) {
        throw AssertionError()
      } else {
        initialized = true
        item
      }
    }

    assertSame(item, getOrProvide())
    assertSame(item, getOrProvide())
    assertSame(item, serviceLocator.get<Any>())
  }

  @Test
  fun getOrProvide_preRegistered_providesRegisteredValue() {
    val item = Any()
    val serviceLocator = TestServiceLocator()
    serviceLocator.put<Any>() { item }

    fun getOrProvide() = serviceLocator.getOrProvide<Any>(allowedScopes = { true }) {
      throw AssertionError()
    }

    assertSame(item, getOrProvide())
    assertSame(item, getOrProvide())
    assertSame(item, serviceLocator.get<Any>())
  }


  @Test
  fun getOrProvide_keyRegistered_allowedScopesNotCalled() {
    val serviceLocator = TestServiceLocator()
    val value = Any()

    serviceLocator.put<Any> { value }

    val result =
      serviceLocator.getOrProvide<Any>(allowedScopes = { throw AssertionError("not expected") }) { value }

    assertSame(value, result)
  }

  @Test
  fun getOrProvide_keyNotRegistered_allowedScopesTrue_providesValue() {
    val serviceLocator = TestServiceLocator()
    val value = Any()

    val result =
      serviceLocator.getOrProvide<Any>(allowedScopes = { true }) { value }

    assertSame(value, result)
  }

  @Test
  fun getOrProvide_keyNotRegistered_allowedScopesTrue_multipleCalls_providesStoredValue() {
    val serviceLocator = TestServiceLocator()

    val firstResult =
      serviceLocator.getOrProvide<Any>(allowedScopes = { true }) { Any() }
    val secondResult =
      serviceLocator.getOrProvide<Any>(allowedScopes = { throw AssertionError("not expected") }) { Any() }

    assertSame(firstResult, secondResult)
  }

  @Test
  fun getOrProvide_keyNotRegistered_allowedScopesFalse_onInvalidScopeCalled() {
    var invalidScopeCalled = false
    val serviceLocator = object : TestServiceLocator() {
      override fun <T : Any> onInvalidScope(key: KClass<T>): T {
        invalidScopeCalled = true
        return super.onInvalidScope(key)
      }
    }

    assertFails {
      serviceLocator.getOrProvide<Any>(allowedScopes = { false }) { Any() }
    }
    assertTrue(invalidScopeCalled)
  }

  private open class TestServiceLocator : SimpleServiceLocator<Unit>(Unit) {
    var missed = false
    override fun <T : Any> onMiss(key: KClass<T>): T {
      missed = true
      return super.onMiss(key)
    }
  }
}