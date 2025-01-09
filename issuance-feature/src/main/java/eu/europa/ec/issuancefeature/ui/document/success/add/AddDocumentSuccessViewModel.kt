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

package eu.europa.ec.issuancefeature.ui.document.success.add

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.ui.document_success.DocumentSuccessViewModel
import eu.europa.ec.commonfeature.ui.document_success.Event
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.issuancefeature.interactor.document.DocumentIssuanceSuccessInteractor
import eu.europa.ec.issuancefeature.interactor.document.DocumentIssuanceSuccessInteractorGetUiItemsPartialState
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.DashboardScreens
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class AddDocumentSuccessViewModel(
    private val interactor: DocumentIssuanceSuccessInteractor,
    @InjectedParam private val flowType: IssuanceFlowUiConfig,
    @InjectedParam private val documentId: DocumentId,
) : DocumentSuccessViewModel() {

    override fun getNextScreenConfigNavigation(): ConfigNavigation {
        return when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> ConfigNavigation(
                navigationType = NavigationType.PushScreen(
                    screen = DashboardScreens.Dashboard
                )
            )

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ConfigNavigation(
                navigationType = NavigationType.PopTo(
                    screen = DashboardScreens.Dashboard
                )
            )
        }
    }

    override fun doWork() {
        setState {
            copy(isLoading = true)
        }

        viewModelScope.launch {
            interactor.getUiItems(documentIds = listOf(documentId)).collect { response ->
                when (response) {
                    is DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Failed -> {
                        setState {
                            copy(
                                error = ContentErrorConfig(
                                    errorSubTitle = response.errorMessage,
                                    onCancel = {
                                        setEvent(Event.Close) //TODO Is this approach ok?
                                    }
                                ),
                                isLoading = false,
                            )
                        }
                    }

                    is DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Success -> {
                        setState {
                            copy(
                                headerConfig = response.headerConfig,
                                items = response.documentsUi,
                                error = null,
                                isLoading = false,
                            )
                        }
                    }
                }
            }
        }
    }
}