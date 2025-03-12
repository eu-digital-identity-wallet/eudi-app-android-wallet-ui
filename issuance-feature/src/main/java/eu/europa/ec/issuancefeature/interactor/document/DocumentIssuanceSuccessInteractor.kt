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

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.extensions.toExpandableListItems
import eu.europa.ec.commonfeature.util.transformPathsToDomainClaims
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.localizedIssuerMetadata
import eu.europa.ec.corelogic.extension.toClaimPaths
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
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.URI

sealed class DocumentIssuanceSuccessInteractorGetUiItemsPartialState {
    data class Success(
        val documentsUi: List<ExpandableListItem.NestedListItemData>,
        val headerConfig: ContentHeaderConfig,
    ) : DocumentIssuanceSuccessInteractorGetUiItemsPartialState()

    data class Failed(
        val errorMessage: String,
    ) : DocumentIssuanceSuccessInteractorGetUiItemsPartialState()
}

interface DocumentIssuanceSuccessInteractor {
    fun getUiItems(documentIds: List<DocumentId>): Flow<DocumentIssuanceSuccessInteractorGetUiItemsPartialState>
}

class DocumentIssuanceSuccessInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
) : DocumentIssuanceSuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUiItems(documentIds: List<DocumentId>): Flow<DocumentIssuanceSuccessInteractorGetUiItemsPartialState> {
        return flow {

            val documentsUi = mutableListOf<ExpandableListItem.NestedListItemData>()

            var issuerName =
                resourceProvider.getString(R.string.issuance_success_header_issuer_default_name)
            val issuerIsTrusted = false
            var issuerLogo: URI? = null

            val userLocale = resourceProvider.getLocale()

            documentIds.forEach { documentId ->
                try {
                    val document =
                        walletCoreDocumentsController.getDocumentById(documentId = documentId) as IssuedDocument

                    val localizedIssuerMetadata = document.localizedIssuerMetadata(userLocale)

                    localizedIssuerMetadata?.name?.let { safeIssuerName ->
                        issuerName = safeIssuerName
                    }

                    localizedIssuerMetadata?.logo?.uri?.let { safeIssuerLogo ->
                        issuerLogo = safeIssuerLogo
                    }

                    val claimsPaths = document.data.claims.flatMap { claim ->
                        claim.toClaimPaths()
                    }

                    val domainClaims = transformPathsToDomainClaims(
                        paths = claimsPaths,
                        claims = document.data.claims,
                        metadata = document.metadata,
                        resourceProvider = resourceProvider,
                    )

                    val claimsUi = domainClaims.map { selectedDomainClaim ->
                        selectedDomainClaim.toExpandableListItems(docId = documentId)
                    }

                    val documentUi = ExpandableListItem.NestedListItemData(
                        header = ListItemData(
                            itemId = documentId,
                            mainContentData = ListItemMainContentData.Text(text = document.name),
                            supportingText = resourceProvider.getString(R.string.document_success_collapsed_supporting_text),
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = AppIcons.KeyboardArrowDown
                            )
                        ),
                        nestedItems = claimsUi,
                        isExpanded = false,
                    )

                    documentsUi.add(documentUi)
                } catch (_: Exception) {
                }
            }

            val headerConfigDescription = if (documentsUi.isEmpty()) {
                resourceProvider.getString(R.string.issuance_success_header_description_when_error)
            } else {
                resourceProvider.getString(R.string.issuance_success_header_description)
            }
            val headerConfig = ContentHeaderConfig(
                description = headerConfigDescription,
                relyingPartyData = RelyingPartyData(
                    logo = issuerLogo,
                    name = issuerName,
                    isVerified = issuerIsTrusted,
                )
            )

            emit(
                DocumentIssuanceSuccessInteractorGetUiItemsPartialState.Success(
                    documentsUi = documentsUi,
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