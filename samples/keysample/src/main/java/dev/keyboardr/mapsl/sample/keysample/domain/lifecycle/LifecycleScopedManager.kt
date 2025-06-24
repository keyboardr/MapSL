package dev.keyboardr.mapsl.sample.keysample.domain.lifecycle

import androidx.lifecycle.LifecycleOwner
import dev.keyboardr.mapsl.ExperimentalKeyType
import dev.keyboardr.mapsl.lifecycle.LifecycleKey
import dev.keyboardr.mapsl.sample.keysample.domain.single.ClockProvider
import dev.keyboardr.mapsl.sample.keysample.locator.MainServiceLocator
import kotlinx.datetime.Instant
import org.jetbrains.annotations.VisibleForTesting

@OptIn(ExperimentalKeyType::class)
class LifecycleScopedManager
@VisibleForTesting constructor(val creationTime: Instant = ClockProvider.clock.now()) {

  companion object {
    @VisibleForTesting
    val key = LifecycleKey<LifecycleScopedManager>()

    fun getInstance(lifecycleOwner: LifecycleOwner): LifecycleScopedManager =
      MainServiceLocator.instance.get(key, lifecycleOwner)
  }
}