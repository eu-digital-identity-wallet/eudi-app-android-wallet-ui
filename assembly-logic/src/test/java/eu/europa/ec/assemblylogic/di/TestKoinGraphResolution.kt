/*
 * Copyright (c) 2026 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.assemblylogic.di

import android.app.Application
import androidx.room.Room
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import eu.europa.ec.storagelogic.service.DatabaseService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.core.definition.BeanDefinition
import org.koin.core.definition.Kind
import org.koin.core.module.flatten
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

/**
 * Starts the graph the way the app does and runs every definition in it, which is what the Koin
 * compiler plugin used to check at build time. Two stores cannot run on the JVM: the database needs
 * SQLCipher's native library, and the preferences need the Android Keystore. Their definitions are
 * swapped for in-memory ones, and the wallet's own Application, whose onCreate reads both, is
 * replaced by a plain one.
 */
@OptIn(KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TestKoinGraphResolution {

    private lateinit var application: Application
    private lateinit var koin: Koin

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
        koin = application.setupKoin().koin
        loadKoinModules(
            module {
                single<DatabaseService> {
                    Room.inMemoryDatabaseBuilder(get(), DatabaseService::class.java).build()
                }
                single<PrefsController> { InMemoryPrefsController() }
            }
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `every definition in the wallet graph resolves`() {
        val definitions = flatten(eudiWalletModules)
            .flatMap { it.mappings.values }
            .map { it.beanDefinition }
            .distinct()

        val rootScope = koin.scopeRegistry.rootScope
        definitions.forEach { definition ->
            when {
                ListenableWorker::class in definition.secondaryTypes -> resolveWorker(definition)
                definition.scopeQualifier == rootScope.scopeQualifier -> resolveInRoot(definition)
                else -> resolveInScope(definition)
            }
        }

        assertEquals(
            "Injected parameter samples for types the graph does not define",
            emptySet<KClass<*>>(),
            injectedParameterSamples.keys - definitions.map { it.primaryType }.toSet(),
        )
    }

    private fun resolveInRoot(definition: BeanDefinition<*>) {
        val scope = koin.scopeRegistry.rootScope
        val instance = scope.get<Any>(definition.primaryType, definition.qualifier) {
            parametersOf(*injectedParameterSamples[definition.primaryType].orEmpty().toTypedArray())
        }
        assertIsInstance(definition.primaryType, instance)

        definition.secondaryTypes.forEach { type ->
            val bound = scope.get<Any>(type, definition.qualifier)
            assertIsInstance(type, bound)
            if (definition.kind == Kind.Singleton) {
                assertSame("$type should be the ${definition.primaryType} single", instance, bound)
            }
        }
    }

    private fun resolveInScope(definition: BeanDefinition<*>) {
        val scope = koin.createScope(
            scopeId = "test-${definition.primaryType.qualifiedName}",
            qualifier = definition.scopeQualifier,
        )
        try {
            val instance = scope.get<Any>(definition.primaryType, definition.qualifier)
            assertIsInstance(definition.primaryType, instance)
            assertSame(
                "${definition.primaryType} should be scoped",
                instance,
                scope.get<Any>(definition.primaryType, definition.qualifier),
            )
        } finally {
            scope.close()
        }
    }

    // WorkManager asks the factory for the worker's class name, so go through the factory to check
    // that the definition is found under that name.
    private fun resolveWorker(definition: BeanDefinition<*>) {
        val worker = KoinWorkerFactory().createWorker(
            appContext = application,
            workerClassName = definition.primaryType.java.name,
            workerParameters = mock<WorkerParameters>(),
        )
        assertIsInstance(definition.primaryType, worker)
    }

    private fun assertIsInstance(type: KClass<*>, instance: Any?) {
        assertTrue("Expected a $type, got $instance", type.isInstance(instance))
    }

    private class InMemoryPrefsController : PrefsController {
        private val values = mutableMapOf<String, Any>()

        override suspend fun contains(key: String) = key in values
        override suspend fun clear(key: String) {
            values.remove(key)
        }

        override suspend fun clear() = values.clear()
        override suspend fun clearExcept(keys: Set<String>) {
            values.keys.retainAll(keys)
        }

        override suspend fun setString(key: String, value: String) = values.set(key, value)
        override suspend fun setLong(key: String, value: Long) = values.set(key, value)
        override suspend fun setBool(key: String, value: Boolean) = values.set(key, value)
        override suspend fun setInt(key: String, value: Int) = values.set(key, value)
        override suspend fun getString(key: String, defaultValue: String) =
            values[key] as? String ?: defaultValue

        override suspend fun getLong(key: String, defaultValue: Long) =
            values[key] as? Long ?: defaultValue

        override suspend fun getBool(key: String, defaultValue: Boolean) =
            values[key] as? Boolean ?: defaultValue

        override suspend fun getInt(key: String, defaultValue: Int) =
            values[key] as? Int ?: defaultValue
    }
}
