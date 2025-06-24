package dev.keyboardr.mapsl.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import dev.keyboardr.mapsl.ExperimentalKeyType
import dev.keyboardr.mapsl.ServiceLocator
import dev.keyboardr.mapsl.keys.ServiceKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalKeyType::class)
class LifecycleKeyTest {
  @Test
  fun registerWithKey_accessWithSameKey_returnsSameInstance() {
    val lifecycleOwner = TestLifecycleOwner()
    val key = LifecycleKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }

    assertSame(item, serviceLocator.get(key, lifecycleOwner))
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun registerWithKey_accessWithDifferentKey_callsOnMiss() {
    val lifecycleOwner = TestLifecycleOwner()
    val key = LifecycleKey<Any>()
    val otherKey = LifecycleKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }

    assertFails { serviceLocator.get(otherKey, lifecycleOwner) }
    assertTrue(serviceLocator.missed)
  }

  @Test
  fun registerWithDifferentKeys_returnRespectiveItems() {
    val lifecycleOwner = TestLifecycleOwner()
    val firstKey = LifecycleKey<Any>()
    val secondKey = LifecycleKey<Any>()
    val firstItem = Any()
    val secondItem = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(firstKey) { firstItem }
    serviceLocator.put(secondKey) { secondItem }

    assertSame(firstItem, serviceLocator.get(firstKey, lifecycleOwner))
    assertSame(secondItem, serviceLocator.get(secondKey, lifecycleOwner))
    assertFalse(serviceLocator.missed)
  }

  @Test
  fun duplicateRegisterSameKey_throws() {
    val key = LifecycleKey<Any>()
    val item = Any()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { item }
    assertFails {
      serviceLocator.put(key) { Any() }
    }
  }

  @Test
  fun multipleGet_onlyInitializesOnce() {
    val lifecycleOwner = TestLifecycleOwner()
    val key = LifecycleKey<Any>()
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

    serviceLocator.get(key, lifecycleOwner)
    serviceLocator.get(key, lifecycleOwner)
    serviceLocator.get(key, lifecycleOwner)
  }

  @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
  @Test
  fun multipleGet_differentThreads_onlyInitializesOnce() {
    val lifecycleOwner = TestLifecycleOwner()
    val key = LifecycleKey<Any>()
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
    coroutineScope.launch(newSingleThreadContext("first")) {
      serviceLocator.get(
        key,
        lifecycleOwner
      )
    }
    coroutineScope.launch(newSingleThreadContext("second")) {
      serviceLocator.get(
        key,
        lifecycleOwner
      )
    }
    coroutineScope.launch(newSingleThreadContext("third")) {
      serviceLocator.get(
        key,
        lifecycleOwner
      )
    }
  }

  @Test
  fun goesBelowMinimumStateAndThenAbove_returnsNewInstance() {
    val lifecycleOwner = TestLifecycleOwner()
    val key = LifecycleKey<Any>()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { Any() }
    val firstItem = serviceLocator.get(key, lifecycleOwner)

    lifecycleOwner.moveToState(Lifecycle.State.CREATED)
    lifecycleOwner.moveToState(Lifecycle.State.STARTED)

    val secondItem = serviceLocator.get(key, lifecycleOwner)

    assertNotSame(firstItem, secondItem)
  }

  @Test
  fun multipleLifecycleOwners_onlyOneGoesBelow_returnsSameInstance() {
    val changingLifecycleOwner = TestLifecycleOwner()
    val stableLifecycleOwner = TestLifecycleOwner()
    val key = LifecycleKey<Any>()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { Any() }
    val firstItem = serviceLocator.get(key, changingLifecycleOwner)
    serviceLocator.get(key, stableLifecycleOwner)

    changingLifecycleOwner.moveToState(Lifecycle.State.CREATED)
    changingLifecycleOwner.moveToState(Lifecycle.State.STARTED)

    val secondItem = serviceLocator.get(key, changingLifecycleOwner)

    assertSame(firstItem, secondItem)
  }


  @Test
  fun belowMinimumState_getThrows() {
    val lifecycleOwner = TestLifecycleOwner()
    val key = LifecycleKey<Any>()
    val serviceLocator = TestServiceLocator()

    serviceLocator.put(key) { Any() }
    lifecycleOwner.moveToState(Lifecycle.State.CREATED)

    assertFails {
      serviceLocator.get(key, lifecycleOwner)
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

  private class TestLifecycleOwner(initialState: Lifecycle.State = Lifecycle.State.STARTED) :
    LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this).apply { currentState = initialState }
    override val lifecycle: Lifecycle = lifecycleRegistry

    fun moveToState(state: Lifecycle.State) {
      lifecycleRegistry.currentState = state
    }

  }

}