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

package eu.europa.ec.commonfeature.ui.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ErrorInfo
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentHeader
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.SimpleBottomSheet
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    navController: NavController,
    viewModel: RequestViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(Event.Pop) },
        stickyBottom = { paddingValues ->
            WrapStickyBottomContent(
                stickyBottomModifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.OneButton(
                        config = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            enabled = !state.isLoading && state.allowShare,
                            onClick = { viewModel.setEvent(Event.StickyButtonPressed) }
                        )
                    )
                )
            ) {
                Text(text = stringResource(R.string.request_sticky_button_text))
            }
        },
        contentErrorConfig = state.error
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {

                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute)
                    }

                    is Effect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is Effect.Navigation.PopTo -> {
                        navController.popBackStack(
                            route = navigationEffect.screenRoute,
                            inclusive = false
                        )
                    }
                }
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState
        )

        if (isBottomSheetOpen) {
            WrapModalBottomSheet(
                onDismissRequest = {
                    viewModel.setEvent(
                        Event.BottomSheet.UpdateBottomSheetState(
                            isOpen = false
                        )
                    )
                },
                sheetState = bottomSheetState
            ) {
                SheetContent(
                    sheetContent = state.sheetContent,
                )
            }
        }
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.DoWork)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                other = if (state.noItems) Modifier else Modifier.verticalScroll(
                    rememberScrollState()
                )
            )
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top
    ) {
        // Screen Header.
        ContentHeader(
            modifier = Modifier.fillMaxWidth(),
            config = state.headerConfig,
        )

        // Screen Main Content.
        DisplayRequestItems(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SPACING_SMALL.dp),
            requestDocuments = state.items,
            noData = state.noItems,
            onEventSend = onEventSend,
        )
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }
            }
        }.collect()
    }
}

@Composable
private fun DisplayRequestItems(
    modifier: Modifier,
    requestDocuments: List<RequestDocumentItemUi>,
    noData: Boolean,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        if (noData) {
            ErrorInfo(
                modifier = Modifier.fillMaxSize(),
                informativeText = stringResource(id = R.string.request_no_data),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
            ) {
                requestDocuments.forEach { requestItem ->
                    WrapExpandableListItem(
                        header = requestItem.headerUi.collapsed,
                        data = requestItem.claimsUi,
                        onItemClick = { item ->
                            //onEventSend(Event.UserIdentificationClicked(itemId = item.itemId))
                            println("onItemClick ${item.itemId}")
                        },
                        onExpandedChange = {
                            println("onExpandedChange ${requestItem.claimsUi}")
                            //onEventSend(Event.ExpandOrCollapseRequestDocumentItem(itemId = requestItem.collapsedUiItem.uiItem.itemId))
                        }
                    )
                    /*requestItem.uiItem.forEach { expandableListItem ->
                        WrapExpandableListItem(
                            data = expandableListItem,
                            onItemClick = { item ->
                                //onEventSend(Event.UserIdentificationClicked(itemId = item.itemId))
                                println("onItemClick ${item.itemId}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            hideSensitiveContent = false,
                            //isExpanded = requestItem.collapsedUiItem.isExpanded,
                            onExpandedChange = {
                                println("onExpandedChange ${requestItem.uiItem}")
                                //onEventSend(Event.ExpandOrCollapseRequestDocumentItem(itemId = requestItem.collapsedUiItem.uiItem.itemId))
                            },
                            throttleClicks = false,
                        )
                    }*/
                }
            }

            if (requestDocuments.isNotEmpty()) {
                SectionTitle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SPACING_SMALL.dp),
                    text = stringResource(R.string.request_warning_text),
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                )
            }
        }
    }
}

