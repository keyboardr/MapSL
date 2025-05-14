package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ServiceLocator
import com.keyboardr.mapsl.get
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SingletonKeyTest {


  @Test
  fun registerWithKey_accessWithSameKey_returnsSameInstance() {
    val key = SingletonKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key, item)

    assertSame(item, serviceLocator.get(key))
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun registerWithKey_accessWithDifferentKey_callsOnMiss() {
    val key = SingletonKey<Any>()
    val otherKey = SingletonKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key, item)

    assertFails { serviceLocator.get(otherKey) }
    assertTrue(serviceLocator.missed)
  }

  @Test
  fun registerWithDifferentKeys_returnRespectiveItems() {
    val firstKey = SingletonKey<Any>()
    val secondKey = SingletonKey<Any>()
    val firstItem = Any()
    val secondItem = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(firstKey, firstItem)
    serviceLocator.put(secondKey, secondItem)

    assertSame(firstItem, serviceLocator.get(firstKey))
    assertSame(secondItem, serviceLocator.get(secondKey))
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun duplicateRegisterSameKey_throws() {
    val key = SingletonKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key, item)
    assertFails {
      serviceLocator.put(key, Any())
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