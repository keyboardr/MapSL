package com.keyboardr.mapsl.sample.keysample.domain.lifecycle

import androidx.lifecycle.LifecycleOwner
import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.lifecycle.LifecycleKey
import com.keyboardr.mapsl.sample.keysample.domain.single.ClockProvider
import com.keyboardr.mapsl.sample.keysample.locator.ProcessServiceLocator
import kotlinx.datetime.Instant
import org.jetbrains.annotations.VisibleForTesting

@OptIn(ExperimentalKeyType::class)
class LifecycleScopedManager
@VisibleForTesting constructor(val creationTime: Instant = ClockProvider.clock.now()) {

  companion object {
    @VisibleForTesting
    val key = LifecycleKey<LifecycleScopedManager>()

    fun getInstance(lifecycleOwner: LifecycleOwner): LifecycleScopedManager =
      ProcessServiceLocator.instance.get(key, lifecycleOwner)
  }
}