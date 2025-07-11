# Migrating from Simple flavor to Full flavor

This tutorial will guide you through the process of migrating your application's dependency
management from MapSL's `Simple` flavor (`SimpleServiceLocator`) to the more flexible `Full`
flavor (`ServiceLocator` and `ScopedServiceLocator`).

The `Simple` flavor is designed for common use cases and provides a streamlined API by abstracting
away the different `Key` types and primarily using `Lazy ClassKey` under the hood. The `Full`
flavor, on the other hand, exposes the full power of MapSL by requiring you to explicitly use
different `Key` kinds (`LazyKey`, `SingletonKey`, `FactoryKey`, etc.), offering more granular
control over service creation and lifetime.

Migrating to the `Full` flavor is primarily about switching the underlying `ServiceLocator`
implementation to `ScopedServiceLocator` (from the `scoped` module, which depends on `core`) to gain
access to the full range of `Key` types and features available in the `core` module. Using different
key types is an advanced step, discussed later in this document.

## Key Differences

The primary difference you'll encounter during migration is the *option* to use explicit `Key`
instances in the `Full` flavor, which is only necessary for advanced features.

- **Simple Flavor:** In the common pattern, you typically register and retrieve services
  lazily using `getOrProvide<MyService> { MyServiceImpl() }`, often via a property delegate. You may
  also use `put<MyService> { MyServiceImpl() }` for preregistration of services intended for later
  retrieval via `get<MyService>()`. The key is implicitly a `ClassKey<MyService>`, and the default
  behavior is always lazy.

- **Full Flavor:** You *can* still use similar extension functions for implicit `ClassKey` usage
  (`put<MyService> { MyServiceImpl() }` for Lazy, `put<MyService>(MyServiceImpl())` for Singleton),
  and retrieval (`get<MyService>()`, `getOrProvide<MyService> { MyServiceImpl() }`). However, to
  leverage different key kinds or define multiple entries for the same type, you *must* explicitly
  create a `Key` instance (e.g., `val myServiceKey = LazyKey<MyService>()`) and use this key for
  both registration and retrieval (`serviceLocator.put(myServiceKey) { MyServiceImpl() }`,
  `serviceLocator.get(myServiceKey)`).

When migrating to the `Full` flavor, most services will continue to use the implicit lazy `ClassKey`
functions for services that were lazy singletons in the `Simple` flavor. This requires minimal code
changes. Explicitly defining `Key` instances is only necessary for services that require different
behaviors (like eager initialization, factory patterns, or lifecycle management) or when you need to
register multiple entries for the same service type.

## Determining If Migration Is Necessary

| Use Case                            | Simple Flavor                                | Full Flavor          |
|-------------------------------------|----------------------------------------------|----------------------|
| Lazy initialization                 | Supported                                    | Supported            |
| Pre registration                    | Supported                                    | Supported            |
| Eager initialization                | Not directly supported, workaround available | Supported            |
| Multiple instances per service type | Not directly supported, workaround available | Supported            |
| Factory patterns                    | Not directly supported, workaround available | Experimental support |
| Lifecycle scoping                   | Not supported, can be manually implemented   | Experimental support |

For many projects, the `Simple` flavor is sufficient and requires less configuration. If the
advanced functionality available in `Full` is desired, but only in a few places, here are some
strategies to avoid migration:

### Multiple instances of the same service

Rather than using multiple keys, you can declare the service to be `open` and use multiple
subclasses. This has the added advantage of making it easy for the different instances to have
subtly different behavior.

### Eagerly loaded singletons

You can use preregistration with a provider that returns a precomputed value to achieve the same
effect.

Example:

```kotlin
MainServiceLocator.register(ScopedServiceLocator(ProductionScope), /* ... */) {
  val myService = MyService()
  put<MyService> { myService }
}
```

### Factory services

Rather than using a `FactoryKey`, create a service that acts as a factory.

Example:

```kotlin
class ItemFactory {
  fun createItem(): Item = Item()

  companion object {
    val instance by serviceLocator<ItemFactory> { ItemFactory() }
  }
}
```

## Migration Steps

Assuming you are using the recommended pattern with a `MainServiceLocator` singleton backed by
`SimpleServiceLocator`, here are the steps to migrate. This process focuses on switching the
underlying locator type, which is the most common first step. Using different key types is covered
in a later section.

### Step 1: Add the `core` and `scoped` Module Dependencies

If you haven't already, add the `core` and `scoped` module dependencies to your project's
`build.gradle.kts` file (or equivalent build configuration). You will likely keep the `simple` and
`simple-testing` dependencies initially during the migration process.

