# Using MapSL in Kotlin Multiplatform Projects (Preferring Expect/Actual with getOrProvide)

Kotlin Multiplatform (KMP) allows you to share code across different platforms. Managing
dependencies that have platform-specific implementations while accessing them from common shared
code is a key challenge. MapSL, combined with Kotlin's `expect`/`actual` mechanism, provides a clean
way for your common code to lazily obtain the correct platform-specific dependency when needed,
while keeping service access encapsulated.

## What is Kotlin Multiplatform and Expect/Actual?

Kotlin Multiplatform projects are structured with `commonMain` for shared code and platform-specific
source sets (e.g., `androidMain`, `iosMain`).

The `expect`/`actual` mechanism is KMP's way of defining a contract in `commonMain` (`expect`
declaration) that is implemented by platform-specific code (`actual` declaration) in platform source
sets. This allows common code to call an `expect` function or access an `expect` property, and at
runtime, the corresponding `actual` implementation for the target platform is executed.

## Why Combine MapSL and Expect/Actual in KMP?

MapSL provides a `ServiceLocator` to manage dependencies. The `getOrProvide()` method, particularly
when used with a property delegate (`by serviceLocator { ... }` or `by commonService { ... }`),
allows services to be created lazily the first time they are accessed via a dedicated property (like
`val instance` in a companion object).

By defining `expect` factory functions (or using `expect` class constructors) in `commonMain` that
are `actual`ized to create platform-specific service implementations, you can call these `expect`
mechanisms *within* the `getOrProvide()` provider lambda of your common service's access property.

This results in a pattern where:

1. Common code defines a common service interface/class and a dedicated access property (e.g.,
   `val instance` in a companion object, typically using a `getOrProvide` delegate).
2. Accessing this property triggers `getOrProvide()` lazily via the delegate.
3. The provider lambda within `getOrProvide()` executes *in the common context*, calls an `expect`
   mechanism that resolves to the correct platform-specific code at runtime to create the service
   instance.
4. The created platform-specific instance is then registered in the `ServiceLocator` for its type
   and returned by the delegate property.

This keeps the logic for obtaining the platform-specific implementation and accessing the service
encapsulated within the service definition itself, simplifying code that consumes the service.

## 1. Add Dependencies

Add the necessary MapSL modules to your multiplatform project's `build.gradle.kts`. The core MapSL
modules, including `core`, `scoped`, `simple`, and `lifecycle`, are multiplatform libraries. Add the
modules you need to your `commonMain` source set if their features are used in shared code.

```kotlin
// shared/build.gradle.kts (example for a shared module)

kotlin {
  // Define your targets (android, ios, jvm, etc.)
  androidTarget()
  jvm("desktop") // Example for Desktop JVM
  // ... other targets

  sourceSets {
    commonMain.dependencies {
      // Add MapSL multiplatform modules needed in common code
      implementation("com.keyboardr.mapsl:simple:<latest_version>") // Or core/scoped

      // ... other common dependencies (coroutines, serialization, etc.)
    }

    // Platform source sets might still need specific dependencies
    // (e.g., for actual implementations or testing tools)
    androidMain.dependencies { }

    commonTest.dependencies {
      implementation("com.keyboardr.mapsl:simple-testing:<latest_version>") // Or scoped-testing
    }
    androidTest.dependencies { }
    desktopTest.dependencies { }
    // ... other source sets
  }
}
```

Your application modules (e.g., `androidApp`, `desktopApp`) will typically depend on your shared
multiplatform module.

## 2. Set up a Shared Service Locator

Define your main `ServiceLocator` singleton (like `ProcessServiceLocator`) and your
`ServiceLocatorScope` in `commonMain`. This object will be the central access point for services
across all platforms.

```kotlin
// commonMain/kotlin/com/yourcompany/yourapp/common/locator/AppServiceLocator.kt


// Define the scopes, typically a sealed interface for multiplatform
sealed interface AppScope {
  sealed interface ProductionScope : AppScope
  data object Android : ProductionScope // Android production
  data object Ios : ProductionScope // iOS production
  data object Desktop : ProductionScope // Desktop production
  data object Testing : AppScope // Test scope
  // Add others like Web, etc.
}

object AppServiceLocator {
  lateinit var instance: SimpleServiceLocator<AppScope>
    private set

  // Register function accessible from platform entry points
  // Only pass the locator instance; fundamental registrations happen within the locator.
  fun register(
    locator: SimpleServiceLocator<AppScope>,
    context: PlatformContext,
    registrationBlock: SimpleServiceLocator<AppScope>.() -> Unit = {},
  ) {
    if (locator.scope is AppScope.ProductionScope) {
      check(!::instance.isInitialized) { "Already initialized" }
    }
    instance = locator.apply {
      put<PlatformContext> { context }
      // other services common to all platforms may be pre-registered here (uncommon)

      registrationBlock()
    }
  }

  val context
    get() = instance.get<PlatformContext>()
}

// Common delegate for accessing services via the shared locator.
// This delegate uses getOrProvide() and runs the provided lambda if needed.
inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (AppScope) -> Boolean = { it is AppScope.ProductionScope }, // Default: Production platforms
  threadSafetyMode: LazyThreadSafetyMode = AppServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (AppScope) -> T,
): ReadOnlyProperty<Any, T> =
  ReadOnlyProperty { _: Any, _: T ->
    AppServiceLocator.instance.getOrProvide(
      allowedScopes,
      threadSafetyMode,
      provider,
    )
  }

// Define a platform-agnostic context interface/class in commonMain with platform-specific actual
// implementations. This will be put() by the platform entry points.
expect class PlatformContext { // ... defined in commonMain
  val applicationId: String
}
```

