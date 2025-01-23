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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.businesslogic.extension.compareLocaleLanguage
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.ui.document_details.transformer.DocumentDetailsTransformer.toListItemData
import eu.europa.ec.commonfeature.ui.document_details.transformer.transformToDocumentDetailsDocumentItem
import eu.europa.ec.commonfeature.ui.document_success.model.DocumentSuccessItemUi
import eu.europa.ec.commonfeature.ui.request.model.CollapsedUiItem
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.RelyingPartyData
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ProximitySuccessInteractorGetUiItemsPartialState {
    data class Success(
        val documentsUi: List<DocumentSuccessItemUi>,
        val headerConfig: ContentHeaderConfig,
    ) : ProximitySuccessInteractorGetUiItemsPartialState()

    data class Failed(
        val errorMessage: String
    ) : ProximitySuccessInteractorGetUiItemsPartialState()
}

interface ProximitySuccessInteractor {
    fun getUiItems(): Flow<ProximitySuccessInteractorGetUiItemsPartialState>
    fun stopPresentation()
}

class ProximitySuccessInteractorImpl(
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
) : ProximitySuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUiItems(): Flow<ProximitySuccessInteractorGetUiItemsPartialState> {
        return flow {

            val documentsUi = mutableListOf<DocumentSuccessItemUi>()

            val verifierName = walletCorePresentationController.verifierName

            val isVerified = walletCorePresentationController.verifierIsTrusted == true

            walletCorePresentationController.disclosedDocuments?.forEach { disclosedDocument ->
                try {
                    val documentId = disclosedDocument.documentId
                    val document =
                        walletCoreDocumentsController.getDocumentById(documentId = documentId) as IssuedDocument

                    val detailsDocumentItems = document.data.claims
                        .filter { claim ->
                            disclosedDocument.disclosedItems.any { disclosedItem ->
                                claim.identifier == disclosedItem.elementIdentifier
                            }
                        }
                        .map { claim ->
                            transformToDocumentDetailsDocumentItem(
                                displayKey = claim.metadata?.display?.firstOrNull {
                                    resourceProvider.getLocale()
                                        .compareLocaleLanguage(it.locale)
                                }?.name,
                                key = claim.identifier,
                                item = claim.value ?: "",
                                resourceProvider = resourceProvider,
                                documentId = documentId
                            )
                        }
                        .toListItemData()

                    val documentUi = DocumentSuccessItemUi(
                        collapsedUiItem = CollapsedUiItem(
                            uiItem = ListItemData(
                                itemId = documentId,
                                mainContentData = ListItemMainContentData.Text(text = document.name),
                                supportingText = resourceProvider.getString(R.string.document_success_collapsed_supporting_text),
                                trailingContentData = ListItemTrailingContentData.Icon(
                                    iconData = AppIcons.KeyboardArrowDown
                                )
                            ),
                            isExpanded = false
                        ),
                        expandedUiItems = detailsDocumentItems
                    )

                    documentsUi.add(documentUi)
                } catch (_: Exception) {
                }
            }

            val headerConfigDescription = if (documentsUi.isEmpty()) {
                resourceProvider.getString(R.string.document_success_header_description_when_error)
            } else {
                resourceProvider.getString(R.string.document_success_header_description)
            }
            val headerConfig = ContentHeaderConfig(
                description = headerConfigDescription,
                relyingPartyData = RelyingPartyData(
                    name = verifierName
                        ?: resourceProvider.getString(R.string.document_success_relying_party_default_name),
                    isVerified = isVerified,
                )
            )

            emit(
                ProximitySuccessInteractorGetUiItemsPartialState.Success(
                    documentsUi = documentsUi,
                    headerConfig = headerConfig
                )
            )
        }.safeAsync {
            ProximitySuccessInteractorGetUiItemsPartialState.Failed(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }
    }

    override fun stopPresentation() {
        walletCorePresentationController.stopPresentation()
    }
}