package com.keyboardr.mapsl.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.keyboardr.mapsl.ExperimentalKeyType
import com.keyboardr.mapsl.ServiceLocator
import com.keyboardr.mapsl.keys.LazyKey
import com.keyboardr.mapsl.keys.ServiceEntry
import com.keyboardr.mapsl.keys.ServiceKey
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

/**
 * A key similar to [LazyKey][com.keyboardr.mapsl.keys.LazyKey], but which will clear its
 * value when outside the getters' lifecycles. When fetching a value, a [LifecycleOwner] must be
 * provided to define the scope. When all of the get calls have had their [LifecycleOwner] go below
 * the minimum [Lifecycle.State] that the key was registered with  ([Lifecycle.State.STARTED] by
 * default), the current value is forgotten and the provider will be invoked the next time a value
 * is requested.
 *
 * It is an error to request a value using a [LifecycleOwner] that is not above the minimum state.
 */
@ExperimentalKeyType
public class LifecycleKey<T : Any>(override val type: KClass<T>) :
  ServiceKey<T, LifecycleKey.Entry<T>, LifecycleOwner, LifecycleKey.PutParams<T>> {
  override fun createEntry(params: PutParams<T>): Entry<T> {
    return Entry(params)
  }

  override fun getValue(
    params: LifecycleOwner,
    entry: ServiceEntry<T>
  ): T {
    return (entry as Entry<T>).getValue(params)
  }

  public data class PutParams<T>(
    val minimumState: Lifecycle.State = Lifecycle.State.STARTED,
    val threadSafetyMode: LazyThreadSafetyMode = LazyKey.PutParams.defaultThreadSafetyMode,
    val provider: () -> T
  )

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
          event: Lifecycle.Event
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

@ExperimentalKeyType
public fun <T : Any> ServiceLocator.put(
  key: LifecycleKey<T>,
  minimumState: Lifecycle.State = Lifecycle.State.STARTED,
  threadSafetyMode: LazyThreadSafetyMode = LazyKey.PutParams.defaultThreadSafetyMode,
  provider: () -> T
) {
  put(key, LifecycleKey.PutParams(minimumState, threadSafetyMode, provider))
}

@ExperimentalKeyType
public inline fun <reified T : Any> LifecycleKey(): LifecycleKey<T> = LifecycleKey<T>(T::class)