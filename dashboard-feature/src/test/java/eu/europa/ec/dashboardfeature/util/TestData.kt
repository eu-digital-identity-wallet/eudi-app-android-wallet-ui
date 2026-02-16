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

package eu.europa.ec.dashboardfeature.util

import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsDomain
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsUi
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.testfeature.util.mockedMdlDocName
import eu.europa.ec.testfeature.util.mockedMdlId
import eu.europa.ec.testfeature.util.mockedMdocMdlNameSpace
import eu.europa.ec.testfeature.util.mockedMdocPidNameSpace
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

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
        ClaimDomain.Primitive(
            key = "family_name",
            value = "ANDERSSON",
            displayTitle = "family_name",
            path = ClaimPathDomain(
                value = listOf("family_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "given_name",
            value = "JAN",
            displayTitle = "given_name",
            path = ClaimPathDomain(
                value = listOf("given_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "age_over_18",
            value = "yes",
            displayTitle = "age_over_18",
            path = ClaimPathDomain(
                value = listOf("age_over_18"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "age_over_65",
            value = "no",
            displayTitle = "age_over_65",
            path = ClaimPathDomain(
                value = listOf("age_over_65"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "age_birth_year",
            value = "1985",
            displayTitle = "age_birth_year",
            path = ClaimPathDomain(
                value = listOf("age_birth_year"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "birth_city",
            value = "KATRINEHOLM",
            displayTitle = "birth_city",
            path = ClaimPathDomain(
                value = listOf("birth_city"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "gender",
            value = "Male",
            displayTitle = "gender",
            path = ClaimPathDomain(
                value = listOf("gender"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "expiry_date",
            value = "30 Mar 2050",
            displayTitle = "expiry_date",
            path = ClaimPathDomain(
                value = listOf("expiry_date"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocPidNameSpace)
            ),
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
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "expiry_date",
                mainContentData = ListItemMainContentDataUi.Text("30 Mar 2050")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "sex",
                mainContentData = ListItemMainContentDataUi.Text("male")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "birth_place",
                mainContentData = ListItemMainContentDataUi.Text("SWEDEN")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "portrait",
                mainContentData = ListItemMainContentDataUi.Image("SE")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "given_name",
                mainContentData = ListItemMainContentDataUi.Text("JAN")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "family_name",
                mainContentData = ListItemMainContentDataUi.Text("ANDERSSON")
            )
        ),
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = "",
                overlineText = "signature_usual_mark",
                mainContentData = ListItemMainContentDataUi.Image("SE")
            )
        )
    )
)

internal val mockedBasicMdlDomain = DocumentDetailsDomain(
    docName = mockedMdlDocName,
    docId = mockedMdlId,
    documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
    documentClaims = listOf(
        ClaimDomain.Primitive(
            key = "family_name",
            value = "ANDERSSON",
            displayTitle = "family_name",
            path = ClaimPathDomain(
                value = listOf("family_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "given_name",
            value = "JAN",
            displayTitle = "given_name",
            path = ClaimPathDomain(
                value = listOf("given_name"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "birth_place",
            value = "SWEDEN",
            displayTitle = "birth_place",
            path = ClaimPathDomain(
                value = listOf("birth_place"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "expiry_date",
            value = "30 Mar 2050",
            displayTitle = "expiry_date",
            path = ClaimPathDomain(
                value = listOf("expiry_date"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "portrait",
            value = "SE",
            displayTitle = "portrait",
            path = ClaimPathDomain(
                value = listOf("portrait"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "signature_usual_mark",
            value = "SE",
            displayTitle = "signature_usual_mark",
            path = ClaimPathDomain(
                value = listOf("signature_usual_mark"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
            isRequired = mockedClaimIsRequired
        ),
        ClaimDomain.Primitive(
            key = "sex",
            value = "Male",
            displayTitle = "sex",
            path = ClaimPathDomain(
                value = listOf("sex"),
                type = ClaimType.MsoMdoc(namespace = mockedMdocMdlNameSpace)
            ),
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