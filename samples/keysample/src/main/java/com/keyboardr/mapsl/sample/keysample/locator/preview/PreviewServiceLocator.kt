package com.keyboardr.mapsl.sample.keysample.locator.preview

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.ScopedServiceLocator
import com.keyboardr.mapsl.keys.put
import com.keyboardr.mapsl.lifecycle.put
import com.keyboardr.mapsl.sample.keysample.domain.factory.FactoryProduced
import com.keyboardr.mapsl.sample.keysample.domain.lifecycle.LifecycleScopedManager
import com.keyboardr.mapsl.sample.keysample.domain.single.LazyPreregisteredSingleton
import com.keyboardr.mapsl.sample.keysample.domain.single.PreregisteredSingleton
import com.keyboardr.mapsl.sample.keysample.locator.ProcessServiceLocator
import com.keyboardr.mapsl.sample.keysample.locator.ServiceLocatorScope
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
    ProcessServiceLocator.register(PreviewServiceLocator, LocalContext.current.applicationContext)
  }
}