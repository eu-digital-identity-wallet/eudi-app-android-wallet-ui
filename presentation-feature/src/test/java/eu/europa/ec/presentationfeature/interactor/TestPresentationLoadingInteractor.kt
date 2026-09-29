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

package eu.europa.ec.presentationfeature.interactor

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
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.asFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI

class TestPresentationLoadingInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCorePresentationController: WalletCorePresentationController

    @Mock
    private lateinit var deviceAuthenticationInteractor: DeviceAuthenticationInteractor

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var resultHandler: DeviceAuthenticationResult

    private lateinit var interactor: PresentationLoadingInteractor

    private lateinit var closeable: AutoCloseable

    private lateinit var crypto: BiometricCrypto

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = PresentationLoadingInteractorImpl(
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

    // Case 1:
    // 1. walletCorePresentationController.events emits:
    // WalletCorePartialState.Failed, with an error message.

    // Case 1 Expected Result:
    // PresentationLoadingObserveResponsePartialState.Failed state, with the same error message.

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
                    TestCase.assertEquals(
                        PresentationLoadingObserveResponsePartialState.Failure(
                            error = mockedPlainFailureMessage
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 2:
    // 1. walletCorePresentationController.events emits:
    // WalletCorePartialState.Success.

    // Case 2 Expected Result:
    // PresentationLoadingObserveResponsePartialState.Success state.

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
                    // Then
                    TestCase.assertEquals(
                        PresentationLoadingObserveResponsePartialState.Success,
                        awaitItem()
                    )
                }
        }
    }

    // Case 3:
    // 1. walletCorePresentationController.events emits:
    // WalletCorePartialState.Redirect with a URI.

    // Case 3 Expected Result:
    // PresentationLoadingObserveResponsePartialState.Redirect with the same URI.

    @Test
    fun `Given Case 3, When observeResponse is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.Redirect(uri = URI("uri"))
            )

            // When
            interactor.observeResponse()
                .runFlowTest {
                    // Then
                    TestCase.assertEquals(
                        PresentationLoadingObserveResponsePartialState.Redirect(URI("uri")),
                        awaitItem()
                    )
                }
        }
    }

    // Case 4:
    // 1. walletCorePresentationController.events emits:
    // WalletCorePartialState.UserAuthenticationRequired.

    // Case 4 Expected Result:
    // PresentationLoadingObserveResponsePartialState.UserAuthenticationRequired.

    @Test
    fun `Given Case 4, When observeResponse is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
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
                    // Then
                    TestCase.assertEquals(
                        PresentationLoadingObserveResponsePartialState.UserAuthenticationRequired(
                            authenticationData = mockedAuthenticationData
                        ),
                        awaitItem()
                    )
                }
        }
    }

    //endregion

    //region observeResponse rejection

    // Case 1:
    // 1. The response is rejected with a redirect URI.
    // Case 1 Expected Result:
    // The URI is preserved without completing observation or stopping the presentation.
    @Test
    fun `Given rejection with a redirect, When observeResponse is called, Then rejection preserves the URI`() {
        coroutineRule.runTest {
            // Given
            val mockedRedirectUri = URI(mockedUriPath1)
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.Rejected(redirectUri = mockedRedirectUri)
            )

            // When
            interactor.observeResponse().runFlowTest {
                // Then
                assertEquals(
                    PresentationLoadingObserveResponsePartialState.Rejected(mockedRedirectUri),
                    awaitItem(),
                )
                verify(walletCorePresentationController, never()).stopPresentation()
                expectNoEvents()
            }
        }
    }

    // Case 2:
    // 1. The response is rejected without a redirect URI.
    // Case 2 Expected Result:
    // Rejection retains the absent URI without completing observation or stopping the presentation.
    @Test
    fun `Given Case 2 without a redirect, When observeResponse is called, Then rejection is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.Rejected(redirectUri = null)
            )

            // When
            interactor.observeResponse().runFlowTest {
                // Then
                assertEquals(
                    PresentationLoadingObserveResponsePartialState.Rejected(redirectUri = null),
                    awaitItem(),
                )
                verify(walletCorePresentationController, never()).stopPresentation()
                expectNoEvents()
            }
        }
    }

    // Case 3:
    // 1. Rejection is followed by success, readiness to send and another rejection.
    // Case 3 Expected Result:
    // All events are mapped in order, without sending documents or stopping the presentation.
    @Test
    fun `Given rejection followed by more events, When observeResponse is called, Then all events are mapped in order`() {
        coroutineRule.runTest {
            // Given
            val mockedRejection = WalletCorePartialState.Rejected(redirectUri = null)
            mockWalletCorePresentationControllerEventEmissions(
                events = listOf(
                    mockedRejection,
                    WalletCorePartialState.Success,
                    WalletCorePartialState.RequestIsReadyToBeSent,
                    mockedRejection,
                )
            )

            // When
            interactor.observeResponse().runFlowTest {
                // Then
                assertEquals(
                    PresentationLoadingObserveResponsePartialState.Rejected(redirectUri = null),
                    awaitItem(),
                )
                assertEquals(PresentationLoadingObserveResponsePartialState.Success, awaitItem())
                assertEquals(
                    PresentationLoadingObserveResponsePartialState.RequestReadyToBeSent,
                    awaitItem(),
                )
                assertEquals(
                    PresentationLoadingObserveResponsePartialState.Rejected(redirectUri = null),
                    awaitItem(),
                )
                awaitComplete()
                verify(walletCorePresentationController, never()).stopPresentation()
                verify(walletCorePresentationController, never()).sendRequestedDocuments()
            }
        }
    }

    //endregion

    //region initiatorRoute

    // Case 1:
    // 1. The presentation belongs to an existing initiating screen.
    // Case 1 Expected Result:
    // That screen's route remains available for the redirect exit.
    @Test
    fun `Given an initiating route, When initiatorRoute is read, Then the recorded route is returned`() {
        // Given
        val mockedInitiatorRoute = "initiator-route"
        whenever(walletCorePresentationController.initiatorRoute).thenReturn(mockedInitiatorRoute)

        // When
        val result = interactor.initiatorRoute

        // Then
        assertEquals(mockedInitiatorRoute, result)
    }

    //endregion

    //region handleUserAuthentication

    // Case 1:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.CanAuthenticate

    // Case 1 Expected Result:
    // deviceAuthenticationInteractor.authenticateWithBiometrics called once.
    @Test
    fun `Given case 1, When handleUserAuthentication is called, Then Case 1 expected result is returned`() {
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
    // End the pending request before opening enrollment so the user can retry afterwards.
    @Test
    fun `Given case 2, When handleUserAuthentication is called, Then Case 2 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.NonEnrolled
        )
        val onError = mock<() -> Unit>()
        val resultHandler = DeviceAuthenticationResult(onAuthenticationError = onError)

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        inOrder(onError, deviceAuthenticationInteractor) {
            verify(onError).invoke()
            verify(deviceAuthenticationInteractor).launchBiometricSystemScreen(crypto)
            verifyNoMoreInteractions()
        }
    }

    // Case 3:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.Failure

    // Case 3 Expected Result:
    // resultHandler.onAuthenticationError called once, even when the scan-failure callback is omitted.
    @Test
    fun `Given case 3, When handleUserAuthentication is called, Then Case 3 expected result is returned`() {
        // Given
        val onError = mock<() -> Unit>()
        val resultHandler = DeviceAuthenticationResult(
            onAuthenticationError = onError
        )

        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.Failure(
                errorMessage = mockedPlainFailureMessage
            )
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(onError).invoke()
    }
    //endregion

    // Case 5:
    // walletCorePresentationController.events emits:
    // WalletCorePartialState.RequestIsReadyToBeSent.

    // Case 5 Expected Result:
    // PresentationLoadingObserveResponsePartialState.RequestReadyToBeSent.
    @Test
    fun `Given Case 5, When observeResponse is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.RequestIsReadyToBeSent
            )

            // When
            interactor.observeResponse()
                .runFlowTest {
                    // Then
                    TestCase.assertEquals(
                        PresentationLoadingObserveResponsePartialState.RequestReadyToBeSent,
                        awaitItem()
                    )
                }
        }
    }

    // Case 6:
    // walletCorePresentationController.events emits:
    // WalletCorePartialState.IntentToSend with an Intent.

    // Case 6 Expected Result:
    // PresentationLoadingObserveResponsePartialState.IntentToSend with the same Intent.
    @Test
    fun `Given Case 6, When observeResponse is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val intent = mock<android.content.Intent>()
            mockWalletCorePresentationControllerEventEmission(
                event = WalletCorePartialState.IntentToSend(intent = intent)
            )

            // When
            interactor.observeResponse()
                .runFlowTest {
                    // Then
                    TestCase.assertEquals(
                        PresentationLoadingObserveResponsePartialState.IntentToSend(intent = intent),
                        awaitItem()
                    )
                }
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
                PresentationLoadingSendRequestedDocumentPartialState.Success,
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
                PresentationLoadingSendRequestedDocumentPartialState.Failure(
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
        val newInteractor = PresentationLoadingInteractorImpl(
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

    private fun mockWalletCorePresentationControllerEventEmissions(events: List<WalletCorePartialState>) {
        whenever(walletCorePresentationController.observeSentDocumentsRequest())
            .thenReturn(events.asFlow())
    }

    private fun mockBiometricsAvailabilityResponse(response: BiometricsAvailability) {
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(crypto)).thenReturn(
            response
        )
    }
    //endregion
}