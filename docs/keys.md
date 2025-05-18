# Key Kinds

MapSL uses the concept of **Keys** to manage and access services within a `ServiceLocator`. A key
acts as a unique identifier for a specific service or a specific way of providing a service.
Different types, or "kinds," of keys offer different behaviors regarding when the service is created
and how long its instance is kept.

This document explains how to use the built-in key types provided by MapSL's `core` and `lifecycle`
modules. You'll typically use these with a `ServiceLocator` instance, often a `ScopedServiceLocator`
from the `scoped` module.

## Key Concepts: Key Kind and Service Type

When working with MapSL keys, it's helpful to distinguish between two things:

1. **Key Kind:** This refers to the class of the key itself (e.g., `LazyKey`, `SingletonKey`,
   `FactoryKey`). The key kind determines *how* the service is managed (when it's created, if
   instances are reused).
2. **Service Type (`T`):** This is the type of the actual service or dependency that the key
   provides (e.g., `MyService`, `DatabaseConnectionManager`). This is specified using generics when
   you create or refer to a key (e.g., `LazyKey<MyService>`).

When you interact with a `ServiceLocator`, you use a specific key *instance* (or, in the case of
`ClassKey`, the service *type*) to either `put` (register) a service provider or instance, or
`get` (retrieve) the service instance.

Here's a quick overview of the built-in key kinds:

| Key Kind                                  | Value Creation                                                     | Value Lifetime                                                              | Key Equivalence       | GetParams        | Main PutParams     | Module      |
|:------------------------------------------|:-------------------------------------------------------------------|:----------------------------------------------------------------------------|:----------------------|:-----------------|:-------------------|:------------|
| `LazyKey<T>`                              | Created the first time it is requested                             | Stored indefinitely                                                         | Specific key instance | `Unit`           | `() -> T`          | `core`      |
| `SingletonKey<T>`                         | Provided when the key is registered                                | Stored indefinitely                                                         | Specific key instance | `Unit`           | `T`                | `core`      |
| `ClassKey<T>`                             | Can be either Lazy or Singleton, depending on registration method  | Stored indefinitely                                                         | Same reifiable `T`    | `Unit`           | `() -> T` or `T`   | `core`      |
| `FactoryKey<T, GetParams>` (Experimental) | New instance created on every request, potentially with parameters | Not retained by the `ServiceLocator`                                        | Specific key instance | Defined per key  | `(GetParams) -> T` | `core`      |
| `LifecycleKey<T>` (Experimental)          | Created lazily on first request with a valid `LifecycleOwner`      | Retained while at least one associated `LifecycleOwner` is in a valid state | Specific key instance | `LifecycleOwner` | `() -> T`          | `lifecycle` |

You can also declare your own key types to customize behavior. This is beyond the scope of this
document, but see the documentation for `ServiceKey<T>` for more details.

Let's explore each key kind in detail:

> [!Note]
> The examples below show usage with a `serviceLocator` declared in local scope, using separate
`put` and `get` calls for clarity. In typical application architectures, you would often use a
> globally accessible `ServiceLocator` singleton (like `ProcessServiceLocator` in the samples),
> declare key instances and their providers within the respective service companion objects, and
> leverage `getOrProvide()` for convenient lazy registration and retrieval when possible.

## 1. LazyKey

`LazyKey` is the most common key type for services that you want to be created only when they are
first needed. You provide a lambda function that creates the service instance. This lambda is stored
by the `ServiceLocator` and is only executed the first time the service is retrieved using this
specific `LazyKey` instance. Subsequent requests for the same key instance will return the same
service object.

- **When to use:** For services that are not needed at startup or that depend on other components
  that might not be available during initial registration.
- **Service Instance Lifetime:** The first created instance is retained indefinitely by the
  `ServiceLocator`.
- **Key Equivalence:** Based on the specific `LazyKey` instance.

```kotlin
// 1. Declare a LazyKey instance
val myLazyServiceKey = LazyKey<MyService>()

// 2. Register the service provider with the key
// You provide a lambda that will create the service when needed.
serviceLocator.put(myLazyServiceKey) {
  println("Creating MyService...") // This will only print once
  MyServiceImpl()
}

// 3. Retrieve the service instance using the key
val service1: MyService = serviceLocator.get(myLazyServiceKey)
val service2: MyService = serviceLocator.get(myLazyServiceKey)

// service1 and service2 are the exact same instance.
// The "Creating MyService..." message printed only on the first 'get' call for this key.
```

