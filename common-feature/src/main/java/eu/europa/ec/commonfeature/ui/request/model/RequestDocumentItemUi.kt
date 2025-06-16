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

import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

sealed class DomainDocumentFormat {
    data object SdJwtVc : DomainDocumentFormat()
    data class MsoMdoc(val namespace: String) :
        DomainDocumentFormat()

    companion object {
        fun getFormat(format: DocumentFormat, namespace: String?): DomainDocumentFormat {
            return when (format) {
                is SdJwtVcFormat -> SdJwtVc
                is MsoMdocFormat -> MsoMdoc(
                    namespace = namespace.toString()
                )
            }
        }
    }
}

data class RequestDocumentItemUi(
    val domainPayload: DocumentPayloadDomain,
    val headerUi: ExpandableListItemUi.NestedListItem,
)

data class DocumentPayloadDomain(
    val docName: String,
    val docId: DocumentId,
    val domainDocFormat: DomainDocumentFormat,
    val docClaimsDomain: List<ClaimDomain>,
)