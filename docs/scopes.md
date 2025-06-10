# Using Scopes in MapSL

In MapSL, a **scope** is a label you attach to a `ServiceLocator` instance to identify the context
or environment in which it operates. Its primary purpose is to allow the application to provide
different service implementations or configurations for different situations, most commonly
distinguishing between a `Production` and `Testing` environment.

## Defining Your Scope Type

You define your own scope type, typically using a Kotlin `enum class` for simple cases or a
`sealed interface` for more complex scenarios. Using a `sealed` type is recommended as it allows the
compiler to perform exhaustive checks when you write logic based on the scope.

```kotlin
// Example using an enum for simple Production vs. Testing scopes
enum class AppScope {
  Production,
  Testing
}

// Example using a sealed interface for more structured scopes, like in a multi-process app
sealed interface ServiceLocatorScope {
  sealed interface ProductionScope : ServiceLocatorScope
  data class Process(val processName: String) : ProductionScope
  object Preview : ServiceLocatorScope
  object Testing : ServiceLocatorScope
}

```

You provide an instance of your scope type when you create your `ServiceLocator`:

```kotlin
val productionLocator = SimpleServiceLocator(AppScope.Production)
val testingLocator = SimpleServiceLocator(AppScope.Testing)

val mainProcessLocator = SimpleServiceLocator(ServiceLocatorScope.Process("main"))
```

## How Scopes are Used: `getOrProvide`

The main way scopes influence behavior is through the `allowedScopes` predicate in the
`getOrProvide` function. This function is the basis for the `serviceLocator` property delegate. When
a service is requested:

1. MapSL first checks if the service is already registered in the `ServiceLocator`. If it is, the
   existing instance is returned.

2. If the service is not registered, the `allowedScopes` predicate is checked against the locator's
   current scope.

- If it returns `true`, the provider lambda is executed to create and register the service.

- If it returns `false`, the locator's `onInvalidScope()` function is called. The default behavior
  of this function depends on the locator type: for a standard production locator, it throws an
  exception. A testing locator, like `SimpleTestingServiceLocator`, will typically override this
  function to provide a mock instance instead.

This mechanism is commonly used to control when a provider is executed, allowing developers to
ensure that real services are only created in appropriate environments.

### Example: Production vs. Testing

This is the most critical use of scopes.

```kotlin
// Define a service that should only be created in production
class AnalyticsReporter {
  companion object {
    // This service will only be provided if the scope is `Production`.
    // In a `Testing` scope, getOrProvide will fall back to the behavior defined by onInvalidScope.
    val instance by serviceLocator(
      allowedScopes = { it == AppScope.Production }
    ) {
      // This provider lambda is only executed in the Production scope
      RealAnalyticsReporter()
    }
  }
}

// In your Application.onCreate():
MainServiceLocator.register(
  SimpleServiceLocator(AppScope.Production),
  // ...
)

// In your test setup:
MainServiceLocator.register(
  TestServiceLocator(AppScope.Testing),
  // ...
)

// --- Behavior ---
// In production code:
// Accessing AnalyticsReporter.instance will create and return RealAnalyticsReporter.
AnalyticsReporter.instance.trackEvent("app_start")

// In a test:
// Accessing AnalyticsReporter.instance will return a mock created by the TestServiceLocator.
val mockReporter = AnalyticsReporter.instance
verify(mockReporter).trackEvent("some_event")
```

### Example: Multi-Process Applications

You can use a more detailed scope to ensure services are only created in the correct process.

```kotlin
// Using the sealed interface from above
sealed interface ServiceLocatorScope {
  sealed interface ProductionScope : ServiceLocatorScope
  data class Process(val processName: String) : ProductionScope
  object Preview : ServiceLocatorScope
  object Testing : ServiceLocatorScope
}

// A service that should only run in the 'background' process
class BackgroundSyncManager {
  companion object {
    val instance by serviceLocator(
      allowedScopes = { it is ServiceLocatorScope.Process && it.processName == "background" }
    ) {
      BackgroundSyncManager()
    }
  }
}

// Attempting to access `BackgroundSyncManager.instance` from a locator with a
// scope of `Process("main")` would fail, preventing the service from being
// instantiated in the wrong process.
```

> **A Note on Naming: Why "Scope" and not "Environment"?**
>
> While the primary use today is for distinguishing environments, the term "scope" was chosen to
> align with industry-standard terminology and to support potential future enhancements, such as
> more granular, hierarchical locators. For now, it is best to think of a scope as a persistent
> label for the top-level context of your application process.

By using scopes, you can make your dependency management robust, type-safe, and adaptable to the
specific context your code is running in.