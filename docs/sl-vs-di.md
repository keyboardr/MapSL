# Service Locator vs Dependency Injection

MapSL is designed as a **Service Locator**, an architectural pattern that provides a central
registry of services available to an application. While Service Locators and Dependency Injection (
DI) frameworks both address the problem of managing dependencies, they do so through different
mechanisms.

DI frameworks typically invert control, injecting dependencies into components rather
than the component explicitly requesting them. This often involves complex setup using annotations,
code generation, or extensive configuration. MapSL, on the other hand, allows components to
explicitly request their dependencies from the service locator. This approach simplifies the setup
process, makes the flow of dependencies more explicit within the code, and avoids the need for
external tools like annotation processors, leading to a faster build times and a shallower learning
curve compared to many DI solutions.

MapSL aims to strike a balance, offering the ease of use and explicitness of a Service Locator while
still providing the benefits of centralized dependency management often associated with DI.

## Main drawbacks compared to DI (and how MapSL addresses them)

### Dependency visibility

A class's dependencies are not immediately obvious from its constructor. This is intrinsic to any
Service Locator approach, and MapSL doesn't fundamentally alter this. However, there are other
places where a class's dependencies are listed (e.g imports or module dependencies), and these are
often sufficient for identifying what a class depends on. More commonly, however, it is important
for consumers of a class to know how to obtain an instance. The patterns recommended by MapSL make
this clear and explicit.

If a clear list of dependencies is still desired, your team can establish a best practice of
only accessing the `instance` properties from the default arguments of a constructor. This
would provide some of the same benefits as constructor-based dependency injection without
exposing transitive dependencies.

For example:

```kotlin
class MyViewModel(
  // Dependencies are explicitly listed in the constructor signature
  private val userRepository: UserRepository = UserRepository.instance,
  private val settingsRepository: SettingsRepository = SettingsRepository.instance
) {
  fun loadUserSettings() {
    val user = userRepository.getCurrentUser()
    val settings = settingsRepository.getSettingsFor(user)
    // ...
  }
}
```

This pattern makes the class's dependencies immediately visible from its signature, improving
clarity while still leveraging the Service Locator for retrieving the actual instances. Creating an
instance for testing is also straightforward:

```kotlin
// In a test class, after registering the TestServiceLocator
@Test
fun testLoadUserSettings_withFakeUserRepo() {
  // Create a specific fake for the user repository to control its behavior
  val fakeUserRepo = FakeUserRepository()

  // Instantiate the ViewModel, providing the fake.
  // The settingsRepository will continue to use its default argument. In a test environment, 
  // SettingsRepository.instance will retrieve a mock from the TestServiceLocator.
  val viewModel = MyViewModel(userRepository = fakeUserRepo)

  // Call the function under test
  viewModel.loadUserSettings()

  // Verify interactions with the fake and the mock
  assertTrue(fakeUserRepo.getCurrentUserWasCalled)
  verify(SettingsRepository.instance).getSettingsFor(any())
}
```

### Global state and testability

Service Locators often make testing difficult by relying on global state. MapSL addresses this by
allowing the top-level `ServiceLocator` to be swapped out for a test instance that is already
configured with your project's common fakes, and provides mocks when no fake has been registered.
Additionally, smaller scoped `ServiceLocators` may be passed in to classes as needed, resulting in a
hybrid approach (hierarchical `ServiceLocators` are possible, which would help here. These would
behave similarly to Context objects. See [Future Plans](../README.md#future-plans)).

### Runtime safety

Service Locators sometimes suffer from a lack of runtime safety due to separation between the
declaration and the resolution of dependencies. The **late registration** pattern (via
`getOrProvide()`) significantly reduces this risk. It is still possible to fail resolution with
**Pre-registration**, but the best practice of coupling the `put()` and the `get()` functions in the
same commit mitigates this.

Circular dependencies can still be a potential issue with any lazy-initialization pattern, but these
are rare in practice (and are mitigated in build systems that prefer tightly scoped modules, e.g.
Bazel).