package com.keyboardr.mapsl.sample.basic

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keyboardr.mapsl.sample.basic.testing.TestServiceLocator
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
}