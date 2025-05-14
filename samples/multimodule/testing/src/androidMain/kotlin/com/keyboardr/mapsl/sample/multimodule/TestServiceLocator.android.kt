package com.keyboardr.mapsl.sample.multimodule

import org.mockito.Mockito.mock
import kotlin.reflect.KClass

actual fun <T : Any> mockForClass(clazz: KClass<T>): T = mock(clazz.java)