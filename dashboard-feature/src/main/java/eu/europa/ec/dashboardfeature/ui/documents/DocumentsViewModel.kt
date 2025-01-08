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

package eu.europa.ec.dashboardfeature.ui.documents

import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean,
    val documents: List<ListItemData> = emptyList(),
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data class GoToDocumentDetails(val docId: DocumentId) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()
    }
}

@KoinViewModel
class DocumentsViewModel(
    val interactor: DocumentsInteractor,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        return State(isLoading = true)
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                setState { copy(documents = interactor.getAllDocuments()) }
            }
            is Event.GoToDocumentDetails -> {
                goToDocumentDetails(event.docId)
            }
        }
    }

    private fun goToDocumentDetails(docId: DocumentId) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = IssuanceScreens.DocumentDetails,
                    arguments = generateComposableArguments(
                        mapOf(
                            "detailsType" to IssuanceFlowUiConfig.EXTRA_DOCUMENT,
                            "documentId" to docId
                        )
                    )
                )
            )
        }
    }
}