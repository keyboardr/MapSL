package dev.keyboardr.mapsl.sample.basic.testing

import androidx.test.core.app.ApplicationProvider
import dev.keyboardr.mapsl.sample.basic.MainServiceLocator
import dev.keyboardr.mapsl.sample.basic.ServiceLocatorScope
import dev.keyboardr.mapsl.testing.SimpleTestingServiceLocator
import org.mockito.Mockito.mock
import kotlin.reflect.KClass

object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {
  override fun <T : Any> createMock(clazz: KClass<T>): T = mock<T>(clazz.java)

  fun register() {
    MainServiceLocator.register(this, ApplicationProvider.getApplicationContext()) {
      // common fakes go here
    }
  }
}