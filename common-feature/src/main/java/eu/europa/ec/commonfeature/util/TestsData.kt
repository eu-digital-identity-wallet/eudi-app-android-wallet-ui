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
import eu.europa.ec.commonfeature.ui.document_details.domain.DocumentItem
import eu.europa.ec.corelogic.model.DocumentIdentifier
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
    const val mockedFormattedExpirationDate = "13 May 2025"
    const val mockedDocumentHasExpired = false
    const val mockedVerifierName = "EUDIW Verifier"
    const val mockedIssuerName = "EUDIW Issuer"
    const val mockedRequestElementIdentifierNotAvailable = "Not available"
    const val mockedOfferedDocumentName = "Offered Document"
    const val mockedOfferedDocumentDocType = "mocked_offered_document_doc_type"
    const val mockedTxCodeFourDigits = 4
    const val mockedSuccessText = "Success text"
    const val mockedSuccessDescription = "Success description"
    const val mockedSuccessContentDescription = "Content description"
    const val mockedIssuanceErrorMessage = "Issuance error message"
    const val mockedInvalidCodeFormatMessage = "Invalid code format message"
    const val mockedWalletActivationErrorMessage = "Wallet activation error message"
    const val mockedPrimaryButtonText = "Primary button text"
    const val mockedRouteArguments = "mockedRouteArguments"
    const val mockedTxCode = "mockedTxCode"

    const val mockedPidNameSpace = "eu.europa.ec.eudi.pid.1"
    const val mockedMdlNameSpace = "org.iso.18013.5.1"

    const val mockedUriPath1 = "eudi-wallet://example.com/path1"
    const val mockedUriPath2 = "eudi-wallet://example.com/path2"

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
        documentDetails = emptyList(),
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
        documentDetails = listOf(
            ListItemData(
                itemId = "",
                overlineText = "birth_city",
                mainContentData = ListItemMainContentData.Text("KATRINEHOLM")
            ),
            ListItemData(
                itemId = "",
                overlineText = "gender",
                mainContentData = ListItemMainContentData.Text("male")
            ),
            ListItemData(
                itemId = "",
                overlineText = "age_over_18",
                mainContentData = ListItemMainContentData.Text("yes")
            ),
            ListItemData(
                itemId = "",
                overlineText = "age_birth_year",
                mainContentData = ListItemMainContentData.Text("1985")
            ),
            ListItemData(
                itemId = "",
                overlineText = "expiry_date",
                mainContentData = ListItemMainContentData.Text("30 Mar 2050")
            ),
            ListItemData(
                itemId = "",
                overlineText = "given_name",
                mainContentData = ListItemMainContentData.Text("JAN")
            ),
            ListItemData(
                itemId = "",
                overlineText = "family_name",
                mainContentData = ListItemMainContentData.Text("ANDERSSON")
            ),
            ListItemData(
                itemId = "",
                overlineText = "age_over_65",
                mainContentData = ListItemMainContentData.Text("no")
            )
        ),
    )

    val mockedBasicPidDomain = DocumentDetailsDomain(
        docName = mockedDocUiNamePid,
        docId = mockedPidId,
        documentIdentifier = DocumentIdentifier.MdocPid,
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
        detailsItems = listOf(
            DocumentItem(
                elementIdentifier = "family_name",
                value = "ANDERSSON",
                readableName = "family_name",
                docId = mockedPidId
            ),
            DocumentItem(
                elementIdentifier = "given_name",
                value = "JAN",
                readableName = "given_name",
                docId = mockedPidId
            ),
            DocumentItem(
                elementIdentifier = "age_over_18",
                value = "yes",
                readableName = "age_over_18",
                docId = mockedPidId
            ),
            DocumentItem(
                elementIdentifier = "age_over_65",
                value = "no",
                readableName = "age_over_65",
                docId = mockedPidId
            ),
            DocumentItem(
                elementIdentifier = "age_birth_year",
                value = "1985",
                readableName = "age_birth_year",
                docId = mockedPidId
            ),

            DocumentItem(
                elementIdentifier = "birth_city",
                value = "KATRINEHOLM",
                readableName = "birth_city",
                docId = mockedPidId
            ),
            DocumentItem(
                elementIdentifier = "gender",
                value = "Male",
                readableName = "gender",
                docId = mockedPidId
            ),
            DocumentItem(
                elementIdentifier = "expiry_date",
                value = "30 Mar 2050",
                readableName = "expiry_date",
                docId = mockedPidId
            )
        )
    )

    val mockedFullMdlUi = DocumentDetailsUi(
        documentId = mockedMdlId,
        documentName = mockedMdlDocName,
        documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
        documentDetails = emptyList(),
        documentIssuanceState = DocumentUiIssuanceState.Issued,
    )

    val mockedPendingMdlUi = mockedFullMdlUi.copy(
        documentIssuanceState = DocumentUiIssuanceState.Pending
    )

    val mockedBasicMdlUi = mockedFullMdlUi.copy(
        documentDetails = listOf(
            ListItemData(
                itemId = "",
                overlineText = "driving_privileges",
                mainContentData = ListItemMainContentData.Text(
                    text = arrayOf(
                        "issue_date: 1 Jul 2010\n" +
                                "expiry_date: 30 Mar 2050\n" +
                                "vehicle_category_code: A\n" +
                                "issue_date: 19 May 2008\n" +
                                "expiry_date: 30 Mar 2050\n" +
                                "vehicle_category_code: B"
                    ).contentDeepToString()
                )
            ),
            ListItemData(
                itemId = "",
                overlineText = "expiry_date",
                mainContentData = ListItemMainContentData.Text("30 Mar 2050")
            ),
            ListItemData(
                itemId = "",
                overlineText = "sex",
                mainContentData = ListItemMainContentData.Text("male")
            ),
            ListItemData(
                itemId = "",
                overlineText = "birth_place",
                mainContentData = ListItemMainContentData.Text("SWEDEN")
            ),
            ListItemData(
                itemId = "",
                overlineText = "portrait",
                mainContentData = ListItemMainContentData.Image("SE")
            ),
            ListItemData(
                itemId = "",
                overlineText = "given_name",
                mainContentData = ListItemMainContentData.Text("JAN")
            ),
            ListItemData(
                itemId = "",
                overlineText = "family_name",
                mainContentData = ListItemMainContentData.Text("ANDERSSON")
            ),
            ListItemData(
                itemId = "",
                overlineText = "signature_usual_mark",
                mainContentData = ListItemMainContentData.Image("SE")
            )
        ),
    )

    val mockedBasicMdlDomain = DocumentDetailsDomain(
        docName = mockedDocUiNameMdl,
        docId = mockedMdlId,
        documentIdentifier = DocumentIdentifier.OTHER("org.iso.18013.5.1.mDL"),
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
        detailsItems = listOf(
            DocumentItem(
                elementIdentifier = "family_name",
                value = "ANDERSSON",
                readableName = "family_name",
                docId = mockedMdlId
            ),
            DocumentItem(
                elementIdentifier = "given_name",
                value = "JAN",
                readableName = "given_name",
                docId = mockedMdlId
            ),
            DocumentItem(
                elementIdentifier = "birth_place",
                value = "SWEDEN",
                readableName = "birth_place",
                docId = mockedMdlId
            ),
            DocumentItem(
                elementIdentifier = "expiry_date",
                value = "30 Mar 2050",
                readableName = "expiry_date",
                docId = mockedMdlId
            ),
            DocumentItem(
                elementIdentifier = "portrait",
                value = "SE",
                readableName = "portrait",
                docId = mockedMdlId
            ),
            DocumentItem(
                elementIdentifier = "driving_privileges",
                value = "issue_date: 1 Jul 2010\n" +
                        "expiry_date: 30 Mar 2050\n" +
                        "vehicle_category_code: A\n" +
                        "issue_date: 19 May 2008\n" +
                        "expiry_date: 30 Mar 2050\n" +
                        "vehicle_category_code: B",
                readableName = "driving_privileges",
                docId = mockedMdlId
            ),
            DocumentItem(
                elementIdentifier = "signature_usual_mark",
                value = "SE",
                readableName = "signature_usual_mark",
                docId = mockedMdlId
            ),
            DocumentItem(
                elementIdentifier = "sex",
                value = "Male",
                readableName = "sex",
                docId = mockedMdlId
            )
        )
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
}