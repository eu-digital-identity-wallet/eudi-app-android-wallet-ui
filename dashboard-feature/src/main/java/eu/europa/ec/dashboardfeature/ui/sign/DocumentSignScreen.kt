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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.utils.ALPHA_ENABLED
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
internal fun DocumentSignScreen(
    navController: NavController,
    viewModel: DocumentSignViewModel,
) {
    val state = viewModel.viewState.value

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

        SignButton(onEventSend)
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
                is Effect.OpenDocumentSelection -> selectPdfLauncher.launch(effect.selection)
                is Effect.LaunchedRQES -> {
                    Toast.makeText(
                        context,
                        "Launched with: ${effect.uri}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.collect()
    }
}

@Composable
private fun SignButton(onEventSend: (Event) -> Unit) {
    WrapCard(
        onClick = {
            onEventSend(
                Event.OnSelectDocument
            )
        },
        throttleClicks = true,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier.padding(SPACING_MEDIUM.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val iconsColor = MaterialTheme.colorScheme.primary
            val iconsAlpha = ALPHA_ENABLED
            val textColor = MaterialTheme.colorScheme.onSurface

            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.document_sign_select_document),
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )

            WrapIcon(
                iconData = AppIcons.Add,
                customTint = iconsColor,
                contentAlpha = iconsAlpha
            )
        }
    }
}
