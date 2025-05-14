package com.keyboardr.mapsl.sample.multimodule

import androidx.compose.ui.window.singleWindowApplication
import com.keyboardr.mapsl.SimpleServiceLocator
import com.keyboardr.mapsl.sample.multimodule.locator.ProcessServiceLocator
import com.keyboardr.mapsl.sample.multimodule.locator.ServiceLocatorScope
import com.keyboardr.mapsl.sample.multimodule.platform.PlatformContext
import com.keyboardr.mapsl.sample.multimodule.ui.MainScreen

object MainApplication {
  @JvmStatic
  fun main(args: Array<String>) {
    ProcessServiceLocator.register(
      SimpleServiceLocator<ServiceLocatorScope.Process>(
        ServiceLocatorScope.Process("desktop")
      ),
      PlatformContext(MainApplication::class.java.packageName)
    )
    singleWindowApplication {
      MainScreen()
    }
  }
}