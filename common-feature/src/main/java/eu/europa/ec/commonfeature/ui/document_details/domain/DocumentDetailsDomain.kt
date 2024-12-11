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

package eu.europa.ec.commonfeature.ui.document_details.domain

import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.eudi.wallet.document.NameSpace

data class DocumentItem(
    val elementIdentifier: ElementIdentifier,
    val value: String,
    val readableName: String,
    val docId: DocumentId
)

data class DocumentDetailsDomain(
    val docName: String,
    val docId: DocumentId,
    val docNamespace: NameSpace,
    val documentIdentifier: DocumentIdentifier,
    val documentExpirationDateFormatted: String,
    val documentHasExpired: Boolean,
    val documentImage: String,
    val userFullName: String?,
    val detailsItems: List<DocumentItem>
)