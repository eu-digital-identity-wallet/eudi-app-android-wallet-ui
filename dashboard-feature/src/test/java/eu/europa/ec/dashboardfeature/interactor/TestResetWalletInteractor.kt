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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.corelogic.controller.ResetWalletPartialState
import eu.europa.ec.corelogic.controller.WalletCoreResetController
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class TestResetWalletInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreResetController: WalletCoreResetController

    @Mock
    private lateinit var sessionController: SessionController

    private lateinit var interactor: ResetWalletInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        interactor = ResetWalletInteractorImpl(walletCoreResetController, sessionController)
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given the session state, When isSessionUnlocked is called, Then it reports the session`() {
        whenever(sessionController.isUnlocked).thenReturn(true)
        assertTrue(interactor.isSessionUnlocked())

        whenever(sessionController.isUnlocked).thenReturn(false)
        assertFalse(interactor.isSessionUnlocked())
    }

    @Test
    fun `Given the reset succeeds, When resetWallet is called, Then Success is returned`() =
        coroutineRule.runTest {
            whenever(walletCoreResetController.resetWallet()).thenReturn(ResetWalletPartialState.Success)

            assertEquals(ResetWalletInteractorPartialState.Success, interactor.resetWallet())
        }

    @Test
    fun `Given the reset fails, When resetWallet is called, Then Failure carries the message`() =
        coroutineRule.runTest {
            whenever(walletCoreResetController.resetWallet())
                .thenReturn(ResetWalletPartialState.Failure(errorMessage = "error"))

            assertEquals(
                ResetWalletInteractorPartialState.Failure(errorMessage = "error"),
                interactor.resetWallet()
            )
        }
}