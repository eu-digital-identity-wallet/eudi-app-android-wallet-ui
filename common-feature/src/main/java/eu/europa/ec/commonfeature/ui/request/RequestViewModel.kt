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

package eu.europa.ec.commonfeature.ui.request

import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.Job

data class State(
    val isLoading: Boolean = true,
    val isShowingFullUserInfo: Boolean = false,
    val headerConfig: ContentHeaderConfig,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: RequestBottomSheetContent = RequestBottomSheetContent.WARNING,
    val hasWarnedUser: Boolean = false,

    val items: List<RequestDocumentItemUi> = emptyList(),
    val noItems: Boolean = false,
    val allowShare: Boolean = false
) : ViewState

sealed class Event : ViewEvent {
    data object DoWork : Event()
    data object DismissError : Event()
    data object Pop : Event()
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
            val screenRoute: String
        ) : Navigation()

        data object Pop : Navigation()
        data class PopTo(
            val screenRoute: String
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

    /**
     * Called during [NavigationType.Pop].
     *
     * Kill presentation scope.
     *
     * */
    open fun cleanUp() {
        getOrCreatePresentationScope().close()
    }

    open fun updateData(
        updatedItems: List<RequestDocumentItemUi>,
        allowShare: Boolean? = null
    ) {
        val hasAtLeastOneFieldSelected = hasAtLeastOneFieldSelected(
            list = updatedItems
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
            is Event.DoWork -> doWork()

            is Event.DismissError -> {
                setState {
                    copy(error = null)
                }
            }

            is Event.Pop -> {
                setState {
                    copy(error = null)
                }
                doNavigation(NavigationType.Pop)
            }

            is Event.StickyButtonPressed -> {
                doNavigation(NavigationType.PushRoute(getNextScreen()))
            }

            is Event.UserIdentificationClicked -> {
                if (viewState.value.hasWarnedUser) {
                    updateUserIdentificationItem(id = event.itemId)
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

    private fun doNavigation(navigationType: NavigationType) {
        when (navigationType) {
            is NavigationType.PushScreen -> {
                unsubscribe()
                setEffect { Effect.Navigation.SwitchScreen(navigationType.screen.screenRoute) }
            }

            is NavigationType.Pop, NavigationType.Finish -> {
                setEffect { Effect.Navigation.Pop }
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
            val newHeader = if (requestDocument.headerUi.collapsed.itemId == id) {
                val newIsExpanded = !requestDocument.headerUi.isExpanded
                val newCollapsed = requestDocument.headerUi.collapsed.copy(
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = if (newIsExpanded) {
                            AppIcons.KeyboardArrowUp
                        } else {
                            AppIcons.KeyboardArrowDown
                        }
                    )
                )

                requestDocument.headerUi.copy(
                    collapsed = newCollapsed,
                    isExpanded = newIsExpanded
                )
            } else {
                requestDocument.headerUi
            }

            requestDocument.copy(
                headerUi = newHeader.copy(
                    expanded = newHeader.expanded.changeNestedItems(id),
                )
            )
        }

        updateData(updatedItems, viewState.value.allowShare)
    }

    //TODO should this be in other place? i.e. should it be used elsewhere?
    private fun List<ExpandableListItem>.changeNestedItems(id: String): List<ExpandableListItem> {
        return this.map { nestedItem ->
            when (nestedItem) {
                is ExpandableListItem.NestedListItemData -> {
                    if (nestedItem.collapsed.itemId == id) {
                        val newIsExpanded = !nestedItem.isExpanded
                        val newCollapsed = nestedItem.collapsed.copy(
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = if (newIsExpanded) {
                                    AppIcons.KeyboardArrowUp
                                } else {
                                    AppIcons.KeyboardArrowDown
                                }
                            )
                        )

                        nestedItem.copy(
                            collapsed = newCollapsed,
                            isExpanded = newIsExpanded
                        )
                    } else {
                        nestedItem.copy(
                            expanded = nestedItem.expanded.changeNestedItems(id)
                        )
                    }
                }

                is ExpandableListItem.SingleListItemData -> {
                    nestedItem
                }
            }
        }
    }

    private fun updateUserIdentificationItem(id: String) {
//        val currentItems = viewState.value.items
//
//        // Iterate over the items and modify the matching expanded item
//        val updatedList: List<RequestDocumentItemUi> = currentItems.map { item ->
//            val updatedExpandedItems = item.expandedUiItems.map { expandedItem ->
//                if (expandedItem.uiItem.itemId == id
//                    && expandedItem.uiItem.trailingContentData is ListItemTrailingContentData.Checkbox
//                ) {
//                    val checkboxData =
//                        (expandedItem.uiItem.trailingContentData as ListItemTrailingContentData.Checkbox).checkboxData
//
//                    expandedItem.copy(
//                        uiItem = expandedItem.uiItem.copy(
//                            trailingContentData = ListItemTrailingContentData.Checkbox(
//                                checkboxData = checkboxData.copy(
//                                    isChecked = !checkboxData.isChecked
//                                )
//                            )
//                        )
//                    )
//                } else {
//                    expandedItem
//                }
//            }
//
//            // Return the updated item with its expanded items updated
//            item.copy(
//                expandedUiItems = updatedExpandedItems
//            )
//        }
//
//        val hasAtLeastOneFieldSelected = hasAtLeastOneFieldSelected(
//            list = updatedList
//        )
//
//        updateData(
//            updatedItems = updatedList,
//            allowShare = hasAtLeastOneFieldSelected
//        )
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
        list: List<RequestDocumentItemUi>
    ): Boolean {
//        val hasAtLeastOneFieldSelected: Boolean = list.any { item ->
//            item.expandedUiItems.any { expandedUiItem ->
//                val trailingContentData = expandedUiItem.uiItem.trailingContentData
//                trailingContentData is ListItemTrailingContentData.Checkbox && trailingContentData.checkboxData.isChecked
//            }
//        }
//
//        return hasAtLeastOneFieldSelected
        return true
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribe()
        cleanUp()
    }
}