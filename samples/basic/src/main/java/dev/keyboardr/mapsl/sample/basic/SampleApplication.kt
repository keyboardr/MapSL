package dev.keyboardr.mapsl.sample.basic

import android.app.Application
import dev.keyboardr.mapsl.SimpleServiceLocator

class SampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    MainServiceLocator.register(
      SimpleServiceLocator(ServiceLocatorScope.Production),
      applicationContext
    )
  }
}
