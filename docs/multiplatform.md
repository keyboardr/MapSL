# Using MapSL in Kotlin Multiplatform Projects

Kotlin Multiplatform (KMP) allows you to share code across different platforms. A key challenge in
KMP is managing dependencies that have platform-specific implementations while accessing them from
common shared code. MapSL, combined with Kotlin's `expect`/`actual` mechanism, provides a clean and
effective solution.

This guide focuses on the recommended pattern: defining `expect` factory functions in `commonMain`
that are called from within a `serviceLocator` delegate, allowing for lazy, platform-specific
instantiation.

## 1. Add Dependencies

Add the necessary MapSL modules to your multiplatform project's `build.gradle.kts`. MapSL's library
modules (`core`, `scoped`, `simple`, etc.) are multiplatform and should be added to your
`commonMain` source set.

```kotlin
// shared/build.gradle.kts (in a shared module)
kotlin {
  // Define your targets (android, ios, jvm, etc.)
  androidTarget()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  jvm("desktop")
  // ... other targets

  sourceSets {
    commonMain.dependencies {
      // Add the MapSL module you are using (e.g., simple)
      implementation("com.keyboardr.mapsl:simple:<latest_version>")
      // ... other common dependencies
    }

    commonTest.dependencies {
      implementation("com.keyboardr.mapsl:simple-testing:<latest_version>")
    }
  }
}
```

## 2. Set up a Shared Service Locator

In your `commonMain` source set, define a single, shared `MainServiceLocator` object. This will be
the central access point for services on all platforms. You should also define a sealed
`ServiceLocatorScope` to represent your different platform targets.

```kotlin
// commonMain/kotlin/com/yourcompany/locator/MainServiceLocator.kt

// A platform-agnostic context class to be implemented on each platform
expect class PlatformContext

// Define scopes for each platform target
sealed interface AppScope {
  sealed interface ProductionScope : AppScope
  object Android : ProductionScope
  object Ios : ProductionScope
  object Desktop : ProductionScope
  object Testing : AppScope
  // Add other platforms as needed
}

object MainServiceLocator {
  lateinit var instance: SimpleServiceLocator<AppScope>
    private set

  fun register(
    locator: SimpleServiceLocator<AppScope>,
    context: PlatformContext,
    registrationBlock: SimpleServiceLocator<AppScope>.() -> Unit = {},
  ) {
    if (locator.scope is AppScope.ProductionScope) {
      check(!::instance.isInitialized) { "MainServiceLocator is already initialized" }
    }
    instance = locator.apply {
      put<PlatformContext> { context }
      // other services common to all platforms may be pre-registered here (uncommon)

      registrationBlock()
    }
  }

  // Convenient property to access the registered PlatformContext
  val context: PlatformContext
    get() = instance.get<PlatformContext>()
}

// A common property delegate for accessing services
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (AppScope) -> Boolean = { it is AppScope.ProductionScope },
  threadSafetyMode: LazyThreadSafetyMode = MainServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (AppScope) -> T,
): ReadOnlyProperty<Any, T> = ReadOnlyProperty { _, _ ->
  MainServiceLocator.instance.getOrProvide(
    allowedScopes = allowedScopes,
    threadSafetyMode = threadSafetyMode,
    provider = provider
  )
}
```

## 3. Define Services and `expect` Factories in Common Code

In `commonMain`, define the interfaces or base classes for your services. For each service that
requires a platform-specific implementation, create a corresponding `expect` factory function.

The recommended pattern is to define a companion object for your service with an `instance` property
that uses the `serviceLocator` delegate. The provider lambda for the delegate should call the
`expect` factory function.

```kotlin
// commonMain/kotlin/com/yourcompany/services/AnalyticsService.kt
interface AnalyticsService {
  fun trackEvent(name: String)

  companion object {
    // The single access point for this service.
    // The delegate calls the expect factory to get the platform-specific instance.
    val instance by serviceLocator { scope -> createAnalyticsService(scope) }
  }
}

// The expect factory function to be implemented on each platform.
expect fun createAnalyticsService(scope: AppScope): AnalyticsService
```

> **Alternative: `expect class`**
>
> Instead of an interface and an `expect` factory function, you could define an `expect class`. The
> platform-specific `actual class` would then provide the implementation. This pattern can be useful
> when you need to call platform-specific methods on an instance from platform-specific code.
> However,
> it is generally less flexible than using interfaces.

## 4. Provide `actual` Implementations

In each platform-specific source set (e.g., `androidMain`, `iosMain`, `desktopMain`), provide the
`actual` implementations for your `expect` declarations.

**Android Implementation**

