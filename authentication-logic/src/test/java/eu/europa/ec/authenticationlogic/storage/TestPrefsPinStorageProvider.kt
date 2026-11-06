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

package eu.europa.ec.authenticationlogic.storage

import eu.europa.ec.authenticationlogic.secure.SecurePinImpl
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verifyNoInteractions

class TestPrefsPinStorageProvider {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var prefsController: PrefsController

    private lateinit var provider: PrefsPinStorageProvider

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        provider = PrefsPinStorageProvider(prefsController = prefsController)
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given an empty PIN, When it is stored, Then it is refused, cleared and nothing is written`() =
        coroutineRule.runTest {
            val pin = SecurePinImpl("")

            val error = runCatching { provider.setPin(pin) }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertTrue(pin.isCleared)
            verifyNoInteractions(prefsController)
        }
}