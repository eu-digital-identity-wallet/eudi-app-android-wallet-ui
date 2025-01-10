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

package eu.europa.ec.dashboardfeature.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIconAndTextData
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SIZE_XX_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ActionCardConfig
import eu.europa.ec.uilogic.component.wrap.WrapActionCard
import eu.europa.ec.uilogic.component.wrap.WrapIconButton

@Composable
fun HomeScreen(
    navHostController: NavController,
    viewModel: HomeViewModel
) {
    val state = viewModel.viewState.value

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        topBar = { TopBar() }
    ) { paddingValues ->
        Content(
            state = state,
            onEventSent = { event -> viewModel.setEvent(event) },
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun TopBar(
    onEventSent: ((event: Event) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .height(SIZE_XX_LARGE.dp)
            .fillMaxSize()
            .padding(SPACING_MEDIUM.dp),
        Arrangement.SpaceBetween
    ) {
        // home menu icon
        WrapIconButton(
            modifier = Modifier.offset(x = -SPACING_SMALL.dp),
            iconData = AppIcons.Menu
        ) {
            // invoke event
        }

        // wallet logo
        AppIconAndText(appIconAndTextData = AppIconAndTextData())

        HSpacer.Large()
    }
}

@Composable
private fun Content(
    state: State,
    onEventSent: ((event: Event) -> Unit),
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier.padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        VSpacer.Medium()
        ContentTitle(
            title = state.welcomeUserMessage,
            titleStyle = MaterialTheme.typography.headlineMedium
        )

        WrapActionCard(
            config = state.authenticateCardConfig,
            onActionClick = {
                onEventSent(
                    Event.AuthenticatePressed
                )
            },
            onLearnMoreClick = {
                onEventSent(
                    Event.LearnMorePressed
                )
            }
        )

        WrapActionCard(
            config = state.signCardConfig,
            onActionClick = {
                onEventSent(
                    Event.SignPressed
                )
            },
            onLearnMoreClick = {
                onEventSent(
                    Event.LearnMorePressed
                )
            }
        )
    }
}

@ThemeModePreviews
@Composable
private fun HomeScreenContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                welcomeUserMessage = "Welcome back, Alex",
                authenticateCardConfig = ActionCardConfig(
                    title = "Authenticate, authorise transactions and share your digital documents in person or online.",
                    icon = AppIcons.WalletActivated,
                    primaryButtonText = "Authenticate",
                    secondaryButtonText = "Learn more",
                ),
                signCardConfig = ActionCardConfig(
                    title = "Sign, authorise transactions and share your digital documents in person or online.",
                    icon = AppIcons.Contract,
                    primaryButtonText = "Sign",
                    secondaryButtonText = "Learn more",
                )

            ),
            onEventSent = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp)
        )
    }
}