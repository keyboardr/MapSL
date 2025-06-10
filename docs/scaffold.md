# Using the simple scaffold in MapSL

The `simple-scaffold` module reduces some of the boilerplate needed to use MapSL by providing a
basic `MainServiceLocator` implementation. It uses a simplified scoping model.

## Features

### Scopes

Two scopes are defined:

- `Production` - used when running the application
- `Testing` - used during tests.

### Property delegate

A `serviceLocator` property delegate function is defined for use with late registration. Services
may be defined as follows:

```kotlin
class MyService private constructor() {
  companion object {
    val instance by serviceLocator { MyService() }
  }
}
```

Services defined this way will lazily create a shared `instance` property. No other registration is
needed. If you are using a `SimpleTestingServiceLocator`, it will automatically return a mock unless
an instance has been registered for testing.

### TestServiceLocator

A `TestServiceLocator` class is included which can be used when registering during tests. You will
need to provide a `MockFactory` instance during registration if you wish to automatically create
mocks.

### Initialization (Android only)

On Android, the `MainServiceLocator` is automatically initialized during application startup. If you
would like to have more fine-tuned control over the startup sequence, or you would like to
pre-register services, add the following to the `application` section of your `AndroidManifest.xml`:

```xml

<provider android:authorities="${applicationId}.androidx-startup" android:exported="false"
    android:name="androidx.startup.InitializationProvider" tools:node="merge">
    <meta-data android:name="com.keyboardr.mapsl.simple.scaffold.ServiceLocatorInitializer"
        tools:node="remove" />
</provider>
```