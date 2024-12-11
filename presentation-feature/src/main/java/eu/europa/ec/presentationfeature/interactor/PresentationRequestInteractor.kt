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

package eu.europa.ec.presentationfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.config.toDomainConfig
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.corelogic.controller.TransferEventPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class PresentationRequestInteractorPartialState {
    data class Success(
        val verifierName: String? = null,
        val verifierIsTrusted: Boolean,
        val requestDocuments: List<RequestDataUi<Event>>
    ) : PresentationRequestInteractorPartialState()

    data class NoData(
        val verifierName: String? = null,
        val verifierIsTrusted: Boolean,
    ) : PresentationRequestInteractorPartialState()

    data class Failure(val error: String) : PresentationRequestInteractorPartialState()
    data object Disconnect : PresentationRequestInteractorPartialState()
}

interface PresentationRequestInteractor {
    fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(items: List<RequestDataUi<Event>>)
    fun setConfig(config: RequestUriConfig)
}

class PresentationRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : PresentationRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun setConfig(config: RequestUriConfig) {
        walletCorePresentationController.setConfig(config.toDomainConfig())
    }

    override fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState> =
        walletCorePresentationController.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    if (response.requestData.all { it.requestedItems.isEmpty() }) {
                        PresentationRequestInteractorPartialState.NoData(
                            verifierName = response.verifierName,
                            verifierIsTrusted = response.verifierIsTrusted,
                        )
                    } else {
                        val requestDataUi = RequestTransformer.transformToUiItems(
                            storageDocuments = walletCoreDocumentsController.getAllIssuedDocuments(),
                            requestDocuments = response.requestData,
                            resourceProvider = resourceProvider,
                        )
                        PresentationRequestInteractorPartialState.Success(
                            verifierName = response.verifierName,
                            verifierIsTrusted = response.verifierIsTrusted,
                            requestDocuments = requestDataUi
                        )
                    }
                }

                is TransferEventPartialState.Error -> {
                    PresentationRequestInteractorPartialState.Failure(error = response.error)
                }

                is TransferEventPartialState.Disconnected -> {
                    PresentationRequestInteractorPartialState.Disconnect
                }

                else -> null
            }
        }.safeAsync {
            PresentationRequestInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun stopPresentation() {
        walletCorePresentationController.stopPresentation()
    }

    override fun updateRequestedDocuments(items: List<RequestDataUi<Event>>) {
        val disclosedDocuments = RequestTransformer.transformToDomainItems(items)
        walletCorePresentationController.updateRequestedDocuments(disclosedDocuments.toMutableList())
    }

    /*private fun toDomainItems(
        storageDocuments: List<IssuedDocument>,
        requestDocuments: List<RequestDocument>,
        resourceProvider: ResourceProvider,
    ): List<RequestDocumentDomain> {

        val resultItems = mutableListOf<RequestDocumentDomain>()

        requestDocuments.forEachIndexed { docIndex, requestDocument ->
            val storageDocument = storageDocuments.first { it.id == requestDocument.documentId }

            val expandedItemsDomain = mutableListOf<RequestItemDomain>()

            requestDocument.docRequest.requestItems.forEachIndexed { itemIndex, docItem ->

                //TODO isAvailable needed in Domain level or no?
                val (value, isAvailable) = try {
                    val values = StringBuilder()
                    parseKeyValueUi(
                        json = storageDocument.nameSpacedDataJSONObject.getDocObject(
                            nameSpace = docItem.namespace
                        )[docItem.elementIdentifier],
                        groupIdentifier = docItem.elementIdentifier,
                        resourceProvider = resourceProvider,
                        allItems = values
                    )
                    (values.toString() to true)
                } catch (ex: Exception) {
                    (resourceProvider.getString(R.string.request_element_identifier_not_available) to false)
                }

                val itemId = requestDocument.docRequest.produceDocUID(
                    elementIdentifier = docItem.elementIdentifier,
                    documentId = requestDocument.documentId
                )

                //TODO optional needed in Domain level or no?
                val optional = !getMandatoryFields(
                    documentIdentifier = requestDocument.toDocumentIdentifier()
                ).contains(docItem.elementIdentifier)

                val requestItemDomain = RequestItemDomain(
                    id = itemId,
                    mainText = value,
                    overlineText = resourceProvider.getReadableElementIdentifier(docItem.elementIdentifier),
                    supportingText = null,
                    onClick = { id ->
                        println("Giannis - requestItemDomain onClick itemId: $id or $itemId")
                        //TODO needed or not?
                    },
                    corePayloadDomain = DocumentItemDomainPayload(
                        docId = requestDocument.documentId,
                        docRequest = requestDocument.docRequest,
                        docType = requestDocument.docType,
                        namespace = docItem.namespace,
                        elementIdentifier = docItem.elementIdentifier,
                    )
                )

                expandedItemsDomain.add(requestItemDomain)
            }

            val requestDocumentDomain = RequestDocumentDomain(
                id = docIndex.toString(),
                collapsedItem = RequestItemDomain(
                    id = null,
                    mainText = requestDocument.toUiName(resourceProvider),
                    overlineText = null,
                    supportingText = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                    corePayloadDomain = null,
                    onClick = {
                        println("Giannis - requestDocumentDomain onClick itemId: $docIndex")
                        //TODO
                    }
                ),
                expandedItems = expandedItemsDomain,
            )
            resultItems.add(requestDocumentDomain)
        }

        return resultItems
    }*/
}