package com.keyboardr.mapsl.sample.keysample.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.keyboardr.mapsl.sample.keysample.domain.factory.FactoryProduced
import com.keyboardr.mapsl.sample.keysample.domain.lifecycle.LifecycleScopedManager
import com.keyboardr.mapsl.sample.keysample.domain.single.LazyPreregisteredSingleton
import com.keyboardr.mapsl.sample.keysample.domain.single.PreregisteredSingleton
import com.keyboardr.mapsl.sample.keysample.domain.single.ProvidedSingleton
import com.keyboardr.mapsl.sample.keysample.locator.preview.EnsurePreviewLocator
import com.keyboardr.mapsl.sample.keysample.ui.theme.SampleTheme
import kotlin.time.ExperimentalTime

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val providedSingleton = ProvidedSingleton.instance
    enableEdgeToEdge()
    setContent {
      SampleTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val factoryProducedItems = remember { mutableStateListOf<FactoryProduced>() }

          Greeting(
            providedSingleton,
            LifecycleScopedManager.getInstance(this),
            factoryProducedItems,
            { factoryProducedItems += FactoryProduced.create() },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalTime::class)
@Composable
fun Greeting(
  providedSingleton: ProvidedSingleton,
  lifecycleScopedManager: LifecycleScopedManager,
  factoryProducedItems: List<FactoryProduced>,
  onCreateFromFactory: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier.verticalScroll(rememberScrollState())) {
    Text(text = "Preregistered: ${PreregisteredSingleton.instance.sayHello()}")
    Text(text = "LazyPreregistered: ${LazyPreregisteredSingleton.instance.sayHello()}")
    Text(text = "Provided: ${providedSingleton.sayHello()}")
    Text(text = "Lifecycle: ${lifecycleScopedManager.creationTime}")

    for (item in factoryProducedItems) {
      Text(item.toString())
    }

    Button(onClick = onCreateFromFactory) {
      Text("Create new factory object")
    }
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
  EnsurePreviewLocator()
  SampleTheme {
    Greeting(
      ProvidedSingleton(),
      LifecycleScopedManager.getInstance(LocalLifecycleOwner.current),
      listOf(FactoryProduced(0), FactoryProduced(1)),
      {}
    )
  }
}