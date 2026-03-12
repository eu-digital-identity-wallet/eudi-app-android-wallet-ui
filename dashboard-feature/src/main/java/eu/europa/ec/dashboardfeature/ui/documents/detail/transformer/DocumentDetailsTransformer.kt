/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.dashboardfeature.ui.documents.detail.transformer

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.businesslogic.util.DAY_MONTH_YEAR_FULL_PATTERN
import eu.europa.ec.businesslogic.util.FULL_DATETIME_PATTERN_24H_SEPARATED_BY_DASH
import eu.europa.ec.businesslogic.util.formatInstant
import eu.europa.ec.commonfeature.extension.toExpandableListItems
import eu.europa.ec.commonfeature.util.transformPathsToDomainClaims
import eu.europa.ec.corelogic.extension.toClaimPaths
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsDomain
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsUi
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.dashboardfeature.ui.documents.model.DocumentCredentialsInfoUi
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

object DocumentDetailsTransformer {

    suspend fun transformToDocumentDetailsDomain(
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

        return@runCatching DocumentDetailsDomain(
            docName = document.name,
            docId = document.id,
            documentIdentifier = document.toDocumentIdentifier(),
            documentClaims = domainClaims,
            documentIssuanceDate = document.issuedAt.formatInstant(
                pattern = FULL_DATETIME_PATTERN_24H_SEPARATED_BY_DASH
            ),
            documentExpirationDate = document.getValidUntil().getOrNull()?.formatInstant(
                pattern = DAY_MONTH_YEAR_FULL_PATTERN
            ),
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
            documentIssuanceStateUi = DocumentIssuanceStateUi.Issued,
            documentClaims = documentDetailsUi,
        )
    }

    suspend fun createDocumentCredentialsInfoUi(
        document: IssuedDocument,
        resourceProvider: ResourceProvider,
    ): DocumentCredentialsInfoUi {
        val availableCredentials = document.credentialsCount()
        val totalCredentials = document.initialCredentialsCount()

        return DocumentCredentialsInfoUi(
            availableCredentials = availableCredentials,
            totalCredentials = totalCredentials,
            title = resourceProvider.getString(
                R.string.document_details_document_credentials_info_text,
                availableCredentials,
                totalCredentials
            )
        )
    }
}