```kotlin
dependencies {
  // Keep simple for now
  implementation("dev.keyboardr.mapsl:simple:<version>")
  testImplementation("dev.keyboardr.mapsl:simple-testing:<version>")

  // Add core and scoped
  implementation("dev.keyboardr.mapsl:core:<version>")
  implementation("dev.keyboardr.mapsl:scoped:<version>")
  testImplementation("dev.keyboardr.mapsl:scoped-testing:<version>")
  // Add other modules for the Full flavor if needed (e.g., lifecycle)
  // implementation("dev.keyboardr.mapsl:lifecycle:<version>")
}
```

### Step 2: Change `MainServiceLocator` to Use `ScopedServiceLocator`

Modify your `MainServiceLocator` singleton to hold a `ScopedServiceLocator` instance instead of
`SimpleServiceLocator`. Assume a `ServiceLocatorScope` enum is defined for the scope type.

```kotlin
// Before (using Simple)
object MainServiceLocator {
  private lateinit var instance: SimpleServiceLocator<ServiceLocatorScope>

  fun register(locator: SimpleServiceLocator<ServiceLocatorScope> /* ... */) {
    instance = locator
    // ...
  }
}

// After (using Full)
object MainServiceLocator {
  // Change type to ScopedServiceLocator, using the same scope type
  private lateinit var instance: ScopedServiceLocator<ServiceLocatorScope>

  // Change type in register function
  fun register(locator: ScopedServiceLocator<ServiceLocatorScope> /* ... */) {
    instance = locator
    // ...
  }

  // ... get/getOrProvide delegates (will need updates if delegates use the locator type directly)
}
```

You will also need to update how the `ServiceLocator` is created during application startup. Instead
of creating a `SimpleServiceLocator` with a scope, you'll create an instance of
`ScopedServiceLocator` with the same scope type and an appropriate scope instance.

```kotlin
// Before (Application.onCreate or main)
MainServiceLocator.register(SimpleServiceLocator(ProductionScope), /* ... */)

// After (Application.onCreate or main)
MainServiceLocator.register(ScopedServiceLocator(ProductionScope), /* ... */)
```

### Step 3: Update Service Registrations (`put` calls)

For services that were registered lazily using `put<MyService> { MyServiceImpl() }` in
the `Simple` flavor, the code syntax remains the same when migrating to the `Full` flavor.
The primary change is that you are now calling the `put` extension function defined for
`ScopedServiceLocator` instead of the one defined for `SimpleServiceLocator`.

To ensure you are using the correct extension function, you may need to add an import for the `put`
extension function from the `dev.keyboardr.mapsl.keys` package.

```kotlin
// Add this import at the top of your registration file if needed
// import dev.keyboardr.mapsl.keys.put

// Inside your registration lambda (syntax is the same as before)
MainServiceLocator.register(ScopedServiceLocator(ProductionScope), /* ... */) { // Updated locator type
  put<MyService> { MyServiceImpl() }
  // ... other registrations
}
```

### Step 4: Update Test ServiceLocators

If you were using `SimpleTestingServiceLocator`, you will likely need to switch to
`TestingServiceLocator` from the `scoped-testing` module. Update your test setup to create and
register this new testing locator, ensuring it uses the same scope type.

```kotlin
// Before (using simple testing)
object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {
  // ... createMock implementation
}

// After (using scoped testing)
object TestServiceLocator :
  TestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {
  // ... createMock implementation
}
```

Update test registrations to use the implicit `ClassKey` extensions on the `TestingServiceLocator`.

```kotlin
// Add this import at the top of the file if needed
// import dev.keyboardr.mapsl.keys.put

// Before (test setup - implicit ClassKey)
TestServiceLocator.register(/* ... */) {
  put<MyService>(FakeMyService())
}

// After (test setup - using implicit ClassKey extensions)
TestServiceLocator.register(/* ... */) {
  put<MyService>(FakeMyService())
}
```

### Step 5: Update Service Retrieval (`get` and `getOrProvide` calls)

Modify all places where you retrieve services to use the implicit `ClassKey` extensions provided by
`ScopedServiceLocator`. The body of the code should remain the same, but you may need to add imports
for the `get` or `getOrProvide` extension functions.

```kotlin
// Add this import at the top of your registration file if needed
//import dev.keyboardr.mapsl.get
//import dev.keyboardr.mapsl.getOrProvide

class MyService {
  companion object {
    val instance = MainServiceLocator.instance.get<MyService>()
  }
}

class AnotherService {
  companion object {
    val instance =
      MainServiceLocator.instance.getOrProvide<AnotherService> { AnotherServiceImpl() }
  }
}
```

