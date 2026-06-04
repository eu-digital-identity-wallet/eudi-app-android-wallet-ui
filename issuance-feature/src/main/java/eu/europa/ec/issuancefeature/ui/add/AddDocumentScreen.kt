/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.issuancefeature.ui.add

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.IssuanceFlowType
import eu.europa.ec.commonfeature.config.IssuanceUiConfig
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.issuancefeature.ui.add.model.AddDocumentUi
import eu.europa.ec.issuancefeature.util.TestTag
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ErrorInfo
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.content.BroadcastAction
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.content.ToolbarActionUi
import eu.europa.ec.uilogic.component.content.ToolbarConfig
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.TextStyleKey
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapText
import eu.europa.ec.uilogic.extension.applyTestTag
import eu.europa.ec.uilogic.extension.finish
import eu.europa.ec.uilogic.extension.getPendingUri
import eu.europa.ec.uilogic.extension.paddingFrom
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
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val toolbarConfig = ToolbarConfig(
        actions = listOf(
            ToolbarActionUi(
                icon = AppIcons.QrScanner,
                enabled = !state.isLoading,
                onClick = { viewModel.setEvent(Event.GoToQrScan) }
            )
        )
    )

    ContentScreen(
        isLoading = state.isLoading,
        toolBarConfig = toolbarConfig,
        navigatableAction = state.navigatableAction,
        onBack = state.onBackAction,
        contentErrorConfig = state.error,
        broadcastAction = BroadcastAction(
            intentFilters = listOf(
                CoreActions.VCI_RESUME_ACTION,
                CoreActions.VCI_DYNAMIC_PRESENTATION
            ),
            callback = {
                when (it?.action) {
                    CoreActions.VCI_RESUME_ACTION -> it.extras?.getString("uri")?.let { link ->
                        viewModel.setEvent(Event.OnResumeIssuance(link))
                    }

                    CoreActions.VCI_DYNAMIC_PRESENTATION -> it.extras?.getString("uri")
                        ?.let { link ->
                            viewModel.setEvent(Event.OnDynamicPresentation(link))
                        }
                }
            }
        )
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
            context = context,
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
        viewModel.setEvent(Event.Init(context.getPendingUri()))
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
    MainContent(
        modifier = Modifier
            .fillMaxSize()
            .paddingFrom(paddingValues, bottom = false),
        state = state,
        onEventSend = onEventSend,
        context = context,
    )

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    state: State,
    onEventSend: (Event) -> Unit,
    context: Context,
) {
    Column(
        modifier = modifier
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = state.title,
            subtitle = state.subtitle,
            subtitleTestTag = TestTag.AddDocumentScreen.SUBTITLE,
        )

        if (state.noOptions) {
            ErrorInfo(
                modifier = Modifier.fillMaxSize(),
                informativeText = stringResource(R.string.issuance_add_document_no_options)
            )
        } else {

            VSpacer.Medium()

            Options(
                options = state.options,
                modifier = Modifier.fillMaxSize(),
                onOptionClicked = { itemIds, issuerId ->
                    onEventSend(
                        Event.IssueDocument(
                            issuanceMethod = IssuanceMethod.OPENID4VCI,
                            issuerId = issuerId,
                            configIds = itemIds,
                            context = context
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun Options(
    options: List<Pair<String, List<AddDocumentUi>>>,
    modifier: Modifier = Modifier,
    onOptionClicked: (itemIds: List<String>, issuerId: String) -> Unit,
) {

    val listState = remember(options) { LazyListState() }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        options.forEachIndexed { sectionIndex, (issuerId, items) ->

            stickyHeader(key = "hdr-$issuerId") {
                WrapText(
                    modifier = modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = SPACING_MEDIUM.dp),
                    text = issuerId,
                    textConfig = TextConfig(
                        styleKey = TextStyleKey.LabelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }

            itemsIndexed(
                items = items,
                key = { _, item -> "$issuerId-${item.configurationIds.joinToString(",")}" }
            ) { index, item ->
                val testTag = TestTag.AddDocumentScreen.optionItem(
                    issuerId = issuerId,
                    configIds = item.configurationIds
                )
                WrapListItem(
                    modifier = Modifier
                        .applyTestTag(testTag)
                        .fillMaxWidth(),
                    item = item.itemData,
                    mainContentVerticalPadding = SPACING_LARGE.dp,
                    mainContentTextStyle = MaterialTheme.typography.titleMedium,
                    onItemClick = {
                        onOptionClicked(item.configurationIds, issuerId)
                    }
                )
                if (index < items.lastIndex) {
                    Spacer(Modifier.height(SPACING_MEDIUM.dp))
                }
            }

            if (sectionIndex != options.lastIndex) {
                item(key = "sep-$issuerId") { Spacer(Modifier.height(SPACING_MEDIUM.dp)) }
            }
        }

        item(key = "footer-spacer") {
            Spacer(
                modifier = Modifier
                    .padding(bottom = SPACING_MEDIUM.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun IssuanceAddDocumentScreenPreview() {
    PreviewTheme {
        Content(
            state = State(
                issuanceConfig = IssuanceUiConfig(
                    flowType = IssuanceFlowType.NoDocument
                ),
                navigatableAction = ScreenNavigateAction.NONE,
                title = stringResource(R.string.issuance_add_document_title),
                subtitle = stringResource(R.string.issuance_add_document_subtitle),
                options = listOf(
                    Pair(
                        "issuer1",
                        listOf(
                            AddDocumentUi(
                                credentialIssuerId = "issuer1",
                                configurationIds = listOf("configId1"),
                                itemData = ListItemDataUi(
                                    itemId = "configId1",
                                    mainContentData = ListItemMainContentDataUi.Text(text = "National ID"),
                                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                                        iconData = AppIcons.Add
                                    )
                                )
                            )
                        )
                    ),
                    Pair(
                        "issuer2",
                        listOf(
                            AddDocumentUi(
                                credentialIssuerId = "issuer2",
                                configurationIds = listOf("configId2"),
                                itemData = ListItemDataUi(
                                    itemId = "configId2",
                                    mainContentData = ListItemMainContentDataUi.Text(text = "Driving Licence"),
                                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                                        iconData = AppIcons.Add
                                    )
                                )
                            )
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
                issuanceConfig = IssuanceUiConfig(
                    flowType = IssuanceFlowType.ExtraDocument(
                        formatType = null
                    )
                ),
                navigatableAction = ScreenNavigateAction.BACKABLE,
                title = stringResource(R.string.issuance_add_document_title),
                subtitle = stringResource(R.string.issuance_add_document_subtitle),
                options = listOf(
                    Pair(
                        "issuer1",
                        listOf(
                            AddDocumentUi(
                                credentialIssuerId = "issuer1",
                                configurationIds = listOf("configId1"),
                                itemData = ListItemDataUi(
                                    itemId = "configId1",
                                    mainContentData = ListItemMainContentDataUi.Text(text = "National ID"),
                                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                                        iconData = AppIcons.Add
                                    )
                                )
                            )
                        )
                    ),
                    Pair(
                        "issuer2",
                        listOf(
                            AddDocumentUi(
                                credentialIssuerId = "issuer2",
                                configurationIds = listOf("configId2"),
                                itemData = ListItemDataUi(
                                    itemId = "configId2",
                                    mainContentData = ListItemMainContentDataUi.Text(text = "Driving Licence"),
                                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                                        iconData = AppIcons.Add
                                    )
                                )
                            )
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