> [!NOTE] `LazyKey` creation can be configured for different thread safety modes (like
`SYNCHRONIZED`) when registering, ensuring the correct tradeoff between safety and performance.

## 2. SingletonKey

`SingletonKey` is for services where you have a specific instance ready to provide, or when you want
the service instance to be created immediately when you register it. You provide the actual service
instance when you register the key.

- **When to use:** For simple value objects, configuration objects, or services that must be
  initialized eagerly at registration time.
- **Service Instance Lifetime:** The provided instance is retained indefinitely by the
  `ServiceLocator`.
- **Key Equivalence:** Based on the specific `SingletonKey` instance.

```kotlin
// 1. Declare a SingletonKey instance
val mySingletonServiceKey = SingletonKey<Configuration>()

// 2. Create the service instance (or obtain it from elsewhere)
val appConfig = Configuration("default_api_key")

// 3. Register the service instance with the key
//    You provide the already created service instance.
serviceLocator.put(mySingletonServiceKey, appConfig)

// 4. Retrieve the service instance using the key
val config1: Configuration = serviceLocator.get(mySingletonServiceKey)
val config2: Configuration = serviceLocator.get(mySingletonServiceKey)

// config1 and config2 are the exact same instance (the one you registered).
```

## 3. ClassKey

`ClassKey` is a special convenience key kind where the service's `KClass` serves as the key itself.
This means you typically don't declare explicit key *instances* for `ClassKey`. Instead, you use
extension functions on `ServiceLocator` that operate directly on the service's type (`T`).
`ClassKey` can be used for both lazy and singleton registration.

- **When to use:** For simple, non-generic services where you only ever need one instance of a
  specific type throughout the `ServiceLocator`. This is the basis of the simplified API in the
  `simple` module.
- **Service Instance Lifetime:** Retained indefinitely.
- **Key Equivalence:** Two `ClassKey` instances for the same reifiable type `T` are considered
  equal.

```kotlin
// No explicit key instance declaration needed for basic usage with ClassKey extensions.

// --- Registration (Lazy, common with ClassKey) ---
// Uses LazyClassKey under the hood.
serviceLocator.put<AnalyticsService> {
  println("Creating AnalyticsService...") // Called on first get<AnalyticsService>()
  AnalyticsServiceImpl()
}

// --- Registration (Singleton) ---
// Uses SingletonClassKey under the hood.
serviceLocator.put<AuthManager>(AuthManager())

// --- Retrieval ---
// Retrieve by specifying the service type T:
val analytics: AnalyticsService = serviceLocator.get<AnalyticsService>()
val authManager: AuthManager = serviceLocator.get<AuthManager>()
```

> [!NOTE] Due to JVM type erasure, `ClassKey` equality is based on the erased type. This means
`ClassKey<List<Int>>` and `ClassKey<List<String>>` would be treated as the same key (
`ClassKey<List>`). Avoid using `ClassKey` for generic types if you need to distinguish between
> different instantiations of the generic type. Use explicit `LazyKey` or `SingletonKey` instances
> instead.

## 4. FactoryKey (Experimental)

`FactoryKey` is used when you need a *new* instance of a service every time it is requested. You
provide a factory function. Every call to `get` with a `FactoryKey` executes this factory function,
producing a fresh instance. `FactoryKey` can also be defined to accept parameters during the `get`
call, which are then passed to the factory function.

- **When to use:** For objects that are inexpensive to create and should not be shared, or when
  creating the object requires runtime parameters that are not available at registration time.
- **Service Instance Lifetime:** Created on demand for each `get` call; not retained by the
  `ServiceLocator` beyond the scope of the call.
- **Key Equivalence:** Based on the specific `FactoryKey` instance.
- **Experimental:** This key type is marked with `@ExperimentalKeyType`. Its API is subject to
  change.

