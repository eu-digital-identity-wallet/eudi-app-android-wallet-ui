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

package eu.europa.ec.dashboardfeature.ui.documents.list

import androidx.lifecycle.ViewModelStore
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorFilterPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorGetDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentUi
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TestDocumentsViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var interactor: DocumentsInteractor

    @Mock
    private lateinit var resources: ResourceProvider

    @Mock
    private lateinit var uiSerializer: UiSerializer

    private val store = ViewModelStore()

    private val filterResults = MutableSharedFlow<DocumentInteractorFilterPartialState>()

    private val unseenDeferredIssuanceIds = MutableStateFlow(emptySet<String>())

    private val effects = mutableListOf<Effect>()

    private lateinit var viewModel: DocumentsViewModel

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        whenever(interactor.unseenDeferredIssuanceIds).thenReturn(unseenDeferredIssuanceIds)
        whenever(interactor.onFilterStateChange()).thenReturn(filterResults)
        whenever(interactor.getDocuments()).thenReturn(emptyFlow())
        whenever(interactor.deleteDocument(any())).thenReturn(emptyFlow())
        viewModel = DocumentsViewModel(
            interactor = interactor,
            resourceProvider = resources,
            uiSerializer = uiSerializer,
        )
        store.put("documents", viewModel)
        coroutineRule.testScope.backgroundScope.launch { viewModel.effect.toList(effects) }
        viewModel.handleEvents(Event.Init)
        runCurrent()
    }

    @After
    fun after() {
        try {
            store.clear()
            coroutineRule.testScope.runCurrent()
        } finally {
            closeable.close()
        }
    }

    //region Filters

    @Test
    fun `Given Filters closed, When an apply result arrives, Then the list is replaced`() =
        coroutineRule.runTest {
            // When
            filterResults.emit(mockedApplyResult)
            runCurrent()

            // Then
            assertEquals(mockedApplyResult.documents, viewModel.viewState.value.documentsUi)
            assertTrue(viewModel.viewState.value.isFilteringActive)
            assertFalse(viewModel.viewState.value.isRefreshPending)
        }

    // Case 2:
    // 1. Filters are open for editing.
    // 2. A queued apply result arrives.
    //
    // Case 2 Expected Result:
    // The list and the active-filter indication stay as they were and a refresh is pending.
    @Test
    fun `Given Filters open, When an apply result arrives, Then only a refresh is marked`() =
        coroutineRule.runTest {
            // Given
            openFilters()

            // When
            filterResults.emit(mockedApplyResult)
            runCurrent()

            // Then
            val state = viewModel.viewState.value
            assertEquals(emptyList<Pair<DocumentCategory, List<DocumentUi>>>(), state.documentsUi)
            assertFalse(state.isFilteringActive)
            assertTrue(state.isEditingFilters)
            assertTrue(state.isRefreshPending)
            verify(interactor, never()).getDocuments()
        }

    // Case 3:
    // 1. An apply result arrived while Filters were open.
    // 2. Filters close, and the screen reports the closure a second time.
    //
    // Case 3 Expected Result:
    // The draft is reverted and the documents are loaded once.
    @Test
    fun `Given a refresh waiting on Filters, When Filters close twice, Then documents load once`() =
        coroutineRule.runTest {
            // Given
            openFilters()
            filterResults.emit(mockedApplyResult)
            runCurrent()

            // When
            viewModel.handleEvents(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
            viewModel.handleEvents(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
            runCurrent()

            // Then
            verify(interactor, times(1)).getDocuments()
            verify(interactor, times(1)).revertFilters()
            assertFalse(viewModel.viewState.value.isEditingFilters)
            assertFalse(viewModel.viewState.value.isRefreshPending)
        }

    //endregion

    //region Deferred documents

    // Case 1:
    // 1. The Remove sheet of a pending document is open.
    // 2. The document is issued, and then Remove is tapped on the closing sheet.
    //
    // Case 1 Expected Result:
    // The sheet closes, the documents reload and the Remove tap deletes nothing.
    @Test
    fun `Given its Remove sheet open, When the document is issued, Then the sheet closes`() =
        coroutineRule.runTest {
            // Given
            openRemovalSheet(PENDING_ID)

            // When
            viewModel.handleEvents(Event.DeferredDocumentIssued(PENDING_ID))
            viewModel.handleEvents(removeTapped(PENDING_ID))
            runCurrent()

            // Then
            assertEquals(listOf(Effect.ShowBottomSheet, Effect.CloseBottomSheet), effects)
            verify(interactor, times(1)).getDocuments()
            verify(interactor, never()).deleteDocument(any())
        }

    @Test
    fun `Given its Remove sheet open, When Remove is tapped, Then the document is deleted`() =
        coroutineRule.runTest {
            // Given
            openRemovalSheet(PENDING_ID)

            // When
            viewModel.handleEvents(removeTapped(PENDING_ID))
            runCurrent()

            // Then
            assertEquals(listOf(Effect.ShowBottomSheet, Effect.CloseBottomSheet), effects)
            verify(interactor).deleteDocument(PENDING_ID)
        }

    /** The row still shows the document as pending until the reload replaces it. */
    @Test
    fun `Given an issued unseen document, When its row is tapped, Then documents reload instead`() =
        coroutineRule.runTest {
            // Given
            givenUnseenIssuance(ISSUED_ID)

            // When
            viewModel.handleEvents(documentSelected(ISSUED_ID))
            runCurrent()

            // Then
            assertEquals(emptyList<Effect>(), effects)
            assertEquals(
                DocumentsBottomSheetContent.Filters,
                viewModel.viewState.value.sheetContent,
            )
            verify(interactor, times(1)).getDocuments()
        }

    @Test
    fun `Given an issued unseen document, When Remove is tapped for it, Then nothing is deleted`() =
        coroutineRule.runTest {
            // Given
            givenUnseenIssuance(ISSUED_ID)

            // When
            viewModel.handleEvents(removeTapped(ISSUED_ID))
            runCurrent()

            // Then
            assertEquals(emptyList<Effect>(), effects)
            verify(interactor, never()).deleteDocument(any())
        }

    //endregion

    //region Screen disposal

    // Case 1:
    // 1. A document load is under way and delivers its result after one second.
    // 2. The screen is disposed before then.
    //
    // Case 1 Expected Result:
    // The load is cancelled and its result never rebuilds the filters.
    @Test
    fun `Given a load under way, When the screen is disposed, Then the load stops`() =
        coroutineRule.runTest {
            // Given
            var loadCompletion: Throwable? = null
            whenever(interactor.getDocuments()).thenReturn(
                delayedLoad(1.seconds).onCompletion { cause -> loadCompletion = cause }
            )
            viewModel.handleEvents(Event.GetDocuments)
            runCurrent()

            // When
            viewModel.handleEvents(Event.OnScreenDisposed)
            advanceTimeBy(2.seconds)
            runCurrent()

            // Then
            assertTrue(loadCompletion is CancellationException)
            verify(interactor, never()).initializeFilters(any())
            verify(interactor, never()).applyFilters()
        }

    // Case 2:
    // 1. Filters are open with a changed selection and a refresh pending.
    // 2. The screen is disposed.
    //
    // Case 2 Expected Result:
    // The draft is reverted, editing ends and no reload starts.
    @Test
    fun `Given a Filters draft, When the screen is disposed, Then the draft is reverted`() =
        coroutineRule.runTest {
            // Given
            openFilters()
            viewModel.handleEvents(
                Event.BottomSheet.Filters.SelectionChanged(filterId = "filter", groupId = "group")
            )
            filterResults.emit(mockedApplyResult)
            runCurrent()

            // When
            viewModel.handleEvents(Event.OnScreenDisposed)
            runCurrent()

            // Then
            verify(interactor).revertFilters()
            verify(interactor, never()).getDocuments()
            assertFalse(viewModel.viewState.value.isEditingFilters)
            assertFalse(viewModel.viewState.value.isRefreshPending)
        }

    //endregion

    //region Mocked objects needed for tests.

    private val mockedApplyResult = DocumentInteractorFilterPartialState.FilterApplyResult(
        documents = listOf(
            DocumentCategory.Government to listOf(
                DocumentUi(
                    documentIssuanceState = DocumentIssuanceStateUi.Issued,
                    uiData = ListItemDataUi(
                        itemId = "pid",
                        mainContentData = ListItemMainContentDataUi.Text("PID"),
                    ),
                    documentIdentifier = DocumentIdentifier.MdocPid,
                    documentCategory = DocumentCategory.Government,
                )
            )
        ),
        filters = emptyList(),
        allDefaultFiltersAreSelected = false,
    )

    private fun openFilters() {
        viewModel.handleEvents(Event.FiltersPressed)
        viewModel.handleEvents(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
        runCurrent()
    }

    private fun openRemovalSheet(documentId: String) {
        viewModel.handleEvents(documentSelected(documentId))
        viewModel.handleEvents(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
        runCurrent()
    }

    private fun givenUnseenIssuance(documentId: String) {
        unseenDeferredIssuanceIds.value = setOf(documentId)
        runCurrent()
    }

    private fun documentSelected(documentId: String) =
        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.DocumentSelected(documentId)

    private fun removeTapped(documentId: String) =
        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed(documentId)

    private fun delayedLoad(duration: Duration): Flow<DocumentInteractorGetDocumentsPartialState> =
        flow {
            delay(duration)
            emit(
                DocumentInteractorGetDocumentsPartialState.Success(
                    allDocuments = FilterableList(items = emptyList()),
                    shouldAllowUserInteraction = true,
                )
            )
        }

    private fun runCurrent() = coroutineRule.testScope.testScheduler.runCurrent()

    private fun advanceTimeBy(duration: Duration) =
        coroutineRule.testScope.testScheduler.advanceTimeBy(duration)

    private companion object {
        const val PENDING_ID = "pending"
        const val ISSUED_ID = "issued"
    }

    //endregion
}
