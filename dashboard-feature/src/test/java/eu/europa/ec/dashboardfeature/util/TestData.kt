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

package eu.europa.ec.dashboardfeature.util

import eu.europa.ec.corelogic.model.ClaimPath
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.DomainClaim
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsDomain
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsUi
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.testfeature.util.mockedMdlDocName
import eu.europa.ec.testfeature.util.mockedMdlId
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

internal const val mockedBookmarkId = "mockedBookmarkId"
internal const val mockedChangeLogUrl = "https://example.com/changelog"

private const val mockedClaimIsRequired = false

internal val mockedFullPidUi = DocumentDetailsUi(
    documentId = mockedPidId,
    documentName = mockedPidDocName,
    documentIdentifier = DocumentIdentifier.MdocPid,
    documentClaims = emptyList(),
    documentIssuanceStateUi = DocumentIssuanceStateUi.Issued,
)

internal val mockedPendingPidUi = mockedFullPidUi.copy(
    documentIssuanceStateUi = DocumentIssuanceStateUi.Pending
)

internal val mockedUnsignedPidUi = mockedFullPidUi.copy(
    documentName = mockedPidDocName,
    documentIssuanceStateUi = DocumentIssuanceStateUi.Pending,
    documentIdentifier = DocumentIdentifier.MdocPid,
)

internal val mockedBasicPidDomain = DocumentDetailsDomain(
    docName = mockedPidDocName,
    docId = mockedPidId,
    documentIdentifier = DocumentIdentifier.MdocPid,
    documentClaims = listOf(
        DomainClaim.Primitive(
            key = "family_name",
            value = "ANDERSSON",
            displayTitle = "family_name",
            path = ClaimPath(value = listOf("family_name")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "given_name",
            value = "JAN",
            displayTitle = "given_name",
            path = ClaimPath(value = listOf("given_name")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "age_over_18",
            value = "yes",
            displayTitle = "age_over_18",
            path = ClaimPath(value = listOf("age_over_18")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "age_over_65",
            value = "no",
            displayTitle = "age_over_65",
            path = ClaimPath(value = listOf("age_over_65")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "age_birth_year",
            value = "1985",
            displayTitle = "age_birth_year",
            path = ClaimPath(value = listOf("age_birth_year")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "birth_city",
            value = "KATRINEHOLM",
            displayTitle = "birth_city",
            path = ClaimPath(value = listOf("birth_city")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "gender",
            value = "Male",
            displayTitle = "gender",
            path = ClaimPath(value = listOf("gender")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "expiry_date",
            value = "30 Mar 2050",
            displayTitle = "expiry_date",
            path = ClaimPath(value = listOf("expiry_date")),
            isRequired = mockedClaimIsRequired
        )
    ).sortedBy {
        it.displayTitle.lowercase()
    }
)

internal val mockedFullMdlUi = DocumentDetailsUi(
    documentId = mockedMdlId,
    documentName = mockedMdlDocName,
    documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
    documentClaims = emptyList(),
    documentIssuanceStateUi = DocumentIssuanceStateUi.Issued,
)

internal val mockedPendingMdlUi = mockedFullMdlUi.copy(
    documentIssuanceStateUi = DocumentIssuanceStateUi.Pending
)

internal val mockedBasicMdlUi = mockedFullMdlUi.copy(
    documentClaims = listOf(
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "",
                overlineText = "expiry_date",
                mainContentData = ListItemMainContentData.Text("30 Mar 2050")
            )
        ),
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "",
                overlineText = "sex",
                mainContentData = ListItemMainContentData.Text("male")
            )
        ),
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "",
                overlineText = "birth_place",
                mainContentData = ListItemMainContentData.Text("SWEDEN")
            )
        ),
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "",
                overlineText = "portrait",
                mainContentData = ListItemMainContentData.Image("SE")
            )
        ),
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "",
                overlineText = "given_name",
                mainContentData = ListItemMainContentData.Text("JAN")
            )
        ),
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "",
                overlineText = "family_name",
                mainContentData = ListItemMainContentData.Text("ANDERSSON")
            )
        ),
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "",
                overlineText = "signature_usual_mark",
                mainContentData = ListItemMainContentData.Image("SE")
            )
        )
    )
)

internal val mockedBasicMdlDomain = DocumentDetailsDomain(
    docName = mockedMdlDocName,
    docId = mockedMdlId,
    documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
    documentClaims = listOf(
        DomainClaim.Primitive(
            key = "family_name",
            value = "ANDERSSON",
            displayTitle = "family_name",
            path = ClaimPath(value = listOf("family_name")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "given_name",
            value = "JAN",
            displayTitle = "given_name",
            path = ClaimPath(value = listOf("given_name")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "birth_place",
            value = "SWEDEN",
            displayTitle = "birth_place",
            path = ClaimPath(value = listOf("birth_place")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "expiry_date",
            value = "30 Mar 2050",
            displayTitle = "expiry_date",
            path = ClaimPath(value = listOf("expiry_date")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "portrait",
            value = "SE",
            displayTitle = "portrait",
            path = ClaimPath(value = listOf("portrait")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "signature_usual_mark",
            value = "SE",
            displayTitle = "signature_usual_mark",
            path = ClaimPath(value = listOf("signature_usual_mark")),
            isRequired = mockedClaimIsRequired
        ),
        DomainClaim.Primitive(
            key = "sex",
            value = "Male",
            displayTitle = "sex",
            path = ClaimPath(value = listOf("sex")),
            isRequired = mockedClaimIsRequired
        )
    ).sortedBy {
        it.displayTitle.lowercase()
    }
)

internal val mockedMdlUiWithNoUserNameAndNoUserImage: DocumentDetailsUi = mockedFullMdlUi

internal val mockedFullDocumentsUi: List<DocumentDetailsUi> = listOf(
    mockedFullPidUi, mockedFullMdlUi
)