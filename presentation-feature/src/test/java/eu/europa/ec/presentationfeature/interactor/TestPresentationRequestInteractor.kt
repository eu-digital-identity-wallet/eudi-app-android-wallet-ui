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

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.config.toDomainConfig
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.corelogic.controller.TransferEventPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.model.PresentationCombinationDomain
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockTransformToUiItemsStrings
import eu.europa.ec.testfeature.util.getMockedMdlWithBasicFields
import eu.europa.ec.testfeature.util.getMockedPidWithBasicFields
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedNonSelectableClaims
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testfeature.util.mockedSelectableClaims
import eu.europa.ec.testfeature.util.mockedValidMdlWithBasicFieldsRequestMatch
import eu.europa.ec.testfeature.util.mockedValidPidWithBasicFieldsRequestMatch
import eu.europa.ec.testfeature.util.mockedVerifierIsTrusted
import eu.europa.ec.testfeature.util.mockedVerifierName
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
                    combinationsDomain = emptyList(),
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
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(mockedValidMdlWithBasicFieldsRequestMatch)
                        )
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
    // PresentationRequestInteractorPartialState.Success state, with:
    // 1. a list with the transformed basic fields to RequestDocumentsUi items,
    // 2. the same not null String for verifier name,
    // 3. true for verifierIsTrusted.
    @Test
    fun `Given Case 5, When getRequestDocuments is called, Then Case 5 expected result is returned`() =
        coroutineRule.runTest {
            // Given

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            mockGetAllIssuedDocumentsCall(
                response = listOf(
                    mockedPidWithBasicFields,
                    mockedMdlWithBasicFields
                )
            )
            mockIsDocumentRevoked(isRevoked = false)
            mockTransformToUiItemsStrings(
                resourceProvider = resourceProvider,
            )
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(
                                mockedValidPidWithBasicFieldsRequestMatch,
                                mockedValidMdlWithBasicFieldsRequestMatch
                            )
                        )
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
                        requestMatchesDomain = listOf(
                            mockedValidPidWithBasicFieldsRequestMatch,
                            mockedValidMdlWithBasicFieldsRequestMatch
                        ),
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )

                    val expectedResult = PresentationRequestInteractorPartialState.Success(
                        verifierName = mockedVerifierName,
                        verifierIsTrusted = mockedVerifierIsTrusted,
                        combinationsUi = listOf(
                            RequestCombinationUi(
                                documents = RequestTransformer.transformToUiItems(
                                    documentsDomain = requestDataUi.getOrThrow(),
                                    resourceProvider = resourceProvider,
                                    claimsAreSelectable = mockedSelectableClaims,
                                ),
                                matches = listOf(
                                    mockedValidPidWithBasicFieldsRequestMatch,
                                    mockedValidMdlWithBasicFieldsRequestMatch,
                                ),
                            )
                        ),
                        claimsAreSelectable = mockedSelectableClaims,
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
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(mockedValidMdlWithBasicFieldsRequestMatch)
                        )
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

    // Case 9:
    // RequestReceived with non-empty combinations, but every issued document is revoked, so all
    // combinations end up empty and the interactor falls through to NoData.

    // Case 9 Expected Result:
    // PresentationRequestInteractorPartialState.NoData with the verifier name/isTrusted.
    @Test
    fun `Given Case 9, When getRequestDocuments is called and all docs are revoked, Then NoData is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetAllIssuedDocumentsCall(response = listOf(mockedPidWithBasicFields))
            mockIsDocumentRevoked(isRevoked = true)
            mockTransformToUiItemsStrings(resourceProvider = resourceProvider)
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(mockedValidPidWithBasicFieldsRequestMatch)
                        )
                    ),
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                // Then
                assertEquals(
                    PresentationRequestInteractorPartialState.NoData(
                        verifierName = mockedVerifierName,
                        verifierIsTrusted = mockedVerifierIsTrusted,
                    ),
                    awaitItem()
                )
            }
        }

    // Case 10:
    // A Disconnected event (emitted via a shared flow) maps to Disconnect.
    @Test
    fun `Given Case 10, When getRequestDocuments emits Disconnected via shareIn, Then Disconnect is mapped`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.events).thenReturn(
                kotlinx.coroutines.flow.flow {
                    emit(TransferEventPartialState.Disconnected)
                }.shareIn(coroutineRule.testScope, SharingStarted.Lazily, 1)
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                // Then
                assertEquals(PresentationRequestInteractorPartialState.Disconnect, awaitItem())
            }
        }
    }

    // Case 11:
    // A not-handled event (Connecting) followed by Disconnected still maps to Disconnect.
    @Test
    fun `Given Case 11, When getRequestDocuments emits a not-handled event then Disconnected, Then Disconnect is mapped after else-null`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.events).thenReturn(
                kotlinx.coroutines.flow.flow {
                    emit(TransferEventPartialState.Connecting)
                    emit(TransferEventPartialState.Disconnected)
                }.shareIn(coroutineRule.testScope, SharingStarted.Lazily, 2)
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                // Then
                assertEquals(PresentationRequestInteractorPartialState.Disconnect, awaitItem())
            }
        }
    }

    // Case 12:
    // RequestReceived with one combination holding two matches (PID + mDL); only the PID is
    // revoked, so the revoked match is dropped and the non-revoked one survives.

    // Case 12 Expected Result:
    // PresentationRequestInteractorPartialState.Success with a single combination whose only
    // item is the mDL.
    @Test
    fun `Given Case 12, When getRequestDocuments is called and one of two documents is revoked, Then only the non-revoked document survives`() =
        coroutineRule.runTest {
            // Given
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            mockGetAllIssuedDocumentsCall(
                response = listOf(
                    mockedPidWithBasicFields,
                    mockedMdlWithBasicFields
                )
            )
            mockIsDocumentRevoked(revokedIds = setOf(mockedPidWithBasicFields.id))
            mockTransformToUiItemsStrings(
                resourceProvider = resourceProvider,
            )
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(
                                mockedValidPidWithBasicFieldsRequestMatch,
                                mockedValidMdlWithBasicFieldsRequestMatch
                            )
                        )
                    ),
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )
            )

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    val survivingDomainItems = RequestTransformer.transformToDomainItems(
                        storageDocuments = listOf(
                            mockedPidWithBasicFields,
                            mockedMdlWithBasicFields
                        ),
                        requestMatchesDomain = listOf(mockedValidMdlWithBasicFieldsRequestMatch),
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )

                    val expectedResult = PresentationRequestInteractorPartialState.Success(
                        verifierName = mockedVerifierName,
                        verifierIsTrusted = mockedVerifierIsTrusted,
                        combinationsUi = listOf(
                            RequestCombinationUi(
                                documents = RequestTransformer.transformToUiItems(
                                    documentsDomain = survivingDomainItems.getOrThrow(),
                                    resourceProvider = resourceProvider,
                                    claimsAreSelectable = mockedSelectableClaims,
                                ),
                                matches = listOf(mockedValidMdlWithBasicFieldsRequestMatch),
                            )
                        ),
                        claimsAreSelectable = mockedSelectableClaims,
                    )
                    // Then
                    assertEquals(
                        expectedResult,
                        awaitItem()
                    )
                }
        }

    // Case 13:
    // 1. The interactor is configured as OpenID4VP via setConfig.
    // 2. walletCorePresentationController.events emits RequestReceived with a single PID.

    // Case 13 Expected Result:
    // PresentationRequestInteractorPartialState.Success with claimsAreSelectable = false and a
    // combination whose leaves are read-only, reflecting OpenID4VP's all-or-nothing disclosure.
    @Test
    fun `Given an OpenID4VP request, When getRequestDocuments is called, Then claims are not selectable`() =
        coroutineRule.runTest {
            // Given
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetAllIssuedDocumentsCall(response = listOf(mockedPidWithBasicFields))
            mockIsDocumentRevoked(isRevoked = false)
            mockTransformToUiItemsStrings(resourceProvider = resourceProvider)
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(mockedValidPidWithBasicFieldsRequestMatch)
                        )
                    ),
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted
                )
            )
            interactor.setConfig(
                config = RequestUriConfig(
                    PresentationMode.OpenId4Vp(
                        uri = mockedOpenId4VpUri,
                        initiatorRoute = mockedInitiatorRoute,
                    )
                ),
                intentAction = null,
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                val domainItems = RequestTransformer.transformToDomainItems(
                    storageDocuments = listOf(mockedPidWithBasicFields),
                    requestMatchesDomain = listOf(mockedValidPidWithBasicFieldsRequestMatch),
                    resourceProvider = resourceProvider,
                    uuidProvider = uuidProvider
                )

                val expectedResult = PresentationRequestInteractorPartialState.Success(
                    verifierName = mockedVerifierName,
                    verifierIsTrusted = mockedVerifierIsTrusted,
                    combinationsUi = listOf(
                        RequestCombinationUi(
                            documents = RequestTransformer.transformToUiItems(
                                documentsDomain = domainItems.getOrThrow(),
                                resourceProvider = resourceProvider,
                                claimsAreSelectable = mockedNonSelectableClaims,
                            ),
                            matches = listOf(mockedValidPidWithBasicFieldsRequestMatch),
                        )
                    ),
                    claimsAreSelectable = mockedNonSelectableClaims,
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Constructor default
    // the null-controller default: construction must not trigger the lazy Koin lookup
    @Test
    fun `When constructed without walletCorePresentationController, Then construction does not throw`() {
        val newInteractor = PresentationRequestInteractorImpl(
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            walletCoreDocumentsController = walletCoreDocumentsController,
        )

        assertEquals("DefaultPresentationScopeId", newInteractor.presentationScopeId)
    }
    //endregion

    //region updateRequestedDocuments

    // Case 1:
    // updateRequestedDocuments is called with no selected combination (null).

    // Case 1 Expected Result:
    // The controller's updateRequestedDocuments is called with an empty selection list.
    @Test
    fun `Given Case 1, When updateRequestedDocuments is called, Then Case 1 expected result is returned`() {

        interactor.updateRequestedDocuments(selectedCombination = null)

        verify(walletCorePresentationController, times(1))
            .updateRequestedDocuments(
                disclosedDocuments = emptyList()
            )
    }

    // Case 2:
    // updateRequestedDocuments is called with a selected combination (a single mDL match), with
    // the interactor configured as OpenID4VP (non-selectable).

    // Case 2 Expected Result:
    // The controller's updateRequestedDocuments is called with the selection built from that
    // combination's matches.
    @Test
    fun `Given Case 2, When updateRequestedDocuments is called with a combination, Then it discloses that combination's own document`() =
        coroutineRule.runTest {
            // Given
            interactor.setConfig(
                config = RequestUriConfig(
                    PresentationMode.OpenId4Vp(
                        uri = mockedOpenId4VpUri,
                        initiatorRoute = mockedInitiatorRoute,
                    )
                ),
                intentAction = null,
            )
            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            mockTransformToUiItemsStrings(resourceProvider = resourceProvider)
            val domainItems = RequestTransformer.transformToDomainItems(
                storageDocuments = listOf(mockedMdlWithBasicFields),
                requestMatchesDomain = listOf(mockedValidMdlWithBasicFieldsRequestMatch),
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
            ).getOrThrow()
            val selectedCombination = RequestCombinationUi(
                documents = RequestTransformer.transformToUiItems(
                    documentsDomain = domainItems,
                    resourceProvider = resourceProvider,
                    claimsAreSelectable = mockedNonSelectableClaims,
                ),
                matches = listOf(mockedValidMdlWithBasicFieldsRequestMatch),
            )

            // When
            interactor.updateRequestedDocuments(selectedCombination = selectedCombination)

            // Then
            val expectedSelections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = selectedCombination.documents,
                matchesDomain = selectedCombination.matches,
                claimsAreSelectable = mockedNonSelectableClaims,
            )
            verify(walletCorePresentationController, times(1))
                .updateRequestedDocuments(disclosedDocuments = expectedSelections)
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
        interactor.setConfig(config = requestConfig, intentAction = null)

        // Then
        verify(walletCorePresentationController, times(1))
            .setConfig(config = requestConfig.toDomainConfig(intentAction = null))

        assertEquals(interactor.presentationScopeId, "ble_presentation_scope_id")
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

    private suspend fun mockIsDocumentRevoked(revokedIds: Set<String>) {
        whenever(walletCoreDocumentsController.isDocumentRevoked(any())).thenAnswer {
            (it.arguments.first() as String) in revokedIds
        }
    }
    //endregion

    //region mocked objects
    private val mockedInitiatorRoute = "mockedInitiatorRoute"
    private val mockedOpenId4VpUri = "https://verifier.example/openid4vp"
    //endregion
}