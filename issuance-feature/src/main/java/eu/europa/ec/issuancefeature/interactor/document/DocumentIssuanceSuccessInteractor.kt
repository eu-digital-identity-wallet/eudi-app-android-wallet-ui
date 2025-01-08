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

package eu.europa.ec.issuancefeature.interactor.document

import eu.europa.ec.businesslogic.extension.compareLocaleLanguage
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.ui.document_details.transformer.DocumentDetailsTransformer.toListItemData
import eu.europa.ec.commonfeature.ui.document_details.transformer.transformToDocumentDetailsDocumentItem
import eu.europa.ec.commonfeature.ui.document_success.model.DocumentSuccessItemUi
import eu.europa.ec.commonfeature.ui.request.model.CollapsedUiItem
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.eudi.wallet.document.DocumentId
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

interface DocumentIssuanceSuccessInteractor {
    fun getUiItem(documentId: DocumentId): Flow<DocumentIssuanceSuccessInteractorGetUiItemsPartialState>
}

class DocumentIssuanceSuccessInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
) : DocumentIssuanceSuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUiItem(documentId: DocumentId): Flow<DocumentIssuanceSuccessInteractorGetUiItemsPartialState> {
        return flow {
            val document =
                walletCoreDocumentsController.getDocumentById(documentId = documentId) as IssuedDocument

            val issuerName = document.data.metadata?.credentialIssuerIdentifier
                ?: resourceProvider.getString(R.string.issuance_success_header_issuer_default_name) //TODO where do we get this information from?
            val issuerIsTrusted = true //TODO where do we get this information from?

            val detailsDocumentItems = document.data.claims
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
                    ).toListItemData()
                }

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

            val headerConfig = ContentHeaderConfig(
                description = resourceProvider.getString(R.string.issuance_success_header_description),
                relyingPartyData = RelyingPartyData(
                    name = issuerName,
                    isVerified = issuerIsTrusted,
                )
            )

            emit(
                DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Success(
                    documentUi = documentUi,
                    headerConfig = headerConfig,
                )
            )
        }.safeAsync {
            DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Failed(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }
    }
}

sealed class DocumentIssuanceSuccessInteractorGetUiItemsPartialState {
    data class Success(
        val documentUi: DocumentSuccessItemUi,
        val headerConfig: ContentHeaderConfig,
    ) : DocumentIssuanceSuccessInteractorGetUiItemsPartialState()

    data class Failed(
        val errorMessage: String
    ) : DocumentIssuanceSuccessInteractorGetUiItemsPartialState()
}