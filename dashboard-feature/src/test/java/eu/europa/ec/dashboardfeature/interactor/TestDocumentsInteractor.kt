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
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterSort
import eu.europa.ec.businesslogic.validator.model.FilterableAttributes
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableItemPayload
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DocumentCategories
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.worker.DeferredIssuanceScheduler
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentFilterIds
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentUi
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentsFilterableAttributes
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.copy
import eu.europa.ec.testfeature.util.getMockedFullDocuments
import eu.europa.ec.testfeature.util.getMockedFullPid
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedMdocPidFormat
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
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

    @Mock
    private lateinit var prefKeys: PrefKeys

    @Mock
    private lateinit var deferredIssuanceScheduler: DeferredIssuanceScheduler

    private lateinit var interactor: DocumentsInteractor

    private lateinit var closeable: AutoCloseable

    private lateinit var mockDocumentId: String
    private lateinit var mockDocumentName: String

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        runBlocking {
            whenever(deferredIssuanceScheduler.getFailedDocumentIds()).thenReturn(
                emptySet()
            )
        }

        interactor = DocumentsInteractorImpl(
            resourceProvider = resourceProvider,
            walletCoreDocumentsController = walletCoreDocumentsController,
            filterValidator = filterValidator,
            configLogic = configLogic,
            prefKeys = prefKeys,
            deferredIssuanceScheduler = deferredIssuanceScheduler,
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
        issuedDate: Instant? = null,
        expiryDate: Instant? = null,
        issuer: String = "Issuer",
        category: DocumentCategory = DocumentCategory.Government,
        isRevoked: Boolean = false,
    ): DocumentsFilterableAttributes =
        DocumentsFilterableAttributes(
            searchTags = emptyList(),
            name = name,
            expiryDate = expiryDate,
            issuedDate = issuedDate,
            issuer = issuer,
            category = category,
            isRevoked = isRevoked,
        )

    //region deleteDocument
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
            verify(walletCoreDocumentsController).deleteDocument(mockDocumentId)
            verify(walletCoreDocumentsController, never()).deleteAllDocuments()
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


    //region getDocuments

    @Test
    fun `Given two issued documents, When getDocuments is called, Then Success with the documents is emitted`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentsBaseCalls(
                documents = mockedFullDocuments,
                mainPid = mockedFullDocuments[0],
            )
            mockGetDocumentsResourceStrings()

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                assertEquals(true, state.shouldAllowUserInteraction)
                assertEquals(mockedFullDocuments.size, state.allDocuments.items.size)
            }
        }
    }

    @Test
    fun `Given a suspended document, When getDocuments is called, Then it is labelled suspended and stays blocked`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentsBaseCalls(
                documents = getMockedFullDocuments(),
                mainPid = getMockedFullDocuments()[0],
                documentIsRevoked = true,
                documentIsSuspended = true,
            )
            mockGetDocumentsResourceStrings()
            whenever(resourceProvider.getString(R.string.dashboard_document_suspended)).thenReturn("Suspended")

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                val payload = state.allDocuments.items.first().payload as DocumentUi
                assertEquals(DocumentIssuanceStateUi.Revoked, payload.documentIssuanceState)
                assertEquals(
                    "Suspended",
                    (payload.uiData.supportingContentData as ListItemSupportingContentDataUi.Text).text
                )
            }
        }
    }

    @Test
    fun `Given a revoked document, When getDocuments is called, Then documentState is Revoked`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentsBaseCalls(
                documents = mockedFullDocuments,
                mainPid = mockedFullDocuments[0],
                documentIsRevoked = true,
            )
            mockGetDocumentsResourceStrings()

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                val firstItem = state.allDocuments.items.first()
                val payload = firstItem.payload as DocumentUi
                assertEquals(DocumentIssuanceStateUi.Revoked, payload.documentIssuanceState)
            }
        }
    }

    @Test
    fun `Given a low-on-credentials document and show batch issuance counter preference is true, When getDocuments is called, Then TextWithIcon trailing is set`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            mockShowBatchIssuanceCounterPreference(response = true)
            mockGetDocumentsBaseCalls(
                documents = mockedFullDocuments,
                mainPid = mockedFullDocuments[0],
                documentLowOnCredentials = true,
            )
            mockGetDocumentsResourceStrings()

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                val firstItem = state.allDocuments.items.first()
                val payload = firstItem.payload as DocumentUi
                assertTrue(payload.uiData.trailingContentData is ListItemTrailingContentDataUi.TextWithIcon)
            }
        }
    }

    @Test
    fun `Given show batch issuance counter preference is false, When getDocuments is called, Then counter text is hidden`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            mockShowBatchIssuanceCounterPreference(response = false)
            mockGetDocumentsBaseCalls(
                documents = mockedFullDocuments,
                mainPid = mockedFullDocuments[0],
            )
            mockGetDocumentsResourceStrings()

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                val firstItem = state.allDocuments.items.first()
                val payload = firstItem.payload as DocumentUi
                assertTrue(payload.uiData.trailingContentData is ListItemTrailingContentDataUi.Icon)
            }
        }
    }

    @Test
    fun `Given no main PID document, When getDocuments is called, Then shouldAllowUserInteraction is false`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(null)
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(emptyList())
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(resourceProvider.getLocale())
                .thenReturn(mockedDefaultLocale)

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                assertEquals(false, state.shouldAllowUserInteraction)
                assertEquals(0, state.allDocuments.items.size)
            }
        }
    }

    @Test
    fun `Given getAllDocuments throws with message, When getDocuments is called, Then Failure is emitted`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(null)
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithMessage)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(resourceProvider.getLocale())
                .thenReturn(mockedDefaultLocale)

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
            mockShowBatchIssuanceCounterPreference(response = true)
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(null)
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithNoMessage)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(resourceProvider.getLocale())
                .thenReturn(mockedDefaultLocale)

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

    /**
     * Case 7:
     * 1. No document matches the applied filters.
     *
     * Case 7 Expected Result:
     * The interactor emits an applied result with an empty document list.
     */
    @Test
    fun `Given Case 7, When onFilterStateChange is called, Then Case 7 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterListResult.FilterListEmptyResult(
                    updatedFilters = Filters.emptyFilters(),
                    allDefaultFiltersAreSelected = false,
                )
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                assertEquals(
                    DocumentInteractorFilterPartialState.FilterApplyResult(
                        documents = emptyList(),
                        filters = emptyList(),
                        allDefaultFiltersAreSelected = false,
                    ),
                    awaitItem(),
                )
            }
        }

    /**
     * Case 8:
     * 1. The validator returns an Education document followed by a Government document.
     *
     * Case 8 Expected Result:
     * The document list uses category order and preserves the contents of each matching document.
     */
    @Test
    fun `Given Case 8, When onFilterStateChange is called, Then Case 8 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val educationDocument =
                stubDocumentUi().copy(documentCategory = DocumentCategory.Education)
            val educationItem = mockFilterableItem.copy(payload = educationDocument)
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterListResult.FilterApplyResult(
                    filteredList = FilterableList(
                        items = listOf(
                            educationItem,
                            mockFilterableItem
                        )
                    ),
                    allDefaultFiltersAreSelected = false,
                    updatedFilters = Filters.emptyFilters(),
                )
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterApplyResult)
                assertEquals(
                    listOf(
                        DocumentCategory.Government to listOf(mockFilterableItem.payload),
                        DocumentCategory.Education to listOf(educationDocument),
                    ),
                    state.documents,
                )
            }
        }

    /**
     * Case 9:
     * 1. The validator updates a group with two filter options.
     *
     * Case 9 Expected Result:
     * The group keeps its name and options and starts collapsed with a down arrow.
     */
    @Test
    fun `Given Case 9, When onFilterStateChange is called, Then Case 9 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val filterGroup = mockFilters.filterGroups.single()
            mockOnFilterChangedEvent(
                FilterValidatorPartialState.FilterUpdateResult(updatedFilters = mockFilters)
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorFilterPartialState.FilterUpdateResult)
                val group = state.filters.single()
                assertFalse(group.isExpanded)
                assertEquals(filterGroup.id, group.header.itemId)
                assertEquals(
                    ListItemMainContentDataUi.Text(filterGroup.name),
                    group.header.mainContentData
                )
                assertEquals(
                    ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown),
                    group.header.trailingContentData,
                )
                assertEquals(
                    filterGroup.filters.map { filter ->
                        filter.id to ListItemMainContentDataUi.Text(
                            filter.name
                        )
                    },
                    group.nestedItems.map { item ->
                        item.header.itemId to item.header.mainContentData
                    },
                )
            }
        }

    @Test
    fun `Given a FilterApplyResult with all four group types, When onFilterStateChange emits it, Then Checkbox and RadioButton trailing types are mapped for each`() {
        coroutineRule.runTest {
            // Given
            val multiple = FilterGroup.MultipleSelectionFilterGroup<DocumentsFilterableAttributes>(
                id = "multi",
                name = "Multi",
                filters = listOf(FilterItem(id = "m1", name = "M1", selected = true)),
                filterableAction = FilterMultipleAction { _, _ -> true },
            )
            val reversibleMultiple =
                FilterGroup.ReversibleMultipleSelectionFilterGroup<DocumentsFilterableAttributes>(
                    id = "rmulti",
                    name = "RMulti",
                    filters = listOf(FilterItem(id = "rm1", name = "RM1", selected = false)),
                    filterableAction = FilterMultipleAction { _, _ -> true },
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

                val trailing = state.filters.flatMap { nested ->
                    nested.nestedItems.map { it.header.trailingContentData }
                }
                assertTrue(trailing.any { it is ListItemTrailingContentDataUi.Checkbox })
                assertTrue(trailing.any { it is ListItemTrailingContentDataUi.RadioButton })
            }
        }
    }

    @Test
    fun `Given a FilterUpdateResult with sort config, When onFilterStateChange emits it, Then only filter groups are exposed`() {
        coroutineRule.runTest {
            // Given
            val sort = FilterSort(
                id = DocumentFilterIds.FILTER_SORT_GROUP_ID,
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
    fun `Given a filterable list, When initializeFilters is called, Then filterValidator#initializeValidator is invoked`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val list = FilterableList(items = emptyList())

        // When
        interactor.initializeFilters(filterableList = list)

        // Then
        verify(filterValidator, times(1))
            .initializeValidator(
                any(),
                eq(list),
            )
    }
    //endregion

    //region getDocuments — UnsignedDocument & expired

    /**
     * Case 6:
     * 1. The scheduler reports a failure for only one of two retained Deferred documents.
     *
     * Case 6 Expected Result:
     * Only the ID in the current failure snapshot renders Failed.
     */
    @Test
    fun `Given a failure for one ID, When documents load, Then Failed and Pending are rendered independently`() =
        coroutineRule.runTest {
            // Given
            val failedId = "failed"
            val pendingId = "pending"
            val failed = mock<DeferredDocument>()
            whenever(failed.id).thenReturn(failedId)
            whenever(failed.name).thenReturn("Failed")
            whenever(failed.format).thenReturn(mockedMdocPidFormat)
            val pending = mock<DeferredDocument>()
            whenever(pending.id).thenReturn(pendingId)
            whenever(pending.name).thenReturn("Pending")
            whenever(pending.format).thenReturn(mockedMdocPidFormat)
            mockShowBatchIssuanceCounterPreference(false)
            mockGetDocumentsBaseCalls(listOf(failed, pending), mainPid = null)
            mockGetDocumentsResourceStrings()
            whenever(deferredIssuanceScheduler.getFailedDocumentIds()).thenReturn(setOf(failedId))

            // When
            interactor.getDocuments().runFlowTest {
                val state = awaitItem() as DocumentInteractorGetDocumentsPartialState.Success

                // Then
                val states = state.allDocuments.items.map { item -> item.payload as DocumentUi }
                    .associate { document ->
                        document.uiData.itemId to document.documentIssuanceState
                    }
                assertEquals(DocumentIssuanceStateUi.Failed, states[failedId])
                assertEquals(DocumentIssuanceStateUi.Pending, states[pendingId])
            }
        }

    @Test
    fun `Given an UnsignedDocument among getAllDocuments, When getDocuments is called, Then it is mapped with Pending state`() {
        coroutineRule.runTest {
            // Given
            val mockedFullDocuments = getMockedFullDocuments()
            mockShowBatchIssuanceCounterPreference(response = true)
            val unsignedDoc =
                mock<UnsignedDocument>()
            whenever(unsignedDoc.id).thenReturn("unsigned-id")
            whenever(unsignedDoc.name).thenReturn("Unsigned Doc")
            whenever(unsignedDoc.format).thenReturn(mockedMdocPidFormat)
            whenever(unsignedDoc.issuerMetadata).thenReturn(null)

            whenever(walletCoreDocumentsController.getMainPidDocument())
                .thenReturn(mockedFullDocuments[0])
            @Suppress("UNCHECKED_CAST")
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments + listOf(unsignedDoc) as List<Document>)
            whenever(walletCoreDocumentsController.getAllDocumentCategories())
                .thenReturn(DocumentCategories(value = emptyMap()))
            whenever(walletCoreDocumentsController.isDocumentRevoked(anyString())).thenReturn(false)
            whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(any()))
                .thenReturn(false)
            whenever(resourceProvider.getLocale())
                .thenReturn(mockedDefaultLocale)
            whenever(resourceProvider.getString(any())).thenReturn("mocked")
            whenever(
                resourceProvider.getString(
                    any(),
                    any<Int>(),
                    any<Int>()
                )
            ).thenReturn("mocked-credentials-info")
            whenever(
                resourceProvider.getString(
                    any(),
                    any<String>()
                )
            ).thenReturn("mocked-expiry-message")

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                val unsignedItem = state.allDocuments.items.firstOrNull {
                    (it.payload as? DocumentUi)?.uiData?.itemId == "unsigned-id"
                }
                assertNotNull(unsignedItem)
                val payload = unsignedItem!!.payload as DocumentUi
                assertEquals(DocumentIssuanceStateUi.Pending, payload.documentIssuanceState)
            }
        }
    }

    /**
     * Given a PID whose credential expired in the past.
     */
    @Test
    fun `Given an expired IssuedDocument, When getDocuments is called, Then documentIssuanceState is Expired`() {
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)
            val expiredPid = getMockedFullPid().copy(
                validUntil = Result.success(Instant.parse("2020-01-01T00:00:00Z")),
            )
            mockGetDocumentsBaseCalls(
                documents = listOf(expiredPid),
                mainPid = expiredPid,
            )
            mockGetDocumentsResourceStrings()

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is DocumentInteractorGetDocumentsPartialState.Success)
                val payload = state.allDocuments.items.first().payload as DocumentUi
                assertEquals(DocumentIssuanceStateUi.Expired, payload.documentIssuanceState)
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
                val total = state.documents.sumOf { it.second.size }
                assertEquals(0, total)
            }
        }
    }

    private object ForeignDocPayload : FilterableItemPayload
    //endregion

    //region getFilters lambdas

    @Test
    fun `Given the filter strings, When getFilters is called, Then the expected static filter groups are returned`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")

        // When
        val filters = interactor.getFilters()

        // Then
        val ids = filters.filterGroups.map { it.id }
        assertEquals(
            listOf(
                DocumentFilterIds.FILTER_BY_PERIOD_GROUP_ID,
                DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID,
                DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID,
                DocumentFilterIds.FILTER_BY_STATE_GROUP_ID,
            ),
            ids
        )
        assertEquals(
            DocumentFilterIds.FILTER_SORT_GROUP_ID,
            filters.sort?.id
        )
    }

    @Test
    fun `Given the default sort filter, When its selector is applied, Then it returns the normalized document name`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val attrs = documentsAttributes(
            name = "PID",
        )
        val sort = interactor.getFilters().sort!!

        @Suppress("UNCHECKED_CAST")
        val defaultSort =
            sort.filters[0].filterableAction as FilterAction.Sort<DocumentsFilterableAttributes, String>

        // Then
        assertEquals(
            listOf(DocumentFilterIds.FILTER_SORT_DEFAULT),
            sort.filters.map { it.id },
        )
        assertEquals("pid", defaultSort.selector(attrs))
    }

    @Test
    fun `Given issuer filter items, When the issuer filter predicate is applied, Then it matches by issuer name`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val group = interactor.getFilters().filterGroups.first {
            it.id == DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val action =
            (group as FilterGroup.MultipleSelectionFilterGroup<DocumentsFilterableAttributes>)
                .filterableAction
        val acmeFilter =
            FilterItem(id = "Acme", name = "Acme", selected = true, isDefault = false)
        val otherFilter =
            FilterItem(id = "Other", name = "Other", selected = true, isDefault = false)

        // Then
        assertTrue(action.predicate(documentsAttributes(issuer = "Acme"), acmeFilter))
        assertTrue(!action.predicate(documentsAttributes(issuer = "Other"), acmeFilter))
        assertTrue(action.predicate(documentsAttributes(issuer = "Other"), otherFilter))
    }

    @Test
    fun `Given category filter items, When the category filter predicate is applied, Then it matches by category id`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val group = interactor.getFilters().filterGroups.first {
            it.id == DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val action =
            (group as FilterGroup.MultipleSelectionFilterGroup<DocumentsFilterableAttributes>)
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

        // Then
        assertTrue(action.predicate(documentsAttributes(category = category), matchFilter))
        assertTrue(!action.predicate(documentsAttributes(category = category), nonMatchFilter))
    }

    @Test
    fun `Given valid, expired, revoked and other filter items, When the state filter predicate is applied, Then the valid, expired, revoked and else arms all evaluate`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val group = interactor.getFilters().filterGroups.first {
            it.id == DocumentFilterIds.FILTER_BY_STATE_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val action =
            (group as FilterGroup.MultipleSelectionFilterGroup<DocumentsFilterableAttributes>)
                .filterableAction
        val validFilter = FilterItem(
            id = DocumentFilterIds.FILTER_BY_STATE_VALID,
            name = "Valid",
            selected = true,
            isDefault = true,
        )
        val expiredFilter = FilterItem(
            id = DocumentFilterIds.FILTER_BY_STATE_EXPIRED,
            name = "Expired",
            selected = true,
            isDefault = false,
        )
        val revokedFilter = FilterItem(
            id = DocumentFilterIds.FILTER_BY_STATE_REVOKED,
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
                expiryDate = Instant.parse("2020-01-01T00:00:00Z"),
                isRevoked = false,
            )
        val valid =
            documentsAttributes(
                expiryDate = Instant.parse("2099-01-01T00:00:00Z"),
                isRevoked = false,
            )
        val revoked = documentsAttributes(expiryDate = null, isRevoked = true)

        // Then
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
    /**
     * The interface declares `filters: Filters = Filters.emptyFilters()`. Calling without
     * the filters argument exercises the default-parameter synthetic.
     * The default filters is Filters.emptyFilters(), which has empty filterGroups.
     */
    @Test
    fun `Given documents and no filters argument, When addDynamicFilters is called, Then the default emptyFilters is used`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val documents = FilterableList(items = emptyList())

        // When
        val result = interactor.addDynamicFilters(documents = documents)

        // Then
        assertEquals(0, result.filterGroups.size)
    }
    //endregion

    //region addDynamicFilters & period predicates

    @Test
    fun `Given a documents list with issuers and categories, When addDynamicFilters is called, Then the ISSUER and CATEGORY groups are populated`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val itemAcme = FilterableItem(
            payload = stubDocumentUi(),
            attributes = documentsAttributes(
                issuer = "Acme",
                category = DocumentCategory.Government,
            ),
        )
        val itemSchool = FilterableItem(
            payload = stubDocumentUi(),
            attributes = documentsAttributes(
                issuer = "School",
                category = DocumentCategory.Education,
            ),
        )
        val documents = FilterableList(items = listOf(itemAcme, itemSchool))
        val initialFilters = interactor.getFilters()

        // When
        val result = interactor.addDynamicFilters(documents, initialFilters)

        // Then
        val issuerGroup = result.filterGroups.first {
            it.id == DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID
        }
        val categoryGroup = result.filterGroups.first {
            it.id == DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID
        }
        assertEquals(2, issuerGroup.filters.size)
        assertEquals(2, categoryGroup.filters.size)
    }

    @Test
    @Suppress("UNCHECKED_CAST") // FilterAction.Filter<T> is erased at runtime — the test asserts the predicate type from the filter group definition.
    fun `Given documents expiring at different times, When the period filter predicates are applied, Then the default issuance and expiry-window predicates evaluate as expected`() {
        // Given
        whenever(resourceProvider.getString(any())).thenReturn("mocked")
        val periodGroup = interactor.getFilters().filterGroups.first {
            it.id == DocumentFilterIds.FILTER_BY_PERIOD_GROUP_ID
        }

        val defaultPred =
            periodGroup.filters[0].filterableAction as FilterAction.Filter<DocumentsFilterableAttributes>
        val next7Pred =
            periodGroup.filters[1].filterableAction as FilterAction.Filter<DocumentsFilterableAttributes>
        val next30Pred =
            periodGroup.filters[2].filterableAction as FilterAction.Filter<DocumentsFilterableAttributes>
        val beyond30Pred =
            periodGroup.filters[3].filterableAction as FilterAction.Filter<DocumentsFilterableAttributes>
        val expiredPred =
            periodGroup.filters[4].filterableAction as FilterAction.Filter<DocumentsFilterableAttributes>

        val dummyFilter = FilterItem(
            id = "x", name = "x", selected = true, isDefault = false,
        )

        val expired = documentsAttributes(
            expiryDate = Instant.parse("2020-01-01T00:00:00Z"),
        )
        val tomorrow = documentsAttributes(
            expiryDate = Instant.now().plus(1, ChronoUnit.DAYS),
        )
        val nextMonth = documentsAttributes(
            expiryDate = Instant.now().plus(20, ChronoUnit.DAYS),
        )
        val farFuture = documentsAttributes(
            expiryDate = Instant.now().plus(365, ChronoUnit.DAYS),
        )

        // Then
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
        documentCategory = DocumentCategory.Government,
    )
    //endregion

    //region filterValidator delegation
    // These tests verify the simple delegation methods to filterValidator.

    @Test
    fun `Given a search query, When applySearch is called, Then filterValidator#applySearch is invoked`() {
        // When
        interactor.applySearch(query = "abc")

        // Then
        verify(filterValidator, times(1)).applySearch("abc")
    }

    @Test
    fun `Given a filter validator, When revertFilters is called, Then filterValidator#revertFilters is invoked`() {
        // When
        interactor.revertFilters()

        // Then
        verify(filterValidator, times(1)).revertFilters()
    }

    @Test
    fun `Given a group id and a filter id, When updateFilter is called, Then filterValidator#updateFilter is invoked`() {
        // When
        interactor.updateFilter(filterGroupId = "groupId", filterId = "filterId")

        // Then
        verify(filterValidator, times(1))
            .updateFilter("groupId", "filterId")
    }

    @Test
    fun `Given a sort id, When updateSort is called, Then filterValidator#updateSort is invoked`() {
        // When
        interactor.updateSort(filterId = "sortId")

        // Then
        verify(filterValidator, times(1))
            .updateSort("sortId")
    }

    @Test
    fun `Given a filter validator, When applyFilters is called, Then filterValidator#applyFilters is invoked`() {
        // When
        interactor.applyFilters()

        // Then
        verify(filterValidator, times(1)).applyFilters()
    }

    @Test
    fun `Given a filter validator, When resetFilters is called, Then filterValidator#resetFilters is invoked`() {
        // When
        interactor.resetFilters()

        // Then
        verify(filterValidator, times(1)).resetFilters()
    }
    //endregion

    //region Mock Calls of the Dependencies
    private fun mockShowBatchIssuanceCounterPreference(response: Boolean) {
        whenever(suspend { prefKeys.getShowBatchIssuanceCounter() }).thenReturn(response)
    }

    private suspend fun mockGetDocumentsBaseCalls(
        documents: List<Document>,
        mainPid: IssuedDocument?,
        documentIsRevoked: Boolean = false,
        documentLowOnCredentials: Boolean = false,
        documentIsSuspended: Boolean = false,
    ) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(mainPid)
        whenever(walletCoreDocumentsController.getAllDocuments())
            .thenReturn(documents)
        whenever(walletCoreDocumentsController.getAllDocumentCategories())
            .thenReturn(DocumentCategories(value = emptyMap()))
        whenever(walletCoreDocumentsController.isDocumentRevoked(anyString()))
            .thenReturn(documentIsRevoked)
        whenever(walletCoreDocumentsController.isDocumentSuspended(anyString()))
            .thenReturn(documentIsSuspended)
        whenever(walletCoreDocumentsController.isDocumentLowOnCredentials(any()))
            .thenReturn(documentLowOnCredentials)
    }

    private fun mockGetDocumentsResourceStrings(
        genericString: String = "mocked",
        credentialsInfoText: String = "mocked-credentials-info",
        expiryMessage: String = "mocked-expiry-message",
    ) {
        whenever(resourceProvider.getLocale())
            .thenReturn(mockedDefaultLocale)
        whenever(resourceProvider.getString(any()))
            .thenReturn(genericString)
        whenever(
            resourceProvider.getString(
                any(),
                any<Int>(),
                any<Int>()
            )
        ).thenReturn(credentialsInfoText)
        whenever(
            resourceProvider.getString(
                any(),
                any<String>()
            )
        ).thenReturn(expiryMessage)
    }

    private fun mockDeleteDocumentCall(response: DeleteDocumentPartialState) {
        whenever(walletCoreDocumentsController.deleteDocument(anyString()))
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
                supportingContentData = null,
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