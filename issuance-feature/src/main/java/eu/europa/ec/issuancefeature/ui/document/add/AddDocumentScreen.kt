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

package eu.europa.ec.issuancefeature.ui.document.add

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.MainContentData
import eu.europa.ec.uilogic.component.SimpleContentTitle
import eu.europa.ec.uilogic.component.SystemBroadcastReceiver
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.extension.finish
import eu.europa.ec.uilogic.extension.getPendingDeepLink
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun AddDocumentScreen(
    navController: NavController,
    viewModel: AddDocumentViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = state.navigatableAction,
        onBack = state.onBackAction,
        contentErrorConfig = state.error
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.Pop -> navController.popBackStack()
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            popUpTo(IssuanceScreens.AddDocument.screenRoute) {
                                inclusive = navigationEffect.inclusive
                            }
                        }
                    }

                    is Effect.Navigation.Finish -> context.finish()
                    is Effect.Navigation.OpenDeepLinkAction -> handleDeepLinkAction(
                        navController,
                        navigationEffect.deepLinkUri,
                        navigationEffect.arguments
                    )
                }
            },
            paddingValues = paddingValues,
            context = context
        )
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE
    ) {
        viewModel.setEvent(Event.OnPause)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(Event.Init(context.getPendingDeepLink()))
    }

    SystemBroadcastReceiver(
        actions = listOf(
            CoreActions.VCI_RESUME_ACTION,
            CoreActions.VCI_DYNAMIC_PRESENTATION
        )
    ) {
        when (it?.action) {
            CoreActions.VCI_RESUME_ACTION -> it.extras?.getString("uri")?.let { link ->
                viewModel.setEvent(Event.OnResumeIssuance(link))
            }

            CoreActions.VCI_DYNAMIC_PRESENTATION -> it.extras?.getString("uri")?.let { link ->
                viewModel.setEvent(Event.OnDynamicPresentation(link))
            }
        }
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SimpleContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = state.title,
            subtitle = state.subtitle
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
        ) {
            state.options.forEach { option ->
                item {
                    WrapListItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = option.itemData,
                        mainContentVerticalPadding = SPACING_LARGE.dp,
                        mainContentTextStyle = MaterialTheme.typography.titleMedium,
                        onItemClick = if (option.available) { optionListItemData ->
                            onEventSend(
                                Event.IssueDocument(
                                    issuanceMethod = IssuanceMethod.OPENID4VCI,
                                    configId = optionListItemData.itemId,
                                    context = context
                                )
                            )
                        } else null
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
    }
}

@ThemeModePreviews
@Composable
private fun IssuanceAddDocumentScreenPreview() {
    PreviewTheme {
        Content(
            state = State(
                navigatableAction = ScreenNavigateAction.NONE,
                title = stringResource(R.string.issuance_add_document_title),
                subtitle = stringResource(R.string.issuance_add_document_subtitle),
                options = listOf(
                    DocumentOptionItemUi(
                        available = true,
                        itemData = ListItemData(
                            itemId = "configId1",
                            mainContentData = MainContentData.Text(text = "National ID"),
                            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
                        )
                    ),
                    DocumentOptionItemUi(
                        available = false,
                        itemData = ListItemData(
                            itemId = "configId2",
                            mainContentData = MainContentData.Text(text = "Driving Licence"),
                            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
                        )
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(all = SPACING_LARGE.dp),
            context = LocalContext.current
        )
    }
}

@ThemeModePreviews
@Composable
private fun DashboardAddDocumentScreenPreview() {
    PreviewTheme {
        Content(
            state = State(
                navigatableAction = ScreenNavigateAction.BACKABLE,
                title = stringResource(R.string.issuance_add_document_title),
                subtitle = stringResource(R.string.issuance_add_document_subtitle),
                options = listOf(
                    DocumentOptionItemUi(
                        available = true,
                        itemData = ListItemData(
                            itemId = "configId1",
                            mainContentData = MainContentData.Text(text = "National ID"),
                            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
                        )
                    ),
                    DocumentOptionItemUi(
                        available = false,
                        itemData = ListItemData(
                            itemId = "configId2",
                            mainContentData = MainContentData.Text(text = "Driving Licence"),
                            trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
                        )
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(all = SPACING_LARGE.dp),
            context = LocalContext.current
        )
    }
}