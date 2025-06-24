package dev.keyboardr.mapsl.sample.keysample.locator.preview

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.keyboardr.mapsl.ExperimentalKeyType
import dev.keyboardr.mapsl.ScopedServiceLocator
import dev.keyboardr.mapsl.keys.put
import dev.keyboardr.mapsl.lifecycle.put
import dev.keyboardr.mapsl.sample.keysample.domain.factory.FactoryProduced
import dev.keyboardr.mapsl.sample.keysample.domain.lifecycle.LifecycleScopedManager
import dev.keyboardr.mapsl.sample.keysample.domain.single.LazyPreregisteredSingleton
import dev.keyboardr.mapsl.sample.keysample.domain.single.PreregisteredSingleton
import dev.keyboardr.mapsl.sample.keysample.locator.MainServiceLocator
import dev.keyboardr.mapsl.sample.keysample.locator.ServiceLocatorScope
import kotlin.time.ExperimentalTime

/**
 * Service locator used in compose previews.
 *
 * In many projects it may be preferable to have this be a `TestingServiceLocator` and provide mock
 * objects as needed, but this requires the previews be declared in a separate module, since you
 * don't want mocks created in your production module.
 */
@SuppressLint("VisibleForTests")
object PreviewServiceLocator :
  ScopedServiceLocator<ServiceLocatorScope.Preview>(ServiceLocatorScope.Preview) {
  init {
    put<PreregisteredSingleton>(PreregisteredSingleton("preregisteredPreview"))
    put<LazyPreregisteredSingleton> { LazyPreregisteredSingleton("lazyPreview") }

    var factoryProducedCount = 0
    @OptIn(ExperimentalKeyType::class)
    put(FactoryProduced.factoryKey) { FactoryProduced(factoryProducedCount++) }

    @OptIn(ExperimentalTime::class, ExperimentalKeyType::class)
    put(LifecycleScopedManager.key) { LifecycleScopedManager() }
  }
}

private var hasRegisteredPreviewLocator = false

@Composable
fun EnsurePreviewLocator() {
  if (!hasRegisteredPreviewLocator) {
    MainServiceLocator.register(PreviewServiceLocator, LocalContext.current.applicationContext)
  }
}