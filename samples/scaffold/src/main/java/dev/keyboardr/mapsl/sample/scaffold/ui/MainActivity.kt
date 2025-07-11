package dev.keyboardr.mapsl.sample.scaffold.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.keyboardr.mapsl.sample.scaffold.MyService

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val myService = MyService.instance
    enableEdgeToEdge()
    setContent {
      Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Text(text = myService.sayHello(), Modifier.padding(innerPadding))
      }
    }
  }
}

