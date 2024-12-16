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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL

data class ExpandableListItemData<T>(
    val collapsed: ListItemData<T>,
    val expanded: List<ListItemData<T>>,
)

@Composable
fun <T> WrapExpandableListItem(
    data: ExpandableListItemData<T>,
    onItemClick: ((T) -> Unit)?,
    modifier: Modifier = Modifier,
    hideSensitiveContent: Boolean = false,
    isExpanded: Boolean,
    onExpandedChange: () -> Unit,
    isClickableWhenExpanded: Boolean = false,
    throttleClicks: Boolean = true,
    collapsedMainContentVerticalPadding: Dp = 16.dp,
    expandedMainContentVerticalPadding: Dp = 12.dp,
) {
    WrapExpandableCard(
        modifier = modifier,
        cardCollapsedContent = {
            WrapListItem(
                item = data.collapsed,
                onItemClick = onItemClick,
                hideSensitiveContent = false,
                mainContentVerticalPadding = collapsedMainContentVerticalPadding,
            )
        },
        cardExpandedContent = {
            WrapListItems(
                items = data.expanded,
                onItemClick = onItemClick,
                hideSensitiveContent = hideSensitiveContent,
                mainContentVerticalPadding = expandedMainContentVerticalPadding,
                shape = RoundedCornerShape(
                    bottomStart = SIZE_SMALL.dp,
                    bottomEnd = SIZE_SMALL.dp,
                ),
                clickable = isClickableWhenExpanded,
            )
        },
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        throttleClicks = throttleClicks,
    )
}

/*
@ThemeModePreviews
@Composable
private fun WrapExpandableListItemPreview() {
    PreviewTheme {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        val data = ExpandableListItemData(
            collapsed = ListItemData(
                mainText = "Digital ID",
                supportingText = stringResource(R.string.request_collapsed_supporting_text),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown
                ),
            ),
            expanded = listOf(
                ListItemData(
                    overlineText = "Family name",
                    mainText = "Doe",
                ),
                ListItemData(
                    overlineText = "Given name",
                    mainText = "John",
                ),
                ListItemData(
                    overlineText = "Date of birth",
                    mainText = "21 Oct 2023",
                ),
            )
        )

        WrapExpandableListItem(
            modifier = Modifier.heightIn(max = 1500.dp),
            data = data,
            isExpanded = isExpanded,
            onExpandedChange = { isExpanded = it },
            onExpandedItemClick = {},
        )
    }
}*/
