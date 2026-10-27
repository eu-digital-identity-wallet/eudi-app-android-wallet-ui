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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.eudi.wallet.dcapi.registration.DCAPIRegistration
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TestWalletCoreDCAPIRegistrationController {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val steps = mutableListOf<String>()

    @Test
    fun `Given an attached registration, When Wallet Core updates the registration, Then the update is passed on`() {
        coroutineRule.runTest {
            val controller = controller()
            controller.attach { registration() }

            controller.registerCredentials()

            assertEquals(listOf("register"), steps)
        }
    }

    @Test
    fun `Given the user turned the Digital Credential API off, When Wallet Core updates the registration, Then the registry is cleared instead`() {
        coroutineRule.runTest {
            val controller = controller(enabled = false)
            controller.attach { registration() }

            controller.registerCredentials()

            assertEquals(listOf("clear registry"), steps)
        }
    }

    @Test
    fun `Given the setting is off and the registry cannot be cleared, When Wallet Core updates the registration, Then the update does not fail`() {
        coroutineRule.runTest {
            val controller = WalletCoreDCAPIRegistrationControllerImpl(
                clearRegistry = { throw IllegalStateException("registry unavailable") },
                isEnabled = { false },
                getBlockedDocumentIds = { emptySet() },
            )
            controller.attach { registration() }

            controller.registerCredentials()
            controller.unregisterCredentials()

            assertEquals(emptyList<String>(), steps)
        }
    }

    @Test
    fun `Given the reset holds the registration, When Wallet Core updates it meanwhile, Then the update runs after the registry is cleared`() {
        coroutineRule.runTest {
            val controller = controller()
            controller.attach { registration() }
            val release = CompletableDeferred<Unit>()

            coroutineRule.testScope.launch {
                controller.clearAround {
                    steps.add("delete documents")
                    release.await()
                }
            }
            coroutineRule.testScope.runCurrent()
            coroutineRule.testScope.launch { controller.registerCredentials() }
            coroutineRule.testScope.runCurrent()
            release.complete(Unit)
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf("delete documents", "clear registry", "register"), steps)
        }
    }

    @Test
    fun `Given an update in progress, When the reset holds the registration, Then it waits for the update`() {
        coroutineRule.runTest {
            val controller = controller()
            val release = CompletableDeferred<Unit>()
            controller.attach { registration(release) }

            coroutineRule.testScope.launch { controller.registerCredentials() }
            coroutineRule.testScope.runCurrent()
            coroutineRule.testScope.launch { controller.clearAround { steps.add("delete documents") } }
            coroutineRule.testScope.runCurrent()
            release.complete(Unit)
            coroutineRule.testScope.runCurrent()

            assertEquals(
                listOf(
                    "register started",
                    "register",
                    "delete documents",
                    "clear registry"
                ), steps
            )
        }
    }

    @Test
    fun `Given the registry cannot be cleared, When the reset holds the registration, Then the result is still returned`() {
        coroutineRule.runTest {
            val controller = WalletCoreDCAPIRegistrationControllerImpl(
                clearRegistry = { throw IllegalStateException("registry unavailable") },
                isEnabled = { true },
                getBlockedDocumentIds = { emptySet() },
            )

            val result = controller.clearAround { true }

            assertEquals(true, result)
        }
    }

    @Test
    fun `Given no wallet has been built, When the reset holds the registration, Then the registry is still cleared`() {
        coroutineRule.runTest {
            controller().clearAround { steps.add("delete documents") }

            assertEquals(listOf("delete documents", "clear registry"), steps)
        }
    }

    @Test
    fun `Given blocked documents, When Wallet Core updates the registration, Then they are excluded from it`() {
        coroutineRule.runTest {
            var blocked = setOf("revoked")
            val controller = controller(getBlockedDocumentIds = { blocked })
            var excludedDuringUpdate = emptySet<String>()
            controller.attach {
                object : DCAPIRegistration {
                    override suspend fun registerCredentials() {
                        excludedDuringUpdate = controller.excludedDocumentIds
                        steps.add("register")
                    }

                    override suspend fun unregisterCredentials() = Unit
                }
            }

            controller.registerCredentials()
            assertEquals(setOf("revoked"), excludedDuringUpdate)

            blocked = emptySet()
            controller.registerCredentials()

            assertEquals(emptySet<String>(), excludedDuringUpdate)
            assertEquals(listOf("register", "register"), steps)
        }
    }

    @Test
    fun `Given the blocked documents cannot be read, When Wallet Core updates the registration, Then the registry is cleared instead`() {
        coroutineRule.runTest {
            val controller = controller(
                getBlockedDocumentIds = { throw IllegalStateException("unreadable") }
            )
            controller.attach { registration() }

            controller.registerCredentials()

            assertEquals(listOf("clear registry"), steps)
        }
    }

    private fun controller(
        enabled: Boolean = true,
        getBlockedDocumentIds: suspend () -> Set<String> = { emptySet() },
    ) = WalletCoreDCAPIRegistrationControllerImpl(
        clearRegistry = { steps.add("clear registry") },
        isEnabled = { enabled },
        getBlockedDocumentIds = getBlockedDocumentIds,
    )

    private fun registration(release: CompletableDeferred<Unit>? = null) =
        object : DCAPIRegistration {
            override suspend fun registerCredentials() {
                if (release != null) {
                    steps.add("register started")
                    release.await()
                }
                steps.add("register")
            }

            override suspend fun unregisterCredentials() {
                steps.add("unregister")
            }
        }
}