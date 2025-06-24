package dev.keyboardr.mapsl.sample.multimodule.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.keyboardr.mapsl.sample.multimodule.services.BarManager
import dev.keyboardr.mapsl.sample.multimodule.ui.MainScreen
import dev.keyboardr.mapsl.sample.multimodule.ui.theme.SampleTheme
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  EnsurePreviewLocator {
    put<BarManager> {
      mock {
        on { sayHello() } doReturn "Stubbed BarManager"
      }
    }
  }
  SampleTheme {
    MainScreen()
  }
}