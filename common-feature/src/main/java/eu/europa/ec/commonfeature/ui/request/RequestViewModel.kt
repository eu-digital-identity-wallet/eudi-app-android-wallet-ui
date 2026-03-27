/*
 * Copyright (c) 2025 European Commission
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

import eu.europa.ec.commonfeature.ui.request.model.DomainDocumentFormat
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.extension.collectAllNestedIds
import eu.europa.ec.uilogic.extension.toggleCheckboxState
import eu.europa.ec.uilogic.extension.toggleExpansionState
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.helper.IntentAction
import eu.europa.ec.uilogic.navigation.helper.IntentType
import kotlinx.coroutines.Job

data class State(
    val isLoading: Boolean = true,
    val isShowingFullUserInfo: Boolean = false,
    val headerConfig: ContentHeaderConfig,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: RequestBottomSheetContent = RequestBottomSheetContent.WARNING,
    val hasWarnedUser: Boolean = false,
    val presentationScopeId: String = "",

    val items: List<RequestDocumentItemUi> = emptyList(),
    val noItems: Boolean = false,
    val allowShare: Boolean = false,

    val intentAction: IntentAction? = null,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val intentAction: IntentAction?) : Event()
    data object DoWork : Event()
    data object DismissError : Event()
    data object OnBack : Event()
    data object StickyButtonPressed : Event()

    data class UserIdentificationClicked(val itemId: String) : Event()
    data class ExpandOrCollapseRequestDocumentItem(val itemId: String) : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
        ) : Navigation()

        data object Pop : Navigation()
        data object Finish : Navigation()
        data class PopTo(
            val screenRoute: String,
        ) : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

enum class RequestBottomSheetContent {
    WARNING,
}

abstract class RequestViewModel : MviViewModel<Event, State, Effect>() {
    protected var viewModelJob: Job? = null

    abstract fun getHeaderConfig(): ContentHeaderConfig
    abstract fun getNextScreen(): String
    abstract fun doWork()

    open fun init(intentAction: IntentAction?) {}

    /**
     * Called during [NavigationType.Pop].
     * */
    open fun cleanUp() {}

    open fun updateData(
        updatedItems: List<RequestDocumentItemUi>,
        allowShare: Boolean? = null,
    ) {
        val hasAtLeastOneFieldSelected = hasAtLeastOneFieldSelected(
            requestDocuments = updatedItems
        )

        setState {
            copy(
                items = updatedItems,
                allowShare = allowShare ?: hasAtLeastOneFieldSelected
            )
        }
    }

    override fun setInitialState(): State {
        return State(
            headerConfig = getHeaderConfig(),
            error = null,
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                init(event.intentAction)
                doWork()
            }

            is Event.DoWork -> doWork()

            is Event.DismissError -> {
                setState {
                    copy(error = null)
                }
            }

            is Event.OnBack -> {
                handleOnBack()
            }

            is Event.StickyButtonPressed -> {
                doNavigation(NavigationType.PushRoute(getNextScreen()))
            }

            is Event.UserIdentificationClicked -> {
                if (viewState.value.hasWarnedUser) {
                    val clickedId = event.itemId

                    val siblingIds = getIdsInSameTopLevelRootGroup(
                        documents = viewState.value.items,
                        clickedItemId = clickedId
                    )

                    updateUserIdentificationItem(id = clickedId, siblingIds = siblingIds)
                } else {
                    setState {
                        copy(hasWarnedUser = true)
                    }
                    showBottomSheet(sheetContent = RequestBottomSheetContent.WARNING)
                }
            }

            is Event.ExpandOrCollapseRequestDocumentItem -> {
                expandOrCollapseRequestDocumentItem(id = event.itemId)
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }
        }
    }

    /**
     * Returns all selectable claim item IDs from the same SD-JWT VC document that belong to the
     * same top-level root segment as the clicked item.
     *
     * The [clickedItemId] is expected to have been produced by [ClaimPathDomain.toId], meaning
     * its structure for SD-JWT VC claims is:
     *
     * `docId,pathSegment1,pathSegment2,...`
     *
     * This function:
     * 1. extracts the document ID and the clicked claim's top-level root segment,
     * 2. finds the matching SD-JWT VC document,
     * 3. scans the document's top-level UI groups,
     * 4. collects all nested leaf item IDs that belong to the same top-level root segment,
     * 5. excludes the clicked item itself from the result.
     *
     * Example:
     * If [clickedItemId] is:
     * `doc-1,address,country`
     *
     * and the document contains item IDs such as:
     * - `doc-1,address,country`
     * - `doc-1,address,city`
     * - `doc-1,address,street,formatted`
     * - `doc-1,name,given_name`
     *
     * then the returned result will be:
     * - `doc-1,address,city`
     * - `doc-1,address,street,formatted`
     *
     * @param documents The list of request document UI models to search through.
     * @param clickedItemId The item ID of the clicked claim.
     * @return All unique item IDs from the same document that share the same top-level root segment
     * as [clickedItemId], excluding [clickedItemId] itself. Returns an empty list if the ID cannot
     * be parsed, the document cannot be found, or no matching group exists.
     */
    private fun getIdsInSameTopLevelRootGroup(
        documents: List<RequestDocumentItemUi>,
        clickedItemId: String
    ): List<String> {
        runCatching {
            val idComponents: List<String> = clickedItemId.split(ClaimPathDomain.PATH_SEPARATOR)

            // For SD-JWT VC IDs, the expected structure is:
            // [docId, topLevelSegment, ...remainingPath]
            val clickedDocId = idComponents[0]
            val clickedClaimFirstPathSegment = idComponents[1]

            val groupItemIds = mutableListOf<String>()

            documents.find { document ->
                document.domainPayload.domainDocFormat is DomainDocumentFormat.SdJwtVc
                        && document.domainPayload.docId == clickedDocId
            }?.let { clickedDocument ->
                // Traverse the document's top-level UI items and find the group whose root segment
                // matches the clicked item's root segment.
                clickedDocument.headerUi.nestedItems.forEach { childItemUi ->
                    val childItemFirstPathSegment = childItemUi.header.itemId
                        .split(ClaimPathDomain.PATH_SEPARATOR)[1]

                    if (childItemFirstPathSegment == clickedClaimFirstPathSegment) {
                        // Collect all nested selectable leaf IDs under the matching top-level group.
                        groupItemIds.addAll(childItemUi.collectAllNestedIds())
                    }
                }
            }

            return groupItemIds
                .distinct()
                .minus(clickedItemId)
        }.getOrElse {
            return emptyList()
        }
    }

    private fun handleOnBack() {
        setState {
            copy(error = null)
        }
        val intentIsDcApi = viewState.value.intentAction?.type == IntentType.DC_API
        val navigationType = if (intentIsDcApi) {
            NavigationType.Finish
        } else {
            NavigationType.Pop
        }
        doNavigation(
            navigationType
        )
    }

    private fun doNavigation(navigationType: NavigationType) {
        when (navigationType) {
            is NavigationType.PushScreen -> {
                unsubscribe()
                setEffect { Effect.Navigation.SwitchScreen(navigationType.screen.screenRoute) }
            }

            is NavigationType.Pop -> {
                setEffect { Effect.Navigation.Pop }
            }

            is NavigationType.Finish -> {
                setEffect { Effect.Navigation.Finish }
            }

            is NavigationType.Deeplink -> {}

            is NavigationType.PopTo -> {
                setEffect { Effect.Navigation.PopTo(navigationType.screen.screenRoute) }
            }

            is NavigationType.PushRoute -> {
                unsubscribe()
                setEffect { Effect.Navigation.SwitchScreen(navigationType.route) }
            }
        }
    }

    private fun expandOrCollapseRequestDocumentItem(id: String) {
        val currentItems = viewState.value.items

        val updatedItems = currentItems.map { requestDocument ->
            val newHeader = if (requestDocument.headerUi.header.itemId == id) {
                val newIsExpanded = !requestDocument.headerUi.isExpanded
                val newCollapsed = requestDocument.headerUi.header.copy(
                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                        iconData = if (newIsExpanded) {
                            AppIcons.KeyboardArrowUp
                        } else {
                            AppIcons.KeyboardArrowDown
                        }
                    )
                )

                requestDocument.headerUi.copy(
                    header = newCollapsed,
                    isExpanded = newIsExpanded
                )
            } else {
                requestDocument.headerUi
            }

            requestDocument.copy(
                headerUi = newHeader.copy(
                    nestedItems = newHeader.nestedItems.toggleExpansionState(id),
                )
            )
        }

        updateData(updatedItems, viewState.value.allowShare)
    }

    private fun updateUserIdentificationItem(id: String, siblingIds: List<String>) {
        val currentItems = viewState.value.items

        val updatedItems: List<RequestDocumentItemUi> = currentItems.map { requestDocument ->
            requestDocument.copy(
                headerUi = requestDocument.headerUi.copy(
                    nestedItems = requestDocument.headerUi.nestedItems.map {
                        it.toggleCheckboxState(id = id, coToggleIds = siblingIds)
                    }
                )
            )
        }

        val hasAtLeastOneFieldSelected = hasAtLeastOneFieldSelected(
            requestDocuments = updatedItems
        )

        updateData(
            updatedItems = updatedItems,
            allowShare = hasAtLeastOneFieldSelected
        )
    }

    private fun showBottomSheet(sheetContent: RequestBottomSheetContent) {
        setState {
            copy(sheetContent = sheetContent)
        }
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet
        }
    }

    private fun unsubscribe() {
        viewModelJob?.cancel()
    }

    private fun hasAtLeastOneFieldSelected(
        requestDocuments: List<RequestDocumentItemUi>,
    ): Boolean {
        val hasAtLeastOneFieldSelected: Boolean = requestDocuments.any { requestDocument ->
            requestDocument.headerUi.nestedItems.hasAnySingleSelected()
        }
        return hasAtLeastOneFieldSelected
    }

    private fun List<ExpandableListItemUi>.hasAnySingleSelected(): Boolean {
        return this.any { expandableItem ->
            when (expandableItem) {
                is ExpandableListItemUi.NestedListItem -> {
                    expandableItem.nestedItems.hasAnySingleSelected()
                }

                is ExpandableListItemUi.SingleListItem -> {
                    val trailingContentData = expandableItem.header.trailingContentData
                    trailingContentData is ListItemTrailingContentDataUi.Checkbox && trailingContentData.checkboxData.isChecked
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribe()
        cleanUp()
    }
}