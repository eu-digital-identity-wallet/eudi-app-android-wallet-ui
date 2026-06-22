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

package eu.europa.ec.proximityfeature.interactor

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.SendRequestedDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCorePartialState
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.model.AuthenticationData
import eu.europa.ec.testfeature.util.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testfeature.util.mockedUriPath1
import eu.europa.ec.testlogic.extension.expectNoEvents
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI

class TestProximityLoadingInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCorePresentationController: WalletCorePresentationController

    @Mock
    private lateinit var deviceAuthenticationInteractor: DeviceAuthenticationInteractor

    @Mock
    private lateinit var resultHandler: DeviceAuthenticationResult

    @Mock
    private lateinit var context: Context

    private lateinit var interactor: ProximityLoadingInteractor

    private lateinit var crypto: BiometricCrypto

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = ProximityLoadingInteractorImpl(
            walletCorePresentationController = walletCorePresentationController,
            deviceAuthenticationInteractor = deviceAuthenticationInteractor
        )

        crypto = BiometricCrypto(cryptoObject = null)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region observeResponse
    // Case 1
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.Failure, with an error message.

    // Case 1 Expected Result:
    // ProximityLoadingObserveResponsePartialState.Failure state, with the same error message.
    @Test
    fun `Given Case 1, When observeResponse is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.Failure(
                    error = mockedPlainFailureMessage
                )
            )

            // When
            interactor.observeResponse()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityLoadingObserveResponsePartialState.Failure(
                            error = mockedPlainFailureMessage
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 2
    // 1. walletCorePresentationController.events emits:
    // WalletCorePartialState.Success

    // Case 2 Expected Result:
    // ProximityLoadingObserveResponsePartialState.Success.
    @Test
    fun `Given Case 2, When observeResponse is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.Success
            )

            // When
            interactor.observeResponse()
                .runFlowTest {
                    val expectedResult = ProximityLoadingObserveResponsePartialState.Success
                    // Then
                    assertEquals(
                        expectedResult,
                        awaitItem()
                    )
                }
        }
    }

    // Case 3:
    // 1. walletCorePresentationController.events emits an event that we do not want to react to, i.e:
    // TransferEventPartialState.Redirect

    // Case 3 Expected Result is that no events are emitted
    @Test
    fun `Given Case 3, When observeResponse is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockEmissionOfIntentionallyNotHandledEvent()

            // When
            interactor.observeResponse()
                .expectNoEvents()
        }
    }

    // Case 4:
    // 1. walletCorePresentationController.events emits:
    // WalletCorePartialState.UserAuthenticationRequired.

    // Case 4 Expected Result:
    // ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired.
    @Test
    fun `Given Case 4, When observeResponse is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {

            val mockedAuthenticationData = listOf(
                AuthenticationData(
                    crypto = crypto,
                    onAuthenticationSuccess = {}
                )
            )

            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.UserAuthenticationRequired(
                    authenticationData = mockedAuthenticationData
                )
            )

            // When
            interactor.observeResponse()
                .runFlowTest {
                    val expectedResult =
                        ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired(
                            authenticationData = mockedAuthenticationData
                        )
                    assertEquals(expectedResult, awaitItem())
                }
        }
    }
    //endregion

    //region handleUserAuthentication
    //
    // Case 1:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.CanAuthenticate

    // Case 1 Expected Result:
    // deviceAuthenticationInteractor.authenticateWithBiometrics called once.
    @Test
    fun `Given case 1, When handleUserAuthentication is called, Then Case 1 Expected Result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.CanAuthenticate
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(
                context,
                crypto,
                mockedNotifyOnAuthenticationFailure,
                resultHandler
            )
    }

    // Case 2:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.NonEnrolled

    // Case 2 Expected Result:
    // deviceAuthenticationInteractor.launchBiometricSystemScreen called once.
    @Test
    fun `Given case 2, When handleUserAuthentication is called, Then Case 2 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.NonEnrolled
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .launchBiometricSystemScreen()
    }

    // Case 3:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.Failure with message

    // Case 3 Expected Result:
    // resultHandler.onAuthenticationFailure called once.
    @Test
    fun `Given case 3, When handleUserAuthentication is called, Then Case 3 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.Failure(
                errorMessage = mockedPlainFailureMessage
            )
        )

        val onFailure = mock<() -> Unit>()
        val resultHandler = DeviceAuthenticationResult(
            onAuthenticationFailure = onFailure
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(onFailure).invoke()
    }

    //endregion

    // Case 5:
    // walletCorePresentationController.events emits:
    // WalletCorePartialState.RequestIsReadyToBeSent.

    // Case 5 Expected Result:
    // ProximityLoadingObserveResponsePartialState.RequestReadyToBeSent.
    @Test
    fun `Given Case 5, When observeResponse is called, Then RequestReadyToBeSent is mapped`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.RequestIsReadyToBeSent
            )

            // When
            interactor.observeResponse()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityLoadingObserveResponsePartialState.RequestReadyToBeSent,
                        awaitItem()
                    )
                }
        }
    }

    // Case 6:
    // walletCorePresentationController.events emits WalletCorePartialState.IntentToSend.

    // Case 6 Expected Result:
    // No event is emitted.
    @Test
    fun `Given Case 6, When observeResponse is called with IntentToSend, Then no event is emitted`() {
        coroutineRule.runTest {
            // Given
            val intent = mock<android.content.Intent>()
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.IntentToSend(intent = intent)
            )

            // When
            interactor.observeResponse().expectNoEvents()
        }
    }

    //endregion

    //region sendRequestedDocuments

    @Test
    fun `Given controller#sendRequestedDocuments returns RequestSent, When sendRequestedDocuments is called, Then Success is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.sendRequestedDocuments())
                .thenReturn(SendRequestedDocumentsPartialState.RequestSent)

            // When
            val result = interactor.sendRequestedDocuments()

            // Then
            assertEquals(
                ProximityLoadingSendRequestedDocumentPartialState.Success,
                result
            )
        }
    }

    @Test
    fun `Given controller#sendRequestedDocuments returns Failure, When sendRequestedDocuments is called, Then Failure with the same error is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.sendRequestedDocuments())
                .thenReturn(SendRequestedDocumentsPartialState.Failure(error = mockedPlainFailureMessage))

            // When
            val result = interactor.sendRequestedDocuments()

            // Then
            assertEquals(
                ProximityLoadingSendRequestedDocumentPartialState.Failure(
                    error = mockedPlainFailureMessage
                ),
                result
            )
        }
    }
    //endregion

    //region constructor default
    @Test
    fun `When constructed without walletCorePresentationController, Then construction does not throw`() {
        // When
        val newInteractor = ProximityLoadingInteractorImpl(
            deviceAuthenticationInteractor = deviceAuthenticationInteractor,
        )

        // Then
        assertEquals("DefaultPresentationScopeId", newInteractor.presentationScopeId)
    }
    //endregion

    //region setScopeId
    @Test
    fun `Given a scopeId, When setScopeId is called, Then Verify presentationScopeId is set to the provided scopeId`() {
        // Given
        val mockScopeId = "mockScopeId"

        // When
        interactor.setScopeId(mockScopeId)

        // Then
        assertEquals(interactor.presentationScopeId, mockScopeId)
    }
    //endregion

    //region helper functions
    private fun mockWalletCorePresentationControllerEventEmission(event: WalletCorePartialState) {
        whenever(walletCorePresentationController.observeSentDocumentsRequest())
            .thenReturn(event.toFlow())
    }

    private fun mockBiometricsAvailabilityResponse(response: BiometricsAvailability) {
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability()).thenReturn(response)
    }

    private fun mockEmissionOfIntentionallyNotHandledEvent() {
        whenever(walletCorePresentationController.observeSentDocumentsRequest()).thenReturn(
            WalletCorePartialState.Redirect(uri = URI(mockedUriPath1)).toFlow()
        )
    }
    //endregion
}