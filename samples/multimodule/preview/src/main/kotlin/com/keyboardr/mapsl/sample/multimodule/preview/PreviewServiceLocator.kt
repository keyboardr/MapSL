package com.keyboardr.mapsl.sample.multimodule.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import com.keyboardr.mapsl.sample.multimodule.locator.MainServiceLocator
import com.keyboardr.mapsl.sample.multimodule.locator.ServiceLocatorScope
import com.keyboardr.mapsl.sample.multimodule.platform.PlatformContext
import com.keyboardr.mapsl.sample.multimodule.services.BazManager
import com.keyboardr.mapsl.sample.multimodule.services.BazManagerImpl
import com.keyboardr.mapsl.sample.multimodule.services.FooManager
import com.keyboardr.mapsl.testing.SimpleTestingServiceLocator
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.reflect.KClass

object PreviewServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope.Preview>(ServiceLocatorScope.Preview) {
  override fun <T : Any> createMock(clazz: KClass<T>): T = mock<T>(clazz.java)

  fun register(context: PlatformContext) {
    MainServiceLocator.register(this, context) {
      // Register preview fakes here
      put<FooManager> {
        mock {
          on { sayHello() } doReturn "preview Foo hello"
        }
      }
      put<BazManager> { BazManagerImpl() }
    }
  }
}

private var hasRegisteredPreviewLocator = false

@Composable
fun EnsurePreviewLocator(registerBlock: PreviewServiceLocator.() -> Unit = {}) {
  if (!hasRegisteredPreviewLocator) {
    PreviewServiceLocator.register(PlatformContext(LocalContext.current.applicationContext))
  }
  SideEffect {
    PreviewServiceLocator.registerBlock()
  }
}
