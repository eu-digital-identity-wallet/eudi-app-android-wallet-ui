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

import eu.europa.ec.businesslogic.provider.UuidProvider
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

class TestProximityRequestInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var walletCorePresentationController: WalletCorePresentationController

    @Mock
    private lateinit var uuidProvider: UuidProvider

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    private lateinit var interactor: ProximityRequestInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = ProximityRequestInteractorImpl(
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
    // 1. walletCorePresentationController.events emits an event that we do not want to react to, i.e:
    // TransferEventPartialState
    //  .Connected, or
    //  .Connecting, or
    //  .QrEngagementReady, or
    //  .Redirect, or
    //  .ResponseSent

    // Case 1 Expected Result:
    // No new partial state/flow emission.
    @Test
    fun `Given Case 1, When getRequestDocuments is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockEmissionOfIntentionallyNotHandledEvents()

            // When
            interactor.getRequestDocuments()
                // Then
                .expectNoEvents()
        }
    }

    // Case 2:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.Error, with an error message.

    // Case 2 Expected Result:
    // ProximityRequestInteractorPartialState.Failed state, with the same error message.
    @Test
    fun `Given Case 2, When getRequestDocuments is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.Error(
                    error = mockedPlainFailureMessage
                )
            )

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Failure(
                            error = mockedPlainFailureMessage
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 3:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.Disconnected.

    // Case 3 Expected Result:
    // ProximityRequestInteractorPartialState.Disconnect state.
    @Test
    fun `Given Case 3, When getRequestDocuments is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.Disconnected
            )

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Disconnect,
                        awaitItem()
                    )
                }
        }
    }

    // Case 4:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. an emptyList() for combinations,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted.

    // Case 4 Expected Result:
    // ProximityRequestInteractorPartialState.NoData state, with:
    // 1. the same not null String for verifier name,
    // 2. true for verifierIsTrusted.
    @Test
    fun `Given Case 4, When getRequestDocuments is called, Then Case 4 Expected Result is returned`() {
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
            interactor.getRequestDocuments()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.NoData(
                            verifierName = mockedVerifierName,
                            verifierIsTrusted = mockedVerifierIsTrusted
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 5:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. an emptyList() for combinations,
    // 2. a null String for verifier name,
    // 3. false for verifierIsTrusted.

    // Case 5 Expected Result:
    // ProximityRequestInteractorPartialState.NoData state, with:
    // 1. null String for verifier name,
    // 2. false for verifierIsTrusted.
    @Test
    fun `Given Case 5, When getRequestDocuments is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    combinationsDomain = emptyList(),
                    verifierName = null,
                    verifierIsTrusted = false
                )
            )

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.NoData(
                            verifierName = null,
                            verifierIsTrusted = false
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 6:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a combination with a PID match whose requestedClaims is empty,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted.

    // Case 6 Expected Result:
    // ProximityRequestInteractorPartialState.NoData state, with:
    // 1. the same not null String for verifier name,
    // 2. true for verifierIsTrusted.
    @Test
    fun `Given Case 6, When getRequestDocuments is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(
                                // no requested claims → the interactor emits NoData
                                mockedValidPidWithBasicFieldsRequestMatch.copy(
                                    requestedClaims = emptyList(),
                                )
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
                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.NoData(
                            verifierName = mockedVerifierName,
                            verifierIsTrusted = mockedVerifierIsTrusted
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 7:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of a PID RequestDocument, with some basic fields,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted.

    // Case 7 Expected Result:
    // ProximityRequestInteractorPartialState.Success state, with:
    // 1. a list with the transformed basic fields to RequestDataUi items,
    // 2. the same not null String for verifier name,
    // 3. true for verifierIsTrusted.
    @Test
    fun `Given Case 7, When getRequestDocuments is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetAllIssuedDocumentsCall(
                response = listOf(mockedPidWithBasicFields)
            )
            mockIsDocumentRevoked(isRevoked = false)
            mockTransformToUiItemsStrings(
                resourceProvider = resourceProvider,
            )

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
            interactor.getRequestDocuments()
                .runFlowTest {
                    val requestDataUi = RequestTransformer.transformToDomainItems(
                        storageDocuments = listOf(mockedPidWithBasicFields),
                        requestMatchesDomain = listOf(mockedValidPidWithBasicFieldsRequestMatch),
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )

                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Success(
                            verifierName = mockedVerifierName,
                            verifierIsTrusted = mockedVerifierIsTrusted,
                            combinationsUi = listOf(
                                RequestCombinationUi(
                                    documents = RequestTransformer.transformToUiItems(
                                        documentsDomain = requestDataUi.getOrThrow(),
                                        resourceProvider = resourceProvider,
                                        claimsAreSelectable = mockedSelectableClaims,
                                    ),
                                    matches = listOf(mockedValidPidWithBasicFieldsRequestMatch),
                                )
                            ),
                            claimsAreSelectable = mockedSelectableClaims,
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 8:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of an mDL RequestDocument, with some basic fields,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted.

    // Case 8 Expected Result:
    // ProximityRequestInteractorPartialState.Success state, with:
    // 1. a list with the transformed basic fields to RequestDataUi items,
    // 2. the same not null String for verifier name,
    // 3. true for verifierIsTrusted.
    @Test
    fun `Given Case 8, When getRequestDocuments is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            mockGetAllIssuedDocumentsCall(
                response = listOf(mockedMdlWithBasicFields)
            )
            mockTransformToUiItemsStrings(
                resourceProvider = resourceProvider,
            )
            mockIsDocumentRevoked(isRevoked = false)
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

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    val requestDataUi = RequestTransformer.transformToDomainItems(
                        storageDocuments = listOf(mockedMdlWithBasicFields),
                        requestMatchesDomain = listOf(mockedValidMdlWithBasicFieldsRequestMatch),
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )

                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Success(
                            verifierName = mockedVerifierName,
                            verifierIsTrusted = mockedVerifierIsTrusted,
                            combinationsUi = listOf(
                                RequestCombinationUi(
                                    documents = RequestTransformer.transformToUiItems(
                                        documentsDomain = requestDataUi.getOrThrow(),
                                        resourceProvider = resourceProvider,
                                        claimsAreSelectable = mockedSelectableClaims,
                                    ),
                                    matches = listOf(mockedValidMdlWithBasicFieldsRequestMatch),
                                )
                            ),
                            claimsAreSelectable = mockedSelectableClaims,
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 9:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of an mDL RequestDocument and a PID, both with some basic fields,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted.

    // Case 9 Expected Result:
    // ProximityRequestInteractorPartialState.Success state, with:
    // 1. a list with the transformed basic fields to RequestDataUi items, for both Documents
    // 2. the same not null String for verifier name,
    // 3. true for verifierIsTrusted.
    @Test
    fun `Given Case 9, When getRequestDocuments is called, Then Case 9 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetAllIssuedDocumentsCall(
                response = listOf(
                    mockedMdlWithBasicFields,
                    mockedPidWithBasicFields
                )
            )
            mockTransformToUiItemsStrings(
                resourceProvider = resourceProvider,
            )
            mockIsDocumentRevoked(isRevoked = false)
            mockWalletCorePresentationControllerEventEmission(
                event = TransferEventPartialState.RequestReceived(
                    combinationsDomain = listOf(
                        PresentationCombinationDomain(
                            matches = listOf(
                                mockedValidMdlWithBasicFieldsRequestMatch,
                                mockedValidPidWithBasicFieldsRequestMatch
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
                            mockedMdlWithBasicFields,
                            mockedPidWithBasicFields
                        ),
                        requestMatchesDomain = listOf(
                            mockedValidMdlWithBasicFieldsRequestMatch,
                            mockedValidPidWithBasicFieldsRequestMatch
                        ),
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )

                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Success(
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
                                        mockedValidMdlWithBasicFieldsRequestMatch,
                                        mockedValidPidWithBasicFieldsRequestMatch,
                                    ),
                                )
                            ),
                            claimsAreSelectable = mockedSelectableClaims,
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 10:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of a PID RequestDocument and an mDL, both with some basic fields,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted.

    // Case 10 Expected Result:
    // ProximityRequestInteractorPartialState.Success state, with:
    // 1. a list with the transformed basic fields to RequestDataUi items, for both Documents
    // 2. the same not null String for verifier name,
    // 3. true for verifierIsTrusted.
    @Test
    fun `Given Case 10, When getRequestDocuments is called, Then Case 10 Expected Result is returned`() {
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
            mockTransformToUiItemsStrings(
                resourceProvider = resourceProvider,
            )
            mockIsDocumentRevoked(isRevoked = false)
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

                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Success(
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
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 11:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of a PID RequestDocument, with some basic fields,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted,
    // 4. walletCoreDocumentsController.getAllIssuedDocuments() throws an exception with a message.

    // Case 11 Expected Result:
    // ProximityRequestInteractorPartialState.Failed state, with:
    // 1. exception's localized message.
    @Test
    fun `Given Case 11, When getRequestDocuments is called, Then Case 11 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
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
            whenever(walletCoreDocumentsController.getAllIssuedDocuments())
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Failure(
                            error = mockedExceptionWithMessage.localizedMessage!!
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 12:
    // 1. walletCorePresentationController.events emits:
    // TransferEventPartialState.RequestReceived, with:
    // 1. a list of a PID RequestDocument, with some basic fields,
    // 2. a not null String for verifier name,
    // 3. true for verifierIsTrusted,
    // 4. walletCoreDocumentsController.getAllIssuedDocuments() throws an exception with no message.

    // Case 12 Expected Result:
    // ProximityRequestInteractorPartialState.Failed state, with:
    // 1. the generic error message.
    @Test
    fun `Given Case 12, When getRequestDocuments is called, Then Case 12 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
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
            whenever(walletCoreDocumentsController.getAllIssuedDocuments())
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getRequestDocuments()
                .runFlowTest {
                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Failure(
                            error = mockedGenericErrorMessage
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Case 13:
    // RequestReceived with non-empty combinations, but every issued document is revoked, so all
    // combinations end up empty and the interactor falls through to NoData.
    @Test
    fun `Given Case 13, When getRequestDocuments is called and all docs are revoked, Then NoData is returned`() =
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
                    ProximityRequestInteractorPartialState.NoData(
                        verifierName = mockedVerifierName,
                        verifierIsTrusted = mockedVerifierIsTrusted,
                    ),
                    awaitItem()
                )
            }
        }

    // Case 14:
    // A Disconnected event (emitted via a shared flow) maps to Disconnect.
    @Test
    fun `Given Case 14, When getRequestDocuments emits Disconnected, Then Disconnect is mapped`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.events).thenReturn(
                flow {
                    emit(TransferEventPartialState.Disconnected)
                }.shareIn(coroutineRule.testScope, SharingStarted.Lazily, 1)
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                // Then
                assertEquals(ProximityRequestInteractorPartialState.Disconnect, awaitItem())
            }
        }
    }

    // Case 15:
    // A not-handled event (Connecting) followed by Disconnected still maps to Disconnect.
    @Test
    fun `Given Case 15, When getRequestDocuments emits a not-handled event then Disconnected, Then Disconnect is mapped after else-null`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCorePresentationController.events).thenReturn(
                flow {
                    emit(TransferEventPartialState.Connecting)
                    emit(TransferEventPartialState.Disconnected)
                }.shareIn(coroutineRule.testScope, SharingStarted.Lazily, 2)
            )

            // When
            interactor.getRequestDocuments().runFlowTest {
                // Then
                assertEquals(ProximityRequestInteractorPartialState.Disconnect, awaitItem())
            }
        }
    }

    // Case 16:
    // RequestReceived with one combination holding two matches (PID + mDL); only the PID is
    // revoked, so the revoked match is dropped and the non-revoked one survives.

    // Case 16 Expected Result:
    // ProximityRequestInteractorPartialState.Success with a single combination whose only
    // item is the mDL.
    @Test
    fun `Given Case 16, When getRequestDocuments is called and one of two documents is revoked, Then only the non-revoked document survives`() {
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

                    // Then
                    assertEquals(
                        ProximityRequestInteractorPartialState.Success(
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
                        ),
                        awaitItem()
                    )
                }
        }
    }

    // Constructor default
    // the null-controller default: construction must not trigger the lazy Koin lookup
    @Test
    fun `When constructed without walletCorePresentationController, Then construction does not throw`() {
        val newInteractor = ProximityRequestInteractorImpl(
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            walletCoreDocumentsController = walletCoreDocumentsController,
        )

        assertEquals("DefaultPresentationScopeId", newInteractor.presentationScopeId)
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
    // the interactor on its BLE (selectable) path.

    // Case 2 Expected Result:
    // The controller's updateRequestedDocuments is called with the selection built from that
    // combination's matches.
    @Test
    fun `Given Case 2, When updateRequestedDocuments is called with a combination, Then it discloses that combination's own document`() =
        coroutineRule.runTest {
            // Given
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
                    claimsAreSelectable = mockedSelectableClaims,
                ),
                matches = listOf(mockedValidMdlWithBasicFieldsRequestMatch),
            )

            // When
            interactor.updateRequestedDocuments(selectedCombination = selectedCombination)

            // Then
            val expectedSelections = RequestTransformer.createSelectionsDomain(
                documentItemsUi = selectedCombination.documents,
                matchesDomain = selectedCombination.matches,
                claimsAreSelectable = mockedSelectableClaims,
            )
            verify(walletCorePresentationController, times(1))
                .updateRequestedDocuments(disclosedDocuments = expectedSelections)
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
}