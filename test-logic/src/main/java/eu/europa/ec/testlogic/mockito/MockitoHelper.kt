package eu.europa.ec.testlogic.mockito

import org.mockito.Mockito

object MockitoHelper {
    fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> uninitialized(): T = null as T
}