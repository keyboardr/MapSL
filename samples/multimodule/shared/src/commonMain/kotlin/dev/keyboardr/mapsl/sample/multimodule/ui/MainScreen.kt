package dev.keyboardr.mapsl.sample.multimodule.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.keyboardr.mapsl.sample.multimodule.services.BarManager
import dev.keyboardr.mapsl.sample.multimodule.services.BazManager
import dev.keyboardr.mapsl.sample.multimodule.services.FooManager
import dev.keyboardr.mapsl.sample.multimodule.services.PlatformSpecificService

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
  Column(modifier) {
    Text("Hello")
    Text(FooManager.instance.sayHello())
    Text(BarManager.instance.sayHello())
    Text(BazManager.instance.sayHello())
    Text(PlatformSpecificService.instance.sayHello())
    Text(PlatformSpecificService.instance.sayHelloCommon())
    Text("End of common")
  }
}