## 3. Define Services, Expect Factories, and Access Properties in Common Code

Define your service interfaces or classes in `commonMain`. For services that require
platform-specific implementations, define a corresponding `expect` factory function or use `expect`
class constructors. Critically, define a dedicated access property (like `val instance`) in the
service's companion object using a delegate that calls `getOrProvide`, and within that delegate's
provider, call the `expect` mechanism.

```kotlin
// commonMain/kotlin/com/yourcompany/yourapp/common/AnalyticsService.kt

interface AnalyticsService {
  fun trackEvent(name: String, params: Map<String, Any> = emptyMap())

  companion object {
    // Define the single access point for AnalyticsService
    val instance by serviceLocator() { scope ->
      println("DEBUG: Creating AnalyticsService for scope: $scope via common provider")
      createAnalyticsService() // <-- Calls the expect function actualized per platform
    }
  }
}

// Define an expect function that the common provider lambda will call
expect fun createAnalyticsService(): AnalyticsService

// commonMain/kotlin/com/yourcompany/yourapp/common/HttpClient.kt


// Assume this is a common interface for an HTTP client
interface HttpClient {
  suspend fun get(url: String): String

  companion object {
    // Define the single access point for HttpClient
    val instance by serviceLocator() { scope ->
      println("DEBUG: Creating HttpClient for scope: $scope via common provider")
      createHttpClient() // <-- Calls the expect function actualized per platform
    }
  }
}

// Define an expect function that the common provider lambda will call
expect fun createHttpClient(): HttpClient
```

> [!NOTE]
> As an alternative to defining a common interface and an `expect fun` factory, you could define the
> service directly as an `expect class` in `commonMain`. The `actual class` in platform source sets
> would provide the platform-specific implementation. A key benefit of this approach is that *
*within
a particular platform source set, code holding a reference of the `expect class` type can directly
call methods or access properties that are defined *only* on the corresponding `actual class`** (and
> not necessarily in the `expect class` definition), without needing to cast. This simplifies
> accessing platform-specific functionality from platform code. Common code, however, is still
> limited
> to calling only the members defined in the `expect class` definition (or using `expect fun`
> extensions).
>
> This pattern comes with the trade-off that `expect` classes are generally less flexible than
> interfaces for defining common behavior that can be implemented in various ways. You will also
> need
> separate implementations of the `instance` property, since an `expect class` cannot have
> implementation in it. See `ProcessSpecificService` in
> the [multimodule sample](../samples/multimodule).

## 4. Provide Platform-Specific Actual Implementations

In each platform source set, provide the `actual` implementations for the services and the `actual`
implementations for the `expect` factory functions or constructors.

```kotlin
// androidMain/kotlin/com/yourcompany/yourapp/android/factories/AnalyticsServiceFactory.android.kt

// Android-specific implementation
class AndroidAnalyticsService(private val context: Context) : AnalyticsService {
  override fun trackEvent(name: String, params: Map<String, Any>) {
    Log.d("Analytics", "Android Tracking Event: $name with params $params")
    // Use platform-specific analytics SDK here
  }
}

// Provide the actual implementation for the expect factory
actual fun createAnalyticsService(scope: AppScope): AnalyticsService {
  // Create and return the Android-specific implementation
  return AndroidAnalyticsService(AppServiceLocator.context.androidContext)
}

// desktopMain/kotlin/com/yourcompany/yourapp/desktop/factories/AnalyticsServiceFactory.desktop.kt

// Desktop-specific implementation
class DesktopAnalyticsService : AnalyticsService {
  override fun trackEvent(name: String, params: Map<String, Any>) {
    println("Desktop Tracking Event: $name with params $params")
    // Use desktop-specific logging or analytics here
  }
}

// Provide the actual implementation for the expect factory
actual fun createAnalyticsService(scope: AppScope): AnalyticsService {
  // Create and return the Desktop-specific implementation
  return DesktopAnalyticsService()
}

// ... (Similar actual implementations for HttpClient and its factory)
```

