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

import eu.europa.ec.commonfeature.ui.request.transformer.DomainClaim
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

//data class RequestDocumentItemUi(
//    val collapsedUiItem: CollapsedUiItem,
//    val expandedUiItems: List<ExpandedUiItem>
//)
//
//data class CollapsedUiItem(
//    val isExpanded: Boolean,
//    val uiItem: ListItemData,
//)

data class RequestDocumentItemUi(
    val domainPayload: DocumentPayloadDomain,
    val uiItem: List<ExpandableListItem>,
)

data class DocumentPayloadDomain(
    val docName: String,
    val docId: DocumentId,
    val docNamespace: NameSpace?,
    val docClaimsDomain: List<DomainClaim>,
)

//data class RequestDocumentClaim(
//    val value: DomainClaim,
//    val isRequired: Boolean,
//    val isAvailable: Boolean,
//    val path: List<String>,
//)