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

package eu.europa.ec.issuancefeature.util

import eu.europa.ec.corelogic.model.ScopedDocument
import eu.europa.ec.eudi.openid4vci.TxCode
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.issuancefeature.ui.add.model.AddDocumentUi
import eu.europa.ec.testfeature.util.mockedAgeVerificationDocName
import eu.europa.ec.testfeature.util.mockedMdlDocName
import eu.europa.ec.testfeature.util.mockedPhotoIdDocName
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedSdJwtPidId
import eu.europa.ec.testfeature.util.mockedUuid
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens

internal const val mockedOfferedDocumentName = "Offered Document"
internal const val mockedOfferedDocumentDocType = "mocked_offered_document_doc_type"
internal const val mockedTxCodeFourDigits = 4
internal const val mockedSuccessContentDescription = "Content description"
internal const val mockedIssuanceErrorMessage = "Issuance error message"
internal const val mockedInvalidCodeFormatMessage = "Invalid code format message"
internal const val mockedWalletActivationErrorMessage = "Wallet activation error message"
internal const val mockedPrimaryButtonText = "Primary button text"
internal const val mockedRouteArguments = "mockedRouteArguments"
internal const val mockedTxCode = "mockedTxCode"
internal const val mockedSuccessText = "Success text"
internal const val mockedSuccessDescription = "Success description"
internal const val mockedErrorDescription = "Error description"

private const val mockedConfigIssuerId = "configurationId"

internal val mockedPidOptionItemUi = AddDocumentUi(
    itemData = ListItemData(
        itemId = mockedConfigIssuerId,
        mainContentData = ListItemMainContentData.Text(text = mockedPidDocName),
        trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
    ),
)

internal val mockedMdlOptionItemUi = AddDocumentUi(
    itemData = ListItemData(
        itemId = mockedConfigIssuerId,
        mainContentData = ListItemMainContentData.Text(text = mockedMdlDocName),
        trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
    ),
)

internal val mockedAgeOptionItemUi = AddDocumentUi(
    itemData = ListItemData(
        itemId = mockedConfigIssuerId,
        mainContentData = ListItemMainContentData.Text(text = mockedAgeVerificationDocName),
        trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
    ),
)

internal val mockedPhotoIdOptionItemUi = AddDocumentUi(
    itemData = ListItemData(
        itemId = mockedConfigIssuerId,
        mainContentData = ListItemMainContentData.Text(text = mockedPhotoIdDocName),
        trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
    ),
)

internal val mockedScopedDocuments: List<ScopedDocument>
    get() = listOf(
        ScopedDocument(
            name = mockedPidDocName,
            configurationId = mockedConfigIssuerId,
            isPid = true
        ),
        ScopedDocument(
            name = mockedMdlDocName,
            configurationId = mockedConfigIssuerId,
            isPid = false
        ),
        ScopedDocument(
            name = mockedAgeVerificationDocName,
            configurationId = mockedConfigIssuerId,
            isPid = false
        ),
        ScopedDocument(
            name = mockedPhotoIdDocName,
            configurationId = mockedConfigIssuerId,
            isPid = false
        )
    ).sortedBy { it.name.lowercase() }

internal val mockedOfferTxCodeFourDigits = TxCode(
    inputMode = TxCodeInputMode.NUMERIC,
    length = mockedTxCodeFourDigits
)

internal val mockedConfigNavigationTypePop = ConfigNavigation(navigationType = NavigationType.Pop)
internal val mockedConfigNavigationTypePush = ConfigNavigation(
    navigationType = NavigationType.PushRoute(
        route = DashboardScreens.Dashboard.screenRoute,
        popUpToRoute = IssuanceScreens.AddDocument.screenRoute
    )
)
internal val mockedConfigNavigationTypePopToScreen = ConfigNavigation(
    navigationType = NavigationType.PopTo(
        screen = DashboardScreens.Dashboard
    )
)

internal val mockedMdocPidClaims = listOf(
    createMdocClaimListItem(mockedPidId, "age_birth_year", "1985"),
    createMdocClaimListItem(mockedPidId, "age_over_18", "yes"),
    createMdocClaimListItem(mockedPidId, "age_over_65", "no"),
    createMdocClaimListItem(mockedPidId, "birth_city", "KATRINEHOLM"),
    createMdocClaimListItem(mockedPidId, "expiry_date", "30 Mar 2050"),
    createMdocClaimListItem(mockedPidId, "family_name", "ANDERSSON"),
    createMdocClaimListItem(mockedPidId, "gender", "Male"),
    createMdocClaimListItem(mockedPidId, "given_name", "JAN"),
)

internal val mockedSdJwtPidClaims = listOf(
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

private fun createMdocClaimListItem(docId: String, claimIdentifier: String, value: String) =
    ExpandableListItem.SingleListItemData(
        header = ListItemData(
            itemId = "$docId,$claimIdentifier",
            overlineText = claimIdentifier,
            mainContentData = ListItemMainContentData.Text(value)
        )
    )