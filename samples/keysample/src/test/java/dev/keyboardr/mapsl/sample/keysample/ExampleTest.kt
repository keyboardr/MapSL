package dev.keyboardr.mapsl.sample.keysample

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.keyboardr.mapsl.sample.keysample.domain.factory.FactoryProduced
import dev.keyboardr.mapsl.sample.keysample.domain.single.LazyPreregisteredSingleton
import dev.keyboardr.mapsl.sample.keysample.domain.single.PreregisteredSingleton
import dev.keyboardr.mapsl.sample.keysample.domain.single.ProvidedSingleton
import dev.keyboardr.mapsl.sample.keysample.testing.TestServiceLocator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mockingDetails
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ExampleTest {
  @Before
  fun setUp() {
    TestServiceLocator.register()
  }

  @Test
  fun factory_createsMultipleMocks() {
    val first = FactoryProduced.create()
    val second = FactoryProduced.create()

    assertNotSame(first, second)
    assertTrue(mockingDetails(first).isMock)
    assertTrue(mockingDetails(second).isMock)
  }

  @Test
  fun preregistered_hasTestingName() {
    val preregisteredSingleton = PreregisteredSingleton.instance

    assertEquals("preregisteredTesting", preregisteredSingleton.name)
  }

  @Test
  fun notRegistered_returnsSameMock() {
    val lazySingleton = LazyPreregisteredSingleton.instance

    assertTrue(mockingDetails(lazySingleton).isMock)
    assertSame(lazySingleton, LazyPreregisteredSingleton.instance)
  }

  @Test
  fun provided_returnsSameMock() {
    val providedSingleton = ProvidedSingleton.instance

    assertTrue(mockingDetails(providedSingleton).isMock)
    assertSame(providedSingleton, ProvidedSingleton.instance)
  }
}