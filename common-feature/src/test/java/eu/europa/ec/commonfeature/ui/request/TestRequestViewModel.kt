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

import eu.europa.ec.commonfeature.extension.toExpandableListItems
import eu.europa.ec.commonfeature.extension.toSelectiveExpandableListItems
import eu.europa.ec.commonfeature.extension.userImageItemIds
import eu.europa.ec.commonfeature.ui.request.model.DocumentFormatDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimItemId
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedUserImageClaimKeys
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.extension.toggleCheckboxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class TestRequestViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    /** The shared request logic, with the flow-specific parts reduced to a fixed next screen. */
    private class FakeRequestViewModel : RequestViewModel() {
        override fun getNextScreen(): String = NEXT_SCREEN

        override fun doWork() {}

        fun show(document: RequestDocumentItemUi) {
            setState {
                copy(
                    isLoading = false,
                    requestDataUi = RequestDataUi.Single(
                        combination = RequestCombinationUi(
                            documents = listOf(document),
                            matches = emptyList(),
                            transactionData = null,
                        ),
                    ),
                )
            }
        }
    }

    private val viewModel = FakeRequestViewModel()

    @Test
    fun `Given a document without the portrait, When Share is pressed, Then the PIN screen opens straight away`() =
        coroutineRule.runTest {
            viewModel.show(document(keys = listOf("family_name")))

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.StickyButtonPressed)

                assertEquals(Effect.Navigation.SwitchScreen(NEXT_SCREEN), awaitItem())
            }
        }

    @Test
    fun `Given a document with the portrait, When Share is pressed, Then a confirmation is asked first`() =
        coroutineRule.runTest {
            viewModel.show(document(keys = listOf("family_name", "portrait")))

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.StickyButtonPressed)

                assertEquals(Effect.ShowBottomSheet, awaitItem())
                assertEquals(
                    RequestBottomSheetContent.UserImageConfirmation,
                    viewModel.viewState.value.sheetContent
                )
                expectNoEvents()
            }
        }

    @Test
    fun `Given the portrait confirmation, When Share is confirmed, Then sharing goes ahead once the sheet has closed`() =
        coroutineRule.runTest {
            viewModel.show(document(keys = listOf("portrait")))

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.StickyButtonPressed)
                assertEquals(Effect.ShowBottomSheet, awaitItem())

                viewModel.handleEvents(Event.BottomSheet.UserImageConfirmation.Share)
                assertEquals(Effect.CloseBottomSheet, awaitItem())
                expectNoEvents()

                viewModel.handleEvents(Event.BottomSheet.FinishedClosing)
                assertEquals(Effect.Navigation.SwitchScreen(NEXT_SCREEN), awaitItem())
                assertFalse(viewModel.viewState.value.shareAfterSheetCloses)
            }
        }

    @Test
    fun `Given the portrait confirmation, When it is cancelled, Then nothing is shared`() =
        coroutineRule.runTest {
            viewModel.show(document(keys = listOf("portrait")))

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.StickyButtonPressed)
                assertEquals(Effect.ShowBottomSheet, awaitItem())

                viewModel.handleEvents(Event.BottomSheet.UserImageConfirmation.Cancel)
                assertEquals(Effect.CloseBottomSheet, awaitItem())

                viewModel.handleEvents(Event.BottomSheet.FinishedClosing)
                expectNoEvents()
            }
        }

    @Test
    fun `Given an unticked portrait, When Share is pressed, Then no confirmation is needed`() =
        coroutineRule.runTest {
            viewModel.show(
                document(
                    keys = listOf(
                        "family_name",
                        "portrait"
                    )
                ).withUnchecked("portrait")
            )

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.StickyButtonPressed)

                assertEquals(Effect.Navigation.SwitchScreen(NEXT_SCREEN), awaitItem())
            }
        }

    private fun document(
        keys: List<String>,
        selectable: Boolean = true,
        docId: String = mockedPidId,
    ): RequestDocumentItemUi {
        val payload = DocumentPayloadDomain(
            docName = mockedPidDocName,
            docId = docId,
            docFormatDomain = DocumentFormatDomain.MsoMdoc,
            docClaimsDomain = keys.map { key ->
                ClaimDomain.Primitive(
                    key = key,
                    displayTitle = key,
                    path = ClaimPathDomain.ofPlainKeys(
                        names = listOf(key),
                        type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace),
                    ),
                    value = "value",
                    isRequired = false,
                )
            },
        )
        return RequestDocumentItemUi(
            domainPayload = payload,
            userImageItemIds = payload.userImageItemIds(mockedUserImageClaimKeys),
            headerUi = ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = docId,
                    mainContentData = ListItemMainContentDataUi.Text(mockedPidDocName),
                ),
                nestedItems = if (selectable) {
                    payload.toSelectiveExpandableListItems(userImageClaimKeys = mockedUserImageClaimKeys)
                } else {
                    payload.toExpandableListItems(userImageClaimKeys = mockedUserImageClaimKeys)
                },
                isExpanded = false,
            ),
        )
    }

    private fun RequestDocumentItemUi.withUnchecked(key: String): RequestDocumentItemUi {
        val itemId = ClaimItemId.Claim(
            docId = domainPayload.docId,
            queryId = domainPayload.queryId,
            path = ClaimPathDomain.ofPlainKeys(
                names = listOf(key),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace),
            ),
        ).encode()
        return copy(
            headerUi = headerUi.copy(
                nestedItems = headerUi.nestedItems.map { item -> item.toggleCheckboxState(id = itemId) },
            ),
        )
    }

    private companion object {
        const val NEXT_SCREEN = "BIOMETRIC"
    }
}