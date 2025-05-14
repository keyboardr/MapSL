package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.LazyKey
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey
import com.keyboardr.mapsl.keys.put
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScopedServiceLocatorTest {

  @Test
  fun getOrProvide_keyRegistered_allowedScopesNotCalled() {
    val serviceLocator = ScopedServiceLocator(Unit)
    val key = LazyKey<Any>()
    val value = Any()

    serviceLocator.put(key) { value }

    val result =
      serviceLocator.getOrProvide(key, { throw AssertionError("not expected") }) { value }

    assertSame(value, result)
  }

  @Test
  fun getOrProvide_keyNotRegistered_allowedScopesTrue_providesValue() {
    val serviceLocator = ScopedServiceLocator(Unit)
    val key = LazyKey<Any>()
    val value = Any()

    val result =
      serviceLocator.getOrProvide(key, { true }) { value }

    assertSame(value, result)
  }

  @Test
  fun getOrProvide_keyNotRegistered_allowedScopesTrue_multipleCalls_providesStoredValue() {
    val serviceLocator = ScopedServiceLocator(Unit)
    val key = LazyKey<Any>()

    val firstResult =
      serviceLocator.getOrProvide(key, { true }) { Any() }
    val secondResult =
      serviceLocator.getOrProvide(key, { throw AssertionError("not expected") }) { Any() }

    assertSame(firstResult, secondResult)
  }

  @Test
  fun getOrProvide_keyNotRegistered_allowedScopesFalse_onInvalidScopeCalled() {
    var invalidScopeCalled = false
    val serviceLocator = object : ScopedServiceLocator<Unit>(Unit) {
      override fun <T : Any, PutParams> onInvalidScope(
        key: ServiceKey<T, *, *, PutParams>,
        putParams: PutParams,
      ): ServiceEntry<T> {
        invalidScopeCalled = true
        return super.onInvalidScope(key, putParams)
      }
    }
    val key = LazyKey<Any>()


    assertFails {
      serviceLocator.getOrProvide(key, { false }) { Any() }
    }
    assertTrue(invalidScopeCalled)
  }
}