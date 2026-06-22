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

import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.config.NavigationType
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
    val headerConfig: ContentHeaderConfig,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: RequestBottomSheetContent = RequestBottomSheetContent.WARNING,
    val hasWarnedUser: Boolean = false,
    val presentationScopeId: String = "",

    val requestDataUi: RequestDataUi = RequestDataUi.Initial,
    val claimsAreSelectable: Boolean = true,

    val intentAction: IntentAction? = null,
) : ViewState {
    val allowShare: Boolean
        get() = if (claimsAreSelectable) {
            requestDataUi.selectedDocuments.anyClaimChecked()
        } else {
            requestDataUi.selectedDocuments.isNotEmpty()
        }
}

sealed class Event : ViewEvent {
    data class Init(val intentAction: IntentAction?) : Event()
    data object DoWork : Event()
    data object DismissError : Event()
    data object OnBack : Event()
    data object StickyButtonPressed : Event()

    data class UserIdentificationClicked(val itemId: String) : Event()
    data class ExpandOrCollapseRequestDocumentItem(val itemId: String) : Event()

    data class CombinationSelected(val index: Int) : Event()

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

    open fun updateData(updatedItems: List<RequestDocumentItemUi>) {
        setState {
            copy(requestDataUi = requestDataUi.withSelectedDocuments(updatedItems))
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
                if (!viewState.value.claimsAreSelectable) return

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

            is Event.CombinationSelected -> {
                selectCombination(index = event.index)
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }
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

    /**
     * Switch the selected combination and hand its documents to [updateData], so the disclosure
     * follows the user's choice. No-op for a single combination, or an unchanged/out-of-range index.
     */
    private fun selectCombination(index: Int) {
        val data = viewState.value.requestDataUi
        if (data !is RequestDataUi.Multiple) return
        if (index !in data.combinations.indices) return
        if (index == data.selectedIndex) return

        setState {
            copy(
                requestDataUi = data
                    .copy(selectedIndex = index)
            )
        }
        updateData(updatedItems = data.combinations[index].documents)
    }

    private fun expandOrCollapseRequestDocumentItem(id: String) {
        val currentItems = viewState.value.requestDataUi.selectedDocuments

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

        updateData(updatedItems)
    }

    private fun updateUserIdentificationItem(id: String) {
        val currentItems = viewState.value.requestDataUi.selectedDocuments

        val updatedItems: List<RequestDocumentItemUi> = currentItems.map { requestDocument ->
            requestDocument.copy(
                headerUi = requestDocument.headerUi.copy(
                    nestedItems = requestDocument.headerUi.nestedItems.map {
                        it.toggleCheckboxState(id = id)
                    }
                )
            )
        }

        updateData(updatedItems)
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

/** True if any document in the list has at least one checked claim. */
private fun List<RequestDocumentItemUi>.anyClaimChecked(): Boolean {
    return any { document ->
        document.headerUi.nestedItems.anyChecked()
    }
}

/** True if any (recursively nested) leaf in the tree is a checked checkbox. */
private fun List<ExpandableListItemUi>.anyChecked(): Boolean {
    return any { item ->
        when (item) {
            is ExpandableListItemUi.NestedListItem -> item.nestedItems.anyChecked()

            is ExpandableListItemUi.SingleListItem -> {
                val trailingContentData = item.header.trailingContentData
                trailingContentData is ListItemTrailingContentDataUi.Checkbox &&
                        trailingContentData.checkboxData.isChecked
            }
        }
    }
}