```kotlin
// androidMain/kotlin/com/yourcompany/platform/Platform.android.kt
actual class PlatformContext(val androidContext: Context)

// androidMain/kotlin/com/yourcompany/services/AnalyticsService.android.kt
class AndroidAnalyticsService(private val context: Context) : AnalyticsService {
  override fun trackEvent(name: String) {
    Log.d("Analytics", "Event: $name")
  }
}

actual fun createAnalyticsService(scope: AppScope): AnalyticsService {
  val platformContext = MainServiceLocator.context
  return AndroidAnalyticsService(platformContext.androidContext)
}
```

**iOS Implementation**

```kotlin
// iosMain/kotlin/com/yourcompany/platform/Platform.ios.kt
actual class PlatformContext(val deviceName: String)

// iosMain/kotlin/com/yourcompany/services/AnalyticsService.ios.kt
class IosAnalyticsService(private val deviceName: String) : AnalyticsService {
  override fun trackEvent(name: String) {
    println("iOS Event on $deviceName: $name")
  }
}

actual fun createAnalyticsService(scope: AppScope): AnalyticsService {
  val platformContext = MainServiceLocator.context
  return IosAnalyticsService(platformContext.deviceName)
}
```

**Desktop Implementation**

```kotlin
// desktopMain/kotlin/com/yourcompany/platform/Platform.desktop.kt
actual class PlatformContext(val appName: String)

// desktopMain/kotlin/com/yourcompany/services/AnalyticsService.desktop.kt
class DesktopAnalyticsService : AnalyticsService {
  override fun trackEvent(name: String) {
    println("Desktop Event: $name")
  }
}

actual fun createAnalyticsService(scope: AppScope): AnalyticsService {
  return DesktopAnalyticsService()
}
```

## 5. Initialize the Locator in Each Platform's Entry Point

In each platform's entry point, create and register a `SimpleServiceLocator` with the appropriate
scope and `PlatformContext`. The `registrationBlock` is the ideal place to provide any other
platform-specific dependencies that your `actual` factories might need to retrieve.

**Android (`Application.onCreate`)**

```kotlin
class App : Application() {
  override fun onCreate() {
    super.onCreate()
    MainServiceLocator.register(
      SimpleServiceLocator(AppScope.Android),
      PlatformContext(this)
    ) {
      // Register platform-specific dependencies needed by actual factories.
      // For example, an OkHttpClient for networking.
      put<OkHttpClient> { OkHttpClient() }
    }
  }
}
```

**iOS (e.g., `AppDelegate` or SwiftUI `App` init)**

```kotlin
fun initMapSL() {
  MainServiceLocator.register(
    SimpleServiceLocator(AppScope.Ios),
    PlatformContext(UIDevice.currentDevice.name)
  ) {
    // Register iOS-specific dependencies, e.g., a networking client.
    put<SomeIosHttpEngine> { SomeIosHttpEngine() }
  }
}
```

**Desktop (`main` function)**

```kotlin
fun main() {
  MainServiceLocator.register(
    SimpleServiceLocator(AppScope.Desktop),
    PlatformContext("MyDesktopApp")
  ) {
    // Register Desktop-specific dependencies.
    put<ApacheHttpEngine> { ApacheHttpEngine() }
  }
  // ... start desktop UI
}
```

## 6. Access Services from Common Code

Your common code can now access any service through its `instance` property, and the correct
platform-specific implementation will be lazily provided at runtime.

```kotlin
// commonMain/kotlin/com/yourcompany/common/BusinessLogic.kt
class BusinessLogic {
  fun onUserLogin() {
    // Accessing .instance triggers the lazy creation via the expect factory
    AnalyticsService.instance.trackEvent("user_login")
  }
}
```

## 7. Testing Multiplatform Code

Testing multiplatform code that uses this pattern follows the standard testing guides, with specific
considerations for `expect`/`actual`.

- **`commonTest`**: When running tests in `commonTest` with a testing locator, if a service accessed
  via its `instance` property is not explicitly registered with `put`, the testing locator's
  `onMiss` or `onInvalidScope` handler runs, providing a default mock. The `expect` factory function
  in the `serviceLocator` delegate is **not** executed in this case.

- **Platform-Specific Tests (`androidTest`, `desktopTest`)**: When running tests on a specific
  platform, calls to the `expect` factory will resolve to the `actual` implementation for that
  platform. This allows you to test the integration between your common code and the
  platform-specific service implementation. You can register specific fakes or mocks for any
  dependencies required by that `actual` factory.

Your multiplatform testing module can define common testing utilities, such as a shared
`TestServiceLocator` and `expect`/`actual` factories for creating test fakes/mocks, mirroring the
production code structure.

## Conclusion

Combining MapSL's `getOrProvide` mechanism with Kotlin's `expect`/`actual` declarations provides a
robust and clean solution for managing platform-specific dependencies in KMP. By defining service
contracts and `expect` factories in `commonMain` and accessing them via lazy `instance` properties,
you keep your common code decoupled from platform specifics while the runtime correctly resolves to
the appropriate `actual` implementation.

For a starting point demonstrating multiplatform structure, explore
the [multimodule sample](../samples/multimodule).