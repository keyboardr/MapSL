package com.keyboardr.mapsl.sample.keysample.testing

import androidx.test.core.app.ApplicationProvider
import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.keys.put
import com.keyboardr.mapsl.lifecycle.put
import com.keyboardr.mapsl.sample.keysample.domain.lifecycle.LifecycleScopedManager
import com.keyboardr.mapsl.sample.keysample.domain.single.PreregisteredSingleton
import com.keyboardr.mapsl.sample.keysample.locator.MainServiceLocator
import com.keyboardr.mapsl.sample.keysample.locator.ServiceLocatorScope
import com.keyboardr.mapsl.testing.TestingServiceLocator
import org.mockito.Mockito.mock
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

object TestServiceLocator :
  TestingServiceLocator<ServiceLocatorScope.Testing>(ServiceLocatorScope.Testing) {
  override fun <T : Any> createMock(clazz: KClass<T>): T = mock<T>(clazz.java)

  fun register() {
    MainServiceLocator.register(this, ApplicationProvider.getApplicationContext()) {
      // common fakes go here
      put<PreregisteredSingleton>(PreregisteredSingleton("preregisteredTesting"))
      @OptIn(ExperimentalKeyType::class, ExperimentalTime::class)
      put(LifecycleScopedManager.key) { LifecycleScopedManager() }
    }
  }
}