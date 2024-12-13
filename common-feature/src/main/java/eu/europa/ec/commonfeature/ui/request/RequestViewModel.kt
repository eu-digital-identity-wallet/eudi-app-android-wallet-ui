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

import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi2
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
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
    val sheetContent: RequestBottomSheetContent = RequestBottomSheetContent.SUBTITLE,

    val verifierName: String? = null,

    val items: List<RequestDocumentItemUi2<Event>> = emptyList(),
    //val newItems: ExpandableListItemData<Event>? = null,
    val noItems: Boolean = false,
    val allowShare: Boolean = false
) : ViewState

sealed class Event : ViewEvent {
    data class SthClicked(val name: String) : Event()
    data object DoWork : Event()
    data object DismissError : Event()
    data object GoBack : Event()
    data object ChangeContentVisibility : Event()
    data class ExpandOrCollapseRequiredDataList(val itemId: String) : Event()

    data class UserIdentificationClicked(val itemId: String) : Event()

    data object BadgeClicked : Event()
    data object SubtitleClicked : Event()
    data object PrimaryButtonPressed : Event()
    data object SecondaryButtonPressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Cancel : BottomSheet() {
            data object PrimaryButtonPressed : Cancel()
            data object SecondaryButtonPressed : Cancel()
        }

        sealed class Subtitle : BottomSheet() {
            data object PrimaryButtonPressed : Subtitle()
        }

        sealed class Badge : BottomSheet() {
            data object PrimaryButtonPressed : Subtitle()
        }
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
    BADGE, SUBTITLE, CANCEL
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
        updatedItems: List<RequestDocumentItemUi2<Event>>,
        allowShare: Boolean? = null
    ) {
        val (hasVerificationItems, hasAtLeastOneFieldSelected) = hasVerificationItemsOrAtLeastOneFieldSelected(
            list = updatedItems
        )

        setState {
            copy(
                items = updatedItems,
                allowShare = allowShare ?: (hasAtLeastOneFieldSelected || hasVerificationItems)
            )
        }
    }

    override fun setInitialState(): State {
        return State(
            headerConfig = getHeaderConfig(),
            error = null,
            verifierName = null
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.SthClicked -> {
                println("Giannis clicked sth: ${event.name}")
            }

            is Event.DoWork -> doWork()

            is Event.DismissError -> {
                setState {
                    copy(error = null)
                }
            }

            is Event.GoBack -> {
                setState {
                    copy(error = null)
                }
                doNavigation(NavigationType.Pop)
            }

            is Event.ChangeContentVisibility -> {
                setState {
                    copy(isShowingFullUserInfo = !isShowingFullUserInfo)
                }
            }

            is Event.ExpandOrCollapseRequiredDataList -> {
                expandOrCollapseRequiredDataList(id = event.itemId)
            }

            is Event.UserIdentificationClicked -> {
                updateUserIdentificationItem(id = event.itemId)
            }

            is Event.BadgeClicked -> {
                showBottomSheet(sheetContent = RequestBottomSheetContent.BADGE)
            }

            is Event.SubtitleClicked -> {
                showBottomSheet(sheetContent = RequestBottomSheetContent.SUBTITLE)
            }

            is Event.PrimaryButtonPressed -> {
                doNavigation(NavigationType.PushRoute(getNextScreen()))
            }

            is Event.SecondaryButtonPressed -> {
                showBottomSheet(sheetContent = RequestBottomSheetContent.CANCEL)
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Cancel.PrimaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Cancel.SecondaryButtonPressed -> {
                hideBottomSheet()
                doNavigation(NavigationType.Pop)
            }

            is Event.BottomSheet.Subtitle.PrimaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Badge.PrimaryButtonPressed -> {
                hideBottomSheet()
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

    private fun expandOrCollapseRequiredDataList(id: String) {
        val currentItems = viewState.value.items
        val updatedItems = currentItems.map { item ->
            if (item.uiCollapsedItem.uiItem.itemId == id) {

                val newIsExpanded = !item.uiCollapsedItem.isExpanded

                // Change the Icon based on the new isExpanded state
                val newIconData = if (newIsExpanded) {
                    AppIcons.KeyboardArrowUp
                } else {
                    AppIcons.KeyboardArrowDown
                }

                item.copy(
                    uiCollapsedItem = item.uiCollapsedItem.copy(
                        isExpanded = newIsExpanded,
                        uiItem = item.uiCollapsedItem.uiItem.copy(
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = newIconData
                            )
                        )
                    )
                )
            } else {
                item
            }
        }
        updateData(updatedItems, viewState.value.allowShare)
    }

    private fun updateUserIdentificationItem(id: String) {
        val currentItems = viewState.value.items

        // Iterate over the items and modify the matching expanded item
        val updatedList: List<RequestDocumentItemUi2<Event>> = currentItems.map { item ->
            val updatedExpandedItems = item.uiExpandedItems.map { expandedItem ->
                if (expandedItem.uiItem.itemId == id
                    && expandedItem.uiItem.trailingContentData is ListItemTrailingContentData.Checkbox
                ) {
                    val checkboxData =
                        (expandedItem.uiItem.trailingContentData as ListItemTrailingContentData.Checkbox).checkboxData

                    expandedItem.copy(
                        uiItem = expandedItem.uiItem.copy(
                            trailingContentData = ListItemTrailingContentData.Checkbox(
                                checkboxData = checkboxData.copy(
                                    isChecked = !checkboxData.isChecked
                                )
                            )
                        )
                    )
                } else {
                    expandedItem
                }
            }

            // Return the updated item with its expanded items updated
            item.copy(
                uiExpandedItems = updatedExpandedItems
            )
        }

        val (hasVerificationItems, hasAtLeastOneFieldSelected) = hasVerificationItemsOrAtLeastOneFieldSelected(
            list = updatedList
        )

        updateData(
            updatedItems = updatedList,
            allowShare = hasAtLeastOneFieldSelected || hasVerificationItems
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

    private fun hasVerificationItemsOrAtLeastOneFieldSelected(
        list: List<RequestDocumentItemUi2<Event>>
    ): Pair<Boolean, Boolean> {

        var hasVerificationItems = false
        var hasAtLeastOneFieldSelected = false

        for (item in list) {
            for (expandedItem in item.uiExpandedItems) {
                val trailingContentData = expandedItem.uiItem.trailingContentData
                if (trailingContentData is ListItemTrailingContentData.Checkbox) {
                    val checkbox = trailingContentData.checkboxData
                    if (checkbox.isChecked) {
                        hasVerificationItems = true
                    }
                    if (checkbox.isChecked && checkbox.enabled) {
                        hasAtLeastOneFieldSelected = true
                    }
                }
                // Exit early if both conditions are true
                if (hasVerificationItems && hasAtLeastOneFieldSelected) {
                    return Pair(true, true)
                }
            }
        }

        return Pair(hasVerificationItems, hasAtLeastOneFieldSelected)
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribe()
        cleanUp()
    }
}