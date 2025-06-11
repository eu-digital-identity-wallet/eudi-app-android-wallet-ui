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

package eu.europa.ec.commonfeature.util

import androidx.annotation.VisibleForTesting
import eu.europa.ec.commonfeature.model.DocumentDetailsUi
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.ui.document_details.domain.DocumentDetailsDomain
import eu.europa.ec.corelogic.model.ClaimPath
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.DomainClaim
import eu.europa.ec.corelogic.model.ScopedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.openid4vci.TxCode
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
object TestsData {

    const val mockedPidDocName = "EU PID"
    const val mockedMdlDocName = "mDL"
    const val mockedPidId = "000001"
    const val mockedMdlId = "000002"
    const val mockedSdJwtPidId = "000003"
    const val mockedUserFirstName = "JAN"
    const val mockedUserBase64Portrait = "SE"
    const val mockedDocUiNamePid = "EU PID"
    const val mockedDocUiNameMdl = "mDL"
    const val mockedDocUiNameAge = "Age Verification"
    const val mockedDocUiNamePhotoId = "Photo ID"
    const val mockedConfigIssuerId = "configurationId"
    const val mockedNoUserFistNameFound = ""
    const val mockedNoUserBase64PortraitFound = ""
    const val mockedNoExpirationDateFound = ""
    const val mockedFormattedExpirationDate = "13 May 2030"
    const val mockedDocumentHasExpired = false
    const val mockedVerifierName = "EUDIW Verifier"
    const val mockedIssuerName = "EUDIW Issuer"
    const val mockedRequestElementIdentifierNotAvailable = "Not available"
    const val mockedOfferedDocumentName = "Offered Document"
    const val mockedOfferedDocumentDocType = "mocked_offered_document_doc_type"
    const val mockedTxCodeFourDigits = 4
    const val mockedSuccessText = "Success text"
    const val mockedSuccessDescription = "Success description"
    const val mockedErrorDescription = "Error description"
    const val mockedSuccessContentDescription = "Content description"
    const val mockedIssuanceErrorMessage = "Issuance error message"
    const val mockedInvalidCodeFormatMessage = "Invalid code format message"
    const val mockedWalletActivationErrorMessage = "Wallet activation error message"
    const val mockedPrimaryButtonText = "Primary button text"
    const val mockedRouteArguments = "mockedRouteArguments"
    const val mockedTxCode = "mockedTxCode"
    const val mockedClaimIsRequired = false

    const val mockedPidNameSpace = "eu.europa.ec.eudi.pid.1"
    const val mockedMdlNameSpace = "org.iso.18013.5.1"

    const val mockedUriPath1 = "eudi-wallet://example.com/path1"
    const val mockedUriPath2 = "eudi-wallet://example.com/path2"
    const val mockedUuid = "00000000-0000-0000-0000-000000000000"

    val mockedValidReaderAuth = ReaderAuth(
        readerAuth = byteArrayOf(),
        readerSignIsValid = true,
        readerCertificateChain = listOf(),
        readerCertificatedIsTrusted = true,
        readerCommonName = mockedVerifierName
    )

