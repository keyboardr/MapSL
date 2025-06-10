package com.keyboardr.mapsl.simple.scaffold

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.startup.Initializer
import com.keyboardr.mapsl.SimpleServiceLocator

/**
 * Registers [MainServiceLocator] on app startup.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ServiceLocatorInitializer : Initializer<SimpleServiceLocator<ServiceLocatorScope>> {
  override fun create(context: Context): SimpleServiceLocator<ServiceLocatorScope> {
    MainServiceLocator.register(object :
      SimpleServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Production), PreRegistered {})
    return MainServiceLocator.instance
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}