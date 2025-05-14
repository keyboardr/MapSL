package com.keyboardr.mapsl.keys

import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.ServiceLocator
import com.keyboardr.mapsl.get
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalKeyType::class)
class FactoryKeyTest {

  @Test
  fun registerWithKey_accessWithSameKey_returnsSameInstance() {
    val key = FactoryKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }

    assertSame(item, serviceLocator.get(key))
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun registerWithKey_accessWithDifferentKey_callsOnMiss() {
    val key = FactoryKey<Any>()
    val otherKey = FactoryKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }

    assertFails { serviceLocator.get(otherKey) }
    assertTrue(serviceLocator.missed)
  }

  @Test
  fun registerWithDifferentKeys_returnRespectiveItems() {
    val firstKey = FactoryKey<Any>()
    val secondKey = FactoryKey<Any>()
    val firstItem = Any()
    val secondItem = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(firstKey) { firstItem }
    serviceLocator.put(secondKey) { secondItem }

    assertSame(firstItem, serviceLocator.get(firstKey))
    assertSame(secondItem, serviceLocator.get(secondKey))
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun duplicateRegisterSameKey_throws() {
    val key = FactoryKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }
    assertFails {
      serviceLocator.put(key) { Any() }
    }
  }

  @Test
  fun multipleGet_returnNewInvocationEachTime() {
    val key = FactoryKey<Int>()
    var currentValue = 0
    val valueCount = 5
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { currentValue++ }

    var values = mutableListOf<Int>()
    repeat(valueCount) { values += serviceLocator.get(key) }

    assertContentEquals((0..<valueCount).toList(), values)

  }

  private class TestServiceLocator : ServiceLocator() {
    var missed = false

    override fun <T : Any, GetParams> onMiss(key: ServiceKey<T, *, GetParams, *>, params: GetParams): T {
      missed = true
      return super.onMiss(key, params)
    }
  }
}