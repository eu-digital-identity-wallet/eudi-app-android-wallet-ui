package eu.europa.ec.commonfeature.extension

import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.corelogic.model.DomainClaim
import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

fun DocumentPayloadDomain.toSelectiveExpandableListItems(): List<ExpandableListItem> {
    return this.docClaimsDomain.map { claim ->
        claim.toSelectiveExpandableListItems(docId)
    }
}

fun DomainClaim.toSelectiveExpandableListItems(docId: String): ExpandableListItem {
    return when (this) {
        is DomainClaim.Group -> {
            ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = path.toId(docId),
                    mainContentData = ListItemMainContentData.Text(text = displayTitle),
                    trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.KeyboardArrowDown)
                ),
                nestedItems = items.map {
                    it.toSelectiveExpandableListItems(docId)
                },
                isExpanded = false
            )
        }

        is DomainClaim.Primitive -> {
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = path.toId(docId),
                    mainContentData = calculateMainContent(key, value),
                    overlineText = calculateOverlineText(displayTitle),
                    leadingContentData = calculateLeadingContent(key, value),
                    trailingContentData = ListItemTrailingContentData.Checkbox(
                        checkboxData = CheckboxData(
                            isChecked = true,
                            enabled = !isRequired
                        )
                    )
                )
            )
        }
    }
}

fun DomainClaim.toExpandableListItems(docId: String): ExpandableListItem {
    return when (this) {
        is DomainClaim.Group -> {
            ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = path.toId(docId),
                    mainContentData = ListItemMainContentData.Text(text = displayTitle),
                    trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.KeyboardArrowDown)
                ),
                nestedItems = items.map { it.toExpandableListItems(docId = docId) },
                isExpanded = false
            )
        }

        is DomainClaim.Primitive -> {
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = path.toId(docId),
                    mainContentData = calculateMainContent(key, value),
                    overlineText = calculateOverlineText(displayTitle),
                    leadingContentData = calculateLeadingContent(key, value),
                )
            )
        }
    }
}

private fun calculateMainContent(
    key: ElementIdentifier,
    value: String,
): ListItemMainContentData {
    return when {
        keyIsPortrait(key = key) -> {
            ListItemMainContentData.Text(text = "")
        }

        keyIsSignature(key = key) -> {
            ListItemMainContentData.Image(base64Image = value)
        }

        else -> {
            ListItemMainContentData.Text(text = value)
        }
    }
}

private fun calculateLeadingContent(
    key: ElementIdentifier,
    value: String,
): ListItemLeadingContentData? {
    return if (keyIsPortrait(key = key)) {
        ListItemLeadingContentData.UserImage(userBase64Image = value)
    } else {
        null
    }
}

private fun calculateOverlineText(displayTitle: String): String? {
    return displayTitle.ifBlank {
        null
    }
}