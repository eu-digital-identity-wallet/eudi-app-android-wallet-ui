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

package eu.europa.ec.dashboardfeature.ui.sign

import android.content.Context
import android.net.Uri
import eu.europa.ec.dashboardfeature.interactor.DocumentSignInteractor
import eu.europa.ec.dashboardfeature.model.SignDocumentButtonUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val title: String,
    val subtitle: String,
    val buttonUi: SignDocumentButtonUi
) : ViewState

sealed class Event : ViewEvent {
    data object Pop : Event()
    data object OnSelectDocument : Event()
    data class DocumentUriRetrieved(val context: Context, val uri: Uri) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
    }

    data class OpenDocumentSelection(val selection: List<String>) : Effect()
}

@KoinViewModel
class DocumentSignViewModel(
    private val documentSignInteractor: DocumentSignInteractor,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State(
        title = resourceProvider.getString(R.string.document_sign_title),
        subtitle = resourceProvider.getString(R.string.document_sign_subtitle),
        buttonUi = documentSignInteractor.getUiItem(),
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.OnSelectDocument -> {
                setEffect { Effect.OpenDocumentSelection(listOf("application/pdf")) }
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.DocumentUriRetrieved -> documentSignInteractor.launchRqesSdk(
                event.context,
                event.uri
            )
        }
    }
}