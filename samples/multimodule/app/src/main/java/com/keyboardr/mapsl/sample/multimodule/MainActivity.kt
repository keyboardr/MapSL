package com.keyboardr.mapsl.sample.multimodule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.keyboardr.mapsl.sample.multimodule.services.PlatformSpecificService
import com.keyboardr.mapsl.sample.multimodule.ui.MainScreen
import com.keyboardr.mapsl.sample.multimodule.ui.theme.SampleTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SampleTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Column(
            modifier = Modifier.padding(innerPadding)
          ) {
            MainScreen()
            Text(PlatformSpecificService.instance.sayHello())
            Text(PlatformSpecificService.instance.sayHelloCommon())
            Text(PlatformSpecificService.instance.sayHelloAndroidOnly())
          }
        }
      }
    }
  }
}