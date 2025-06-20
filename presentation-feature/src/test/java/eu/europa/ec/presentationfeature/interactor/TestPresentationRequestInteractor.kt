/*
 * Copyright (c) 2023 European Commission
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

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.config.toDomainConfig
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.commonfeature.util.TestsData.mockedRequestElementIdentifierNotAvailable
import eu.europa.ec.commonfeature.util.TestsData.mockedValidMdlWithBasicFieldsRequestDocument
import eu.europa.ec.commonfeature.util.TestsData.mockedValidPidWithBasicFieldsRequestDocument
import eu.europa.ec.commonfeature.util.TestsData.mockedVerifierName
import eu.europa.ec.corelogic.controller.TransferEventPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.MockResourceProviderForStringCalls.mockTransformToUiItemsCall
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedMdlWithBasicFields
import eu.europa.ec.testfeature.mockedPidWithBasicFields
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testfeature.mockedVerifierIsTrusted
import eu.europa.ec.testlogic.extension.expectNoEvents
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI

class TestPresentationRequestInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var walletCorePresentationController: WalletCorePresentationController

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var uuidProvider: UuidProvider

    private lateinit var interactor: PresentationRequestInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = PresentationRequestInteractorImpl(
            resourceProvider = resourceProvider,
            walletCorePresentationController = walletCorePresentationController,
            walletCoreDocumentsController = walletCoreDocumentsController,
            uuidProvider = uuidProvider
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getRequestDocuments

    // Case 1:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.Disconnected.

    // Case 1 Expected Result:
    // PresentationRequestInteractorPartialState.Disconnect
    @Test
    fun `Given Case 1, When getRequestDocuments is called, Then Case 1 expected result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.Disconnected
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                val expectedResult = PresentationRequestInteractorPartialState.Disconnect

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 2:
    // walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. an empty list of RequestDocument items
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted

    // Case 2 Expected Result:
    // PresentationRequestInteractorPartialState.NoData, with:
    // 1. the same not null String for verifier name,
    // 2. true for verifierIsTrusted.
    @Test
    fun `Given Case 2, When getRequestDocuments is called, Then Case 2 expected result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    requestData = emptyList(),
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                val expectedResult = PresentationRequestInteractorPartialState.NoData(
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 3:
    // walletCorePresentationController.events emits:
    // TransferEventPartialState.Error, with:
    // 1. an error message

    // Case 3 Expected Result:
    // PresentationRequestInteractorPartialState.Failure with the same error message
    @Test
    fun `Given Case 3, When getRequestDocuments is called, Then Case 3 expected result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.Error(
                    error = mockedPlainFailureMessage
                )
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                val expectedResult = PresentationRequestInteractorPartialState.Failure(
                    error = mockedPlainFailureMessage
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of RequestDocument items
    // 2. a verifier name (non-null)
    // 3. true for verifierIsTrusted
    // and 2. walletCoreDocumentsController getAllIssuedDocuments throws an exception

    // Case 4 Expected Result:
    // PresentationRequestInteractorPartialState.Failure with the generic error message
    @Test
    fun `Given Case 4, When getRequestDocuments is called, Then Case 4 expected result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    requestData = listOf(
                        mockedValidMdlWithBasicFieldsRequestDocument
                    ),
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )
            )
            whenever(walletCoreDocumentsController.getAllIssuedDocuments())
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getRequestDocuments().runFlowTest {
                val expectedResult =
                    PresentationRequestInteractorPartialState.Failure(error = mockedGenericErrorMessage)

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 5:
    // walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of a PID RequestDocument and a mDL RequestDocument
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted.

    // Case 5 Expected Result:
    // ProximityRequestInteractorPartialState.Success state, with:
    // 1. a list with the transformed basic fields to RequestDocumentsUi items,
    // 2. the same not null String for verifier name,
    // 3. true for verifierIsTrusted.
    @Test
    fun `Given Case 5, When getRequestDocuments is called, Then Case 5 expected result is returned`() =
        coroutineRule.runTest {
            // Given
            mockGetAllIssuedDocumentsCall(
                response = listOf(
                    mockedPidWithBasicFields,
                    mockedMdlWithBasicFields
                )
            )
            mockIsDocumentRevoked(isRevoked = false)
            mockTransformToUiItemsCall(
                resourceProvider = resourceProvider,
                notAvailableString = mockedRequestElementIdentifierNotAvailable
            )
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    requestData = listOf(
                        mockedValidPidWithBasicFieldsRequestDocument,
                        mockedValidMdlWithBasicFieldsRequestDocument
                    ),
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )
            )

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    val requestDataUi = RequestTransformer.transformToDomainItems(
                        storageDocuments = listOf(
                            mockedPidWithBasicFields,
                            mockedMdlWithBasicFields
                        ),
                        requestDocuments = listOf(
                            mockedValidPidWithBasicFieldsRequestDocument,
                            mockedValidMdlWithBasicFieldsRequestDocument
                        ),
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )

                    val expectedResult = PresentationRequestInteractorPartialState.Success(
                        verifierName = mockedVerifierName,
                        verifierIsTrusted = mockedVerifierIsTrusted,
                        requestDocuments = RequestTransformer.transformToUiItems(
                            documentsDomain = requestDataUi.getOrThrow(),
                            resourceProvider = resourceProvider,
                        )
                    )
                    // Then
                    assertEquals(
                        expectedResult,
                        awaitItem()
                    )
                }
        }

    // Case 6:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of RequestDocument items
    // 2. a verifier name (non-null)
    // 3. true for verifierIsTrusted
    // and 2. walletCoreDocumentsController getAllIssuedDocuments throws an exception with message

    // Case 6 Expected Result:
    // PresentationRequestInteractorPartialState.Failure with the same exception message
    @Test
    fun `Given Case 6, When getRequestDocuments is called, Then Case 6 expected result is returned`() =
        coroutineRule.runTest {
            // When
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    requestData = listOf(
                        mockedValidMdlWithBasicFieldsRequestDocument
                    ),
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )
            )
            whenever(walletCoreDocumentsController.getAllIssuedDocuments())
                .thenThrow(mockedExceptionWithMessage)

            interactor.getRequestDocuments().runFlowTest {
                val expectedResult = PresentationRequestInteractorPartialState.Failure(
                    error = mockedExceptionWithMessage.localizedMessage!!
                )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 7:
    // 1. walletCorePresentationController.events emits an event that we do not want to react to, i.e:
    // TransferEventPartialState
    //  .Connected, or
    //  .Connecting, or
    //  .QrEngagementReady, or
    //  .Redirect, or
    //  .ResponseSent

    // Case 7 Expected Result is that no events are emitted
    @Test
    fun `Given Case 7, When getRequestDocuments is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockEmissionOfIntentionallyNotHandledEvents()

            // When
            interactor.getRequestDocuments()
                // Then
                .expectNoEvents()
        }
    }

    // Case 8:
    // walletCorePresentationController.events emits:
    // TransferEventPartialState.Connecting

    // Case 8 Expected Result is that no events are emitted
    @Test
    fun `Given Case 8, When getRequestDocuments is called, Then Case 8 expected result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.Connecting
            )

            // When
            interactor.getRequestDocuments().expectNoEvents()
        }
    //endregion

    //region updateRequestedDocuments

    // Case 1:
    // updateRequestedDocuments is called with empty list of items

    // Case 1 Expected Result
    // updateRequestedDocuments is called with:
    // a DisclosedDocuments object with an empty list of documents
    @Test
    fun `Given Case 1, When updateRequestedDocuments is called, Then Case 1 expected result is returned`() {

        interactor.updateRequestedDocuments(items = emptyList())

        verify(walletCorePresentationController, times(1))
            .updateRequestedDocuments(
                disclosedDocuments = mutableListOf()
            )
    }
    //endregion

    //region setConfig
    // Case 1:
    // setConfig is called with a request configuration provided as parameter

    // Case 1 Expected Result
    // setConfig is called with a PresentationControllerConfig argument
    // derived from RequestUriConfig
    @Test
    fun `Given Case 1, When setConfig is called, Then Case 1 expected result is returned`() {
        // Given
        val requestConfig = RequestUriConfig(
            PresentationMode.Ble(initiatorRoute = mockedInitiatorRoute)
        )
        // When
        interactor.setConfig(config = requestConfig)

        // Then
        verify(walletCorePresentationController, times(1))
            .setConfig(config = requestConfig.toDomainConfig())
    }
    //endregion

    //region stopPresentation
    @Test
    fun `When stopPresentation on the interactor is called, Then stopPresentation should be executed on the controller`() {
        // When
        interactor.stopPresentation()

        // Then
        verify(walletCorePresentationController, times(1))
            .stopPresentation()
    }
    //endregion

    //region helper functions
    private fun mockWalletCorePresentationControllerEventEmission(event: TransferEventPartialState) {
        whenever(walletCorePresentationController.events)
            .thenReturn(event.toFlow())
    }

    private fun mockEmissionOfIntentionallyNotHandledEvents() {
        whenever(walletCorePresentationController.events)
            .thenReturn(
                flow {
                    emit(TransferEventPartialState.Connected)
                    emit(TransferEventPartialState.Connecting)
                    emit(TransferEventPartialState.QrEngagementReady(""))
                    emit(TransferEventPartialState.Redirect(uri = URI("")))
                    emit(TransferEventPartialState.ResponseSent)
                }.shareIn(coroutineRule.testScope, SharingStarted.Lazily, 2)
            )
    }

    private fun mockGetAllIssuedDocumentsCall(response: List<IssuedDocument>) {
        whenever(walletCoreDocumentsController.getAllIssuedDocuments())
            .thenReturn(response)
    }

    private suspend fun mockIsDocumentRevoked(isRevoked: Boolean) {
        whenever(walletCoreDocumentsController.isDocumentRevoked(any())).thenReturn(isRevoked)
    }
    //endregion

    //region mocked objects
    private val mockedInitiatorRoute = "mockedInitiatorRoute"
    //endregion
}