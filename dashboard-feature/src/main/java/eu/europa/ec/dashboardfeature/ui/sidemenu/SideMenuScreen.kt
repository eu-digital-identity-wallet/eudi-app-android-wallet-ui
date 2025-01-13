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

package eu.europa.ec.dashboardfeature.ui.sidemenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.europa.ec.dashboardfeature.ui.dashboard_new.Event
import eu.europa.ec.dashboardfeature.ui.dashboard_new.State
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.SimpleContentTitle
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.wrap.WrapListItem

@Composable
internal fun SideMenuScreen(
    state: State,
    onEventSent: (Event) -> Unit,
) {
    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = false,
        onBack = {
            onEventSent(
                Event.SideMenu.Hide
            )
        }
    ) { paddingValues ->
        Content(
            state = state,
            paddingValues = paddingValues,
            onEventSent = onEventSent
        )
    }
}

@Composable
private fun Content(
    state: State,
    onEventSent: (Event) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp)
    ) {
        SimpleContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = state.sideMenuTitle,
            subtitle = null
        )

        LazyColumn {
            items(state.sideMenuOptions) { menuOption ->
                WrapListItem(
                    mainContentVerticalPadding = SPACING_LARGE.dp,
                    item = menuOption,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    onItemClick = {
                        onEventSent(
                            Event.SideMenu.OpenChangeQuickPin
                        )
                    }
                )
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun SideMenuContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                isSideMenuVisible = false,
                sideMenuTitle = "My EU Wallet",
                sideMenuOptions = listOf(
                    ListItemData(
                        itemId = "changePinId",
                        mainContentData = ListItemMainContentData.Text(
                            text = "Change PIN"
                        ),
                        leadingContentData = ListItemLeadingContentData.Icon(
                            iconData = AppIcons.ChangePin
                        ),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    )
                )
            ),
            onEventSent = {},
            paddingValues = PaddingValues(SPACING_LARGE.dp)
        )
    }
}