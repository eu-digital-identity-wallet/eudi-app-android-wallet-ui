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
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.TitleWithBadge
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.Job

data class State(
    val isLoading: Boolean = true,
    val isShowingFullUserInfo: Boolean = false,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: RequestBottomSheetContent = RequestBottomSheetContent.SUBTITLE,

    val verifierName: String? = null,
    val screenTitle: TitleWithBadge,
    val screenSubtitle: String,
    val screenClickableSubtitle: String?,
    val warningText: String,

    val items: List<RequestDataUi<Event>> = emptyList(),
    val noItems: Boolean = false,
    val showWarningCard: Boolean = false,
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

    abstract fun getScreenSubtitle(): String
    abstract fun getScreenTitle(): TitleWithBadge
    abstract fun getScreenClickableSubtitle(): String?
    abstract fun getWarningText(): String
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

    open fun updateData(updatedItems: List<RequestDataUi<Event>>, allowShare: Boolean? = null) {
        val hasVerificationItems = hasVerificationItems(updatedItems)

        val hasAtLeastOneFieldSelected = hasAtLeastOneFieldSelected(updatedItems)

        setState {
            copy(
                items = updatedItems,
                showWarningCard = updatedItems.any {
                    it is RequestDataUi.OptionalField
                            && it.optionalFieldItemUi.requestDocumentItemUi.enabled
                            && !it.optionalFieldItemUi.requestDocumentItemUi.checked
                },
                allowShare = allowShare ?: (hasAtLeastOneFieldSelected || hasVerificationItems)
            )
        }
    }

    override fun setInitialState(): State {
        return State(
            screenTitle = getScreenTitle(),
            screenSubtitle = getScreenSubtitle(),
            screenClickableSubtitle = getScreenClickableSubtitle(),
            warningText = getWarningText(),
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
                unsubscribe()
                cleanUp()
                setEffect { Effect.Navigation.Pop }
            }

            is NavigationType.Deeplink -> {}

            is NavigationType.PopTo -> {
                unsubscribe()
                cleanUp()
                setEffect { Effect.Navigation.PopTo(navigationType.screen.screenRoute) }
            }

            is NavigationType.PushRoute -> {
                unsubscribe()
                setEffect { Effect.Navigation.SwitchScreen(navigationType.route) }
            }
        }
    }

    private fun expandOrCollapseRequiredDataList(id: Int) {
        val items = viewState.value.items
        val updatedItems = items.map { item ->
            if (item is RequestDataUi.RequiredFields
                && id == item.requiredFieldsItemUi.id
            ) {
                item.copy(
                    requiredFieldsItemUi = item.requiredFieldsItemUi
                        .copy(expanded = !item.requiredFieldsItemUi.expanded)
                )
            } else {
                item
            }
        }
        updateData(updatedItems, viewState.value.allowShare)
    }

    private fun updateUserIdentificationItem(id: String) {
        val items: List<RequestDataUi<Event>> = viewState.value.items
        val updatedList = items.map { item ->
            if (item is RequestDataUi.OptionalField
                && id == item.optionalFieldItemUi.requestDocumentItemUi.id
            ) {
                val itemCurrentCheckedState = item.optionalFieldItemUi.requestDocumentItemUi.checked
                val updatedUiItem = item.optionalFieldItemUi.requestDocumentItemUi.copy(
                    checked = !itemCurrentCheckedState
                )
                item.copy(
                    optionalFieldItemUi = item.optionalFieldItemUi
                        .copy(requestDocumentItemUi = updatedUiItem)
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

    private fun hasVerificationItems(list: List<RequestDataUi<Event>>): Boolean {
        return list
            .filterIsInstance<RequestDataUi.RequiredFields<Event>>()
            .any { requiredFields ->
                requiredFields.requiredFieldsItemUi.requestDocumentItemsUi
                    .any { itemUi ->
                        itemUi.checked
                    }
            }
    }

    private fun hasAtLeastOneFieldSelected(list: List<RequestDataUi<Event>>): Boolean {
        return list
            .filterIsInstance<RequestDataUi.OptionalField<Event>>()
            .any { optionalField ->
                with(optionalField.optionalFieldItemUi.requestDocumentItemUi) {
                    enabled && checked
                }
            }
    }
}