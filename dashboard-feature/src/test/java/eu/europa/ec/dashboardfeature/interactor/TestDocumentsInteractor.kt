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

import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterSort
import eu.europa.ec.businesslogic.validator.model.FilterableAttributes
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DeferredDocumentDataDomain
import eu.europa.ec.corelogic.model.DocumentCategories
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentUi
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes
import eu.europa.ec.dashboardfeature.util.mockedPendingMdlUi
import eu.europa.ec.dashboardfeature.util.mockedPendingPidUi
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.getMockedFullDocuments
import eu.europa.ec.testfeature.util.getMockedFullPid
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

class TestDocumentsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var filterValidator: FilterValidator

    @Mock
    private lateinit var configLogic: ConfigLogic

    private lateinit var interactor: DocumentsInteractor

    private lateinit var closeable: AutoCloseable

    private lateinit var mockDocumentId: String
    private lateinit var mockDocumentName: String

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DocumentsInteractorImpl(
            resourceProvider = resourceProvider,
            walletCoreDocumentsController = walletCoreDocumentsController,
            filterValidator = filterValidator,
            configLogic = configLogic
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(configLogic.forcePidActivation).thenReturn(true)

        mockDocumentId = "mockDocumentId"
        mockDocumentName = "mockDocumentName"
    }

    @After
    fun after() {
        closeable.close()
    }

    private fun documentsAttributes(
        name: String = "PID",
        issuedDate: java.time.Instant? = null,
        expiryDate: java.time.Instant? = null,
        issuer: String = "Issuer",
        category: DocumentCategory = DocumentCategory.Government,
        isRevoked: Boolean = false,
    ): eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes =
        eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes(
            searchTags = emptyList(),
            name = name,
            expiryDate = expiryDate,
            issuedDate = issuedDate,
            issuer = issuer,
            category = category,
            isRevoked = isRevoked,
        )

    // region deleteDocument
    // Case 1:
    // walletCoreDocumentsController.getAllDocuments() returns a list of Documents
    // with a size of two.

    // Case 1 Expected Result:
    // DocumentInteractorDeleteDocumentPartialState.SingleDocumentDeleted state.
    @Test
    fun `Given Case 1, When deleteDocument is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)
            assert(walletCoreDocumentsController.getAllDocuments().size == 2)
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                val expectedFlow =
                    DocumentInteractorDeleteDocumentPartialState.SingleDocumentDeleted

                assertEquals(expectedFlow, awaitItem())
            }
        }
    }

    // Case 2:
    // walletCoreDocumentsController.getAllDocuments() returns an empty list.

    // Case 2 Expected Result:
    // DocumentInteractorDeleteDocumentPartialState.AllDocumentsDeleted state
    @Test
    fun `Given Case 2, When deleteDocument is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockDocumentsList = mock<List<Document>>()
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(mockDocumentsList)
            whenever(mockDocumentsList.isEmpty()).thenReturn(true)
            assert(walletCoreDocumentsController.getAllDocuments().isEmpty())

            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                val expectedFlow = DocumentInteractorDeleteDocumentPartialState.AllDocumentsDeleted

                // Then
                assertEquals(expectedFlow, awaitItem())
            }
        }
    }

    // Case 3:
    // walletCoreDocumentsController.getAllDocuments() returns Failure

    // Case 3 Expected Result:
    // DocumentInteractorDeleteDocumentPartialState.Failure state.
    @Test
    fun `Given Case 3, When deleteDocument is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            mockDeleteDocumentCall(
                response = DeleteDocumentPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
            )

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }

    // Case 4:
    // walletCoreDocumentsController.deleteDocument() throws an exception with a message.

    // Case 4 Expected Result:
    // DocumentInteractorDeleteDocumentPartialState.Failure state with exception's localized message.
    @Test
    fun `Given Case 4, When deleteDocument is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockDocumentId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }

    // Case 5:
    // walletCoreDocumentsController.deleteDocument() throws an exception with no message.

    // Case 5 Expected Result:
    // DocumentInteractorDeleteDocumentPartialState.Failure state with the generic error message.
    @Test
    fun `Given Case 5, When deleteDocument is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockDocumentId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    //endregion

    //region tryIssuingDeferredDocumentsFlow

    // Case 1:
    // When issueDeferredDocument was called:
    // 1. IssueDeferredDocumentPartialState.Issued was emitted, with
    //  - successData, the successfully issued deferred document's DeferredDocumentData, and also,
    // 2. IssueDeferredDocumentPartialState.Failed was emitted, with
    //  - documentId, the failed deferred document's DocumentId.

    // Case 1 Expected Result:
    // DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result state, with
    //  - successfullyIssuedDeferredDocuments: a list with the successfully issued deferred document's DeferredDocumentData,
    //  - failedIssuedDeferredDocuments: a list with the failed deferred document's DocumentId.
    @Test
    fun `Given Case 1, When tryIssuingDeferredDocumentsFlow is called, Then Case 1 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId1 = mockedPendingPidUi.documentId
            val mockDeferredPendingType1 = mockedPendingPidUi.documentIdentifier.formatType
            val mockDeferredPendingName1 = mockedPendingPidUi.documentName

            val mockDeferredPendingDocId2 = mockedPendingMdlUi.documentId
            val mockDeferredPendingType2 = mockedPendingMdlUi.documentIdentifier.formatType

            val deferredDocuments: Map<DocumentId, FormatType> = mapOf(
                mockDeferredPendingDocId1 to mockDeferredPendingType1,
                mockDeferredPendingDocId2 to mockDeferredPendingType2
            )
            val successData = DeferredDocumentDataDomain(
                documentId = mockDeferredPendingDocId1,
                formatType = mockDeferredPendingType1,
                docName = mockDeferredPendingName1
            )

            mockIssueDeferredDocumentCall(
                docId = mockDeferredPendingDocId1,
                response = IssueDeferredDocumentPartialState.Issued(
                    deferredDocumentData = successData
                )
            )
            mockIssueDeferredDocumentCall(
                docId = mockDeferredPendingDocId2,
                response = IssueDeferredDocumentPartialState.Failed(
                    documentId = mockDeferredPendingDocId2,
                    errorMessage = mockedPlainFailureMessage
                )
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments)
                .runFlowTest {
                    // Then
                    val expectedResult =
                        DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                            successfullyIssuedDeferredDocuments = listOf(successData),
                            failedIssuedDeferredDocuments = listOf(mockDeferredPendingDocId2)
                        )
                    assertEquals(expectedResult, awaitItem())
                }
        }

    // Case 2:
    // IssueDeferredDocumentPartialState.Expired was emitted when issueDeferredDocument was called.

    // Case 2 Expected Result:
    // DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result with,
    // - successfullyIssuedDeferredDocuments = emptyList.
    // - failedIssuedDeferredDocuments = emptyList.
    @Test
    fun `Given Case 2, When tryIssuingDeferredDocumentsFlow is called, Then Case 2 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredExpiredDocId = mockedPendingPidUi.documentId
            val mockDeferredExpiredDocType = mockedPendingPidUi.documentIdentifier.formatType

            val deferredDocuments: Map<DocumentId, FormatType> = mapOf(
                mockDeferredExpiredDocId to mockDeferredExpiredDocType
            )

            mockIssueDeferredDocumentCall(
                docId = mockDeferredExpiredDocId,
                response = IssueDeferredDocumentPartialState.Expired(
                    documentId = mockDeferredExpiredDocId
                )
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments)
                .runFlowTest {
                    // Then
                    val expectedResult =
                        DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                            successfullyIssuedDeferredDocuments = emptyList(),
                            failedIssuedDeferredDocuments = emptyList()
                        )
                    assertEquals(expectedResult, awaitItem())
                }
        }

    // Case 3:
    // IssueDeferredDocumentPartialState.NotReady was emitted when issueDeferredDocument was called.

    // Case 3 Expected Result:
    // DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result with,
    // - successfullyIssuedDeferredDocuments = emptyList.
    // - failedIssuedDeferredDocuments = emptyList.
    @Test
    fun `Given Case 3, When tryIssuingDeferredDocumentsFlow is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.formatType
            val mockDeferredPendingName = mockedPendingPidUi.documentName

            val deferredDocuments: Map<DocumentId, FormatType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            val successData = DeferredDocumentDataDomain(
                documentId = mockDeferredPendingDocId,
                formatType = mockDeferredPendingType,
                docName = mockDeferredPendingName
            )

            mockIssueDeferredDocumentCall(
                docId = mockDeferredPendingDocId,
                response = IssueDeferredDocumentPartialState.NotReady(
                    deferredDocumentData = successData
                )
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                val expectedResult =
                    DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                        successfullyIssuedDeferredDocuments = emptyList(),
                        failedIssuedDeferredDocuments = emptyList()
                    )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // walletCoreDocumentsController.issueDeferredDocument() throws an exception with a message.

    // Case 4 Expected Result:
    // DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure state with exception's localized message.
    @Test
    fun `Given Case 4, When tryIssuingDeferredDocumentsFlow is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.formatType

            val deferredDocuments: Map<DocumentId, FormatType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            whenever(walletCoreDocumentsController.issueDeferredDocument(mockDeferredPendingDocId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }

    // Case 5:
    // walletCoreDocumentsController.issueDeferredDocument() throws an exception with no message.

    // Case 5 Expected Result:
    // DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure state with the generic error message.
    @Test
    fun `Given Case 5, When tryIssuingDeferredDocumentsFlow is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.formatType

            val deferredDocuments: Map<DocumentId, FormatType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            whenever(walletCoreDocumentsController.issueDeferredDocument(mockDeferredPendingDocId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }

    // Case 6:
    // emptyFlow was returned when issueDeferredDocument was called.

    // Case 6 Expected Result:
    // DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result state.
    @Test
    fun `Given Case 6, When tryIssuingDeferredDocumentsFlow is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.formatType

            val deferredDocuments: Map<DocumentId, FormatType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            whenever(walletCoreDocumentsController.issueDeferredDocument(mockDeferredPendingDocId))
                .thenReturn(emptyFlow())

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                val expectedResult =
                    DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                        successfullyIssuedDeferredDocuments = emptyList(),
                        failedIssuedDeferredDocuments = listOf(mockDeferredPendingDocId)
                    )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 7:
    // Empty state was returned when onFilterStateChange is collected.

    // Case 7 Expected Result:
    // DocumentInteractorFilterPartialState.FilterApplyResult state with empty documents.
    @Test
    fun `Given Case 7, When onFilterStateChange is called, Then Case 7 Expected Result is returned`() =
        coroutineRule.runTest {
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterListResult.FilterListEmptyResult(
                    updatedFilters = Filters.emptyFilters(), allDefaultFiltersAreSelected = false
                )
            )

            interactor.onFilterStateChange().runFlowTest {
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterApplyResult)
                assertTrue(state.documents.isEmpty())
            }
        }

    // Case 8:
    // Valid state with documents was returned when onFilterStateChange is collected.

    // Case 8 Expected Result:
    // DocumentInteractorFilterPartialState.FilterApplyResult state with correct data.
    @Test
    fun `Given Case 8, When onFilterStateChange is called, Then Case 8 Expected Result is returned`() =
        coroutineRule.runTest {
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterListResult.FilterApplyResult(
                    filteredList = FilterableList(items = listOf(mockFilterableItem)),
                    allDefaultFiltersAreSelected = false,
                    updatedFilters = Filters.emptyFilters()
                )
            )

            interactor.onFilterStateChange().runFlowTest {
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterApplyResult)
                assertEquals(state.documents.first().second.first().documentCategory.id, 1)
                assertEquals(
                    (state.documents.first().second.first().uiData.mainContentData as ListItemMainContentDataUi.Text).text,
                    "test"
                )
            }
        }

    // Case 9:
    // Updated filters state was returned when onFilterStateChange is collected.

    // Case 9 Expected Result:
    // DocumentInteractorFilterPartialState.FilterUpdateResult state with updated ui filters.
    @Test
    fun `Given Case 9, When onFilterStateChange is called, Then Case 9 Expected Result is returned`() =
        coroutineRule.runTest {
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterUpdateResult(updatedFilters = mockFilters)
            )

            interactor.onFilterStateChange().runFlowTest {
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterUpdateResult)
                assertEquals(state.filters.size, mockFilters.filterGroups.size)
                assertEquals(
                    (state.filters.first().header.mainContentData as ListItemMainContentDataUi.Text).text,
                    mockFilters.filterGroups.first().name
                )
                assertEquals(
                    state.filters.first().nestedItems.size,
                    mockFilters.filterGroups.first().filters.size
                )
                assertEquals(false, state.filters.first().isExpanded)
                val trailingContent = state.filters.first().header.trailingContentData
                val trailingIcon = trailingContent as ListItemTrailingContentDataUi.Icon
                assertEquals(
                    AppIcons.KeyboardArrowDown,
                    trailingIcon.iconData
                )
            }
        }

    //endregion

    //region getDocuments

    @Test
    fun `Given two issued documents, When getDocuments is called, Then Success with the documents is emitted`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            whenever(walletCoreDocumentsController.getMainPidDocument())
                .thenReturn(mockedFullDocuments[0])
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(walletCoreDocumentsController.isDocumentRevoked(anyString())).thenReturn(false)
            whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(org.mockito.kotlin.any()))
                .thenReturn(false)
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)
            whenever(resourceProvider.getString(org.mockito.kotlin.any()))
                .thenReturn("mocked-string")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<Int>(),
                    org.mockito.kotlin.any<Int>()
                )
            ).thenReturn("mocked-credentials-info")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<String>()
                )
            ).thenReturn("mocked-expiry-message")

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                state as DocumentInteractorGetDocumentsPartialState.Success
                assertEquals(true, state.shouldAllowUserInteraction)
                assertEquals(mockedFullDocuments.size, state.allDocuments.items.size)
            }
        }
    }

    @Test
    fun `Given a revoked document, When getDocuments is called, Then documentState is Revoked`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            whenever(walletCoreDocumentsController.getMainPidDocument())
                .thenReturn(mockedFullDocuments[0])
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(walletCoreDocumentsController.isDocumentRevoked(anyString())).thenReturn(true)
            whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(org.mockito.kotlin.any()))
                .thenReturn(false)
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)
            whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<Int>(),
                    org.mockito.kotlin.any<Int>()
                )
            ).thenReturn("mocked-credentials-info")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<String>()
                )
            ).thenReturn("mocked-expiry-message")

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                state as DocumentInteractorGetDocumentsPartialState.Success
                val firstItem = state.allDocuments.items.first()
                val payload = firstItem.payload as DocumentUi
                assertEquals(DocumentIssuanceStateUi.Revoked, payload.documentIssuanceState)
            }
        }
    }

    @Test
    fun `Given a low-on-credentials document, When getDocuments is called, Then TextWithIcon trailing is set`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            whenever(walletCoreDocumentsController.getMainPidDocument())
                .thenReturn(mockedFullDocuments[0])
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(walletCoreDocumentsController.isDocumentRevoked(anyString())).thenReturn(false)
            whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(org.mockito.kotlin.any()))
                .thenReturn(true)
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)
            whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<Int>(),
                    org.mockito.kotlin.any<Int>()
                )
            ).thenReturn("mocked-credentials-info")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<String>()
                )
            ).thenReturn("mocked-expiry-message")

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                state as DocumentInteractorGetDocumentsPartialState.Success
                val firstItem = state.allDocuments.items.first()
                val payload = firstItem.payload as DocumentUi
                assertTrue(payload.uiData.trailingContentData is eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi.TextWithIcon)
            }
        }
    }

    @Test
    fun `Given no main PID document, When getDocuments is called, Then shouldAllowUserInteraction is false`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(null)
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(emptyList())
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                state as DocumentInteractorGetDocumentsPartialState.Success
                assertEquals(false, state.shouldAllowUserInteraction)
                assertEquals(0, state.allDocuments.items.size)
            }
        }
    }

    @Test
    fun `Given getAllDocuments throws with message, When getDocuments is called, Then Failure is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(null)
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithMessage)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorGetDocumentsPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given getAllDocuments throws no message, When getDocuments is called, Then Failure with generic message is emitted`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(null)
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithNoMessage)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorGetDocumentsPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region onFilterStateChange filter-group mapping

    @Test
    fun `Given a FilterApplyResult with all four group types, When onFilterStateChange emits it, Then Checkbox and RadioButton trailing types are mapped for each`() {
        coroutineRule.runTest {
            // Given
            val multiple = FilterGroup.MultipleSelectionFilterGroup<DocumentsFilterableAttributes>(
                id = "multi",
                name = "Multi",
                filters = listOf(FilterItem(id = "m1", name = "M1", selected = true)),
                filterableAction = eu.europa.ec.businesslogic.validator.model.FilterMultipleAction { _, _ -> true },
            )
            val reversibleMultiple =
                FilterGroup.ReversibleMultipleSelectionFilterGroup<DocumentsFilterableAttributes>(
                    id = "rmulti",
                    name = "RMulti",
                    filters = listOf(FilterItem(id = "rm1", name = "RM1", selected = false)),
                    filterableAction = eu.europa.ec.businesslogic.validator.model.FilterMultipleAction { _, _ -> true },
                )
            val single = FilterGroup.SingleSelectionFilterGroup(
                id = "single",
                name = "Single",
                filters = listOf(FilterItem(id = "s1", name = "S1", selected = true)),
            )
            val reversibleSingle = FilterGroup.ReversibleSingleSelectionFilterGroup(
                id = "rsingle",
                name = "RSingle",
                filters = listOf(FilterItem(id = "rs1", name = "RS1", selected = false)),
            )

            val updatedFilters = Filters(
                filterGroups = listOf(multiple, reversibleMultiple, single, reversibleSingle),
                sortOrder = SortOrder.Ascending(isDefault = true),
            )

            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterListResult.FilterApplyResult(
                    filteredList = FilterableList(items = emptyList()),
                    allDefaultFiltersAreSelected = true,
                    updatedFilters = updatedFilters,
                )
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterApplyResult)
                state as DocumentInteractorFilterPartialState.FilterApplyResult

                val trailing = state.filters.flatMap { nested ->
                    nested.nestedItems.map { it.header.trailingContentData }
                }
                assertTrue(trailing.any { it is eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi.Checkbox })
                assertTrue(trailing.any { it is eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi.RadioButton })
            }
        }
    }

    @Test
    fun `Given a FilterUpdateResult with sort config, When onFilterStateChange emits it, Then only filter groups are exposed`() {
        coroutineRule.runTest {
            // Given
            val sort = FilterSort(
                id = eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_SORT_GROUP_ID,
                name = "Sort",
                filters = listOf(FilterItem(id = "sort", name = "Sort", selected = true)),
            )
            val single = FilterGroup.SingleSelectionFilterGroup(
                id = "single",
                name = "Single",
                filters = listOf(FilterItem(id = "s1", name = "S1", selected = true)),
            )
            val updatedFilters = Filters(
                filterGroups = listOf(single),
                sortOrder = SortOrder.Descending(),
                sort = sort,
            )
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterUpdateResult(updatedFilters = updatedFilters)
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterUpdateResult)
                state as DocumentInteractorFilterPartialState.FilterUpdateResult
                assertEquals(
                    listOf("single"),
                    state.filters.map { it.header.itemId }
                )
            }
        }
    }
    //endregion

    //region initializeFilters

    @Test
    fun `When initializeFilters is called, Then filterValidator#initializeValidator is invoked`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val list = FilterableList(items = emptyList())

        // When
        interactor.initializeFilters(filterableList = list)

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1))
            .initializeValidator(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.eq(list),
            )
    }
    //endregion

    //region getDocuments — UnsignedDocument & expired

    @Test
    fun `Given an UnsignedDocument among getAllDocuments, When getDocuments is called, Then it is mapped with Pending state`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            val unsignedDoc =
                org.mockito.kotlin.mock<eu.europa.ec.eudi.wallet.document.UnsignedDocument>()
            whenever(unsignedDoc.id).thenReturn("unsigned-id")
            whenever(unsignedDoc.name).thenReturn("Unsigned Doc")
            whenever(unsignedDoc.format).thenReturn(eu.europa.ec.testfeature.util.mockedMdocPidFormat)
            whenever(unsignedDoc.issuerMetadata).thenReturn(null)

            whenever(walletCoreDocumentsController.getMainPidDocument())
                .thenReturn(mockedFullDocuments[0])
            // Cast to List<Document> since both IssuedDocument and UnsignedDocument implement Document.
            @Suppress("UNCHECKED_CAST")
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments + listOf(unsignedDoc) as List<Document>)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(walletCoreDocumentsController.isDocumentRevoked(anyString())).thenReturn(false)
            whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(org.mockito.kotlin.any()))
                .thenReturn(false)
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)
            whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<Int>(),
                    org.mockito.kotlin.any<Int>()
                )
            ).thenReturn("mocked-credentials-info")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<String>()
                )
            ).thenReturn("mocked-expiry-message")

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                state as DocumentInteractorGetDocumentsPartialState.Success
                val unsignedItem = state.allDocuments.items.firstOrNull {
                    (it.payload as? DocumentUi)?.uiData?.itemId == "unsigned-id"
                }
                assertNotNull(unsignedItem)
                val payload = unsignedItem!!.payload as DocumentUi
                assertEquals(DocumentIssuanceStateUi.Pending, payload.documentIssuanceState)
            }
        }
    }

    @Test
    fun `Given an expired IssuedDocument, When getDocuments is called, Then documentIssuanceState is Failed`() {
        coroutineRule.runTest {
            // Given
            val mockedFullPid = getMockedFullPid()
            val pidFormat = mockedFullPid.format
            val pidData = mockedFullPid.data
            // Override validUntil to a past date so documentHasExpired evaluates true.
            val expiredPid =
                org.mockito.kotlin.mock<eu.europa.ec.eudi.wallet.document.IssuedDocument>()
            whenever(expiredPid.id).thenReturn("expired-pid-id")
            whenever(expiredPid.name).thenReturn("Expired PID")
            whenever(expiredPid.format).thenReturn(pidFormat)
            whenever(expiredPid.data).thenReturn(pidData)
            whenever(expiredPid.issuerMetadata).thenReturn(null)
            whenever(expiredPid.getValidUntil()).thenReturn(
                Result.success(java.time.Instant.parse("2020-01-01T00:00:00Z"))
            )
            whenever(expiredPid.credentialsCount()).thenReturn(3)
            whenever(expiredPid.initialCredentialsCount()).thenReturn(5)

            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(expiredPid)
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(listOf(expiredPid))
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(walletCoreDocumentsController.isDocumentRevoked(anyString())).thenReturn(false)
            whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(org.mockito.kotlin.any()))
                .thenReturn(false)
            whenever(resourceProvider.getLocale())
                .thenReturn(eu.europa.ec.testfeature.util.mockedDefaultLocale)
            whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<Int>(),
                    org.mockito.kotlin.any<Int>()
                )
            ).thenReturn("mocked-credentials-info")
            whenever(
                resourceProvider.getString(
                    org.mockito.kotlin.any(),
                    org.mockito.kotlin.any<String>()
                )
            ).thenReturn("mocked-expiry-message")

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                state as DocumentInteractorGetDocumentsPartialState.Success
                val payload = state.allDocuments.items.first().payload as DocumentUi
                assertEquals(DocumentIssuanceStateUi.Failed, payload.documentIssuanceState)
            }
        }
    }
    //endregion

    //region onFilterStateChange — non-DocumentUi payload

    @Test
    fun `Given a FilterApplyResult that contains a non-DocumentUi payload, When onFilterStateChange emits it, Then that item is filtered out`() {
        coroutineRule.runTest {
            // Given
            val foreignItem = FilterableItem(
                payload = ForeignDocPayload,
                attributes = object : FilterableAttributes {
                    override val searchTags: List<String> = emptyList()
                },
            )
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterListResult.FilterApplyResult(
                    filteredList = FilterableList(items = listOf(foreignItem)),
                    allDefaultFiltersAreSelected = true,
                    updatedFilters = Filters.emptyFilters(),
                )
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterApplyResult)
                state as DocumentInteractorFilterPartialState.FilterApplyResult
                val total = state.documents.sumOf { it.second.size }
                assertEquals(0, total)
            }
        }
    }

    private object ForeignDocPayload :
        eu.europa.ec.businesslogic.validator.model.FilterableItemPayload
    //endregion

    //region getFilters lambdas

    @Test
    fun `When getFilters is called, Then the expected static filter groups are returned`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")

        // When
        val filters = interactor.getFilters()

        // Then
        val ids = filters.filterGroups.map { it.id }
        assertEquals(
            listOf(
                eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_PERIOD_GROUP_ID,
                eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID,
                eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID,
                eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_STATE_GROUP_ID,
            ),
            ids
        )
        assertEquals(
            eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_SORT_GROUP_ID,
            filters.sort?.id
        )
    }

    @Test
    fun `When the sort filters' selectors are applied, Then they return name issuedDate and expiryDate respectively`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val attrs = documentsAttributes(
            name = "PID",
            issuedDate = java.time.Instant.parse("2026-01-01T00:00:00Z"),
            expiryDate = java.time.Instant.parse("2030-01-01T00:00:00Z"),
        )
        val sort = interactor.getFilters().sort!!

        @Suppress("UNCHECKED_CAST")
        val defaultSort =
            sort.filters[0].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Sort<eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes, String>

        @Suppress("UNCHECKED_CAST")
        val issuedSort =
            sort.filters[1].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Sort<eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes, java.time.Instant>

        @Suppress("UNCHECKED_CAST")
        val expirySort =
            sort.filters[2].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Sort<eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes, java.time.Instant>

        // When + Then
        assertEquals("pid", defaultSort.selector(attrs))
        assertEquals(attrs.issuedDate, issuedSort.selector(attrs))
        assertEquals(attrs.expiryDate, expirySort.selector(attrs))
    }

    @Test
    fun `When the issuer filter predicate is applied, Then it matches by issuer name`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val group = interactor.getFilters().filterGroups.first {
            it.id == eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val action =
            (group as FilterGroup.MultipleSelectionFilterGroup<eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes>)
                .filterableAction
        val acmeFilter =
            FilterItem(id = "Acme", name = "Acme", selected = true, isDefault = false)
        val otherFilter =
            FilterItem(id = "Other", name = "Other", selected = true, isDefault = false)

        // When + Then
        assertTrue(action.predicate(documentsAttributes(issuer = "Acme"), acmeFilter))
        assertTrue(!action.predicate(documentsAttributes(issuer = "Other"), acmeFilter))
        assertTrue(action.predicate(documentsAttributes(issuer = "Other"), otherFilter))
    }

    @Test
    fun `When the category filter predicate is applied, Then it matches by category id`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val group = interactor.getFilters().filterGroups.first {
            it.id == eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val action =
            (group as FilterGroup.MultipleSelectionFilterGroup<eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes>)
                .filterableAction
        val category = DocumentCategory.Government
        val matchFilter = FilterItem(
            id = category.id.toString(),
            name = "Government",
            selected = true,
            isDefault = false,
        )
        val nonMatchFilter = FilterItem(
            id = "9999",
            name = "Other",
            selected = true,
            isDefault = false,
        )

        // When + Then
        assertTrue(action.predicate(documentsAttributes(category = category), matchFilter))
        assertTrue(!action.predicate(documentsAttributes(category = category), nonMatchFilter))
    }

    @Test
    fun `When the state filter predicate is applied, Then VALID EXPIRED REVOKED and else arms all evaluate`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val group = interactor.getFilters().filterGroups.first {
            it.id == eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_STATE_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val action =
            (group as FilterGroup.MultipleSelectionFilterGroup<eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes>)
                .filterableAction
        val validFilter = FilterItem(
            id = eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_STATE_VALID,
            name = "Valid",
            selected = true,
            isDefault = true,
        )
        val expiredFilter = FilterItem(
            id = eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_STATE_EXPIRED,
            name = "Expired",
            selected = true,
            isDefault = false,
        )
        val revokedFilter = FilterItem(
            id = eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_STATE_REVOKED,
            name = "Revoked",
            selected = true,
            isDefault = false,
        )
        val otherFilter = FilterItem(
            id = "other",
            name = "Other",
            selected = true,
            isDefault = false,
        )
        val nullExpiry = documentsAttributes(expiryDate = null, isRevoked = false)
        val expired =
            documentsAttributes(
                expiryDate = java.time.Instant.parse("2020-01-01T00:00:00Z"),
                isRevoked = false,
            )
        val valid =
            documentsAttributes(
                expiryDate = java.time.Instant.parse("2099-01-01T00:00:00Z"),
                isRevoked = false,
            )
        val revoked = documentsAttributes(expiryDate = null, isRevoked = true)

        // When + Then
        assertTrue(action.predicate(valid, validFilter))
        assertTrue(action.predicate(nullExpiry, validFilter))
        assertTrue(!action.predicate(revoked, validFilter))

        assertTrue(action.predicate(expired, expiredFilter))
        assertTrue(!action.predicate(valid, expiredFilter))

        assertTrue(action.predicate(revoked, revokedFilter))
        assertTrue(!action.predicate(valid, revokedFilter))

        assertTrue(action.predicate(valid, otherFilter))
    }
    //endregion

    //region addDynamicFilters default parameter
    // The interface declares `filters: Filters = Filters.emptyFilters()`. Calling without
    // the filters argument exercises the default-parameter synthetic.
    @Test
    fun `When addDynamicFilters is called without filters, Then the default emptyFilters is used`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val documents = FilterableList(items = emptyList())

        // When
        val result = interactor.addDynamicFilters(documents = documents)

        // Then
        // Default filters is Filters.emptyFilters() which has empty filterGroups.
        assertEquals(0, result.filterGroups.size)
    }
    //endregion

    //region addDynamicFilters & period predicates

    @Test
    fun `Given a documents list with issuers and categories, When addDynamicFilters is called, Then the ISSUER and CATEGORY groups are populated`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val itemAcme = FilterableItem(
            payload = stubDocumentUi(),
            attributes = documentsAttributes(
                issuer = "Acme",
                category = eu.europa.ec.corelogic.model.DocumentCategory.Government,
            ),
        )
        val itemSchool = FilterableItem(
            payload = stubDocumentUi(),
            attributes = documentsAttributes(
                issuer = "School",
                category = eu.europa.ec.corelogic.model.DocumentCategory.Education,
            ),
        )
        val documents = FilterableList(items = listOf(itemAcme, itemSchool))
        val initialFilters = interactor.getFilters()

        // When
        val result = interactor.addDynamicFilters(documents, initialFilters)

        // Then
        val issuerGroup = result.filterGroups.first {
            it.id == eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID
        }
        val categoryGroup = result.filterGroups.first {
            it.id == eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID
        }
        assertEquals(2, issuerGroup.filters.size)
        assertEquals(2, categoryGroup.filters.size)
    }

    @Test
    fun `When the period filter predicates are applied, Then default issuance and expiry-window predicates evaluate as expected`() {
        // Given
        whenever(resourceProvider.getString(org.mockito.kotlin.any())).thenReturn("mocked")
        val periodGroup = interactor.getFilters().filterGroups.first {
            it.id == eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds.FILTER_BY_PERIOD_GROUP_ID
        }

        val defaultPred =
            periodGroup.filters[0].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Filter<DocumentsFilterableAttributes>
        val next7Pred =
            periodGroup.filters[1].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Filter<DocumentsFilterableAttributes>
        val next30Pred =
            periodGroup.filters[2].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Filter<DocumentsFilterableAttributes>
        val beyond30Pred =
            periodGroup.filters[3].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Filter<DocumentsFilterableAttributes>
        val expiredPred =
            periodGroup.filters[4].filterableAction as eu.europa.ec.businesslogic.validator.model.FilterAction.Filter<DocumentsFilterableAttributes>

        val dummyFilter = eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem(
            id = "x", name = "x", selected = true, isDefault = false,
        )

        val expired = documentsAttributes(
            expiryDate = java.time.Instant.parse("2020-01-01T00:00:00Z"),
        )
        val tomorrow = documentsAttributes(
            expiryDate = java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS),
        )
        val nextMonth = documentsAttributes(
            expiryDate = java.time.Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS),
        )
        val farFuture = documentsAttributes(
            expiryDate = java.time.Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS),
        )

        // When + Then
        assertTrue(defaultPred.predicate(expired, dummyFilter))
        assertTrue(next7Pred.predicate(tomorrow, dummyFilter))
        assertTrue(!next7Pred.predicate(farFuture, dummyFilter))
        assertTrue(next30Pred.predicate(nextMonth, dummyFilter))
        assertTrue(!next30Pred.predicate(farFuture, dummyFilter))
        assertTrue(beyond30Pred.predicate(farFuture, dummyFilter))
        assertTrue(!beyond30Pred.predicate(tomorrow, dummyFilter))
        assertTrue(expiredPred.predicate(expired, dummyFilter))
        assertTrue(!expiredPred.predicate(farFuture, dummyFilter))
    }

    private fun stubDocumentUi(): DocumentUi = DocumentUi(
        documentIssuanceState = DocumentIssuanceStateUi.Issued,
        uiData = ListItemDataUi(
            itemId = "any",
            mainContentData = ListItemMainContentDataUi.Text("any"),
        ),
        documentIdentifier = DocumentIdentifier.MdocPid,
        documentCategory = eu.europa.ec.corelogic.model.DocumentCategory.Government,
    )
    //endregion

    //region filterValidator delegation
    // These tests verify the simple delegation methods to filterValidator.

    @Test
    fun `When updateLists is called, Then filterValidator#updateLists is invoked`() {
        // Given
        val list = FilterableList(items = emptyList())

        // When
        interactor.updateLists(filterableList = list)

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1)).updateLists(list)
    }

    @Test
    fun `When applySearch is called, Then filterValidator#applySearch is invoked`() {
        // When
        interactor.applySearch(query = "abc")

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1)).applySearch("abc")
    }

    @Test
    fun `When revertFilters is called, Then filterValidator#revertFilters is invoked`() {
        // When
        interactor.revertFilters()

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1)).revertFilters()
    }

    @Test
    fun `When updateFilter is called, Then filterValidator#updateFilter is invoked`() {
        // When
        interactor.updateFilter(filterGroupId = "groupId", filterId = "filterId")

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1))
            .updateFilter("groupId", "filterId")
    }

    @Test
    fun `When updateSort is called, Then filterValidator#updateSort is invoked`() {
        // When
        interactor.updateSort(filterId = "sortId")

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1))
            .updateSort("sortId")
    }

    @Test
    fun `When updateSortOrder is called, Then filterValidator#updateSortOrder is invoked`() {
        // Given
        val order = SortOrder.Ascending(isDefault = false)

        // When
        interactor.updateSortOrder(sortOrder = order)

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1))
            .updateSortOrder(order)
    }

    @Test
    fun `When applyFilters is called, Then filterValidator#applyFilters is invoked`() {
        // When
        interactor.applyFilters()

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1)).applyFilters()
    }

    @Test
    fun `When resetFilters is called, Then filterValidator#resetFilters is invoked`() {
        // When
        interactor.resetFilters()

        // Then
        org.mockito.kotlin.verify(filterValidator, org.mockito.kotlin.times(1)).resetFilters()
    }
    //endregion

    //region Mock Calls of the Dependencies
    private fun mockDeleteDocumentCall(response: DeleteDocumentPartialState) {
        whenever(walletCoreDocumentsController.deleteDocument(anyString()))
            .thenReturn(response.toFlow())
    }

    private fun mockIssueDeferredDocumentCall(
        docId: DocumentId,
        response: IssueDeferredDocumentPartialState,
    ) {
        whenever(walletCoreDocumentsController.issueDeferredDocument(docId))
            .thenReturn(response.toFlow())
    }

    private fun mockOnFilterChangedEvent(response: FilterValidatorPartialState) {
        whenever(filterValidator.onFilterStateChange())
            .thenReturn(response.toFlow())
    }
    //endregion

    //region Mock domain models
    private val mockFilterableItem = FilterableItem(
        payload = DocumentUi(
            documentIssuanceState = DocumentIssuanceStateUi.Pending,
            uiData = ListItemDataUi(
                itemId = "sumo",
                mainContentData = ListItemMainContentDataUi.Text("test"),
                overlineText = null,
                supportingText = null,
                leadingContentData = null,
                trailingContentData = null
            ),
            documentIdentifier = DocumentIdentifier.MdocPid,
            documentCategory = DocumentCategory.Government,
        ), attributes = object : FilterableAttributes {
            override val searchTags: List<String>
                get() = listOf("docName", "issuerName")
        })

    private val mockFilters = Filters(
        filterGroups = listOf(
            FilterGroup.SingleSelectionFilterGroup(
                id = "group",
                name = "Group",
                filters = listOf(
                    FilterItem(
                        id = "f1",
                        name = "Filter1",
                        selected = true,
                        isDefault = true
                    ),
                    FilterItem(
                        id = "f2",
                        name = "Filter 2",
                        selected = false,
                        isDefault = false
                    )
                )
            )
        ), sortOrder = SortOrder.Ascending()
    )
    //endregion
}
