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

data class RequestDocumentsUi<T>(
    val documentsUi: List<RequestDocumentItemUi<T>>
)

data class RequestDocumentDomain(
    val id: String,
    val collapsedItem: RequestItemDomain,
    val expandedItems: List<RequestItemDomain>,
)

data class RequestItemDomain(
    val id: String?,
    val mainText: String,
    val overlineText: String? = null,
    val supportingText: String? = null,
    val corePayloadDomain: DocumentItemDomainPayload?,
    val onClick: ((String) -> Unit)? = null,
)