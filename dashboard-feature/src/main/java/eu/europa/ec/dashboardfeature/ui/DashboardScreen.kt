/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.dashboardfeature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.model.DashboardUiModel
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.resourceslogic.theme.templates.ThemeDimensTemplate
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.resourceslogic.theme.values.ThemeShapes
import eu.europa.ec.resourceslogic.theme.values.ThemeTypography
import eu.europa.ec.resourceslogic.theme.values.allCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.backgroundDefault
import eu.europa.ec.resourceslogic.theme.values.bottomCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.resourceslogic.theme.values.textSecondaryLight
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.wrap.FabData
import eu.europa.ec.uilogic.component.wrap.WrapFab
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapImage
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) }
    ) { paddingValues ->
        Content(
            state = viewModel.viewState.value,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues = paddingValues
        )
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> navController.popBackStack()
        is Effect.Navigation.SwitchScreen -> navController.navigate(navigationEffect.screen)
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>?,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()

    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp )
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.bottomCorneredShapeSmall
                ),
            contentAlignment = Alignment.Center
        ) {

            Row(modifier = Modifier.fillMaxWidth()) {


                WrapImage(
                    iconData = AppIcons.UserProfilePlaceholder,
                    modifier = Modifier
                        .size(64.dp, 64.dp)
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                )
                Column {
                    Text(
                        "Welcome back",
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.textSecondaryLight
                    )
                    Text(
                        "Jane",
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            }

        }


            DocumentTypeListScreen(sections = state.dashboardDocumentItems, Modifier.weight(1f))



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 33.dp, bottom = 24.dp, end = 33.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            WrapFab(
                modifier = Modifier
                    .weight(1f),
                FabData(onClick = { onEventSend(Event.Pop)  } ){ Text(
                    text = stringResource(id = R.string.dashboard_button_add_doc),
                    fontWeight = FontWeight.Medium,
                )}

            )

            WrapFab(
                modifier = Modifier
                    .weight(1f),
                FabData(onClick = { onEventSend(Event.Pop)  } ){ Text(
                    text = stringResource(id = R.string.dashboard_button_show_qr_tap),
                    fontWeight = FontWeight.Medium,
                )}
            )
        }
    }

    LaunchedEffect(Unit) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchScreen -> onNavigationRequested(effect)
                is Effect.Navigation.Pop -> onNavigationRequested(effect)
            }
        }?.collect()
    }
}

@Composable
private fun DocumentTypeListScreen(
    sections: List<DashboardUiModel>, modifier: Modifier
    // onEventSend: (Event) -> Unit
) {

    LazyVerticalGrid(modifier = modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 48.dp),
        columns = GridCells.Adaptive(minSize = 148.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sections.size) { section ->
            CardListItem(sections[section])
        }
    }
}


@Composable
private fun CardListItem(
    dataItem: DashboardUiModel,
    // paddingValues: PaddingValues,
    // onHeaderClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .background(
                MaterialTheme.colorScheme.backgroundDefault,
                shape = MaterialTheme.shapes.allCorneredShapeSmall
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

        // .clickable(onClick = onHeaderClicked)
    ) {

        WrapIcon(
            iconData = AppIcons.Id,
            customTint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = dataItem.documentType,

            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.textPrimaryDark),
        )
        Text(
            text = dataItem.documentStatus.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.success),
        )
    }
}

//@Composable
//fun CardTestitems(viewModel: DashboardViewModel = viewModel) {
//    DocumentTypeListScreen(
//
//        // ViewModel sends the network requests and makes posts available as a state
//        sections = viewModel.viewState.value
//    )
//}

@Composable
@Preview(showSystemUi = true, showBackground = true)
private fun WelcomeScreenPreview() {
    ThemeManager.Builder()
        .withLightColors(ThemeColors.lightColors)
        .withDarkColors(ThemeColors.darkColors)
        .withTypography(ThemeTypography.typo)
        .withShapes(ThemeShapes.shapes)
        .withDimensions(
            ThemeDimensTemplate(
                screenPadding = 10.0
            )
        )
        .build()
    ThemeManager.instance.Theme {
        Content(
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            state = State(
                listOf(
                    DashboardUiModel("Digital ID", true, "image1"),
                    DashboardUiModel("Driving Licence", true, "image1"),
                    DashboardUiModel("Other document", true, "image1"),
                    DashboardUiModel("Other document 1", true, "image1"),
                    DashboardUiModel("Other document 2", true, "image1")
                )
            ),
            paddingValues = PaddingValues(20.dp)
        )
    }

}