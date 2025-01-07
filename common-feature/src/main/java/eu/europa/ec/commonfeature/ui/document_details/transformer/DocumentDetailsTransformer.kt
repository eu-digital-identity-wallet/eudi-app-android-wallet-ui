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

package eu.europa.ec.commonfeature.ui.document_details.transformer

import eu.europa.ec.businesslogic.extension.compareLocaleLanguage
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.ui.document_details.domain.DocumentDetailsDomain
import eu.europa.ec.commonfeature.ui.document_details.domain.DocumentItem
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.ui.request.model.generateUniqueFieldId
import eu.europa.ec.commonfeature.util.documentHasExpired
import eu.europa.ec.commonfeature.util.extractFullNameFromDocumentOrEmpty
import eu.europa.ec.commonfeature.util.extractValueFromDocumentOrEmpty
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.commonfeature.util.parseKeyValueUi
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData

object DocumentDetailsTransformer {

    fun transformToDocumentDetailsDomain(
        document: IssuedDocument,
        resourceProvider: ResourceProvider
    ): Result<DocumentDetailsDomain> = runCatching {

        val detailsDocumentItems = document.data.claims
            .map { claim ->
                transformToDocumentDetailsDocumentItem(
                    displayKey = claim.metadata?.display?.firstOrNull {
                        resourceProvider.getLocale().compareLocaleLanguage(it.locale)
                    }?.name,
                    key = claim.identifier,
                    item = claim.value ?: "",
                    resourceProvider = resourceProvider,
                    documentId = document.id
                )
            }

        val documentImage = extractValueFromDocumentOrEmpty(
            document = document,
            key = DocumentJsonKeys.PORTRAIT
        )

        val documentExpirationDate = extractValueFromDocumentOrEmpty(
            document = document,
            key = DocumentJsonKeys.EXPIRY_DATE
        )

        val docHasExpired = documentHasExpired(documentExpirationDate)

        return@runCatching DocumentDetailsDomain(
            docName = document.name,
            docId = document.id,
            documentIdentifier = document.toDocumentIdentifier(),
            documentExpirationDateFormatted = documentExpirationDate.toDateFormatted().orEmpty(),
            documentHasExpired = docHasExpired,
            documentImage = documentImage,
            userFullName = extractFullNameFromDocumentOrEmpty(document),
            detailsItems = detailsDocumentItems
        )
    }

    fun DocumentDetailsDomain.transformToDocumentDetailsUi(): DocumentUi {
        val documentDetailsListItemData = this.detailsItems.map { documentItem ->
            documentItem.toListItemData()
        }
        return DocumentUi(
            documentId = this.docId,
            documentName = this.docName,
            documentIdentifier = this.documentIdentifier,
            documentExpirationDateFormatted = this.documentExpirationDateFormatted,
            documentHasExpired = this.documentHasExpired,
            documentImage = this.documentImage,
            documentDetails = documentDetailsListItemData,
            userFullName = this.userFullName,
            documentIssuanceState = DocumentUiIssuanceState.Issued,
        )
    }

    private fun DocumentItem.toListItemData(): ListItemData {

        val mainTextContentData = when {
            keyIsPortrait(key = this.elementIdentifier) -> {
                ListItemMainContentData.Text(text = "")
            }

            keyIsSignature(key = this.elementIdentifier) -> {
                ListItemMainContentData.Image(base64Image = this.value)
            }

            else -> {
                ListItemMainContentData.Text(text = this.value)
            }
        }

        val itemId = generateUniqueFieldId(
            elementIdentifier = this.elementIdentifier,
            documentId = this.docId
        )

        val leadingContent = if (keyIsPortrait(key = this.elementIdentifier)) {
            ListItemLeadingContentData.UserImage(userBase64Image = this.value)
        } else {
            null
        }

        return ListItemData(
            itemId = itemId,
            mainContentData = mainTextContentData,
            overlineText = this.readableName,
            leadingContentData = leadingContent
        )
    }
}

private fun transformToDocumentDetailsDocumentItem(
    key: String,
    displayKey: String?,
    item: Any,
    resourceProvider: ResourceProvider,
    documentId: String
): DocumentItem {

    val values = StringBuilder()
    val localizedKey = displayKey ?: key

    parseKeyValueUi(
        item = item,
        groupIdentifier = localizedKey,
        resourceProvider = resourceProvider,
        allItems = values
    )
    val groupedValues = values.toString()

    return DocumentItem(
        elementIdentifier = key,
        value = groupedValues,
        readableName = localizedKey,
        docId = documentId
    )
}