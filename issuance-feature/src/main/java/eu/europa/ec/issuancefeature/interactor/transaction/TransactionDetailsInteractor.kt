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

package eu.europa.ec.issuancefeature.interactor.transaction

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.util.formatInstant
import eu.europa.ec.commonfeature.extensions.toExpandableListItems
import eu.europa.ec.commonfeature.model.TransactionUiStatus.Companion.toUiText
import eu.europa.ec.commonfeature.model.toTransactionUiStatus
import eu.europa.ec.commonfeature.util.createKeyValue
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.getLocalizedDocumentName
import eu.europa.ec.corelogic.extension.sortRecursivelyBy
import eu.europa.ec.corelogic.model.ClaimPath.Companion.toClaimPath
import eu.europa.ec.corelogic.model.DomainClaim
import eu.europa.ec.corelogic.model.TransactionLogData
import eu.europa.ec.corelogic.model.TransactionLogData.Companion.getTransactionTypeLabel
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import eu.europa.ec.issuancefeature.model.transaction.details.TransactionDetailsCardData
import eu.europa.ec.issuancefeature.model.transaction.details.TransactionDetailsDataSharedHolder
import eu.europa.ec.issuancefeature.model.transaction.details.TransactionDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

sealed class TransactionDetailsInteractorPartialState {
    data class Success(
        val transactionDetailsUi: TransactionDetailsUi,
    ) : TransactionDetailsInteractorPartialState()

    data class Failure(val error: String) : TransactionDetailsInteractorPartialState()
}

interface TransactionDetailsInteractor {
    fun getTransactionDetails(
        transactionId: String
    ): Flow<TransactionDetailsInteractorPartialState>
}

class TransactionDetailsInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
) : TransactionDetailsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getTransactionDetails(transactionId: String): Flow<TransactionDetailsInteractorPartialState> =
        flow {
            walletCoreDocumentsController.getTransactionLog(id = transactionId)
                ?.let { transaction ->

                    val userLocale = resourceProvider.getLocale()

                    val relyingPartyData: TransactionLog.RelyingParty?
                    val dataShared: List<ExpandableListItem.NestedListItemData>?

                    when (transaction) {
                        is TransactionLogData.IssuanceLog -> {
                            //TODO change this once Core supports more transaction types
                            relyingPartyData = null
                            dataShared = null
                        }

                        is TransactionLogData.PresentationLog -> {
                            relyingPartyData = transaction.relyingParty

                            dataShared = transaction.documents.toGroupedNestedClaims(
                                documentSupportingText = resourceProvider.getString(R.string.transaction_details_collapsed_supporting_text),
                                itemIdentifierPrefix = resourceProvider.getString(R.string.transaction_details_data_shared_prefix_id),
                                userLocale = userLocale,
                                resourceProvider = resourceProvider,
                            )
                        }

                        is TransactionLogData.SigningLog -> {
                            //TODO change this once Core supports more transaction types
                            relyingPartyData = null
                            dataShared = null
                        }
                    }

                    val transactionDetailsUi = TransactionDetailsUi(
                        transactionId = transactionId,
                        transactionDetailsCardData = TransactionDetailsCardData(
                            transactionTypeLabel = transaction.getTransactionTypeLabel(
                                resourceProvider
                            ),
                            transactionStatusLabel = transaction.status
                                .toTransactionUiStatus()
                                .toUiText(resourceProvider),
                            transactionDate = transaction.creationDate.formatInstant(),
                            relyingPartyName = relyingPartyData?.name,
                            relyingPartyIsVerified = relyingPartyData?.isVerified
                        ),
                        transactionDetailsDataShared = TransactionDetailsDataSharedHolder(
                            dataSharedItems = dataShared ?: emptyList()
                        ),
                        transactionDetailsDataSigned = null //TODO change this once Core adds support for it
                    )

                    emit(
                        TransactionDetailsInteractorPartialState.Success(
                            transactionDetailsUi = transactionDetailsUi
                        )
                    )
                } ?: emit(
                TransactionDetailsInteractorPartialState.Failure(
                    error = genericErrorMsg
                )
            )
        }.safeAsync {
            TransactionDetailsInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    private fun List<PresentedDocument>.toGroupedNestedClaims(
        documentSupportingText: String,
        itemIdentifierPrefix: String,
        userLocale: Locale,
        resourceProvider: ResourceProvider
    ): List<ExpandableListItem.NestedListItemData> {
        return this.mapIndexed { index, presentedDocument ->
            val domainClaims: MutableList<DomainClaim> = mutableListOf()

            presentedDocument.claims.forEach { presentedClaim ->
                val elementIdentifier = when (presentedDocument.format) {
                    is MsoMdocFormat -> presentedClaim.path.last()
                    is SdJwtVcFormat -> presentedClaim.path.joinToString(".")
                }

                val itemPath = when (presentedDocument.format) {
                    is MsoMdocFormat -> listOf(elementIdentifier)
                    is SdJwtVcFormat -> presentedClaim.path
                }.toClaimPath()

                createKeyValue(
                    item = presentedClaim.value!!,
                    groupKey = elementIdentifier,
                    disclosurePath = itemPath,
                    resourceProvider = resourceProvider,
                    metadata = presentedDocument.metadata,
                    allItems = domainClaims,
                )
            }

            val uniqueId = itemIdentifierPrefix + index

            ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = uniqueId,
                    mainContentData = ListItemMainContentData.Text(
                        text = presentedDocument.metadata.getLocalizedDocumentName(
                            userLocale = userLocale,
                            fallback = presentedDocument.metadata?.display?.firstOrNull()?.name
                                ?: when (val format = presentedDocument.format) {
                                    is MsoMdocFormat -> format.docType
                                    is SdJwtVcFormat -> format.vct
                                }
                        )
                    ),
                    supportingText = documentSupportingText,
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowDown
                    )
                ),
                nestedItems = domainClaims
                    .sortRecursivelyBy {
                        it.displayTitle.lowercase()
                    }.map { domainClaim ->
                        domainClaim.toExpandableListItems(docId = uniqueId)
                    },
                isExpanded = false
            )
        }
    }
}