If your services use a property delegate, you may only need to add the import there.

### Step 6: Clean Up `simple` Module Dependencies

Once you have successfully migrated all your service registrations and retrievals to use the `Full`
flavor modules (`core`, `scoped`, etc.), you can remove the `simple` and `simple-testing`
dependencies from your build configuration.

### Step 7: Refactor and Refine

With the core migration complete, take time to refactor your code. At this point, all your services
are likely still using implicit lazy `ClassKey` behavior. Review your test setup and make sure mocks
and fakes are being provided correctly via the `TestingServiceLocator`.

## Using Different Key Types in the Full Flavor

Once you have migrated to using `ScopedServiceLocator`, you gain the ability to use different `Key`
kinds beyond the implicit `Lazy ClassKey` and `Singleton ClassKey`. This is necessary when you need
specific behaviors like eager initialization, factory patterns, or lifecycle management. For more
detail on the key kinds provided by MapSL (and how to build your own),
see [Key Kinds](keys.md "null").

To use a different key type, you must:

1. **Define an Explicit Key:** Create a `val` for an instance of the desired `Key` kind (e.g.,
   `SingletonKey`, `FactoryKey`, `LifecycleKey`). It's recommended to place this key alongside the
   service interface or implementation, often in a companion object.

   ```kotlin
   // Services that need Eager Initialization (require explicit SingletonKey)
   class AnotherService {
     companion object {
       val Key = SingletonKey<AnotherService>()
     }
   }
   
   // Services that need Factory behavior (require explicit FactoryKey)
   class CreatedItem(val params: ItemParams) {
     companion object {
       val Key = FactoryKey<CreatedItem, ItemParams>()
     }
   }
   
   // Services tied to Lifecycles (require explicit LifecycleKey)
   class LifecycleScopedService {
     companion object {
       val Key = LifecycleKey<LifecycleScopedService>()
     }
   }
   
   // You can also define an explicit LazyKey for clarity, even if implicit is possible
   class MyService {
     companion object {
       val Key = LazyKey<MyService>()
     }
   }
   ```

2. **Register Using the Explicit Key:** When registering the service, use the `put` function that
   takes a `Key` instance as the first parameter. Provide the appropriate `PutParams` for that key
   type.

   ```kotlin
   MainServiceLocator.register(ScopedServiceLocator(ProductionScope), /* ... */) {
     // Using explicit LazyKey
     put(MyService.Key) { MyService() }
   
     // Using explicit SingletonKey
     put(AnotherService.Key, AnotherService())
   
     // Using explicit FactoryKey
     put(CreatedItem.Key) { params -> CreatedItem(params) }
   
     // Using explicit LifecycleKey
     put(LifecycleScopedService.Key) { LifecycleScopedService() }
   }
   ```

3. **Retrieve Using the Explicit Key:** When retrieving the service, use the `get` or `getOrProvide`
   function that takes a `Key` instance as the first parameter. Provide the appropriate `GetParams`
   for that key type (often `Unit`, but `FactoryKey` can require other params and `LifecycleKey`
   requires a `LifecycleOwner`).

   ```kotlin
   // Retrieving with explicit LazyKey
   val myService: MyService = MainServiceLocator.instance.get(MyService.Key)
   
   // Retrieving with explicit SingletonKey
   val anotherService: AnotherService = MainServiceLocator.instance.get(AnotherService.Key)
   
   // Retrieving with explicit FactoryKey
   val createdItem: CreatedItem = MainServiceLocator.instance.get(CreatedItem.Key, ItemParams()) // Pass ItemParams
   
   // Retrieving with explicit LifecycleKey
   val lifecycleService: LifecycleScopedService = MainServiceLocator.instance.get(LifecycleScopedService.Key, lifecycleOwner) // Pass LifecycleOwner
   ```

By following these steps, you can selectively introduce different key types for services that
require specific behaviors, while keeping the majority of your services using the convenient
implicit `ClassKey` extensions if desired.

## Conclusion

Migrating from the `Simple` flavor to the `Full` flavor is primarily about switching the underlying
implementation to unlock access to the full range of `Key` types and features. The most common
migration path involves minimal changes by continuing to use implicit `ClassKey` extensions. Using
explicit keys is an advanced step necessary for specific behaviors like eager initialization,
factory patterns, or lifecycle management, providing greater control over your dependency
management.