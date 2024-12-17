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

import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentDetailsUi
import eu.europa.ec.commonfeature.util.TestsData
import eu.europa.ec.commonfeature.util.TestsData.mockedBasicMdlUi
import eu.europa.ec.commonfeature.util.TestsData.mockedBasicPidUi
import eu.europa.ec.corelogic.controller.DeleteAllDocumentsPartialState
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.MockResourceProviderForStringCalls.mockDocumentTypeUiToUiNameCall
import eu.europa.ec.testfeature.MockResourceProviderForStringCalls.mockTransformToUiItemCall
import eu.europa.ec.testfeature.createMockedNamespaceData
import eu.europa.ec.testfeature.mockedEmptyPid
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedMdlId
import eu.europa.ec.testfeature.mockedMdlWithBasicFields
import eu.europa.ec.testfeature.mockedOldestPidId
import eu.europa.ec.testfeature.mockedOldestPidWithBasicFields
import eu.europa.ec.testfeature.mockedPidId
import eu.europa.ec.testfeature.mockedPidNameSpace
import eu.europa.ec.testfeature.mockedPidWithBasicFields
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class TestDocumentDetailsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: DocumentDetailsInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DocumentDetailsInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            resourceProvider = resourceProvider,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getDocumentDetails

    // Case 1:
    // 1. walletCoreDocumentsController.getDocumentById() returns a PID document.

    // Case 1 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with a PID document UI item.
    @Test
    fun `Given Case 1, When getDocumentDetails is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTransformToUiItemCall(resourceProvider)
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        documentUi = mockedBasicPidUi
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // 1. walletCoreDocumentsController.getDocumentById() returns an mDL document.

    // Case 2 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with an mDL document UI item.
    @Test
    fun `Given Case 2, When getDocumentDetails is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTransformToUiItemCall(resourceProvider)
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            mockGetDocumentByIdCall(response = mockedMdlWithBasicFields)

            // When
            interactor.getDocumentDetails(
                documentId = mockedMdlId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        documentUi = mockedBasicMdlUi.copy(
                            documentImage = "SE"
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // 1. walletCoreDocumentsController.getDocumentById() returns an empty PID document.

    // Case 3 Expected Result:
    // DocumentDetailsInteractorPartialState.Failed state,
    // with the generic error message.
    @Test
    fun `Given Case 3, When getDocumentDetails is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetDocumentByIdCall(response = mockedEmptyPid)
            whenever(walletCoreDocumentsController.getDocumentById(mockedPidId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // 1. walletCoreDocumentsController.getDocumentById() returns null.

    // Case 4 Expected Result:
    // DocumentDetailsInteractorPartialState.Failed state,
    // with the generic error message.
    @Test
    fun `Given Case 4, When getDocumentDetails is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetDocumentByIdCall(response = null)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 5:
    // 1. walletCoreDocumentsController.getDocumentById() returns a PID document, with:
    // no expiration date,
    // no image, and
    // no user name.

    // Case 5 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with a PID document UI item, with:
    // an empty string for documentExpirationDateFormatted,
    // an empty string for documentImage, and
    // an empty string for userFullName,
    @Test
    fun `Given Case 5, When getDocumentDetails is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTransformToUiItemCall(resourceProvider)
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            mockGetDocumentByIdCall(
                response = mockedPidWithBasicFields.copy(
                    nameSpacedData = createMockedNamespaceData(
                        mockedPidNameSpace, mapOf(
                            "no_data_item" to byteArrayOf(0)
                        )
                    )
                )
            )

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        documentUi = DocumentUi(
                            documentId = TestsData.mockedPidId,
                            documentName = TestsData.mockedDocUiNamePid,
                            documentIdentifier = DocumentIdentifier.MdocPid,
                            documentExpirationDateFormatted = "",
                            documentHasExpired = TestsData.mockedDocumentHasExpired,
                            documentImage = "",
                            documentDetails = listOf(
                                DocumentDetailsUi.DefaultItem(
                                    itemData = InfoTextWithNameAndValueData.create(
                                        title = "no_data_item",
                                        infoValues = arrayOf("0")
                                    )
                                )
                            ),
                            userFullName = "",
                            documentIssuanceState = DocumentUiIssuanceState.Issued
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 6:
    // 1. walletCoreDocumentsController.getDocumentById() throws an exception with a message.

    // Case 6 Expected Result:
    // DocumentDetailsInteractorPartialState.Failed state,
    // with the exception's localized message.
    @Test
    fun `Given Case 6, When getDocumentDetails is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getDocumentById(mockedPidId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 7:
    // 1. walletCoreDocumentsController.getDocumentById() throws an exception with no message.

    // Case 7 Expected Result:
    // DocumentDetailsInteractorPartialState.Failed state,
    // with the generic error message.
    @Test
    fun `Given Case 7, When getDocumentDetails is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getDocumentById(mockedPidId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region deleteDocument

    // Case 1:

    // 1. A documentId and document is PID.
    // 2. walletCoreDocumentsController.getAllDocuments() returns 1 Document and it is PID.
    // 3. walletCoreDocumentsController.getDocumentById returns that PID Document.
    // 4. walletCoreDocumentsController.deleteAllDocuments() returns Failed.
    @Test
    fun `Given Case 1, When deleteDocument is called, Then it returns Failure with failure's error message`() {
        coroutineRule.runTest {
            // Given
            mockGetAllDocumentsCall(
                response = listOf(
                    mockedPidWithBasicFields
                )
            )
            mockDeleteAllDocumentsCall(
                response = DeleteAllDocumentsPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
            )
            mockGetDocumentByIdCall(
                response = mockedPidWithBasicFields
            )

            // When
            interactor.deleteDocument(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:

    // 1. A documentId and document is PID.
    // 2. walletCoreDocumentsController.getAllDocuments() returns 1 Document and it is PID.
    // 3. walletCoreDocumentsController.getDocumentById returns that PID Document.
    // 4. walletCoreDocumentsController.deleteAllDocuments() returns Success.
    @Test
    fun `Given Case 2, When deleteDocument is called, Then it returns AllDocumentsDeleted`() {
        coroutineRule.runTest {
            // Given
            mockGetAllDocumentsCall(
                response = listOf(
                    mockedPidWithBasicFields
                )
            )
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)
            mockGetDocumentByIdCall(
                response = mockedPidWithBasicFields
            )

            // When
            interactor.deleteDocument(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 3:

    // 1. A documentId and document is PID.
    // 2. walletCoreDocumentsController.getAllDocuments() returns more than 1 PIDs
    // 3. walletCoreDocumentsController.getDocumentById returns the oldest Document.
    // 4. walletCoreDocumentsController.deleteAllDocuments() returns Success.
    @Test
    fun `Given Case 3, When deleteDocument is called, Then it returns AllDocumentsDeleted`() {
        coroutineRule.runTest {
            // Given
            mockGetAllDocumentsCall(
                response = listOf(
                    mockedMdlWithBasicFields,
                    mockedPidWithBasicFields,
                    mockedOldestPidWithBasicFields
                )
            )
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)
            mockGetDocumentByIdCall(
                response = mockedOldestPidWithBasicFields
            )

            // When
            interactor.deleteDocument(
                documentId = mockedOldestPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 4:

    // 1. A documentId and document is PID.
    // 2. walletCoreDocumentsController.getAllDocuments(documentIdentifier: DocumentIdentifier) returns more than 1 PIDs
    //      AND the documentId we are about to delete is NOT the one of the oldest PID.
    // 3. walletCoreDocumentsController.deleteDocument() returns Success.
    @Test
    fun `Given Case 4, When deleteDocument is called, Then it returns SingleDocumentDeleted`() {
        coroutineRule.runTest {
            // Given
            mockGetAllDocumentsWithTypeCall(
                response = listOf(
                    mockedPidWithBasicFields,
                    mockedOldestPidWithBasicFields
                )
            )
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)
            mockGetMainPidDocument(mockedOldestPidWithBasicFields)

            // When
            interactor.deleteDocument(
                documentId = mockedPidId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 5:

    // 1. A documentId and document is mDL.
    // 2. walletCoreDocumentsController.deleteDocument() returns Failed.
    @Test
    fun `Given Case 5, When deleteDocument is called, Then it returns Failure with failure's error message`() {
        coroutineRule.runTest {
            // Given
            mockDeleteDocumentCall(
                response = DeleteDocumentPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
            )

            // When
            interactor.deleteDocument(
                documentId = mockedMdlId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 6:

    // 1. A documentId and document is mDL.
    // 2. walletCoreDocumentsController.deleteDocument() returns Success.
    @Test
    fun `Given Case 6, When deleteDocument is called, Then it returns SingleDocumentDeleted`() {
        coroutineRule.runTest {
            // Given
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(
                documentId = mockedMdlId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 7:

    // 1. A documentId and document is mDL.
    // 2. walletCoreDocumentsController.deleteDocument() throws an exception with a message.
    @Test
    fun `Given Case 7, When deleteDocument is called, Then it returns Failure with the exception's localized message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockedMdlId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.deleteDocument(
                documentId = mockedMdlId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 8:

    // 1. A documentId and document is mDL.
    // 2. walletCoreDocumentsController.deleteDocument() throws an exception with no message.
    @Test
    fun `Given Case 8, When deleteDocument is called, Then it returns Failure with the generic error message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockedMdlId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.deleteDocument(
                documentId = mockedMdlId,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region helper functions
    private fun mockGetAllDocumentsCall(response: List<IssuedDocument>) {
        whenever(walletCoreDocumentsController.getAllDocuments())
            .thenReturn(response)
    }

    private fun mockGetAllDocumentsWithTypeCall(response: List<IssuedDocument>) {
        whenever(walletCoreDocumentsController.getAllDocumentsByType(documentIdentifier = any()))
            .thenReturn(response)
    }

    private fun mockGetDocumentByIdCall(response: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getDocumentById(anyString()))
            .thenReturn(response)
    }

    private fun mockGetMainPidDocument(response: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(response)
    }

    private fun mockDeleteAllDocumentsCall(response: DeleteAllDocumentsPartialState) {
        whenever(walletCoreDocumentsController.deleteAllDocuments(anyString()))
            .thenReturn(response.toFlow())
    }

    private fun mockDeleteDocumentCall(response: DeleteDocumentPartialState) {
        whenever(walletCoreDocumentsController.deleteDocument(anyString()))
            .thenReturn(response.toFlow())
    }
    //endregion
}