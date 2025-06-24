package dev.keyboardr.mapsl.sample.multimodule

import dev.keyboardr.mapsl.sample.multimodule.locator.MainServiceLocator
import dev.keyboardr.mapsl.sample.multimodule.locator.ServiceLocatorScope
import dev.keyboardr.mapsl.sample.multimodule.platform.PlatformContext
import dev.keyboardr.mapsl.sample.multimodule.services.BazManager
import dev.keyboardr.mapsl.testing.SimpleTestingServiceLocator
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.reflect.KClass

object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope.Testing>(ServiceLocatorScope.Testing) {
  override fun <T : Any> createMock(clazz: KClass<T>): T = mockForClass<T>(clazz)

  fun register(context: PlatformContext) {
    MainServiceLocator.register(this, context) {
      // Register common fakes here
      put<BazManager> {
        mock<BazManager> {
          on { sayHello() } doReturn "stubbed hello"
        }
      }
    }
  }
}

internal expect fun <T : Any> mockForClass(clazz: KClass<T>): T
