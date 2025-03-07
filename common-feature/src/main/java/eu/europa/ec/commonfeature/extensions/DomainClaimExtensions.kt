package eu.europa.ec.commonfeature.extensions

import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.corelogic.model.DomainClaim
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

fun DocumentPayloadDomain.toExpandableListItems(notAvailableId: String): List<ExpandableListItem> {
    return this.docClaimsDomain.map { claim ->
        claim.toExpandableListItems(docId, notAvailableId)
    }
}

fun DomainClaim.toExpandableListItems(docId: String, notAvailableId: String): ExpandableListItem {
    return when (this) {
        is DomainClaim.Claim.Group -> {
            ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = path.toId(docId),
                    mainContentData = ListItemMainContentData.Text(text = displayTitle),
                    trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.KeyboardArrowDown)
                ),
                nestedItems = items.map { it.toExpandableListItems(docId, notAvailableId) },
                isExpanded = false
            )
        }

        is DomainClaim.Claim.Primitive -> {
            val leadingContent =
                if (keyIsPortrait(key = key)) {
                    ListItemLeadingContentData.UserImage(userBase64Image = value)
                } else {
                    null
                }

            val mainContent = when {
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

            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = path.toId(docId),
                    mainContentData = mainContent,
                    overlineText = displayTitle,
                    leadingContentData = leadingContent,
                    trailingContentData = ListItemTrailingContentData.Checkbox(
                        checkboxData = CheckboxData(
                            isChecked = true,
                            enabled = !isRequired
                        )
                    )
                )
            )
        }

        is DomainClaim.NotAvailableClaim -> {
            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = notAvailableId,
                    mainContentData = ListItemMainContentData.Text(text = value),
                    overlineText = displayTitle,
                    trailingContentData = ListItemTrailingContentData.Checkbox(
                        checkboxData = CheckboxData(
                            isChecked = false,
                            enabled = false
                        )
                    )
                )
            )
        }
    }
}