## 5. Initialize and Register Fundamental Platform Needs

In each platform's main entry point, create a `SimpleServiceLocator` instance with the appropriate
platform scope and pass *only* this locator instance to the shared `AppServiceLocator.register`
function. Then, use `put` on this locator instance to register any fundamental, platform-specific
*values* or services that are needed *by the common providers or actual factories*. The
`PlatformContext` wrapper is the most common example here.

```kotlin
// androidApp/src/main/java/com/yourcompany/yourapp/android/App.kt (Android Application class)

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    val locator = SimpleServiceLocator(ServiceLocatorScope.Android)
    AppServiceLocator.register(locator, PlatformContext(this)) {
      // Use the locator instance to put platform fundamentals needed by common providers/actuals

      put<OkHttpClient> { OkHttpClient() } // Put platform-specific dependencies needed by actuals/common providers
    }
  }
}

// desktopApp/src/main/kotlin/com/yourcompany/yourapp/desktop/main.kt (Desktop main function)

fun main() {
  val locator = SimpleServiceLocator(ServiceLocatorScope.Desktop)
  AppServiceLocator.register(locator, PlatformContext("com.yourcompany.yourapp.desktop")) {
    // Use the locator instance to put platform fundamentals needed by common providers/actuals

    put<ApacheHttpClient> { ApacheHttpClient() } // Put platform-specific dependencies needed by actuals/common providers
  }
  // ... start your desktop UI
}
```

## 6. Access Services in Common Code

Your common code accesses services defined in `commonMain` through their dedicated access
properties (e.g., `val instance`) which use a delegate calling `getOrProvide()`. The provider lambda
within this delegate calls the `expect` factory function to obtain the platform-specific instance.

```kotlin
// commonMain/kotlin/com/yourcompany/yourapp/common/BusinessLogic.kt

object BusinessLogic {

  // The PlatformContext wrapper is registered in the common AppServiceLocator registration block
  // and exposed directly.
  private val platformContext: PlatformContext = AppServiceLocator.context


  fun performUserAction() {
    // Use the services obtained via their instance properties.
    // Accessing these properties triggers the lazy getOrProvide() call via the delegate.
    AnalyticsService.instance.trackEvent("user_action", mapOf("feature" to "xyz"))
    val response = HttpClient.instance.get("https://example.com/api/data")
    println(response)

    // Use the platform context wrapper if needed
    println("Running on platform: ${platformContext.platformName}")
  }
}
```

In this preferred pattern:

- Common code defines the service interface, an `expect` factory, and a lazy `instance` property
  using a `getOrProvide` delegate.
- Platform source sets provide the `actual` service implementation and the `actual` factory
  implementation (which may get platform fundamentals from the locator).
- The provider lambda in the common delegate calls the `expect` factory.
- The platform entry points primarily register fundamental platform-specific *values* or services
  needed by common providers/actuals using `put`.
- Consumers access services solely via their common `instance` properties.

This approach keeps the logic for creating platform-specific implementations tied to the `expect`/
`actual` mechanism called from the common provider, aligning well with the lazy `getOrProvide`
pattern and promoting encapsulation.

## 7. Testing Multiplatform Code

Testing multiplatform code that uses this pattern with MapSL follows the standard testing guide
patterns, with specific considerations for `expect`/`actual`.

- **commonTest:** When running tests in `commonTest` with a testing locator, if a service accessed
  via its `instance` property (using the `getOrProvide` delegate) is not explicitly registered with
  `put` in the test setup:
    - The testing locator's `onMiss` or `onInvalidScope` handler runs, providing a default mock (
      from `createMock`).
    - The provider lambda within the delegate is **not** executed in this case since the testing
      locator provides the mock without calling the provider.
- **platformTest:** When running tests on a specific platform with a testing locator, the `expect`
  factory calls in common providers will resolve to the `actual` implementation for that platform in
  `platformTest`. You can then register specific mocks or fakes for dependencies *needed by that
  actual factory* using `put` in the platform test setup. The testing locator's `createMock` in
  `platformTest` can also be implemented using a platform-specific mocking library.

Your multiplatform testing module can define common testing utilities like a shared
`TestServiceLocator` structure and potentially `expect`/`actual` factories for creating test
fakes/mocks, mirroring the production code structure.

## Conclusion

Combining MapSL's `getOrProvide` mechanism with Kotlin's `expect`/`actual` declarations and an
encapsulated access pattern provides a robust solution for managing platform-specific dependencies
in KMP. By defining service contracts and `expect` factories in `commonMain` and accessing them via
lazy `instance` properties using `getOrProvide` delegates, you keep your common code clean while the
runtime platform correctly resolves to the appropriate `actual` implementation. Platform entry
points then focus on registering fundamental platform components needed by the common providers or
actual factories.

For a starting point demonstrating multiplatform structure, explore
the [multimodule sample](../samples/multimodule).