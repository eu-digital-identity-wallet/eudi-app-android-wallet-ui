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

package eu.europa.ec.startupfeature.router

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import eu.europa.ec.startupfeature.BuildConfig
import eu.europa.ec.startupfeature.ui.splash.SplashScreen
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ExpandableListItem
import eu.europa.ec.uilogic.component.ExpandableListItemData
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.WrapChip
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapListItems
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSearchBar
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.StartupScreens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureStartupGraph(navController: NavController) {
    navigation(
        startDestination = StartupScreens.Splash.screenRoute,
        route = ModuleRoute.StartupModule.route
    ) {
        composable(
            route = StartupScreens.Splash.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + StartupScreens.Splash.screenRoute
                }
            )
        ) {
            SplashScreen(navController, koinViewModel())
        }

        composable(
            route = StartupScreens.DesignSystem.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + StartupScreens.DesignSystem.screenRoute
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SPACING_MEDIUM.dp)
                //.verticalScroll(rememberScrollState())
            ) {
                //Buttons()
                //SearchBars()
                //Chips()
                ListItems()
                //VSpacer.Medium()
                ListItemsList()
                ExpandableCard()
                repeat(5) {
                    VSpacer.Medium()
                }
            }
        }
    }
}

@Composable
private fun Buttons() {
    WrapPrimaryButton(
        enabled = true,
        onClick = { }
    ) {
        Text("Enabled Primary Button")
    }
    WrapPrimaryButton(
        enabled = false,
        onClick = { }
    ) {
        Text("Disabled Primary Button")
    }
    WrapPrimaryButton(
        enabled = true,
        isWarning = true,
        onClick = { }
    ) {
        Text("Enabled Warning Primary Button")
    }
    WrapPrimaryButton(
        enabled = false,
        isWarning = true,
        onClick = { }
    ) {
        Text("Disabled Warning Primary Button")
    }

    WrapSecondaryButton(
        enabled = true,
        onClick = { }
    ) {
        Text("Enabled Secondary Button")
    }
    WrapSecondaryButton(
        enabled = false,
        onClick = { }
    ) {
        Text("Disabled Secondary Button")
    }
    WrapSecondaryButton(
        enabled = true,
        isWarning = true,
        onClick = { }
    ) {
        Text("Enabled Warning Secondary Button")
    }
    WrapSecondaryButton(
        enabled = false,
        isWarning = true,
        onClick = { }
    ) {
        Text("Disabled Warning Secondary Button")
    }
}

@Composable
private fun SearchBars() {
    var searchText by rememberSaveable { mutableStateOf("") }
    WrapSearchBar(
        modifier = Modifier.fillMaxWidth(),
        value = searchText,
        onValueChange = {
            searchText = it
        }
    )
}

@Composable
private fun Chips() {
    var selected by rememberSaveable { mutableStateOf(false) }

    WrapChip(
        modifier = Modifier.wrapContentWidth(),
        labelText = "Label text",
        selected = selected,
        onClick = { selected = !selected }
    )

    WrapChip(
        modifier = Modifier.wrapContentWidth(),
        labelText = "Label text",
        trailingIcon = AppIcons.Close,
        selected = selected,
        onClick = { selected = !selected }
    )

    WrapChip(
        modifier = Modifier.wrapContentWidth(),
        labelText = "Label text",
        leadingIcon = AppIcons.Close,
        selected = selected,
        onClick = { selected = !selected }
    )

    WrapChip(
        modifier = Modifier.wrapContentWidth(),
        labelText = "Label text",
        leadingIcon = AppIcons.Close,
        trailingIcon = AppIcons.Close,
        selected = selected,
        onClick = { selected = !selected }
    )
}

@Composable
private fun ListItems() {
    Column(
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        WrapListItem(
            modifier = Modifier.fillMaxWidth(),
            item = ListItemData(
                mainText = "Main text",
            ),
            mainTextVerticalPadding = 26,
            onItemClick = {
                println("ListItem0 Clicked")
            },
        )
        WrapListItem(
            modifier = Modifier.fillMaxWidth(),
            item = ListItemData(
                mainText = "Main text",
                overlineText = "Overline text",
                supportingText = "Supporting text",
                leadingIcon = AppIcons.Sign,
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            onItemClick = {
                println("ListItem1 Clicked")
            },
        )
        WrapListItem(
            modifier = Modifier.fillMaxWidth(),
            item = ListItemData(
                mainText = "Main text",
                supportingText = "Supporting text",
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            onItemClick = {
                println("ListItem2 Clicked")
            },
        )
    }
}

@Composable
private fun ListItemsList() {
    val items = listOf(
        ListItemData(
            mainText = "Main text",
            overlineText = "Overline text",
            supportingText = "Supporting text",
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowRight,
            ),
        ),
        ListItemData(
            mainText = "Main text",
            overlineText = "Overline text",
            supportingText = "Supporting text",
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowRight,
            ),
        ),
        ListItemData(
            mainText = "Main text",
            overlineText = "Overline text",
            supportingText = "Supporting text",
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowRight,
            ),
        ),
        ListItemData(
            mainText = "Main text",
            overlineText = "Overline text",
            supportingText = "Supporting text",
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowRight,
            ),
        ),
    )

    WrapListItems(
        modifier = Modifier,
        clickable = true,
        items = items,
        onItemClick = { item ->
            println("ListItem ${item.mainText} Clicked")
        }
    )
}

@Composable
private fun ExpandableCard() {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    var item1Checked by rememberSaveable { mutableStateOf(false) }

    val data = ExpandableListItemData(
        collapsed = ListItemData(
            mainText = "Digital ID",
            supportingText = "View details",
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown
            ),
        ),
        collapsedMainTextVerticalPadding = 16,
        expanded = listOf(
            ListItemData(
                overlineText = "Family name",
                mainText = "Doe",
                trailingContentData = ListItemTrailingContentData.Checkbox(
                    checkboxData = CheckboxData(
                        isChecked = item1Checked,
                        enabled = true,
                        onCheckedChange = {
                            item1Checked = it
                        },
                    ),
                ),
            ),
            ListItemData(
                overlineText = "Given name",
                mainText = "John",
            ),
            ListItemData(
                overlineText = "Date of birth",
                mainText = "21 Oct 2023",
            ),
        ),
        expandedMainTextVerticalPadding = 12
    )

    ExpandableListItem(
        //modifier = Modifier.heightIn(max = 1500.dp),
        data = data,
        isExpanded = isExpanded,
        isClickableWhenExpanded = true,
        onExpandedChange = { isExpanded = it },
        onExpandedItemClick = { item ->
            println("Clicked: ${item.mainText}")
        },
    )
}