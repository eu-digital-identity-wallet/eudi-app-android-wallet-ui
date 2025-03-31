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

package eu.europa.ec.dashboardfeature.ui.sign

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.model.SignDocumentButtonUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
internal fun DocumentSignScreen(
    navController: NavController,
    viewModel: DocumentSignViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
        contentErrorConfig = state.error
    ) { contentPadding ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    Effect.Navigation.Pop -> navController.popBackStack()
                }
            },
            paddingValues = contentPadding
        )
    }
}


@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        ContentTitle(
            title = state.title,
            subtitle = state.subtitle,
        )

        VSpacer.Medium()

        SignButton(
            modifier = Modifier.fillMaxWidth(),
            buttonUi = state.buttonUi,
            onEventSend = onEventSend,
        )
    }

    val selectPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            onEventSend(Event.DocumentUriRetrieved(context, it))
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation.Pop -> onNavigationRequested(effect)
                is Effect.OpenDocumentSelection -> selectPdfLauncher.launch(
                    effect.selection.toTypedArray()
                )
            }
        }.collect()
    }
}

@Composable
private fun SignButton(
    modifier: Modifier = Modifier,
    buttonUi: SignDocumentButtonUi,
    onEventSend: (Event) -> Unit,
) {
    WrapListItem(
        modifier = modifier,
        item = buttonUi.data,
        onItemClick = {
            onEventSend(Event.OnSelectDocument)
        },
        mainContentVerticalPadding = SPACING_LARGE.dp,
        mainContentTextStyle = MaterialTheme.typography.titleMedium,
    )
}

@ThemeModePreviews
@Composable
private fun DocumentSignScreenPreview() {
    PreviewTheme {
        Content(
            state = State(
                title = stringResource(R.string.document_sign_title),
                subtitle = stringResource(R.string.document_sign_subtitle),
                buttonUi = SignDocumentButtonUi(
                    data = ListItemData(
                        itemId = "0",
                        mainContentData = ListItemMainContentData.Text(
                            text = stringResource(R.string.document_sign_select_document),
                        ),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.Add
                        ),
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
        )
    }
}