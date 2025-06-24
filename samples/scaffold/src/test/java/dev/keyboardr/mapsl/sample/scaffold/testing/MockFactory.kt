package dev.keyboardr.mapsl.sample.scaffold.testing

import dev.keyboardr.mapsl.simple.scaffold.MockFactory
import org.mockito.Mockito.mock
import kotlin.reflect.KClass

object MockFactory : MockFactory {
  override fun <T : Any> createMock(clazz: KClass<T>): T = mock<T>(clazz.java)
}