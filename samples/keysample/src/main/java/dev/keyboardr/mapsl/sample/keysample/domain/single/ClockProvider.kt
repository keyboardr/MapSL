package dev.keyboardr.mapsl.sample.keysample.domain.single

import dev.keyboardr.mapsl.sample.keysample.locator.serviceLocator
import kotlinx.datetime.Clock


object ClockProvider {
  val clock by serviceLocator<Clock> { Clock.System }
}