@Composable
private fun SheetContent(
    sheetContent: RequestBottomSheetContent,
) {
    when (sheetContent) {
        RequestBottomSheetContent.WARNING -> {
            SimpleBottomSheet(
                textData = BottomSheetTextData(
                    title = stringResource(id = R.string.request_bottom_sheet_warning_title),
                    message = stringResource(id = R.string.request_bottom_sheet_warning_subtitle),
                ),
                leadingIcon = AppIcons.Warning,
                leadingIconTint = MaterialTheme.colorScheme.warning
            )
        }
    }
}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@ThemeModePreviews
//@Composable
//private fun ContentPreview() {
//    PreviewTheme {
//        Content(
//            state = State(
//                headerConfig = ContentHeaderConfig(
//                    description = stringResource(R.string.request_header_description),
//                    mainText = stringResource(R.string.request_header_main_text),
//                    relyingPartyData = RelyingPartyData(
//                        isVerified = true,
//                        name = stringResource(R.string.request_relying_party_default_name),
//                        description = stringResource(R.string.request_relying_party_description)
//                    )
//                ),
//                items = listOf(
//                    RequestDocumentItemUi(
//                        collapsedUiItem = CollapsedUiItem(
//                            isExpanded = false,
//                            uiItem = ListItemData(
//                                itemId = "000",
//                                mainContentData = ListItemMainContentData.Text(text = "Digital ID"),
//                                supportingText = stringResource(R.string.request_collapsed_supporting_text),
//                                trailingContentData = ListItemTrailingContentData.Icon(
//                                    iconData = AppIcons.KeyboardArrowDown
//                                ),
//                            )
//                        ),
//                        expandedUiItems = listOf(
//                            DocumentUiItem(
//                                domainPayload = DocumentPayloadDomain(
//                                    docName = "docName",
//                                    docId = "docId",
//                                    docNamespace = "docNamespace",
//                                    docClaimsDomain = listOf(
//                                        RequestDocumentClaim(
//                                            value = DomainClaim.Claim.Primitive(
//                                                key = "key",
//                                                displayTitle = "title",
//                                                value = "value",
//                                                path = listOf()
//                                            ),
//                                            isRequired = true,
//                                            isAvailable = true,
//                                            path = listOf(),
//                                        )
//                                    )
//                                ),
//                                uiItem = ListItemData(
//                                    itemId = "00",
//                                    overlineText = "Family name",
//                                    mainContentData = ListItemMainContentData.Text(text = "Doe"),
//                                )
//                            ),
//                        )
//                    ),
//                    RequestDocumentItemUi(
//                        collapsedUiItem = CollapsedUiItem(
//                            isExpanded = true,
//                            uiItem = ListItemData(
//                                itemId = "111",
//                                mainContentData = ListItemMainContentData.Text(text = "mDL"),
//                                supportingText = stringResource(R.string.request_collapsed_supporting_text),
//                                trailingContentData = ListItemTrailingContentData.Icon(
//                                    iconData = AppIcons.KeyboardArrowUp
//                                ),
//                            )
//                        ),
//                        expandedUiItems = listOf(
//                            DocumentUiItem(
//                                domainPayload = DocumentPayloadDomain(
//                                    docName = "docName",
//                                    docId = "docId",
//                                    docNamespace = "docNamespace",
//                                    docClaimsDomain = listOf(
//                                        RequestDocumentClaim(
//                                            value = DomainClaim.Claim.Primitive(
//                                                key = "key",
//                                                displayTitle = "title",
//                                                value = "value",
//                                                path = listOf()
//                                            ),
//                                            isRequired = true,
//                                            isAvailable = true,
//                                            path = listOf(),
//                                        )
//                                    )
//                                ),
//                                uiItem = ListItemData(
//                                    itemId = "10",
//                                    overlineText = "Family name",
//                                    mainContentData = ListItemMainContentData.Text(text = "Doe"),
//                                    trailingContentData = ListItemTrailingContentData.Checkbox(
//                                        checkboxData = CheckboxData(
//                                            isChecked = false
//                                        )
//                                    )
//                                ),
//                            ),
//                            DocumentUiItem(
//                                domainPayload = DocumentPayloadDomain(
//                                    docName = "docName",
//                                    docId = "docId",
//                                    docNamespace = "docNamespace",
//                                    docClaimsDomain = listOf(
//                                        RequestDocumentClaim(
//                                            value = DomainClaim.Claim.Primitive(
//                                                key = "key",
//                                                displayTitle = "title",
//                                                value = "value",
//                                                path = listOf()
//                                            ),
//                                            isRequired = true,
//                                            isAvailable = true,
//                                            path = listOf(),
//                                        )
//                                    )
//                                ),
//                                uiItem = ListItemData(
//                                    itemId = "11",
//                                    overlineText = "Given name",
//                                    mainContentData = ListItemMainContentData.Text(text = "John"),
//                                    trailingContentData = ListItemTrailingContentData.Checkbox(
//                                        checkboxData = CheckboxData(
//                                            isChecked = true
//                                        )
//                                    )
//                                ),
//                            ),
//                            DocumentUiItem(
//                                domainPayload = DocumentPayloadDomain(
//                                    docName = "docName",
//                                    docId = "docId",
//                                    docNamespace = "docNamespace",
//                                    docClaimsDomain = listOf(
//                                        RequestDocumentClaim(
//                                            value = DomainClaim.Claim.Primitive(
//                                                key = "key",
//                                                displayTitle = "title",
//                                                value = "value",
//                                                path = listOf()
//                                            ),
//                                            isRequired = true,
//                                            isAvailable = true,
//                                            path = listOf(),
//                                        )
//                                    )
//                                ),
//                                uiItem = ListItemData(
//                                    itemId = "12",
//                                    overlineText = "Age in years",
//                                    mainContentData = ListItemMainContentData.Text(text = "18"),
//                                    trailingContentData = ListItemTrailingContentData.Checkbox(
//                                        checkboxData = CheckboxData(
//                                            isChecked = true,
//                                            enabled = false,
//                                        )
//                                    )
//                                ),
//                            ),
//                        )
//                    ),
//                )
//            ),
//            effectFlow = Channel<Effect>().receiveAsFlow(),
//            onEventSend = {},
//            onNavigationRequested = {},
//            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
//            coroutineScope = rememberCoroutineScope(),
//            modalBottomSheetState = rememberModalBottomSheetState()
//        )
//    }
//}

@ThemeModePreviews
@Composable
private fun SheetContentWarningPreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = RequestBottomSheetContent.WARNING,
        )
    }
}