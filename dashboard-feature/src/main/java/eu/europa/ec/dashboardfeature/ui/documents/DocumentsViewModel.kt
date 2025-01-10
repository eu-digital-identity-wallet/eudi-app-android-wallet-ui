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
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean,
    val documents: List<ListItemData> = emptyList(),
    val showAddDocumentBottomSheet: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object GetDocuments : Event()
    data object GoToAddDocument : Event()
    data object GoToQrScan : Event()
    data class GoToDocumentDetails(val docId: DocumentId) : Event()
    data class ToggleAddDocumentBottomSheet(val isOpen: Boolean) : Event()
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
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        return State(isLoading = true)
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.GetDocuments -> {
                setState { copy(documents = interactor.getAllDocuments()) }
            }

            is Event.GoToDocumentDetails -> {
                goToDocumentDetails(event.docId)
            }

            is Event.ToggleAddDocumentBottomSheet -> {
                setState { copy(showAddDocumentBottomSheet = event.isOpen) }
            }

            is Event.GoToAddDocument -> {
                goToAddDocument()
            }

            is Event.GoToQrScan -> {
                goToQrScan()
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

    private fun goToAddDocument() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = IssuanceScreens.AddDocument,
                    arguments = generateComposableArguments(
                        mapOf("flowType" to IssuanceFlowUiConfig.EXTRA_DOCUMENT)
                    )
                )
            )
        }
    }

    private fun goToQrScan() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = CommonScreens.QrScan,
                    arguments = generateComposableArguments(
                        mapOf(
                            QrScanUiConfig.serializedKeyName to uiSerializer.toBase64(
                                QrScanUiConfig(
                                    title = resourceProvider.getString(R.string.issuance_qr_scan_title),
                                    subTitle = resourceProvider.getString(R.string.issuance_qr_scan_subtitle),
                                    qrScanFlow = QrScanFlow.Issuance(IssuanceFlowUiConfig.EXTRA_DOCUMENT)
                                ),
                                QrScanUiConfig.Parser
                            )
                        )
                    )
                ),
                inclusive = false
            )
        }
    }
}