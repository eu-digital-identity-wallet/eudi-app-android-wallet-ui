/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
