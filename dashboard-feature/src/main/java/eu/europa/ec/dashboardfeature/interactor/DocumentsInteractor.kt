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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.util.extractValueFromDocumentOrEmpty
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.MainContentData

interface DocumentsInteractor {
    fun getAllDocuments(): List<ListItemData>
}

class DocumentsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val documentsController: WalletCoreDocumentsController,
) : DocumentsInteractor {
    override fun getAllDocuments(): List<ListItemData> {
        return documentsController.getAllDocuments().map {
            val documentExpirationDate: String = when (it) {
                is IssuedDocument -> {
                    "${resourceProvider.getString(R.string.dashboard_document_has_not_expired)}: " +
                            extractValueFromDocumentOrEmpty(
                                document = it,
                                key = DocumentJsonKeys.EXPIRY_DATE
                            )
                }

                else -> ""
            }
            ListItemData(
                itemId = it.id,
                mainContentData = MainContentData.Text(text = it.name),
                overlineText = "Hellenic Goverment", // TODO Here we want to show issuer name
                supportingText = documentExpirationDate,
                leadingContentData = ListItemLeadingContentData.Icon(
                    iconData = AppIcons.IssuerPlaceholder
                ), // TODO Get the actual issuer image
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowRight
                )
            )
        }
    }
}