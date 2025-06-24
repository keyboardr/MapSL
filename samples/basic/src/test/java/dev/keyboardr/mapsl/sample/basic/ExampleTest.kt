package dev.keyboardr.mapsl.sample.basic

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.keyboardr.mapsl.sample.basic.testing.TestServiceLocator
import junit.framework.TestCase.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mockingDetails
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ExampleTest {
  @Before
  fun setUp() {
    TestServiceLocator.register()
  }

  @Test
  fun provided_returnsSameMock() {
    val myService = MyService.instance

    assertTrue(mockingDetails(myService).isMock)
    assertSame(myService, MyService.instance)
  }

  @Test
  fun registered_returnsSameInstance() {
    val instance = MyService()
    TestServiceLocator.put<MyService> { instance }

    val myService = MyService.instance

    assertFalse(mockingDetails(myService).isMock)
    assertSame(myService, instance)
  }
}