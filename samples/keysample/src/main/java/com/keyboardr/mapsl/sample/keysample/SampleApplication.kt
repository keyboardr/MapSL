package com.keyboardr.mapsl.sample.keysample

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService
import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.ScopedServiceLocator
import com.keyboardr.mapsl.keys.put
import com.keyboardr.mapsl.lifecycle.put
import com.keyboardr.mapsl.sample.keysample.domain.factory.FactoryProduced
import com.keyboardr.mapsl.sample.keysample.domain.lifecycle.LifecycleScopedManager
import com.keyboardr.mapsl.sample.keysample.domain.single.LazyPreregisteredSingleton
import com.keyboardr.mapsl.sample.keysample.domain.single.PreregisteredSingleton
import com.keyboardr.mapsl.sample.keysample.locator.MainServiceLocator
import com.keyboardr.mapsl.sample.keysample.locator.ServiceLocatorScope
import kotlin.time.ExperimentalTime

class SampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    MainServiceLocator.register(
      ScopedServiceLocator(ServiceLocatorScope.Process(processName = getProcessNameCompat())),
      this
    ) {
      put<PreregisteredSingleton>(PreregisteredSingleton("preregistered"))
      put<LazyPreregisteredSingleton> { LazyPreregisteredSingleton("lazy") }

      var factoryProducedCount = 0
      @OptIn(ExperimentalKeyType::class)
      put(FactoryProduced.factoryKey) { FactoryProduced(factoryProducedCount++) }

      // In a real project module visibility would be used to ensure key and constructor are not
      // visible outside of registration.
      @SuppressLint("VisibleForTests")
      @OptIn(ExperimentalTime::class, ExperimentalKeyType::class)
      put(LifecycleScopedManager.key) { LifecycleScopedManager() }
    }
  }

  private fun getProcessNameCompat(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      getProcessName()
    } else {
      processNameFromActivityThread() ?: processNameFromActivityManager() ?: ""
    }
  }

  private fun processNameFromActivityManager(): String? {
    val pid: Int = Process.myPid()
    val manager = getSystemService<ActivityManager>()!!

    val fromActivityManager =
      manager.runningAppProcesses?.filterNotNull()?.firstOrNull { it.pid == pid }?.processName
    return fromActivityManager
  }

  private fun processNameFromActivityThread(): String? {
    // Using the same technique as Application.getProcessName() for older devices
    // Using reflection since ActivityThread is an internal API
    try {
      @SuppressLint("PrivateApi")
      val activityThread = Class.forName("android.app.ActivityThread")
      val methodName = "currentProcessName"

      @SuppressLint("DiscouragedPrivateApi")
      val getProcessName = activityThread.getDeclaredMethod(methodName)

      return getProcessName.invoke(null) as String
    } catch (_: Exception) {
      return null
    }
  }
}
