package com.keyboardr.mapsl.sample.basic

import android.app.Application
import com.keyboardr.mapsl.SimpleServiceLocator

class SampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    ProcessServiceLocator.register(
      SimpleServiceLocator(ServiceLocatorScope.Production),
      applicationContext
    )
  }
}
