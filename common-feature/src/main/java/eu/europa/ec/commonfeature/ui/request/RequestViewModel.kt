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

import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
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

    val items: List<RequestDataUi<Event>> = emptyList(),
    val noItems: Boolean = false,
    val allowShare: Boolean = false
) : ViewState

sealed class Event : ViewEvent {
    data object DoWork : Event()
    data object DismissError : Event()
    data object GoBack : Event()
    data object ChangeContentVisibility : Event()
    data class ExpandOrCollapseRequiredDataList(val id: Int) : Event()
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
        updatedItems: List<RequestDataUi<Event>>,
        allowShare: Boolean? = null
    ) {
        val hasVerificationItems = hasVerificationItems(updatedItems)

        val hasAtLeastOneFieldSelected = hasAtLeastOneFieldSelected(updatedItems)

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
                expandOrCollapseRequiredDataList(id = event.id)
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

    private fun expandOrCollapseRequiredDataList(id: Int) {
        //val items = viewState.value.items
        //val updatedItems = items.map { item ->
        //    if (item is RequestDocumentsUi.RequiredFields
        //        && id == item.requiredFieldsItemUi.id
        //    ) {
        //        item.copy(
        //            requiredFieldsItemUi = item.requiredFieldsItemUi
        //                .copy(expanded = !item.requiredFieldsItemUi.expanded)
        //        )
        //    } else {
        //        item
        //    }
        //}
        //updateData(updatedItems, viewState.value.allowShare)
    }

    private fun updateUserIdentificationItem(id: String) {
        val items: List<RequestDataUi<Event>> = viewState.value.items
        val updatedList = items.map { item ->
            if (item is RequestDataUi.ExpandableField) {
                val expandableListItem = item.expandableFieldItemUi.expandableListItem

                // Update `expanded` list
                val updatedExpanded = expandableListItem.expanded.map { listItem ->
                    if (listItem.itemId == id &&
                        listItem.trailingContentData is ListItemTrailingContentData.Checkbox
                    ) {
                        val checkboxData =
                            listItem.trailingContentData as ListItemTrailingContentData.Checkbox
                        val updatedCheckbox = checkboxData.copy(
                            checkboxData = checkboxData.checkboxData.copy(
                                isChecked = !checkboxData.checkboxData.isChecked
                            )
                        )
                        listItem.copy(trailingContentData = updatedCheckbox)
                    } else {
                        listItem
                    }
                }

                //TODO probably remove this
                // Update `collapsed` item if it matches the ID
                val updatedCollapsed = if (expandableListItem.collapsed.itemId == id &&
                    expandableListItem.collapsed.trailingContentData is ListItemTrailingContentData.Checkbox
                ) {
                    val checkboxData =
                        expandableListItem.collapsed.trailingContentData as ListItemTrailingContentData.Checkbox
                    val updatedCheckbox = checkboxData.copy(
                        checkboxData = checkboxData.checkboxData.copy(
                            isChecked = !checkboxData.checkboxData.isChecked
                        )
                    )
                    expandableListItem.collapsed.copy(trailingContentData = updatedCheckbox)
                } else {
                    expandableListItem.collapsed
                }

                // Return updated ExpandableField
                item.copy(
                    expandableFieldItemUi = item.expandableFieldItemUi.copy(
                        expandableListItem = expandableListItem.copy(
                            collapsed = updatedCollapsed,
                            expanded = updatedExpanded
                        )
                    )
                )
            } else {
                item
            }
        }

        val hasVerificationItems = hasVerificationItems(updatedList)
        val hasAtLeastOneFieldSelected = hasAtLeastOneFieldSelected(updatedList)

        updateData(
            updatedItems = updatedList,
            allowShare = hasAtLeastOneFieldSelected || hasVerificationItems
        )
    }

    private fun hasVerificationItems(list: List<RequestDataUi<Event>>): Boolean {
        return list
            .filterIsInstance<RequestDataUi.ExpandableField<Event>>()
            .any { expandableField ->
                val expandableListItem = expandableField.expandableFieldItemUi.expandableListItem
                // Check both collapsed and expanded items for checked state
                val collapsedChecked = expandableListItem.collapsed.trailingContentData
                    ?.let { trailingContent ->
                        trailingContent is ListItemTrailingContentData.Checkbox &&
                                trailingContent.checkboxData.isChecked
                    } ?: false

                val expandedChecked = expandableListItem.expanded.any { listItem ->
                    listItem.trailingContentData is ListItemTrailingContentData.Checkbox &&
                            (listItem.trailingContentData as ListItemTrailingContentData.Checkbox)
                                .checkboxData.isChecked
                }

                collapsedChecked || expandedChecked
            }
    }

    private fun hasAtLeastOneFieldSelected(list: List<RequestDataUi<Event>>): Boolean {
        return list
            .filterIsInstance<RequestDataUi.ExpandableField<Event>>()
            .any { expandableField ->
                val expandableListItem = expandableField.expandableFieldItemUi.expandableListItem
                // Check both collapsed and expanded items for enabled and checked state
                val collapsedSelected = expandableListItem.collapsed.trailingContentData
                    ?.let { trailingContent ->
                        trailingContent is ListItemTrailingContentData.Checkbox &&
                                trailingContent.checkboxData.isChecked &&
                                trailingContent.checkboxData.enabled
                    } ?: false

                val expandedSelected = expandableListItem.expanded.any { listItem ->
                    listItem.trailingContentData is ListItemTrailingContentData.Checkbox &&
                            (listItem.trailingContentData as ListItemTrailingContentData.Checkbox)
                                .checkboxData.isChecked &&
                            (listItem.trailingContentData as ListItemTrailingContentData.Checkbox)
                                .checkboxData.enabled
                }

                collapsedSelected || expandedSelected
            }
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

    override fun onCleared() {
        super.onCleared()
        unsubscribe()
        cleanUp()
    }
}