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

package eu.europa.ec.commonfeature.ui.request

import eu.europa.ec.commonfeature.ui.request.model.DocumentFormatDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TestCredentialSelection {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private class PickerViewModel : RequestViewModel() {
        override fun getNextScreen() = "next"
        override fun doWork() = Unit

        fun show(combinations: List<RequestCombinationUi>) = setState {
            copy(
                isLoading = false,
                claimsAreSelectable = false,
                requestDataUi = RequestDataUi.Multiple(combinations, selectedIndex = 0),
            )
        }
    }

    @Test
    fun `Given three candidates, When two are selected, Then exactly those two are shared`() =
        coroutineRule.runTest {
            val viewModel = PickerViewModel()
            viewModel.show(listOf(combination((1..3).map { document("vc-$it", "pid", false) })))
            assertFalse(viewModel.viewState.value.allowShare)

            viewModel.handleEvents(Event.CredentialSelectionChanged("vc-1:pid", true))
            viewModel.handleEvents(Event.CredentialSelectionChanged("vc-3:pid", true))

            assertTrue(viewModel.viewState.value.allowShare)
            assertEquals(
                listOf("vc-1", "vc-3"),
                viewModel.viewState.value.requestDataUi.selectedCombination!!.documentsForSharing
                    .map { it.domainPayload.docId },
            )
        }

    @Test
    fun `When the last credential for a query is deselected, Then Share is disabled`() =
        coroutineRule.runTest {
            val viewModel = PickerViewModel()
            viewModel.show(listOf(combination(listOf(document("vc", "pid", true)))))
            assertTrue(viewModel.viewState.value.allowShare)
            viewModel.handleEvents(Event.CredentialSelectionChanged("vc:pid", false))
            assertFalse(viewModel.viewState.value.allowShare)
        }

    @Test
    fun `Given two queries, Then each query in the combination needs a selected credential`() =
        coroutineRule.runTest {
            val viewModel = PickerViewModel()
            val documents = listOf(
                document("employee", "employee", null),
                document("publisher", "entitlement", false),
                document("reader", "entitlement", false),
            )
            viewModel.show(listOf(combination(documents)))
            assertFalse(viewModel.viewState.value.allowShare)
            viewModel.handleEvents(Event.CredentialSelectionChanged("reader:entitlement", true))
            assertTrue(viewModel.viewState.value.allowShare)
        }

    @Test
    fun `When switching combination options, Then each option retains its selection`() =
        coroutineRule.runTest {
            val viewModel = PickerViewModel()
            val combinations = listOf(
                combination(listOf(document("first", "pid", false))),
                combination(listOf(document("second", "pid", false))),
            )
            viewModel.show(combinations)
            viewModel.handleEvents(Event.CredentialSelectionChanged("first:pid", true))
            viewModel.handleEvents(Event.CombinationSelected(1))
            assertFalse(viewModel.viewState.value.allowShare)
            viewModel.handleEvents(Event.CredentialSelectionChanged("second:pid", true))
            viewModel.handleEvents(Event.CombinationSelected(0))
            assertEquals(
                "first",
                viewModel.viewState.value.requestDataUi.selectedCombination!!
                    .documentsForSharing.single().domainPayload.docId,
            )
            assertTrue(viewModel.viewState.value.allowShare)
        }

    @Test
    fun `Given an optional set skipped by the SDK option, Then its absent query does not block Share`() {
        val selected = combination(listOf(document("pid", "pid", null)))
        assertTrue(selected.canShareDocuments)
    }

    @Test
    fun `Given a fixed single credential, Then a checkbox event cannot deselect it`() =
        coroutineRule.runTest {
            val viewModel = PickerViewModel()
            viewModel.show(listOf(combination(listOf(document("pid", "pid", null)))))
            viewModel.handleEvents(Event.CredentialSelectionChanged("pid:pid", false))
            assertTrue(viewModel.viewState.value.allowShare)
            assertEquals(
                1,
                viewModel.viewState.value.requestDataUi.selectedCombination!!.documentsForSharing.size,
            )
        }

    @Test
    fun `Given a deselected credential containing a portrait, Then it is not shared`() {
        val first = document("plain", "pid", true)
        val portrait = document("portrait", "pid", false).let { document ->
            document.copy(
                userImageItemIds = setOf("portrait-claim"),
                headerUi = document.headerUi.copy(nestedItems = listOf(
                    ExpandableListItemUi.SingleListItem(
                        header = ListItemDataUi(
                            itemId = "portrait-claim",
                            mainContentData = ListItemMainContentDataUi.Text("Portrait"),
                        ),
                    ),
                )),
            )
        }
        val data = RequestDataUi.Single(combination(listOf(first, portrait)))
        assertTrue(data.requestsUserImage)
        assertFalse(data.sharesUserImage)
    }

    private fun combination(documents: List<RequestDocumentItemUi>) = RequestCombinationUi(
        documents = documents,
        matches = emptyList(),
        transactionData = null,
    )

    private fun document(id: String, queryId: String, selected: Boolean?) = RequestDocumentItemUi(
        domainPayload = DocumentPayloadDomain(
            docName = id,
            docId = id,
            docFormatDomain = DocumentFormatDomain.SdJwtVc,
            docClaimsDomain = emptyList(),
            queryId = queryId,
        ),
        headerUi = ExpandableListItemUi.NestedListItem(
            header = ListItemDataUi(
                itemId = "$id:$queryId",
                mainContentData = ListItemMainContentDataUi.Text(id),
            ),
            nestedItems = emptyList(),
            isExpanded = false,
        ),
        userImageItemIds = emptySet(),
        isSelectedForSharing = selected,
    )
}
