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

package eu.europa.ec.issuancefeature.interactor.document

import eu.europa.ec.businesslogic.controller.walletcore.AddSampleDataPartialState
import eu.europa.ec.businesslogic.controller.walletcore.IssuanceMethod
import eu.europa.ec.businesslogic.controller.walletcore.IssueDocumentPartialState
import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.interactor.UserAuthenticationInteractor
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.util.TestsConstants.mockedId1
import eu.europa.ec.commonfeature.util.TestsConstants.mockedMdlOptionItemUi
import eu.europa.ec.commonfeature.util.TestsConstants.mockedPidOptionItemUi
import eu.europa.ec.commonfeature.util.TestsConstants.mockedSampleDataOptionItemUi
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.MockResourceProviderForStringCalls.mockDocumentTypeUiToUiNameCall
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedFullMdl
import eu.europa.ec.testfeature.mockedFullPid
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class TestAddDocumentInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var userAuthenticationInteractor: UserAuthenticationInteractor

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: AddDocumentInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = AddDocumentInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            userAuthenticationInteractor = userAuthenticationInteractor,
            resourceProvider = resourceProvider,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getAddDocumentOption

    // Case 1:
    // 1. walletCoreDocumentsController.getAllDocuments() returns no documents.
    // 2. flowType == IssuanceFlowUiConfig.NO_DOCUMENT

    // Case 1 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. a PID option, available to add.
    // 2. an mDL option, unavailable to add.
    // 2. an Load Sample Data option, available to add.
    @Test
    fun `Given Case 1, When getAddDocumentOption is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(emptyList())
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.NO_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedPidOptionItemUi,
                            mockedMdlOptionItemUi.copy(
                                available = false
                            ),
                            mockedSampleDataOptionItemUi
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // 1. walletCoreDocumentsController.getAllDocuments() returns only an mDL.
    // 2. flowType == IssuanceFlowUiConfig.EXTRA_DOCUMENT

    // Case 2 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. a PID option, available to add.
    // 2. an mDL option, unavailable to add.
    @Test
    fun `Given Case 2, When getAddDocumentOption is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(listOf(mockedFullMdl))
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedPidOptionItemUi,
                            mockedMdlOptionItemUi.copy(
                                available = false
                            )
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // 1. walletCoreDocumentsController.getAllDocuments() returns no documents.
    // 2. flowType == IssuanceFlowUiConfig.EXTRA_DOCUMENT

    // Case 3 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. a PID option, available to add.
    // 2. an mDL option, available to add.
    @Test
    fun `Given Case 3, When getAddDocumentOption is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(emptyList())
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedPidOptionItemUi,
                            mockedMdlOptionItemUi
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // 1. walletCoreDocumentsController.getAllDocuments() returns only a PID.
    // 2. flowType == IssuanceFlowUiConfig.EXTRA_DOCUMENT

    // Case 4 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. a PID option, unavailable to add.
    // 2. an mDL option, available to add.
    @Test
    fun `Given Case 4, When getAddDocumentOption is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(listOf(mockedFullPid))
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedPidOptionItemUi.copy(
                                available = false
                            ),
                            mockedMdlOptionItemUi
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 5:
    // 1. walletCoreDocumentsController.getAllDocuments() throws an exception with a message.
    @Test
    fun `Given Case 5, When getAddDocumentOption is called, Then it returns Failure with exception's localized message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 6:
    // 1. walletCoreDocumentsController.getAllDocuments() throws an exception with no message.
    @Test
    fun `Given Case 6, When getAddDocumentOption is called, Then it returns Failure with the generic error message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region issueDocument
    @Test
    fun `Given an issuance method and a document type, When issueDocument is called, Then it calls walletCoreDocumentsController#issueDocument`() {
        coroutineRule.runTest {
            // Given
            val mockedIssuanceMethod = IssuanceMethod.OPENID4VCI
            val mockedDocumentType = DocumentTypeUi.PID.docType

            whenever(
                walletCoreDocumentsController.issueDocument(
                    issuanceMethod = mockedIssuanceMethod,
                    documentType = mockedDocumentType
                )
            ).thenReturn(IssueDocumentPartialState.Success(mockedId1).toFlow())

            // When
            interactor.issueDocument(
                issuanceMethod = mockedIssuanceMethod,
                documentType = mockedDocumentType
            ).runFlowTest {
                awaitItem()

                // Then
                verify(walletCoreDocumentsController, times(1))
                    .issueDocument(
                        issuanceMethod = mockedIssuanceMethod,
                        documentType = mockedDocumentType
                    )
            }
        }
    }
    //endregion

    //region addSampleData
    @Test
    fun `When addSampleData is called, Then it calls walletCoreDocumentsController#addSampleData`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.addSampleData())
                .thenReturn(AddSampleDataPartialState.Success.toFlow())

            // When
            interactor.addSampleData()
                .runFlowTest {
                    awaitItem()

                    // Then
                    verify(walletCoreDocumentsController, times(1))
                        .addSampleData()
                }
        }
    }
    //endregion
}