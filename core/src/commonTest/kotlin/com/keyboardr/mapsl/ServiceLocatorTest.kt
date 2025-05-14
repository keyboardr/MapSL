package com.keyboardr.mapsl

import com.keyboardr.mapsl.keys.LazyKey
import com.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import com.keyboardr.mapsl.keys.ServiceKey
import com.keyboardr.mapsl.keys.put
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue


class ServiceLocatorTest {

  @Test
  fun registerWithKey_accessWithSameKey_returnsSameInstance() {
    val key = LazyKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }

    assertSame(item, serviceLocator.get(key))
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun registerWithKey_accessWithDifferentKey_callsOnMiss() {
    val key = LazyKey<Any>()
    val otherKey = LazyKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }

    assertFails { serviceLocator.get(otherKey) }
    assertTrue(serviceLocator.missed)
  }

  @Test
  fun registerWithDifferentKeys_returnRespectiveItems() {
    val firstKey = LazyKey<Any>()
    val secondKey = LazyKey<Any>()
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
    val key = LazyKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }
    assertFails {
      serviceLocator.put(key) { Any() }
    }
  }

  @Test
  fun duplicateRegisterSameKey_allowReregister_overwrites() {
    val key = LazyKey<Any>()
    val item = Any()
    val secondItem = Any()
    val serviceLocator = TestServiceLocator(allowReregister = true)

    serviceLocator.put(key) { item }
    serviceLocator.put(key) { secondItem }

    assertSame(secondItem, serviceLocator.get(key))
  }

  @Test
  fun hasReturnInOnMiss_returnsMissValue() {
    val missItem = Any()
    @Suppress("UNCHECKED_CAST") val serviceLocator = object : ServiceLocator() {
      override fun <T : Any, GetParams> onMiss(
        key: ServiceKey<T, *, GetParams, *>,
        params: GetParams,
      ): T {
        return missItem as? T ?: throw AssertionError()
      }
    }

    assertSame(missItem, serviceLocator.get<Any>())
  }

  @Test
  fun getOrProvide_providesValue() {
    var initialized = false
    val key = LazyKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    fun getOrProvide() = serviceLocator.getOrProvide<Any>(key) {
      if (initialized) {
        throw AssertionError()
      } else {
        initialized = true
        item
      }
    }

    assertSame(item, getOrProvide())
    assertSame(item, getOrProvide())
    assertSame(item, serviceLocator.get(key))
  }

  @Test
  fun getOrProvide_preRegistered_providesRegisteredValue() {
    val key = LazyKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()
    serviceLocator.put(key) { item }

    fun getOrProvide() = serviceLocator.getOrProvide<Any>(key) {
      throw AssertionError()
    }

    assertSame(item, getOrProvide())
    assertSame(item, getOrProvide())
    assertSame(item, serviceLocator.get(key))
  }

  private class TestServiceLocator(allowReregister: Boolean = false) :
    ServiceLocator(allowReregister) {
    var missed = false

    override fun <T : Any, GetParams> onMiss(
      key: ServiceKey<T, *, GetParams, *>,
      params: GetParams,
    ): T {
      missed = true
      return super.onMiss(key, params)
    }

    fun <T : Any> getOrProvide(
      key: LazyKey<T>,
      threadSafetyMode: LazyThreadSafetyMode = defaultLazyKeyThreadSafetyMode,
      provider: () -> T,
    ): T = getValue(
      key,
      Unit,
      getOrProvideEntry(key) { key.createEntry(LazyKey.PutParams(provider, threadSafetyMode)) })
  }
}