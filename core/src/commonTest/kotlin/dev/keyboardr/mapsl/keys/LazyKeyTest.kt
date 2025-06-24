package dev.keyboardr.mapsl.keys

import dev.keyboardr.mapsl.ServiceLocator
import dev.keyboardr.mapsl.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LazyKeyTest {


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
  fun multipleGet_onlyInitializesOnce() {
    val key = LazyKey<Any>()
    var initialized = false
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) {
      if (initialized) {
        throw AssertionError()
      } else {
        initialized = true
        Any()
      }
    }

    serviceLocator.get(key)
    serviceLocator.get(key)
    serviceLocator.get(key)
  }

  @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
  @Test
  fun multipleGet_differentThreads_onlyInitializesOnce() {
    val key = LazyKey<Any>()
    var initialized = false
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) {
      if (initialized) {
        throw AssertionError()
      } else {
        initialized = true
        Any()
      }
    }

    val coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    coroutineScope.launch(newSingleThreadContext("first")) { serviceLocator.get(key) }
    coroutineScope.launch(newSingleThreadContext("second")) { serviceLocator.get(key) }
    coroutineScope.launch(newSingleThreadContext("third")) { serviceLocator.get(key) }
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