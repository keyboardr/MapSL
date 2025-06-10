# Using Scopes in MapSL

MapSL's `SimpleServiceLocator` (and the underlying `ScopedServiceLocator` in the `core` module)
allows you to associate a service locator instance with a specific **Scope**. This scope is a value
of a type `S` that you define, and it helps represent the environment or context in which the
service locator is operating (e.g., production application, a specific test environment, a
background process).

Using scopes provides several benefits:

- **Environment Differentiation:** Easily provide different service implementations or
  configurations for different environments (e.g., a real database in production, an in-memory
  database or mock in testing).
- **Conditional Registration/Provision:** Control whether a service should be provided (especially
  lazily via `getOrProvide`) based on the current scope.
- **Multi-Process/Component Support:** Distinguish between different instances of your application
  or different logical components within a larger system.

## Defining Your Scope Type

You define your scope type `S` using a Kotlin `enum class` for a fixed set of distinct scopes or a
`sealed class` or `sealed interface` for more complex or structured scope information. Using an enum
or sealed type is recommended because it allows for exhaustive checking of different scope cases.

```kotlin
// Example using an enum class for simple scopes
enum class AppScope {
  Production,
  Testing
}

// Example using a sealed interface for more structured scopes
sealed interface ServiceScope {
  sealed interface ProductionScope : ServiceScope // Group production-like scopes
  data class Process(val processName: String) : ProductionScope // Specific process scope
  data object Preview : ServiceScope // Scope for UI previews
  data object Testing : ServiceScope // Dedicated testing scope
}
```

You provide an instance of this scope type when you create a `SimpleServiceLocator`:

```kotlin
val productionLocator = SimpleServiceLocator(AppScope.Production)
val testingLocator = SimpleServiceLocator(AppScope.Testing)

val mainProcessLocator = SimpleServiceLocator(ServiceScope.Process("main"))
val backgroundProcessLocator = SimpleServiceLocator(ServiceScope.Process("sync_service"))
```

## Scopes in Practice: `getOrProvide` and Conditional Logic

The primary way scopes influence behavior in the `simple` module is through the
`allowedScopes: (S) -> Boolean` predicate parameter in the `getOrProvide` function. When
`getOrProvide` is called for a service:

1. MapSL first checks if an entry for that service's class is already registered in the
   `SimpleServiceLocator`. If yes, the existing service instance is returned immediately.
2. If no entry is found, MapSL then evaluates the `allowedScopes` predicate, passing the
   `SimpleServiceLocator`'s current `scope` to it.
    1. If the `allowedScopes` predicate returns `true` for the current scope, the provider is
       lambda executed. The result of the provider lambda is then registered for that service's
       class
       and returned. The provider lambda receives the current `scope` value as a parameter when it
       is executed (most providers will ignore it).
    2. If the `allowedScopes` predicate returns `false` for the current scope (and no entry was
       found), the locator's `onInvalidService()` is called. This throws an exception by default. In
       tests this may create a mock instead.

```kotlin
// Assuming MainServiceLocator.kt with the serviceLocator delegate is available
// (as shown in the Getting Started guide)

// Delegate for lazy service access using getOrProvide,
// with an allowedScopes predicate.
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { true }, // Predicate using your scope type
  threadSafetyMode: LazyThreadSafetyMode = MainServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T, // Provider receives the scope
): ReadOnlyProperty<Any, T> = object : ReadOnlyProperty<Any, T> {
  override fun getValue(thisRef: Any, property: KProperty<*>): T =
    MainServiceLocator.instance.getOrProvide(
      allowedScopes,
      threadSafetyMode,
      provider
    )
}

// Assuming MainServiceLocator and ServiceLocatorScope enum as defined in Getting Started
enum class ServiceLocatorScope { Production, Testing }
object MainServiceLocator { // ... instance and register function ... }
```

## Simple Example: Production vs. Testing Scopes

This is the most common use of scopes in many applications. You have one scope for your production
code and another for tests.

