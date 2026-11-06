/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.commonfeature.extension

import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.commonfeature.util.keyIsUserImage
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimItemId
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.CheckboxDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

fun DocumentPayloadDomain.toSelectiveExpandableListItems(
    userImageClaimKeys: Set<String>,
): List<ExpandableListItemUi> {
    return this.docClaimsDomain.map { claim ->
        claim.toSelectiveExpandableListItems(
            docId = docId,
            queryId = queryId,
            userImageClaimKeys = userImageClaimKeys,
        )
    }
}

/**
 * Read-only sibling of [toSelectiveExpandableListItems]: the same claim tree without leaf
 * checkboxes, for requests whose claims aren't individually selectable (DCQL — see
 * [WalletCorePresentationController.requestAllowsClaimSelection][eu.europa.ec.corelogic.controller.WalletCorePresentationController.requestAllowsClaimSelection]).
 */
fun DocumentPayloadDomain.toExpandableListItems(
    userImageClaimKeys: Set<String>,
): List<ExpandableListItemUi> {
    return this.docClaimsDomain.map { claimDomain ->
        claimDomain.toExpandableListItems(
            docId = docId,
            queryId = queryId,
            userImageClaimKeys = userImageClaimKeys,
        )
    }
}

fun ClaimDomain.toSelectiveExpandableListItems(
    docId: String,
    queryId: String?,
    userImageClaimKeys: Set<String>,
): ExpandableListItemUi {
    return when (this) {
        is ClaimDomain.Group -> {
            ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = ClaimItemId.Claim(
                        docId = docId,
                        queryId = queryId,
                        path = path,
                    ).encode(),
                    mainContentData = ListItemMainContentDataUi.Text(text = displayTitle),
                    trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown)
                ),
                nestedItems = items.map {
                    it.toSelectiveExpandableListItems(
                        docId = docId,
                        queryId = queryId,
                        userImageClaimKeys = userImageClaimKeys,
                    )
                },
                isExpanded = false
            )
        }

        is ClaimDomain.Primitive -> {
            ExpandableListItemUi.SingleListItem(
                header = ListItemDataUi(
                    itemId = ClaimItemId.Claim(
                        docId = docId,
                        queryId = queryId,
                        path = path,
                    ).encode(),
                    mainContentData = calculateMainContent(key, value, userImageClaimKeys),
                    overlineText = calculateOverlineText(displayTitle),
                    leadingContentData = calculateLeadingContent(key, value, userImageClaimKeys),
                    trailingContentData = ListItemTrailingContentDataUi.Checkbox(
                        checkboxData = CheckboxDataUi(
                            isChecked = true,
                            enabled = !isRequired
                        )
                    )
                )
            )
        }
    }
}

fun ClaimDomain.toExpandableListItems(
    docId: String,
    queryId: String?,
    userImageClaimKeys: Set<String>,
): ExpandableListItemUi {
    return when (this) {
        is ClaimDomain.Group -> {
            ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = ClaimItemId.Claim(
                        docId = docId,
                        queryId = queryId,
                        path = path,
                    ).encode(),
                    mainContentData = ListItemMainContentDataUi.Text(text = displayTitle),
                    trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown)
                ),
                nestedItems = items.map {
                    it.toExpandableListItems(
                        docId = docId,
                        queryId = queryId,
                        userImageClaimKeys = userImageClaimKeys,
                    )
                },
                isExpanded = false
            )
        }

        is ClaimDomain.Primitive -> {
            ExpandableListItemUi.SingleListItem(
                header = ListItemDataUi(
                    itemId = ClaimItemId.Claim(
                        docId = docId,
                        queryId = queryId,
                        path = path,
                    ).encode(),
                    mainContentData = calculateMainContent(key, value, userImageClaimKeys),
                    overlineText = calculateOverlineText(displayTitle),
                    leadingContentData = calculateLeadingContent(key, value, userImageClaimKeys),
                )
            )
        }
    }
}

private fun calculateMainContent(
    key: String,
    value: String,
    userImageClaimKeys: Set<String>,
): ListItemMainContentDataUi {
    return when {
        keyIsUserImage(key = key, userImageClaimKeys = userImageClaimKeys) -> {
            ListItemMainContentDataUi.Text(text = "")
        }

        keyIsSignature(key = key) -> {
            ListItemMainContentDataUi.Image(base64Image = value)
        }

        else -> {
            ListItemMainContentDataUi.Text(text = value)
        }
    }
}

private fun calculateLeadingContent(
    key: String,
    value: String,
    userImageClaimKeys: Set<String>,
): ListItemLeadingContentDataUi? {
    return if (keyIsUserImage(key = key, userImageClaimKeys = userImageClaimKeys)) {
        ListItemLeadingContentDataUi.UserImage(userBase64Image = value)
    } else {
        null
    }
}

private fun calculateOverlineText(displayTitle: String): String? {
    return displayTitle.ifBlank {
        null
    }
}

/**
 * The ids of this document's rows that hold a picture of the holder, meaning a claim named in
 * [userImageClaimKeys]. They are the ids the rows are built with.
 */
fun DocumentPayloadDomain.userImageItemIds(userImageClaimKeys: Set<String>): Set<String> =
    docClaimsDomain
        .flatMap { claim -> claim.userImagePaths(userImageClaimKeys) }
        .map { path ->
            ClaimItemId.Claim(
                docId = docId,
                queryId = queryId,
                path = path,
            ).encode()
        }
        .toSet()

private fun ClaimDomain.userImagePaths(userImageClaimKeys: Set<String>): List<ClaimPathDomain> =
    when (this) {
        is ClaimDomain.Primitive -> if (keyIsUserImage(
                key = key,
                userImageClaimKeys = userImageClaimKeys
            )
        ) {
            listOf(path)
        } else {
            emptyList()
        }

        is ClaimDomain.Group -> items.flatMap { item -> item.userImagePaths(userImageClaimKeys) }
    }