```kotlin
// Example with parameters during retrieval:
data class UserCreationParams(val userId: String, val name: String)
data class User(val id: String, val displayName: String)

// 1. Declare a FactoryKey instance
//    The second type parameter (UserCreationParams) is the type of parameters needed for 'get'.
val userFactoryKey = FactoryKey<User, UserCreationParams>()

// 2. Register the factory function with the key
//    The lambda accepts the parameters provided during the 'get' call.
serviceLocator.put(userFactoryKey) { params: UserCreationParams ->
  println("Creating User with ID: ${params.userId}") // Called on every get()
  User(params.userId, params.name)
}

// 3. Retrieve new service instances using the key and providing parameters
val userA: User = serviceLocator.get(userFactoryKey, UserCreationParams("a123", "Alice"))
val userB: User = serviceLocator.get(userFactoryKey, UserCreationParams("b456", "Bob"))

// userA and userB are different instances.
// The "Creating User..." message prints for each get() call.

// Example without parameters (using Unit as GetParams):
val diceRollFactoryKey =
  FactoryKey<Int, Unit>() // Or simply FactoryKey<Int>() using a convenience function
serviceLocator.put(diceRollFactoryKey) {
  // return a random Int between 1 and 6
  (1..6).random()
}
val roll1: Int = serviceLocator.get(diceRollFactoryKey, Unit) // Must pass Unit explicitly
val roll2: Int =
  serviceLocator.get(diceRollFactoryKey) // Convenience extension allows omitting Unit
```

## 5. LifecycleKey (Experimental)

`LifecycleKey` (available in the separate `lifecycle` module) ties the service's lifetime to
AndroidX Lifecycles. A service provided by a `LifecycleKey` is created lazily upon the first request
where a valid `LifecycleOwner` is provided. The `ServiceLocator` retains this instance as long as
*any* `LifecycleOwner` instances used in `get` calls for this key remain above a specified minimum
state (defaults to `STARTED`). When all associated lifecycles drop below that state, the stored
instance is discarded. The next time the service is requested with a valid `LifecycleOwner`, a new
instance will be created.

- **When to use:** For services that should exist and be active only while a specific AndroidX
  `LifecycleOwner` (like an Activity or Fragment) is in a relevant state, allowing for automatic
  cleanup.
- **Service Instance Lifetime:** Retained as long as associated `LifecycleOwner`s are in a valid
  state. Discarded when all drop below the minimum state.
- **Key Equivalence:** Based on the specific `LifecycleKey` instance.
- **Experimental:** This key type is marked with `@ExperimentalKeyType`. Its API is subject to
  change. Requires the `com.keyboardr.mapsl:lifecycle` module dependency.

```kotlin
// Requires adding the 'com.keyboardr.mapsl:lifecycle' dependency.
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import com.keyboardr.mapsl.lifecycle.LifecycleKey
import com.keyboardr.mapsl.lifecycle.put // Import the put extension for LifecycleKey

class LifecycleAwareService {
  init {
    println("LifecycleAwareService created")
  }

  fun doSomething() {
    println("LifecycleAwareService doing something")
  }
}

// 1. Declare a LifecycleKey instance
val lifecycleServiceKey = LifecycleKey<LifecycleAwareService>()

// --- Registration ---
// In your ServiceLocator registration block:
// Provide a lambda that creates the service. You can optionally set a minimumState
// and threadSafetyMode here.
serviceLocator.put(lifecycleServiceKey) {
  LifecycleAwareService()
}

// --- Retrieval ---
// In your Activity or Fragment (which implements LifecycleOwner):
class MyActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 2. You MUST provide a LifecycleOwner when getting the service
    val service: LifecycleAwareService = serviceLocator.get(lifecycleServiceKey, this)
    service.doSomething()

    // If this activity is backgrounded and then foregrounded again, and no other
    // LifecycleOwner was holding a reference with this key, the next time
    // serviceLocator.get(lifecycleServiceKey, this) is called (e.g., in onResume),
    // a *new* LifecycleAwareService instance will be created.
  }
}
```

**Important Considerations:**

- You *must* pass a `LifecycleOwner` when calling `get` with a `LifecycleKey`.
- The provided `LifecycleOwner` must be in a state greater than or equal to the `minimumState` (
  defaulting to `STARTED`) at the time you call `get`.
- The service instance is retained as long as at least one `LifecycleOwner` that has retrieved it is
  above the minimum state.

## Choosing the Right Key Type

The best key type depends on the desired creation timing, instance sharing behavior, and lifetime
management needs of your service:

- For singletons created lazily on first use: **`LazyKey`** (or implicit `ClassKey`).
- For singletons created eagerly at registration: **`SingletonKey`** (or implicit `ClassKey`).
- For simple, non-generic singletons accessed by type: Implicit **`ClassKey`** extensions are often
  the most convenient.
- For new instances on every request, potentially with parameters: **`FactoryKey`**.
- For services whose lifetime should follow an AndroidX `LifecycleOwner`: **`LifecycleKey`**.

By understanding these distinctions and how to use each key type's `put` and `get` methods, you can
effectively manage dependencies with MapSL's core modules. Remember to add the necessary module
dependencies (`core`, `scoped`, and `lifecycle` if needed) to your project.