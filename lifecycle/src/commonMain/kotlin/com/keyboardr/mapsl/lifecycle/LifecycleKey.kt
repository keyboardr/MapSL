package com.keyboardr.mapsl.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.ServiceLocator
import com.keyboardr.mapsl.keys.LazyKey
import com.keyboardr.mapsl.keys.LazyKey.Companion.defaultLazyKeyThreadSafetyMode
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

/**
 * A [ServiceKey] that ties the lifetime of a service instance to one or more
 * AndroidX [LifecycleOwner]s.
 *
 * The service is created lazily, similar to a [LazyKey]. However, when retrieving the service,
 * a [LifecycleOwner] must be provided. The service instance is retained by the locator as long as
 * at least one of the `LifecycleOwner`s used to retrieve it is active (i.e., its state is at or
 * above the specified [PutParams.minimumState]).
 *
 * Once all associated `LifecycleOwner`s become inactive (i.e., their state drops below the
 * `minimumState`), the service instance is discarded. A new instance will be created on the
 * next request with an active `LifecycleOwner`.
 *
 * It is an error to request a value using a `LifecycleOwner` that is not in at least the
 * `minimumState`.
 */
@ExperimentalKeyType
public class LifecycleKey<T : Any>(override val type: KClass<T>) :
  ServiceKey<T, LifecycleKey.Entry<T>, LifecycleOwner, LifecycleKey.PutParams<T>> {
  override fun createEntry(params: PutParams<T>): Entry<T> {
    return Entry(params)
  }

  override fun getValue(
    params: LifecycleOwner,
    entry: ServiceEntry<T>,
  ): T {
    return (entry as Entry<T>).getValue(params)
  }

  /**
   * Parameters required to register a [LifecycleKey].
   *
   * @param minimumState The minimum [Lifecycle.State] that a `LifecycleOwner` must be in for the
   * service instance to be retained. Defaults to [Lifecycle.State.STARTED].
   * @param threadSafetyMode The [LazyThreadSafetyMode] for the service's lazy initialization.
   * @param provider A lambda that creates the service instance.
   */
  public data class PutParams<T>(
    val minimumState: Lifecycle.State = Lifecycle.State.STARTED,
    val threadSafetyMode: LazyThreadSafetyMode,
    val provider: () -> T,
  )

  /**
   * The [ServiceEntry] for a [LifecycleKey].
   *
   * This entry tracks the set of active `LifecycleOwner`s that have requested the service.
   * It observes their lifecycles and clears the stored service instance when all owners
   * become inactive.
   */
  public class Entry<T>(private val params: PutParams<T>) : ServiceEntry<T> {
    private val lifecycles = mutableSetOf<LifecycleOwner>()

    @Volatile
    private var currentValue: Lazy<T> = lazy(params.threadSafetyMode, params.provider)

    public fun getValue(scope: LifecycleOwner): T {
      check(scope.lifecycle.currentState >= params.minimumState)
      lifecycles.add(scope)
      scope.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(
          source: LifecycleOwner,
          event: Lifecycle.Event,
        ) {
          if (source.lifecycle.currentState < params.minimumState) {
            removeScope(scope)
            scope.lifecycle.removeObserver(this)
          }
        }
      })

      return currentValue.value
    }

    private fun removeScope(scope: LifecycleOwner) {
      lifecycles.remove(scope)
      if (lifecycles.isEmpty()) {
        currentValue = lazy(params.threadSafetyMode, params.provider)
      }
    }
  }
}

/**
 * Registers a lifecycle-aware service provider for the given [key].
 *
 * @param key The [LifecycleKey] to associate with the provider.
 * @param minimumState The lowest state the [LifecycleOwner] should be in for the service to be
 * retained. Defaults to [Lifecycle.State.STARTED].
 * @param threadSafetyMode The thread safety mode for the lazy initialization.
 * @param provider A lambda that creates the service instance.
 */
@ExperimentalKeyType
public fun <T : Any> ServiceLocator.put(
  key: LifecycleKey<T>,
  minimumState: Lifecycle.State = Lifecycle.State.STARTED,
  threadSafetyMode: LazyThreadSafetyMode = defaultLazyKeyThreadSafetyMode,
  provider: () -> T,
) {
  put(key, LifecycleKey.PutParams(minimumState, threadSafetyMode, provider))
}

/**
 * Creates a [LifecycleKey] for the reified type `T`.
 */
@ExperimentalKeyType
public inline fun <reified T : Any> LifecycleKey(): LifecycleKey<T> = LifecycleKey<T>(T::class)
