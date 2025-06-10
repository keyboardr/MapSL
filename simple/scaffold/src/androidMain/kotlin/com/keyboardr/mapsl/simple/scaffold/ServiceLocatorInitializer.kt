package com.keyboardr.mapsl.simple.scaffold

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.startup.Initializer
import com.keyboardr.mapsl.SimpleServiceLocator

/**
 * An [Initializer] that automatically registers a production [MainServiceLocator]
 * on app startup using the `androidx.startup` library.
 *
 * This provides the out-of-the-box behavior for the `simple-scaffold`, ensuring the
 * service locator is available without any manual setup in the `Application` class.
 *
 * If you need to customize the startup sequence (e.g., to pre-register services),
 * you can disable this initializer in your `AndroidManifest.xml`.
 *
 * @see [Disable individual initializers](https://developer.android.com/topic/libraries/app-startup#disable-individual)
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
