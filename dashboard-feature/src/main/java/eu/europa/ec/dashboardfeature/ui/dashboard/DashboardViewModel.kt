/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.dashboardfeature.ui.dashboard

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorPartialState
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,

    val userName: String,
    val documents: List<DocumentUi> = emptyList()
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data class NavigateToDocument(val documentId: String) : Event()
    sealed class Fab : Event() {
        data object PrimaryFabPressed : Fab()
        data object SecondaryFabPressed : Fab()
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data object OpenDeepLinkAction : Navigation()
    }
}

@KoinViewModel
class DashboardViewModel(
    private val dashboardInteractor: DashboardInteractor,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        return State(
            userName = dashboardInteractor.getUserName()
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getDocuments(event)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.NavigateToDocument -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        generateComposableNavigationLink(
                            screen = DashboardScreens.DocumentDetails,
                            arguments = generateComposableArguments(mapOf("documentId" to event.documentId))
                        )
                    )
                }
            }

            is Event.Fab.PrimaryFabPressed -> {
                navigateTo(ProximityScreens.QR)
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = ProximityScreens.QR.screenRoute
                    )
                }
            }

            is Event.Fab.SecondaryFabPressed -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(DashboardScreens.AddDocument.screenRoute)
                }
            }
        }
    }

    private fun getDocuments(event: Event) {

        setState {
            copy(
                isLoading = documents.isEmpty(),
                error = null
            )
        }

        viewModelScope.launch {
            dashboardInteractor.getDocuments().collect { response ->
                when (response) {
                    is DashboardInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.Pop) }
                                )
                            )
                        }
                    }

                    is DashboardInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documents = response.documents
                            )
                        }
                        setEffect { Effect.Navigation.OpenDeepLinkAction }
                    }
                }
            }
        }
    }

    private fun navigateTo(screen: Screen) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = screen.screenRoute
            )
        }
    }
}