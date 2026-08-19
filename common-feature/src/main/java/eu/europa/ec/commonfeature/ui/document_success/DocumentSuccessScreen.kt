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

package eu.europa.ec.commonfeature.ui.document_success

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.util.TestTag
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.resourceslogic.theme.values.surfaceAtElevation1
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.RelyingParty
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import eu.europa.ec.uilogic.component.RelyingPartyLayout
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.DEFAULT_ICON_SIZE
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.TextStyleKey
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.component.wrap.WrapText
import eu.europa.ec.uilogic.component.wrap.shadowsAtElevation1
import eu.europa.ec.uilogic.extension.applyTestTag
import eu.europa.ec.uilogic.extension.cacheUri
import eu.europa.ec.uilogic.extension.findActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DocumentSuccessScreen(
    navController: NavController,
    viewModel: DocumentSuccessViewModel,
) {
    val context = LocalContext.current
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        isLoading = false,
        stickyBottom = { paddingValues ->
            WrapStickyBottomContent(
                modifier = Modifier
                    .applyTestTag(TestTag.DocumentSuccessScreen.BUTTON)
                    .fillMaxWidth()
                    .padding(paddingValues),
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.OneButton(
                        config = ButtonConfig(
                            type = ButtonType.SECONDARY,
                            enabled = !state.isLoading,
                            onClick = { viewModel.setEvent(Event.StickyButtonPressed) }
                        )
                    ),
                    showDivider = false
                )
            ) {
                Text(text = stringResource(R.string.document_success_sticky_button_text))
            }
        },
        navigatableAction = ScreenNavigateAction.NONE,
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            navigationEffect.popUpRoute?.let { popUpToRoute ->
                                popUpTo(popUpToRoute) {
                                    inclusive = true
                                }
                            }
                        }
                    }

                    is Effect.Navigation.PopBackStackUpTo -> {
                        navController.popBackStack(
                            route = navigationEffect.screenRoute,
                            inclusive = navigationEffect.inclusive
                        )
                    }

                    is Effect.Navigation.DeepLink -> {
                        context.cacheUri(navigationEffect.link)
                        navigationEffect.routeToPop?.let {
                            navController.popBackStack(
                                route = it,
                                inclusive = false
                            )
                        } ?: navController.popBackStack()
                    }

                    is Effect.Navigation.Pop -> navController.popBackStack()

                    is Effect.Navigation.FinishWithResult -> {
                        context.findActivity().let { activity ->
                            navigationEffect.intent.let { intent ->
                                activity.setResult(navigationEffect.resultCode, intent)
                                activity.finish()
                            }
                        }
                    }
                }
            },
            paddingValues = paddingValues
        )
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.DoWork)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        WrapCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            enabled = false,
            onClick = null,
            throttleClicks = false,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceAtElevation1,
            ),
            border = null,
            shadows = shadowsAtElevation1,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(all = SPACING_SMALL.dp),
                verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIconAndText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SPACING_SMALL.dp),
                    appIconAndTextData = state.headerConfig.appIconAndTextData,
                )

                SuccessBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = SPACING_MEDIUM.dp
                        ),
                    text = state.bannerText,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(SPACING_EXTRA_SMALL.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    state.headerConfig.description?.let { safeDescription ->
                        WrapText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .applyTestTag(TestTag.DocumentSuccessScreen.CONTENT_HEADER_DESCRIPTION),
                            text = safeDescription,
                            textConfig = state.headerConfig.descriptionTextConfig ?: TextConfig(
                                styleKey = TextStyleKey.BodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = Int.MAX_VALUE,
                            ),
                        )
                    }

                    state.headerConfig.relyingPartyData?.let { safeRelyingPartyData ->
                        RelyingParty(
                            modifier = Modifier.fillMaxWidth(),
                            relyingPartyData = safeRelyingPartyData,
                            layout = RelyingPartyLayout.StackedCentered,
                        )
                    }
                }

                state.items.forEachIndexed { index, successItem ->
                    WrapExpandableListItem(
                        modifier = Modifier
                            .applyTestTag(TestTag.DocumentSuccessScreen.successDocument(index = index))
                            .fillMaxWidth(),
                        header = successItem.header,
                        data = successItem.nestedItems,
                        onItemClick = null,
                        onExpandedChange = { expandedItem ->
                            onEventSend(Event.ExpandOrCollapseSuccessDocumentItem(itemId = expandedItem.itemId))
                        },
                        isExpanded = successItem.isExpanded,
                        throttleClicks = false,
                        hideSensitiveContent = false,
                        collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                        expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        )
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

@Composable
private fun SuccessBanner(
    modifier: Modifier,
    text: String,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimary

    WrapCard(
        modifier = modifier,
        enabled = false,
        onClick = null,
        throttleClicks = false,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.success,
            contentColor = contentColor,
        ),
        border = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SPACING_MEDIUM.dp,
                    vertical = SPACING_SMALL.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(
                space = SPACING_SMALL.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WrapIcon(
                modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                iconData = AppIcons.Check,
                customTint = contentColor,
                enabled = true,
            )
            WrapText(
                text = text,
                textConfig = TextConfig(
                    styleKey = TextStyleKey.TitleMedium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                ),
            )
        }
    }
}

