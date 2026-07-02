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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.businesslogic.extension.ifEmptyOrNull
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.extension.toExpandableListItems
import eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractor
import eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractorDelegate
import eu.europa.ec.commonfeature.util.transformPathsToDomainClaims
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.extension.toClaimPaths
import eu.europa.ec.corelogic.model.ClaimItemId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ProximitySuccessInteractorGetUiItemsPartialState {
    data class Success(
        val documentsUi: List<ExpandableListItemUi.NestedListItem>,
        val headerConfig: ContentHeaderConfig,
    ) : ProximitySuccessInteractorGetUiItemsPartialState()

    data class Failed(
        val errorMessage: String,
    ) : ProximitySuccessInteractorGetUiItemsPartialState()
}

interface ProximitySuccessInteractor : ScopedPresentationInteractor {
    fun getUiItems(): Flow<ProximitySuccessInteractorGetUiItemsPartialState>
    fun stopPresentation()
}

class ProximitySuccessInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
    walletCorePresentationController: WalletCorePresentationController? = null
) : ProximitySuccessInteractor,
    ScopedPresentationInteractorDelegate(walletCorePresentationController) {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUiItems(): Flow<ProximitySuccessInteractorGetUiItemsPartialState> {
        return flow {

            val documentsUi = mutableListOf<ExpandableListItemUi.NestedListItem>()

            val verifierName = walletCorePresentationController.verifierName

            val isVerified = walletCorePresentationController.verifierIsTrusted == true

            walletCorePresentationController.disclosedDocuments?.forEach { selection ->
                try {
                    val documentId = selection.documentId
                    val document =
                        walletCoreDocumentsController.getDocumentById(documentId = documentId) as IssuedDocument

                    // the request can be a wildcard or ancestor (e.g. ["nationalities", null]) that
                    // won't equal a stored path, so keep every stored leaf it matches
                    val storagePaths = document.data.claims.flatMap { it.toClaimPaths() }
                    val requestedPaths = selection.selectedClaims
                    val disclosedClaimPaths = storagePaths.filter { available ->
                        requestedPaths.any { requested -> requested.matches(available) }
                    }

                    val disclosedClaims = transformPathsToDomainClaims(
                        paths = disclosedClaimPaths,
                        claims = document.data.claims,
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )

                    val disclosedClaimsUi = disclosedClaims.map { disclosedClaim ->
                        disclosedClaim.toExpandableListItems(
                            docId = documentId,
                            queryId = selection.queryId,
                        )
                    }

                    if (disclosedClaimsUi.isNotEmpty()) {
                        val disclosedDocumentUi = ExpandableListItemUi.NestedListItem(
                            header = ListItemDataUi(
                                itemId = ClaimItemId.DocumentHeader(
                                    docId = documentId,
                                    queryId = selection.queryId,
                                ).encode(),
                                mainContentData = ListItemMainContentDataUi.Text(text = document.name),
                                supportingText = resourceProvider.getString(R.string.document_success_collapsed_supporting_text),
                                trailingContentData = ListItemTrailingContentDataUi.Icon(
                                    iconData = AppIcons.KeyboardArrowDown
                                )
                            ),
                            nestedItems = disclosedClaimsUi,
                            isExpanded = false,
                        )

                        documentsUi.add(disclosedDocumentUi)
                    }
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
                relyingPartyData = RelyingPartyDataUi(
                    name = verifierName.ifEmptyOrNull(
                        default = resourceProvider.getString(R.string.document_success_relying_party_default_name)
                    ),
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