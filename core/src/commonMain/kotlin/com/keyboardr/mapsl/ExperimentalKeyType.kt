package com.keyboardr.mapsl

import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks a [ServiceKey][com.keyboardr.mapsl.keys.ServiceKey] type that is considered
 * experimental. Its API may be changed or removed in future releases.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(
  CLASS,
  ANNOTATION_CLASS,
  PROPERTY,
  FIELD,
  LOCAL_VARIABLE,
  VALUE_PARAMETER,
  CONSTRUCTOR,
  FUNCTION,
  PROPERTY_GETTER,
  PROPERTY_SETTER,
  TYPEALIAS
)
public annotation class ExperimentalKeyType
