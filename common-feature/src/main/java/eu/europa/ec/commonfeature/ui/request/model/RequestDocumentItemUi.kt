/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.commonfeature.ui.request.model

import eu.europa.ec.eudi.iso18013.transfer.DocItem
import eu.europa.ec.eudi.iso18013.transfer.DocRequest
import eu.europa.ec.eudi.wallet.document.ElementIdentifier

data class RequestDocumentItemUi<T>(
    val id: String,
    val domainPayload: DocumentItemDomainPayload,
    val readableName: String,
    val checked: Boolean,
    val enabled: Boolean,
    val docItem: DocItem,
    val event: T? = null
)

data class DocumentItemDomainPayload(
    val docId: String,
    val docType: String,
    val docRequest: DocRequest,
    val namespace: String,
    val elementIdentifier: ElementIdentifier
) {
    // We need to override equals in order for "groupBy" internal comparisons
    override fun equals(other: Any?): Boolean {
        return if (other is DocumentItemDomainPayload) {
            other.docId == this.docId
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return docId.hashCode()
    }
}

fun <T> DocItem.toRequestDocumentItemUi(
    uID: String,
    docPayload: DocumentItemDomainPayload,
    readableName: String,
    optional: Boolean,
    event: T?
): RequestDocumentItemUi<T> {
    return RequestDocumentItemUi(
        id = uID,
        domainPayload = docPayload,
        checked = true,
        enabled = optional,
        readableName = readableName,
        docItem = this,
        event = event
    )
}

fun DocRequest.produceDocUID(elementIdentifier: ElementIdentifier): String =
    docType + elementIdentifier