    val mockedPidWithBasicFieldsDocRequest = RequestedDocument(
        documentId = mockedPidId,
        requestedItems = mapOf(
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "family_name"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "given_name"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "age_over_18"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "age_over_65"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "age_birth_year"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "birth_city"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "gender"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "expiry_date"
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "portrait",
            ) to false,
            MsoMdocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "issuing_country",
            ) to false,
        ),
        readerAuth = mockedValidReaderAuth
    )

    val mockedMdlWithBasicFieldsDocRequest = RequestedDocument(
        documentId = mockedMdlId,
        requestedItems = mapOf(
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "family_name"
            ) to false,
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "given_name"
            ) to false,
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "birth_place"
            ) to false,
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "expiry_date"
            ) to false,
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "portrait"
            ) to false,
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "driving_privileges"
            ) to false,
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "signature_usual_mark"
            ) to false,
            MsoMdocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "sex"
            ) to false,
        ),
        readerAuth = mockedValidReaderAuth
    )

    val mockedValidPidWithBasicFieldsRequestDocument = mockedPidWithBasicFieldsDocRequest

    val mockedValidMdlWithBasicFieldsRequestDocument = mockedMdlWithBasicFieldsDocRequest

    val mockedFullPidUi = DocumentDetailsUi(
        documentId = mockedPidId,
        documentName = mockedPidDocName,
        documentIdentifier = DocumentIdentifier.MdocPid,
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
        documentClaims = emptyList(),
        documentIssuanceState = DocumentUiIssuanceState.Issued,
    )

    val mockedPendingPidUi = mockedFullPidUi.copy(
        documentIssuanceState = DocumentUiIssuanceState.Pending
    )

    val mockedUnsignedPidUi = mockedFullPidUi.copy(
        documentName = mockedPidDocName,
        documentIssuanceState = DocumentUiIssuanceState.Pending,
        documentIdentifier = DocumentIdentifier.MdocPid,
        documentExpirationDateFormatted = ""
    )

    val mockedBasicPidUi = mockedFullPidUi.copy(
        documentClaims = listOf(
            createMdocClaimListItem(mockedFullPidUi.documentId, "age_birth_year", "1985"),
            createMdocClaimListItem(mockedFullPidUi.documentId, "age_over_18", "yes"),
            createMdocClaimListItem(mockedFullPidUi.documentId, "age_over_65", "no"),
            createMdocClaimListItem(mockedFullPidUi.documentId, "birth_city", "KATRINEHOLM"),
            createMdocClaimListItem(mockedFullPidUi.documentId, "expiry_date", "30 Mar 2050"),
            createMdocClaimListItem(mockedFullPidUi.documentId, "family_name", "ANDERSSON"),
            createMdocClaimListItem(mockedFullPidUi.documentId, "gender", "Male"),
            createMdocClaimListItem(mockedFullPidUi.documentId, "given_name", "JAN"),
        )
    )

    val mockedBasicSdJwtPidUi = mockedFullPidUi.copy(
        documentId = mockedSdJwtPidId,
        documentClaims = listOf(
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,age_birth_year",
                    overlineText = "age_birth_year",
                    mainContentData = ListItemMainContentData.Text("1985")
                )
            ),
            ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,age_equal_or_over",
                    mainContentData = ListItemMainContentData.Text("age_equal_or_over"),
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowDown
                    )
                ),
                nestedItems = listOf(
                    ExpandableListItem.SingleListItemData(
                        header = ListItemData(
                            itemId = "$mockedSdJwtPidId,age_equal_or_over,18",
                            overlineText = "18",
                            mainContentData = ListItemMainContentData.Text("true")
                        )
                    ),
                    ExpandableListItem.SingleListItemData(
                        header = ListItemData(
                            itemId = "$mockedSdJwtPidId,age_equal_or_over,65",
                            overlineText = "65",
                            mainContentData = ListItemMainContentData.Text("unset")
                        )
                    )
                ),
                isExpanded = false
            ),
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,birth_date",
                    overlineText = "birth_date",
                    mainContentData = ListItemMainContentData.Text("30 Mar 1985")
                )
            ),
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,exp",
                    overlineText = "exp",
                    mainContentData = ListItemMainContentData.Text(text = "1755730800")
                )
            ),
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,family_name",
                    overlineText = "family_name",
                    mainContentData = ListItemMainContentData.Text("ANDERSSON")
                )
            ),
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,given_name",
                    overlineText = "given_name",
                    mainContentData = ListItemMainContentData.Text("JAN")
                )
            ),
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,iat",
                    overlineText = "iat",
                    mainContentData = ListItemMainContentData.Text(text = "1747954800")
                )
            ),
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,issuing_authority",
                    overlineText = "issuing_authority",
                    mainContentData = ListItemMainContentData.Text(text = "Test PID issuer")
                )
            ),
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,issuing_country",
                    overlineText = "issuing_country",
                    mainContentData = ListItemMainContentData.Text("FC")
                )
            ),
            ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,$mockedUuid",
                    overlineText = null,
                    mainContentData = ListItemMainContentData.Text("nationalities"),
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowDown
                    )
                ),
                nestedItems = listOf(
                    ExpandableListItem.SingleListItemData(
                        header = ListItemData(
                            itemId = "$mockedSdJwtPidId,nationalities",
                            overlineText = "nationalities",
                            mainContentData = ListItemMainContentData.Text("SE")
                        )
                    )
                ),
                isExpanded = false
            ),
            ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = "$mockedSdJwtPidId,place_of_birth",
                    overlineText = null,
                    mainContentData = ListItemMainContentData.Text("place_of_birth"),
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowDown
                    )
                ),
                nestedItems = listOf(
                    ExpandableListItem.SingleListItemData(
                        header = ListItemData(
                            itemId = "$mockedSdJwtPidId,place_of_birth,locality",
                            overlineText = "locality",
                            mainContentData = ListItemMainContentData.Text("KATRINEHOLM")
                        )
                    )
                ),
                isExpanded = false
            ),
        )
    )

    val mockedBasicPidDomain = DocumentDetailsDomain(
        docName = mockedDocUiNamePid,
        docId = mockedPidId,
        documentIdentifier = DocumentIdentifier.MdocPid,
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
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

    val mockedFullMdlUi = DocumentDetailsUi(
        documentId = mockedMdlId,
        documentName = mockedMdlDocName,
        documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
        documentClaims = emptyList(),
        documentIssuanceState = DocumentUiIssuanceState.Issued,
    )

    val mockedPendingMdlUi = mockedFullMdlUi.copy(
        documentIssuanceState = DocumentUiIssuanceState.Pending
    )

    val mockedBasicMdlUi = mockedFullMdlUi.copy(
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

    val mockedBasicMdlDomain = DocumentDetailsDomain(
        docName = mockedDocUiNameMdl,
        docId = mockedMdlId,
        documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
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

    val mockedMdlUiWithNoUserNameAndNoUserImage: DocumentDetailsUi = mockedFullMdlUi

    val mockedMdlUiWithNoExpirationDate: DocumentDetailsUi = mockedFullMdlUi.copy(
        documentExpirationDateFormatted = mockedNoExpirationDateFound
    )

    val mockedFullDocumentsUi: List<DocumentDetailsUi> = listOf(
        mockedFullPidUi, mockedFullMdlUi
    )

    val mockedScopedDocuments: List<ScopedDocument>
        get() = listOf(
            ScopedDocument(
                name = mockedDocUiNamePid,
                configurationId = mockedConfigIssuerId,
                isPid = true
            ),
            ScopedDocument(
                name = mockedDocUiNameMdl,
                configurationId = mockedConfigIssuerId,
                isPid = false
            ),
            ScopedDocument(
                name = mockedDocUiNameAge,
                configurationId = mockedConfigIssuerId,
                isPid = false
            ),
            ScopedDocument(
                name = mockedDocUiNamePhotoId,
                configurationId = mockedConfigIssuerId,
                isPid = false
            )
        ).sortedBy { it.name.lowercase() }

    val mockedConfigNavigationTypePop = ConfigNavigation(navigationType = NavigationType.Pop)
    val mockedConfigNavigationTypePush = ConfigNavigation(
        navigationType = NavigationType.PushRoute(
            route = DashboardScreens.Dashboard.screenRoute,
            popUpToRoute = IssuanceScreens.AddDocument.screenRoute
        )
    )
    val mockedConfigNavigationTypePopToScreen = ConfigNavigation(
        navigationType = NavigationType.PopTo(
            screen = DashboardScreens.Dashboard
        )
    )

    val mockedOfferTxCodeFourDigits =
        TxCode(
            inputMode = TxCodeInputMode.NUMERIC,
            length = mockedTxCodeFourDigits
        )

    val mockedPidOptionItemUi = DocumentOptionItemUi(
        itemData = ListItemData(
            itemId = mockedConfigIssuerId,
            mainContentData = ListItemMainContentData.Text(text = mockedDocUiNamePid),
            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
        ),
    )

    val mockedMdlOptionItemUi = DocumentOptionItemUi(
        itemData = ListItemData(
            itemId = mockedConfigIssuerId,
            mainContentData = ListItemMainContentData.Text(text = mockedDocUiNameMdl),
            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
        ),
    )

    val mockedAgeOptionItemUi = DocumentOptionItemUi(
        itemData = ListItemData(
            itemId = mockedConfigIssuerId,
            mainContentData = ListItemMainContentData.Text(text = mockedDocUiNameAge),
            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
        ),
    )

    val mockedPhotoIdOptionItemUi = DocumentOptionItemUi(
        itemData = ListItemData(
            itemId = mockedConfigIssuerId,
            mainContentData = ListItemMainContentData.Text(text = mockedDocUiNamePhotoId),
            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
        ),
    )

    fun createMdocClaimListItem(docId: String, claimIdentifier: String, value: String) =
        ExpandableListItem.SingleListItemData(
            header = ListItemData(
                itemId = "$docId,$claimIdentifier",
                overlineText = claimIdentifier,
                mainContentData = ListItemMainContentData.Text(value)
            )
        )
}