```kotlin
// 1. Define your simple enum scope
enum class AppEnvironment { Production, Testing }

// 2. Set up your MainServiceLocator (similar to the Getting Started guide)
object AppServiceLocator {
  lateinit var instance: SimpleServiceLocator<AppEnvironment>
    private set

  fun register(
    locator: SimpleServiceLocator<AppEnvironment>,
    appContext: Any,
    registrationBlock: SimpleServiceLocator<AppEnvironment>.() -> Unit,
  ) { // Use Any for simplicity
    if (locator.scope == AppEnvironment.Production) {
      check(!::instance.isInitialized) { "Already initialized" }
    }
    instance = locator.apply {
      put<AppContextHolder> { AppContextHolder(appContext) }
      registrationBlock()
    }
  }
}

inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it == AppEnvironment.Production },
  threadSafetyMode: LazyThreadSafetyMode = AppServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (AppEnvironment) -> T,
): ReadOnlyProperty<Any, T> = ReadOnlyProperty { _: Any, _: T ->
  AppServiceLocator.instance.getOrProvide(
    allowedScopes,
    threadSafetyMode,
    provider
  )
}

// 3. Initialize in Production (e.g., Application.onCreate)
fun startProductionApp(context: Any) {
  AppServiceLocator.register(SimpleServiceLocator(AppEnvironment.Production), context) {
    // Any explicit put registrations for Production could go here
  }
}

// 4. Initialize in Testing (e.g., @Before method in tests)
fun setupTest(context: Any) {
  // SimpleTestingServiceLocator<S> is a subclass of SimpleServiceLocator<S>
  AppServiceLocator.register(TestServiceLocator(AppEnvironment.Testing), context) {
    // Any explicit put registrations for Testing (fakes/mocks) could go here
    // Services not registered will provide a mock by default.
  }
}

// 5. Define a Service that uses scopes
class AnalyticsReporter(private val baseUrl: String) {
  fun reportEvent(name: String) {
    println("Reporting to $baseUrl: Event '$name'")
  }

  companion object {
    // Use the scopedService delegate.
    // Only allow providing the real reporter in Production.
    val instance by serviceLocator(
      allowedScopes = { it == AppEnvironment.Production }
    ) { scope -> // Here `scope` is guaranteed to be Production
      println("Creating AnalyticsReporter for scope: $scope")
      AnalyticsReporter("https://prod.analytics.com")
    }
  }
}

// --- How it behaves ---

fun main() {
  // Production flow
  startProductionApp(Any())
  AnalyticsReporter.instance.reportEvent("user_login")
  // Output will include: "Creating AnalyticsReporter for scope: Production",
  //   "Reporting to https://prod.analytics.com: Event 'user_login'"

  // Testing flow (conceptually in a test)
  setupTest(Any())
  AnalyticsReporter.instance.reportEvent("user_click") // AnalyticsReporter.instance returns a mock
  // No output will be printed since the reporter was just a mock.
  verify(AnalyticsReporter.instance).reportEvent("user_click") // This will succeed.
}
```

In this example, the `allowedScopes = { it == AppEnvironment.Production }` predicate on the
`scopedService` delegate ensures that the provider lambda (`{ scope -> ... }`) is only executed to
*register* the service if the locator's scope is `Production`. However, the provider lambda *itself*
can use the `scope` parameter to decide *what kind* of service to create for that scope (e.g. if
there are multiple production scopes with slightly different behavior). This is a powerful
combination: `allowedScopes` controls *if* the `getOrProvide` registration happens, and the `scope`
parameter within the provider controls *what* gets registered.

When `getOrProvide` is called in the `Testing` scope,
`allowedScopes = { it == AppEnvironment.Production }` is false. By default, `SimpleServiceLocator`
would throw an error in this case. However, `SimpleTestingServiceLocator` overrides this behavior (
via `onInvalidScope`) to instead provide a default mock (from its `createMock` function). This is
why the testing locator is essential for this pattern to work seamlessly in tests.

## Advanced Example: Multiple Production Scopes (e.g., Multi-Process)

In applications with multiple entry points or processes (like an Android app with a main process and
a background service process), you might want services to behave differently based on the specific
process. A `sealed class` or `sealed interface` allows you to define structured scopes that carry
extra information.

