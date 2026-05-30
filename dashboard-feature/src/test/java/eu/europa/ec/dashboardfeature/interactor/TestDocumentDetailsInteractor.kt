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

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.DeleteAllDocumentsPartialState
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsDomain
import eu.europa.ec.dashboardfeature.ui.documents.model.DocumentCredentialsInfoUi
import eu.europa.ec.dashboardfeature.util.mockedBasicMdlDomain
import eu.europa.ec.dashboardfeature.util.mockedBasicPidDomain
import eu.europa.ec.dashboardfeature.util.mockedBookmarkId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker
import eu.europa.ec.testfeature.util.copy
import eu.europa.ec.testfeature.util.createMockedNamespaceData
import eu.europa.ec.testfeature.util.getMockedMdlWithBasicFields
import eu.europa.ec.testfeature.util.getMockedOldestPidWithBasicFields
import eu.europa.ec.testfeature.util.getMockedPidWithBasicFields
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedDocumentAvailableCredentials
import eu.europa.ec.testfeature.util.mockedDocumentTotalCredentials
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedFormattedExpirationDate
import eu.europa.ec.testfeature.util.mockedFormattedIssuanceDate
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedMdlId
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testfeature.util.mockedOldestPidId
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.IssuerDetailsCardDataUi
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.net.URI

class TestDocumentDetailsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uuidProvider: UuidProvider

    @Mock
    private lateinit var configLogic: ConfigLogic

    @Mock
    private lateinit var deviceAuthenticationInteractor: DeviceAuthenticationInteractor

    @Mock
    private lateinit var mockContext: Context

    private lateinit var interactor: DocumentDetailsInteractor

    private val mockedReIssueIssuerId = "mockedReIssueIssuerId"
    private val mockedReIssueUri = "mockedReIssueUri"

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DocumentDetailsInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            deviceAuthenticationInteractor = deviceAuthenticationInteractor,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
            configLogic = configLogic
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)
        whenever(configLogic.forcePidActivation).thenReturn(true)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getDocumentDetails

    // Case 1:
    // 1. walletCoreDocumentsController.getDocumentById() returns a PID document.
    // 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
    // 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.

    // Case 1 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with a PID document item and
    // documentIsBookmarked is false.
    @Test
    fun `Given Case 1, When getDocumentDetails is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi()

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicPidDomain,
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // 1. walletCoreDocumentsController.getDocumentById() returns a PID document.
    // 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
    // 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns true.

    // Case 2 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with
    // a PID document item.
    @Test
    fun `Given Case 2, When getDocumentDetails is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = true
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi()

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicPidDomain,
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // 1. walletCoreDocumentsController.getDocumentById() returns a PID document.
    // 2. walletCoreDocumentsController.isDocumentBookmarked() returns true.
    // 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.

    // Case 3 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with a PID document item and
    // documentIsBookmarked is true.
    @Test
    fun `Given Case 3, When getDocumentDetails is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi()

            mockRetrieveBookmarkCall(response = true)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicPidDomain,
                        documentIsBookmarked = true,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // 1. walletCoreDocumentsController.getDocumentById() returns an mDL document.
    // 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
    // 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.

    // Case 4 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with an mDL document item and
    // documentIsBookmarked is false.
    @Test
    fun `Given Case 4, When getDocumentDetails is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            mockGetDocumentByIdCall(response = mockedMdlWithBasicFields)

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi()

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedMdlId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = mockedBasicMdlDomain,
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 5:
    // 1. walletCoreDocumentsController.getDocumentById() returns null.

    // Case 5 Expected Result:
    // DocumentDetailsInteractorPartialState.Failed state,
    // with the generic error message.
    @Test
    fun `Given Case 5, When getDocumentDetails is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetDocumentByIdCall(response = null)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
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

    // Case 6:
    // 1. walletCoreDocumentsController.getDocumentById() returns a PID document, with:
    // no expiration date,
    // no image, and
    // no user name.
    // 2. walletCoreDocumentsController.isDocumentBookmarked() returns false.
    // 3. walletCoreDocumentsController.isDocumentLowOnCredentials() returns false.

    // Case 6 Expected Result:
    // DocumentDetailsInteractorPartialState.Success state, with a PID document item, with:
    // documentIsBookmarked is false.
    @Test
    fun `Given Case 6, When getDocumentDetails is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedDocIsLowOnCredentials = false
            mockGetDocumentDetailsStrings(
                resourceProvider = resourceProvider,
            )

            val mockedPidWithBasicFields = getMockedPidWithBasicFields()

            mockGetDocumentByIdCall(
                response = mockedPidWithBasicFields.copy(
                    data = MsoMdocData(
                        format = MsoMdocFormat(mockedMdocPidNameSpace),
                        issuerMetadata = null,
                        nameSpacedData = createMockedNamespaceData(
                            mockedMdocPidNameSpace, mapOf(
                                "no_data_item" to byteArrayOf(0)
                            )
                        )
                    )
                )
            )

            mockIsDocumentLowOnCredentialsCall(response = mockedDocIsLowOnCredentials)
            val documentCredentialsInfoUi = getMockedDocumentCredentialsInfoUi(
                resourceProvider = resourceProvider,
            )

            val issuerDetails = getMockedIssuerDetailsCardDataUi()

            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = DocumentDetailsDomain(
                            docName = mockedPidDocName,
                            docId = mockedPidId,
                            issuerId = "",
                            documentConfigId = "",
                            documentIdentifier = DocumentIdentifier.MdocPid,
                            documentClaims = listOf(
                                ClaimDomain.Primitive(
                                    key = "no_data_item",
                                    value = "0",
                                    displayTitle = "no_data_item",
                                    path = ClaimPathDomain(
                                        value = listOf("no_data_item"),
                                        type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
                                    ),
                                    isRequired = false,
                                ),
                            ),
                            documentIssuanceDate = mockedFormattedIssuanceDate,
                            documentExpirationDate = mockedFormattedExpirationDate,
                        ),
                        documentIsBookmarked = false,
                        documentCredentialsInfoUi = documentCredentialsInfoUi,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 7:
    // 1. walletCoreDocumentsController.getDocumentById() throws an exception with a message.

    // Case 7 Expected Result:
    // DocumentDetailsInteractorPartialState.Failed state,
    // with the exception's localized message.
    @Test
    fun `Given Case 7, When getDocumentDetails is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getDocumentById(mockedPidId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
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

    // Case 8:
    // 1. walletCoreDocumentsController.getDocumentById() throws an exception with no message.

    // Case 8 Expected Result:
    // DocumentDetailsInteractorPartialState.Failed state,
    // with the generic error message.
    @Test
    fun `Given Case 8, When getDocumentDetails is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getDocumentById(mockedPidId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
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

    // Case 9:
    // Same as Case 1 but with wasIssuerDetailsExpanded = true → IssuerDetailsCardDataUi.isExpanded
    // is true (covers the non-null branch of `wasIssuerDetailsExpanded ?: false`).
    @Test
    fun `Given Case 9, When getDocumentDetails is called with wasIssuerDetailsExpanded=true, Then issuerDetails#isExpanded is true`() {
        coroutineRule.runTest {
            // Given
            mockGetDocumentDetailsStrings(resourceProvider = resourceProvider)
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = false)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = true,
            ).runFlowTest {
                // Then
                val result = awaitItem()
                kotlin.test.assertTrue(result is DocumentDetailsInteractorPartialState.Success)
                assertEquals(true, result.issuerDetails.isExpanded)
            }
        }
    }

    // Case 10:
    // isDocumentRevoked returns true → IssuerDetailsCardDataUi.documentState is Revoked.
    @Test
    fun `Given Case 10, When getDocumentDetails is called for a revoked document, Then DocumentState is Revoked`() {
        coroutineRule.runTest {
            // Given
            mockGetDocumentDetailsStrings(resourceProvider = resourceProvider)
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockIsDocumentLowOnCredentialsCall(response = false)
            mockRetrieveBookmarkCall(response = false)
            mockIsDocumentRevoked(isRevoked = true)

            // When
            interactor.getDocumentDetails(
                documentId = mockedPidId,
                wasIssuerDetailsExpanded = null,
            ).runFlowTest {
                // Then
                val result = awaitItem()
                kotlin.test.assertTrue(result is DocumentDetailsInteractorPartialState.Success)
                assertEquals(
                    IssuerDetailsCardDataUi.DocumentState.Revoked,
                    result.issuerDetails.documentState
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
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
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
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
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
            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            val mockedOldestPidWithBasicFields = getMockedOldestPidWithBasicFields()

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
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            val mockedOldestPidWithBasicFields = getMockedOldestPidWithBasicFields()

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

    // Case 9 (deleteDocument):
    // forcePidActivation=false → shouldDeleteAllDocuments=false (short-circuit on `&&`)
    // → deleteDocument single-document path.
    @Test
    fun `Given Case 9, When deleteDocument is called with forcePidActivation false, Then SingleDocumentDeleted is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(configLogic.forcePidActivation).thenReturn(false)
            val mockedPidWithBasicFields = getMockedPidWithBasicFields()
            mockGetDocumentByIdCall(response = mockedPidWithBasicFields)
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = mockedPidId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 10 (deleteDocument):
    // The fetched document is non-PID (e.g., mDL) → shouldDeleteAllDocuments=false
    // (short-circuit on docIdentifier check) → deleteDocument single-document path.
    @Test
    fun `Given Case 10, When deleteDocument is called for a non-PID document, Then SingleDocumentDeleted is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedMdlWithBasicFields = getMockedMdlWithBasicFields()
            mockGetDocumentByIdCall(response = mockedMdlWithBasicFields)
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = mockedMdlId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem()
                )
            }
        }
    }
    //endregion

    // Case 11 (deleteDocument):
    // The document is an SdJwt PID → the `docType` resolution falls through the
    // `(format as? MsoMdocFormat)?.docType ?: (format as? SdJwtVcFormat)?.vct` chain to the
    // SdJwt branch (line 598). With forcePidActivation=true and one SdJwt PID, the
    // shouldDeleteAllDocuments branch returns true → deleteAllDocuments path.
    @Test
    fun `Given Case 11, When deleteDocument is called for an SdJwt PID, Then AllDocumentsDeleted is returned`() {
        coroutineRule.runTest {
            // Given
            val sdJwtPid = eu.europa.ec.testfeature.util.getMockedSdJwtPidWithBasicFields()
            mockGetAllDocumentsCall(response = listOf(sdJwtPid))
            mockGetAllDocumentsWithTypeCall(response = listOf(sdJwtPid))
            mockGetDocumentByIdCall(response = sdJwtPid)
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)

            // When
            interactor.deleteDocument(documentId = sdJwtPid.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 12 (deleteDocument):
    // Multiple PID documents exist; the document being deleted is NOT the main PID
    // (allPidDocuments.count > 1 AND getMainPidDocument().id != documentId).
    // → shouldDeleteAllDocuments evaluates false → single-document delete path.
    @Test
    fun `Given Case 12, When deleteDocument is called for a non-main PID with multiple PIDs, Then SingleDocumentDeleted is returned`() {
        coroutineRule.runTest {
            // Given
            val mainPid = getMockedPidWithBasicFields()
            val otherPid = getMockedOldestPidWithBasicFields()
            mockGetAllDocumentsWithTypeCall(response = listOf(mainPid, otherPid))
            mockGetDocumentByIdCall(response = otherPid)
            mockGetMainPidDocument(response = mainPid)
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = otherPid.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem()
                )
            }
        }
    }
    //endregion

    // Case 13 (deleteDocument):
    // forcePidActivation=true, doc is PID, allPidDocuments has exactly 1 entry → the
    // `if (allPidDocuments.count() > 1)` false branch runs → `else true` returns true →
    // shouldDeleteAllDocuments=true → deleteAllDocuments path with single PID present.
    @Test
    fun `Given Case 13, When deleteDocument is called for the single PID, Then AllDocumentsDeleted is returned`() {
        coroutineRule.runTest {
            // Given
            val mainPid = getMockedPidWithBasicFields()
            mockGetAllDocumentsWithTypeCall(response = listOf(mainPid))
            mockGetDocumentByIdCall(response = mainPid)
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)

            // When
            interactor.deleteDocument(documentId = mainPid.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 15 (deleteDocument):
    // Multiple PIDs exist but getMainPidDocument returns null → the `?.id` safe-call returns
    // null → `null == documentId` is false → shouldDeleteAllDocuments=false → SingleDocumentDeleted.
    @Test
    fun `Given Case 15, When deleteDocument is called and getMainPidDocument returns null, Then SingleDocumentDeleted is returned`() {
        coroutineRule.runTest {
            // Given
            val pid1 = getMockedPidWithBasicFields()
            val pid2 = getMockedOldestPidWithBasicFields()
            mockGetAllDocumentsWithTypeCall(response = listOf(pid1, pid2))
            mockGetDocumentByIdCall(response = pid1)
            mockGetMainPidDocument(response = null)
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(documentId = pid1.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted,
                    awaitItem()
                )
            }
        }
    }

    // Case 14 (deleteDocument):
    // forcePidActivation=true, doc is PID, multiple PIDs exist, AND getMainPidDocument().id
    // matches the documentId being deleted → the inner `getMainPidDocument()?.id == documentId`
    // branch evaluates true → shouldDeleteAllDocuments=true → deleteAllDocuments path.
    @Test
    fun `Given Case 14, When deleteDocument is called for the main PID with multiple PIDs, Then AllDocumentsDeleted is returned`() {
        coroutineRule.runTest {
            // Given
            val mainPid = getMockedPidWithBasicFields()
            val otherPid = getMockedOldestPidWithBasicFields()
            mockGetAllDocumentsWithTypeCall(response = listOf(mainPid, otherPid))
            mockGetDocumentByIdCall(response = mainPid)
            mockGetMainPidDocument(response = mainPid)
            mockDeleteAllDocumentsCall(response = DeleteAllDocumentsPartialState.Success)

            // When
            interactor.deleteDocument(documentId = mainPid.id).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted,
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region storeBookmark()
    // Case 1:
    // 1. A valid bookmarkId is provided.
    // 2. walletCoreDocumentsController.storeBookmark() succeeds.
    // Expected result:
    // DocumentDetailsInteractorStoreBookmarkPartialState.Success state is returned with the bookmarkId.
    @Test
    fun `Given Case 1, When storeBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Arrange
            mockStoreBookmarkCall(bookmarkId = mockedBookmarkId)

            // Act
            interactor.storeBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Assert
                assertEquals(
                    DocumentDetailsInteractorStoreBookmarkPartialState.Success(
                        bookmarkId = mockedBookmarkId
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // 1. A valid bookmarkId is provided.
    // 2. When walletCoreDocumentsController.storeBookmark() is called, an exception is thrown.
    // Expected result:
    // DocumentDetailsInteractorStoreBookmarkPartialState.Failure state is returned.
    @Test
    fun `Given Case 2, When storeBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Arrange
            mockStoreBookmarkCall(
                bookmarkId = mockedBookmarkId,
                throwable = mockedExceptionWithMessage
            )

            // Act
            interactor.storeBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Assert
                assertEquals(
                    DocumentDetailsInteractorStoreBookmarkPartialState.Failure,
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region deleteBookmark()
    // Case 1:
    // 1. A valid bookmarkId is provided.
    // 2. walletCoreDocumentsController.deleteBookmark() succeeds.
    // Expected result:
    // DocumentDetailsInteractorDeleteBookmarkPartialState.Success state is returned.
    @Test
    fun `Given Case 1, When deleteBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Arrange
            mockDeleteBookmarkCall(bookmarkId = mockedBookmarkId)

            // Act
            interactor.deleteBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Assert
                assertEquals(
                    DocumentDetailsInteractorDeleteBookmarkPartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // 1. A valid bookmarkId is provided for the bookmark to be deleted.
    // 2. When walletCoreDocumentsController.deleteBookmark() is called, an exception is thrown.
    // Expected result:
    // DocumentDetailsInteractorDeleteBookmarkPartialState.Failure state is returned.
    @Test
    fun `Given Case 2, When deleteBookmark is called, Then the expected result is returned`() {
        coroutineRule.runTest {
            // Given
            mockDeleteBookmarkCall(
                bookmarkId = mockedBookmarkId,
                throwable = mockedExceptionWithMessage
            )

            // When
            interactor.deleteBookmark(documentId = mockedBookmarkId).runFlowTest {
                // Then
                assertEquals(
                    DocumentDetailsInteractorDeleteBookmarkPartialState.Failure,
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region reIssueDocument

    @Test
    fun `Given controller emits DeferredSuccess, When reIssueDocument is called, Then Success is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.DeferredSuccess(deferredDocuments = emptyMap()).toFlow()
            )

            // When
            interactor.reIssueDocument(documentId = mockedPidId, issuerId = mockedReIssueIssuerId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        DocumentDetailsInteractorIssuancePartialState.Success,
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun `Given controller emits Failure, When reIssueDocument is called, Then Failure with the same message is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.Failure(errorMessage = mockedPlainFailureMessage)
                    .toFlow()
            )

            // When
            interactor.reIssueDocument(documentId = mockedPidId, issuerId = mockedReIssueIssuerId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        DocumentDetailsInteractorIssuancePartialState.Failure(
                            errorMessage = mockedPlainFailureMessage
                        ),
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun `Given controller emits Success, When reIssueDocument is called, Then Success is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.Success(documentIds = listOf(mockedPidId)).toFlow()
            )

            // When
            interactor.reIssueDocument(documentId = mockedPidId, issuerId = mockedReIssueIssuerId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        DocumentDetailsInteractorIssuancePartialState.Success,
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun `Given controller emits PartialSuccess, When reIssueDocument is called, Then Success is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf(mockedPidId),
                    nonIssuedDocuments = emptyMap(),
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(documentId = mockedPidId, issuerId = mockedReIssueIssuerId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        DocumentDetailsInteractorIssuancePartialState.Success,
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun `Given controller emits UserAuthRequired, When reIssueDocument is called, Then UserAuthRequired is emitted`() {
        coroutineRule.runTest {
            // Given
            val crypto = BiometricCrypto(cryptoObject = null)
            val resultHandler = DeviceAuthenticationResult()
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenReturn(
                IssueDocumentsPartialState.UserAuthRequired(
                    crypto = crypto,
                    resultHandler = resultHandler,
                ).toFlow()
            )

            // When
            interactor.reIssueDocument(documentId = mockedPidId, issuerId = mockedReIssueIssuerId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        DocumentDetailsInteractorIssuancePartialState.UserAuthRequired(
                            crypto = crypto,
                            resultHandler = resultHandler,
                        ),
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun `Given controller throws with message, When reIssueDocument is called, Then Failure with localized message is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenThrow(mockedExceptionWithMessage)

            // When
            interactor.reIssueDocument(documentId = mockedPidId, issuerId = mockedReIssueIssuerId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        DocumentDetailsInteractorIssuancePartialState.Failure(
                            errorMessage = mockedExceptionWithMessage.localizedMessage!!
                        ),
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun `Given controller throws without message, When reIssueDocument is called, Then Failure with generic message is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = mockedPidId,
                    issuerId = mockedReIssueIssuerId,
                    allowAuthorizationFallback = true,
                )
            ).thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.reIssueDocument(documentId = mockedPidId, issuerId = mockedReIssueIssuerId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        DocumentDetailsInteractorIssuancePartialState.Failure(
                            errorMessage = mockedGenericErrorMessage
                        ),
                        awaitItem()
                    )
                }
        }
    }
    //endregion

    //region handleUserAuth

    @Test
    fun `Given biometrics availability is CanAuthenticate, When handleUserAuth is called, Then authenticateWithBiometrics is invoked`() {
        // Given
        val context = mockContext
        val crypto = BiometricCrypto(cryptoObject = null)
        val resultHandler = DeviceAuthenticationResult()
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.CanAuthenticate)

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler,
        )

        // Then
        org.mockito.kotlin.verify(deviceAuthenticationInteractor, org.mockito.kotlin.times(1))
            .authenticateWithBiometrics(
                context,
                crypto,
                mockedNotifyOnAuthenticationFailure,
                resultHandler,
            )
    }

    @Test
    fun `Given biometrics availability is NonEnrolled, When handleUserAuth is called, Then launchBiometricSystemScreen is invoked`() {
        // Given
        val crypto = BiometricCrypto(cryptoObject = null)
        val resultHandler = DeviceAuthenticationResult()
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.NonEnrolled)

        // When
        interactor.handleUserAuth(
            context = mockContext,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler,
        )

        // Then
        org.mockito.kotlin.verify(deviceAuthenticationInteractor, org.mockito.kotlin.times(1))
            .launchBiometricSystemScreen()
    }

    @Test
    fun `Given biometrics availability is Failure, When handleUserAuth is called, Then resultHandler#onAuthenticationFailure is invoked`() {
        // Given
        val onFailure = org.mockito.kotlin.mock<() -> Unit>()
        val resultHandler = DeviceAuthenticationResult(onAuthenticationFailure = onFailure)
        val crypto = BiometricCrypto(cryptoObject = null)
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.Failure(errorMessage = mockedPlainFailureMessage))

        // When
        interactor.handleUserAuth(
            context = mockContext,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler,
        )

        // Then
        org.mockito.kotlin.verify(onFailure).invoke()
    }
    //endregion

    //region resumeOpenId4VciWithAuthorization
    @Test
    fun `When resumeOpenId4VciWithAuthorization is called, Then it delegates to walletCoreDocumentsController`() {
        // When
        interactor.resumeOpenId4VciWithAuthorization(mockedReIssueUri)

        // Then
        org.mockito.kotlin.verify(walletCoreDocumentsController, org.mockito.kotlin.times(1))
            .resumeOpenId4VciWithAuthorization(mockedReIssueUri)
    }
    //endregion

    //region helper functions
    private fun mockGetAllDocumentsCall(response: List<IssuedDocument>) {
        whenever(walletCoreDocumentsController.getAllDocuments())
            .thenReturn(response)
    }

    private fun mockGetAllDocumentsWithTypeCall(response: List<IssuedDocument>) {
        whenever(walletCoreDocumentsController.getAllDocumentsByType(documentIdentifiers = any()))
            .thenReturn(response)
    }

    private fun mockGetDocumentByIdCall(response: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getDocumentById(ArgumentMatchers.anyString()))
            .thenReturn(response)
    }

    private fun mockGetMainPidDocument(response: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(response)
    }

    private fun mockDeleteAllDocumentsCall(response: DeleteAllDocumentsPartialState) {
        whenever(walletCoreDocumentsController.deleteAllDocuments())
            .thenReturn(response.toFlow())
    }

    private fun mockDeleteDocumentCall(response: DeleteDocumentPartialState) {
        whenever(walletCoreDocumentsController.deleteDocument(ArgumentMatchers.anyString()))
            .thenReturn(response.toFlow())
    }

    private suspend fun mockStoreBookmarkCall(bookmarkId: String, throwable: Throwable? = null) {
        whenever(walletCoreDocumentsController.storeBookmark(bookmarkId))
            .thenAnswer {
                throwable?.let { throw throwable }
                Unit
            }
    }

    private suspend fun mockDeleteBookmarkCall(bookmarkId: String, throwable: Throwable? = null) {
        whenever(walletCoreDocumentsController.deleteBookmark(bookmarkId))
            .thenAnswer {
                throwable?.let { throw throwable }
                Unit
            }
    }

    private suspend fun mockRetrieveBookmarkCall(response: Boolean) {
        whenever(walletCoreDocumentsController.isDocumentBookmarked(ArgumentMatchers.anyString()))
            .thenReturn(response)
    }

    private suspend fun mockIsDocumentRevoked(isRevoked: Boolean) {
        whenever(walletCoreDocumentsController.isDocumentRevoked(any())).thenReturn(isRevoked)
    }

    private suspend fun mockIsDocumentLowOnCredentialsCall(response: Boolean) {
        whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(any()))
            .thenReturn(response)
    }

    private fun getMockedDocumentCredentialsInfoUi(
        resourceProvider: ResourceProvider,
        availableCredentials: Int = mockedDocumentAvailableCredentials,
        totalCredentials: Int = mockedDocumentTotalCredentials,
    ): DocumentCredentialsInfoUi {
        return DocumentCredentialsInfoUi(
            availableCredentials = availableCredentials,
            totalCredentials = totalCredentials,
            title = resourceProvider.getString(
                R.string.document_details_document_credentials_info_text,
                availableCredentials,
                totalCredentials
            ),
        )
    }

    private fun getMockedIssuerDetailsCardDataUi(
        issuerName: String? = null,
        issuerLogo: URI? = null,
        documentState: IssuerDetailsCardDataUi.DocumentState = IssuerDetailsCardDataUi.DocumentState.Issued(
            issuanceDate = mockedFormattedIssuanceDate,
            expirationDate = mockedFormattedExpirationDate
        ),
        isExpanded: Boolean = false,
    ): IssuerDetailsCardDataUi {
        return IssuerDetailsCardDataUi(
            issuerName = issuerName,
            issuerLogo = issuerLogo,
            documentState = documentState,
            isExpanded = isExpanded,
        )
    }

    private fun mockGetDocumentDetailsStrings(
        resourceProvider: ResourceProvider,
        availableCredentials: Int = mockedDocumentAvailableCredentials,
        totalCredentials: Int = mockedDocumentTotalCredentials
    ) {
        StringResourceProviderMocker.mockGetDocumentDetailsStrings(
            resourceProvider = resourceProvider,
            availableCredentials = availableCredentials,
            totalCredentials = totalCredentials,
        )
    }
    //endregion
}