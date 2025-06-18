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

package eu.europa.ec.dashboardfeature.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.SwitchDataUi
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.extension.openIntentChooser
import eu.europa.ec.uilogic.extension.openUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = false,
        onBack = { viewModel.setEvent(Event.Pop) }
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController, context)
            },
            context = context,
            paddingValues = paddingValues,
        )
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context,
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> navController.popBackStack()

        is Effect.Navigation.OpenUrlExternally -> context.openUrl(uri = navigationEffect.url)
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    context: Context,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ContentTitle(
                modifier = Modifier.fillMaxWidth(),
                title = state.screenTitle,
            )

            SettingsItems(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                items = state.settingsItems,
                onEventSent = onEventSend,
            )
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SPACING_MEDIUM.dp),
            text = state.appVersion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
                is Effect.ShareLogFile -> {
                    context.openIntentChooser(
                        effect.intent,
                        effect.chooserTitle
                    )
                }
            }
        }.collect()
    }
}

@Composable
private fun SettingsItems(
    modifier: Modifier = Modifier,
    items: List<SettingsItemUi>,
    onEventSent: (Event) -> Unit,
) {
    Column(
        modifier = modifier
    ) {
        items.forEachIndexed { index, settingsItemUi ->
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = settingsItemUi.data,
                onItemClick = {
                    onEventSent(
                        Event.ItemClicked(itemType = settingsItemUi.type)
                    )
                },
                throttleClicks = false,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                mainContentVerticalPadding = SPACING_MEDIUM.dp,
            )


            if (index != items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SPACING_SMALL.dp)
                )
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun SettingsScreenPreview() {
    PreviewTheme {
        val context = LocalContext.current

        val settingsItems = listOf(
            SettingsItemUi(
                type = SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER,
                data = ListItemDataUi(
                    itemId = stringResource(R.string.settings_screen_option_show_batch_issuance_counter_id),
                    mainContentData = ListItemMainContentDataUi.Text(
                        text = stringResource(R.string.settings_screen_option_show_batch_issuance_counter)
                    ),
                    trailingContentData = ListItemTrailingContentDataUi.Switch(
                        switchData = SwitchDataUi(
                            isChecked = true,
                            enabled = true,
                        )
                    )
                )
            ),
            SettingsItemUi(
                type = SettingsMenuItemType.RETRIEVE_LOGS,
                data = ListItemDataUi(
                    itemId = stringResource(R.string.settings_screen_option_retrieve_logs_id),
                    mainContentData = ListItemMainContentDataUi.Text(
                        text = stringResource(R.string.settings_screen_option_retrieve_logs)
                    ),
                    leadingContentData = ListItemLeadingContentDataUi.Icon(
                        iconData = AppIcons.OpenNew
                    ),
                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                        iconData = AppIcons.KeyboardArrowRight
                    )
                )
            )
        )

        Content(
            state = State(
                screenTitle = stringResource(R.string.settings_screen_title),
                settingsItems = settingsItems,
                appVersion = "1.0.0",
                changelogUrl = ""
            ),
            effectFlow = emptyFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            context = context,
            paddingValues = PaddingValues(SPACING_MEDIUM.dp)
        )
    }
}