```kotlin
// 1. Define your sealed interface scope
sealed interface AppServiceScope {
  sealed interface ProductionScope : AppServiceScope // Group production-like scopes
  data class Process(val processName: String) : ProductionScope // Specific process scope
  object Testing : AppServiceScope // Dedicated testing scope
}

// 2. Set up your MainServiceLocator with the sealed interface scope type
object MultiMainServiceLocator {
  lateinit var instance: SimpleServiceLocator<AppServiceScope>
    private set

  fun register(locator: SimpleServiceLocator<AppServiceScope>, appContext: Any) {
    if (locator.scope is AppServiceScope.ProductionScope) {
      check(!::instance.isInitialized) { "Already initialized" }
    }
    instance = locator.apply {
      put<AppContextHolder> { AppContextHolder(appContext) }
    }
  }
}

// Delegate using AppServiceScope for the scope type
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (AppServiceScope) -> Boolean = { it is AppServiceScope.ProductionScope }, // Default: Any ProductionScope
  threadSafetyMode: LazyThreadSafetyMode = MultiMainServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (AppServiceScope) -> T
): ReadOnlyProperty<Any, T> =
  ReadOnlyProperty { _: Any, _: T ->
    MultiMainServiceLocator.instance.getOrProvide(
      allowedScopes,
      threadSafetyMode,
      provider
    )
  }

// 3. Initialize in Different Processes
// In your Main Process entry point:
fun startMainProcess(context: Any) {
  MultiMainServiceLocator.register(
    SimpleServiceLocator(AppServiceScope.Process("main")),
    context
  )
}

// In your Sync Service Process entry point:
fun startSyncProcess(context: Any) {
  MultiMainServiceLocator.register(
    SimpleServiceLocator(AppServiceScope.Process("sync_service")),
    context
  )
}

// 4. Define a Service whose behavior depends on the specific scope value
class Logger(private val contextName: String) {
  fun debug(tag: String, msg: String) {
    println("$tag(:$contextName) - DEBUG: $msg")
  }

  fun error(tag: String, msg: String) {
    println("$tag(:$contextName) - ERROR: $msg")
  }

  companion object {
    val instance by serviceLocator(
      // Allow providing this service in any scope
      allowedScopes = { true }
    ) { scope ->
      val name = when (scope) {
        // Use the processName from the scope to determine the context name
        is AppServiceScope.Process -> scope.processName
        AppServiceScope.Testing -> "test"
        else -> scope.toString()
      }
      Logger(name)
    }
  }
}

// --- How it behaves ---

fun main() {
  // Main Process flow
  startMainProcess(Any())
  Logger.instance.debug("echo", "Hello, prod!")
  // Output: "echo(:main) - DEBUG: Hello, prod!"

  // Sync Service Process flow (conceptually in another process)
  startSyncProcess(Any())
  Logger.instance.debug("echo", "Hello, prod!")
  // Output: "echo(:sync_service) - DEBUG: Hello, prod!"

  // Testing flow (conceptually in a test)
  MultiMainServiceLocator.register(
    TestServiceLocator(AppServiceScope.Testing),
    Any()
  ) // Assuming TestServiceLocator exists
  Logger.instance.debug("echo", "Hello, testing!")
  // Output: "echo(:test) - DEBUG: Hello, testing!"
}
```

In this advanced example, the `sealed interface` allows the `Process` scope to carry a`processName`.
The `allowedScopes = { true }` predicate on the `scopedService` delegate allows the service to be
lazily provided in *any* scope that is a subtype of `AppServiceScope`. Within the provider lambda,
`when (scope) { ... }` uses pattern matching on the scope object to determine the specific
implementation details based on the actual process the code is running in.

This demonstrates how scopes can be used not just to switch between broad environments like
Production and Testing, but also to tailor service behavior based on more granular context
information.

While this guide focuses on `SimpleServiceLocator`, the concept of scopes and the `allowedScopes`
predicate is fundamental to `ScopedServiceLocator` as well, which is used internally by
`SimpleServiceLocator` and directly in the `core` module setup.

Using scopes effectively allows you to make your service management more robust, adaptable to
different environments, and clearer about where specific dependencies can and should be provided.