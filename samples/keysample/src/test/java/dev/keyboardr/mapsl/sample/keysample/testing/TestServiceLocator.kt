package dev.keyboardr.mapsl.sample.keysample.testing

import androidx.test.core.app.ApplicationProvider
import dev.keyboardr.mapsl.ExperimentalKeyType
import dev.keyboardr.mapsl.keys.put
import dev.keyboardr.mapsl.lifecycle.put
import dev.keyboardr.mapsl.sample.keysample.domain.lifecycle.LifecycleScopedManager
import dev.keyboardr.mapsl.sample.keysample.domain.single.PreregisteredSingleton
import dev.keyboardr.mapsl.sample.keysample.locator.MainServiceLocator
import dev.keyboardr.mapsl.sample.keysample.locator.ServiceLocatorScope
import dev.keyboardr.mapsl.testing.TestingServiceLocator
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