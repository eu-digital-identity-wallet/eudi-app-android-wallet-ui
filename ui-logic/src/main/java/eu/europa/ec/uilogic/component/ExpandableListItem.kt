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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapExpandableCard
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapListItems

data class ExpandableListItemData(
    val collapsed: ListItemData,
    val collapsedMainTextVerticalPadding: Dp? = null,
    val expanded: List<ListItemData>,
    val expandedMainTextVerticalPadding: Dp? = null,
)

@Composable
fun ExpandableListItem(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isClickableWhenExpanded: Boolean = false,
    data: ExpandableListItemData,
    onExpandedItemClick: ((ListItemData) -> Unit)? = null
) {
    WrapExpandableCard(
        modifier = modifier,
        cardCollapsedContent = {
            WrapListItem(
                item = data.collapsed,
                mainTextVerticalPadding = data.collapsedMainTextVerticalPadding,
                onItemClick = { onExpandedChange(!isExpanded) }
            )
        },
        cardExpandedContent = {
            WrapListItems(
                items = data.expanded,
                mainTextVerticalPadding = data.expandedMainTextVerticalPadding,
                shape = RoundedCornerShape(
                    bottomStart = SIZE_SMALL.dp,
                    bottomEnd = SIZE_SMALL.dp,
                ),
                onItemClick = onExpandedItemClick,
                clickable = isClickableWhenExpanded,
            )
        },
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        throttleClicks = true,
    )
}

@ThemeModePreviews
@Composable
private fun ExpandableListItemPreview() {
    PreviewTheme {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        val data = ExpandableListItemData(
            collapsed = ListItemData(
                mainText = "Digital ID",
                supportingText = "View details",
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

        ExpandableListItem(
            modifier = Modifier.heightIn(max = 1500.dp),
            data = data,
            isExpanded = isExpanded,
            onExpandedChange = { isExpanded = it },
            onExpandedItemClick = { item ->
                println("Clicked: ${item.mainText}")
            },
        )
    }
}