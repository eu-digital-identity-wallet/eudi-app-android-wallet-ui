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

package eu.europa.ec.dashboardfeature.ui.documents.detail.transformer

import eu.europa.ec.businesslogic.provider.UuidProvider
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

        return@runCatching DocumentDetailsDomain(
            docName = document.name,
            docId = document.id,
            documentIdentifier = document.toDocumentIdentifier(),
            documentClaims = domainClaims,
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
            ),
            collapsedInfo = DocumentCredentialsInfoUi.CollapsedInfo(
                moreInfoText = resourceProvider.getString(R.string.document_details_document_credentials_info_more_info_text),
            ),
            expandedInfo = DocumentCredentialsInfoUi.ExpandedInfo(
                subtitle = resourceProvider.getString(R.string.document_details_document_credentials_info_expanded_text_subtitle),
                updateNowButtonText = null,
                hideButtonText = resourceProvider.getString(R.string.document_details_document_credentials_info_expanded_button_hide_text),
            )
        )
    }
}