private const val PREVIEW_BODY_HEIGHT = 700

@Composable
private fun previewSuccessDocument(
    itemId: String,
    name: String,
): ExpandableListItemUi.NestedListItem {
    return ExpandableListItemUi.NestedListItem(
        header = ListItemDataUi(
            itemId = itemId,
            mainContentData = ListItemMainContentDataUi.Text(text = name),
            overlineText = null,
            supportingContentData = ListItemSupportingContentDataUi.Text(
                text = stringResource(R.string.document_success_collapsed_supporting_text),
                textColorKey = null,
            ),
            leadingContentData = null,
            trailingContentData = ListItemTrailingContentDataUi.Icon(
                iconData = AppIcons.KeyboardArrowDown,
                tint = null,
            ),
        ),
        nestedItems = listOf(
            ExpandableListItemUi.SingleListItem(
                header = ListItemDataUi(
                    itemId = "${itemId}_family_name",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Doe"),
                    overlineText = "Family name",
                    supportingContentData = null,
                    leadingContentData = null,
                    trailingContentData = null,
                ),
            ),
        ),
        isExpanded = false,
    )
}

@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        Box(modifier = Modifier.height(PREVIEW_BODY_HEIGHT.dp)) {
            Content(
                state = State(
                    headerConfig = ContentHeaderConfig(
                        description = stringResource(R.string.document_success_header_description),
                        relyingPartyData = RelyingPartyDataUi(
                            logo = null,
                            isVerified = true,
                            name = "NordicBank A/S",
                            uniqueId = null,
                            description = null,
                        ),
                    ),
                    bannerText = stringResource(R.string.document_success_banner_text),
                    items = listOf(
                        previewSuccessDocument(itemId = "0", name = "Driving License"),
                        previewSuccessDocument(itemId = "1", name = "Digital ID"),
                    ),
                ),
                effectFlow = Channel<Effect>().receiveAsFlow(),
                onEventSend = {},
                onNavigationRequested = {},
                paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun ContentIssuancePreview() {
    PreviewTheme {
        Box(modifier = Modifier.height(PREVIEW_BODY_HEIGHT.dp)) {
            Content(
                state = State(
                    headerConfig = ContentHeaderConfig(
                        description = stringResource(R.string.issuance_success_header_description),
                        relyingPartyData = RelyingPartyDataUi(
                            logo = null,
                            isVerified = false,
                            name = "Aegean Airlines",
                            uniqueId = null,
                            description = null,
                        ),
                    ),
                    bannerText = stringResource(R.string.issuance_success_banner_text),
                    items = listOf(
                        previewSuccessDocument(itemId = "0", name = "Boarding Pass"),
                    ),
                ),
                effectFlow = Channel<Effect>().receiveAsFlow(),
                onEventSend = {},
                onNavigationRequested = {},
                paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            )
        }
    }
}