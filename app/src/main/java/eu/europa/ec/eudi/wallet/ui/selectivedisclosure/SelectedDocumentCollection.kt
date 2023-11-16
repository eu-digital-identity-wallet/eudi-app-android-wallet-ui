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

package eu.europa.ec.eudi.wallet.ui.selectivedisclosure

import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.DocItem
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument

class SelectedDocumentCollection {

    private val requestedDocuments = mutableMapOf<String, RequestDocument>()
    private val selectedDocItems = mutableMapOf<String, MutableList<DocItem>>()

    fun addDocument(requestedData: RequestDocument) {
        this.requestedDocuments[requestedData.documentId] = requestedData
        this.selectedDocItems[requestedData.documentId] = mutableListOf()
    }

    fun toggleDocItem(credentialName: String, docItem: DocItem) {
        if (false == selectedDocItems[credentialName]?.remove(docItem)) {
            selectedDocItems[credentialName]?.add(docItem)
        }
    }

    fun collect(): List<DisclosedDocument> {
        return requestedDocuments.keys.map { namespace ->
            val document = requestedDocuments.getValue(namespace)
            DisclosedDocument(
                document.documentId,
                document.docType,
                selectedDocItems[document.documentId]?.toList() ?: emptyList(),
                document.docRequest
            )
        }
    }

    fun clear() {
        requestedDocuments.clear()
        selectedDocItems.clear()
    }
}
