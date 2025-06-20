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

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.businesslogic.util.formatInstant
import eu.europa.ec.commonfeature.extension.toExpandableListItems
import eu.europa.ec.commonfeature.model.DocumentDetailsUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.ui.document_details.domain.DocumentDetailsDomain
import eu.europa.ec.commonfeature.util.documentHasExpired
import eu.europa.ec.commonfeature.util.transformPathsToDomainClaims
import eu.europa.ec.corelogic.extension.toClaimPaths
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider

object DocumentDetailsTransformer {

    fun transformToDocumentDetailsDomain(
        document: IssuedDocument,
        resourceProvider: ResourceProvider,
        uuidProvider: UuidProvider
    ): Result<DocumentDetailsDomain> = runCatching {
        val claimsPaths = document.data.claims.flatMap { claim ->
            claim.toClaimPaths()
        }

        val domainClaims = transformPathsToDomainClaims(
            paths = claimsPaths,
            claims = document.data.claims,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider
        )

        val docHasExpired = documentHasExpired(document.validUntil)

        return@runCatching DocumentDetailsDomain(
            docName = document.name,
            docId = document.id,
            documentIdentifier = document.toDocumentIdentifier(),
            documentExpirationDateFormatted = document.validUntil.formatInstant(),
            documentHasExpired = docHasExpired,
            documentClaims = domainClaims
        )
    }

    fun DocumentDetailsDomain.transformToDocumentDetailsUi(): DocumentDetailsUi {
        val documentDetailsUi = this.documentClaims.map { domainClaim ->
            domainClaim.toExpandableListItems(docId = this.docId)
        }
        return DocumentDetailsUi(
            documentId = this.docId,
            documentName = this.docName,
            documentIdentifier = this.documentIdentifier,
            documentExpirationDateFormatted = this.documentExpirationDateFormatted,
            documentHasExpired = this.documentHasExpired,
            documentIssuanceState = DocumentUiIssuanceState.Issued,
            documentClaims = documentDetailsUi,
        )
    }
}