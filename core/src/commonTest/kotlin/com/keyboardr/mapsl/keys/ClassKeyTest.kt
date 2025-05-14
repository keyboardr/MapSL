package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ServiceLocator
import com.keyboardr.mapsl.get
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ClassKeyTest {


  @Test
  fun registerWithClass_lazy_accessReturnsSameInstance() {
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any> { item }

    assertSame(item, serviceLocator.get<Any>())
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun registerWithClass_singleton_accessReturnsSameInstance() {
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any>(item)

    assertSame(item, serviceLocator.get<Any>())
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun registerWithClass_accessWithDifferentClass_callsOnMiss() {
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any> { item }

    assertFails { serviceLocator.get<Int>() }
    assertTrue(serviceLocator.missed)
  }

  @Test
  fun duplicateRegisterSameClass_throws() {
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put<Any> { item }
    assertFails {
      serviceLocator.put<Any> { Any() }
    }
  }

  private class TestServiceLocator : ServiceLocator() {
    var missed = false

    override fun <T : Any, GetParams> onMiss(
      key: ServiceKey<T, *, GetParams, *>,
      params: GetParams,
    ): T {
      missed = true
      return super.onMiss(key, params)
    }
  }
}