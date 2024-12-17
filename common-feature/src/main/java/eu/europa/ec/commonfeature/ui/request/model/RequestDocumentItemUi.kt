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

package eu.europa.ec.commonfeature.ui.request.model

import eu.europa.ec.commonfeature.util.keyIsBase64
import eu.europa.ec.eudi.iso18013.transfer.response.DocItem
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.uilogic.component.ListItemData

//TODO Giannis To be removed
data class RequestDocumentItemUiOld<T>(
    val id: String,
    val domainPayload: DocumentDomainPayload,
    val readableName: String,
    val value: String,
    val checked: Boolean,
    val enabled: Boolean,
    val docItem: DocItem,
    val event: T? = null
) {
    val keyIsBase64: Boolean
        get() {
            return keyIsBase64(docItem.elementIdentifier)
        }
}

data class RequestDocumentItemUi(
    val collapsedUiItem: CollapsedUiItem,
    val expandedUiItems: List<ExpandedUiItem>
)

data class CollapsedUiItem(
    val isExpanded: Boolean,
    val uiItem: ListItemData,
)

data class ExpandedUiItem(
    val domainPayload: DocumentDomainPayload,
    val uiItem: ListItemData,
)

data class DocumentDomainPayload(
    val docName: String,
    val docId: DocumentId,
    val docNamespace: NameSpace,
    val documentDetailsDomain: DocumentDetailsDomain
) {
    // We need to override equals in order for "groupBy" internal comparisons
    override fun equals(other: Any?): Boolean {
        return if (other is DocumentDomainPayload) {
            other.docId == this.docId
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return docId.hashCode()
    }
}

//TODO Giannis Should this be in other package?
data class DocumentDetailsDomain(
    val items: List<DocumentItemDomain>
)

data class DocumentItemDomain(
    val elementIdentifier: ElementIdentifier,
    val value: String,
    val readableName: String,
    val isRequired: Boolean,
    val isAvailable: Boolean,
)

val Document.docType: String
    get() = (this.format as? MsoMdocFormat)?.docType.orEmpty()

fun generateUniqueFieldId(
    elementIdentifier: ElementIdentifier,
    documentId: DocumentId,
): String =
    elementIdentifier + documentId