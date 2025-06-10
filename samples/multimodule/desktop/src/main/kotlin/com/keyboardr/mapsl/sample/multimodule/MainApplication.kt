package com.keyboardr.mapsl.sample.multimodule

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.window.singleWindowApplication
import com.keyboardr.mapsl.SimpleServiceLocator
import com.keyboardr.mapsl.sample.multimodule.locator.MainServiceLocator
import com.keyboardr.mapsl.sample.multimodule.locator.ServiceLocatorScope
import com.keyboardr.mapsl.sample.multimodule.platform.PlatformContext
import com.keyboardr.mapsl.sample.multimodule.services.PlatformSpecificService
import com.keyboardr.mapsl.sample.multimodule.ui.MainScreen

object MainApplication {
  @JvmStatic
  fun main(args: Array<String>) {
    MainServiceLocator.register(
      SimpleServiceLocator(ServiceLocatorScope.Process("desktop")),
      PlatformContext(MainApplication::class.java.packageName)
    )
    singleWindowApplication {
      Column {
        MainScreen()
        Text(PlatformSpecificService.instance.sayHello())
        Text(PlatformSpecificService.instance.sayHelloCommon())
        Text(PlatformSpecificService.instance.sayHelloDesktopOnly())
      }
    }
  }
}