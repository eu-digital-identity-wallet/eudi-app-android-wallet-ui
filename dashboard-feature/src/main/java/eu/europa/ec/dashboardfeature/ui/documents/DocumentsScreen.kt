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

package eu.europa.ec.dashboardfeature.ui.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.model.SearchItem
import eu.europa.ec.dashboardfeature.ui.FiltersSearchBar
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SIZE_XX_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.BottomSheetWithTwoBigIcons
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(navHostController: NavController, viewModel: DocumentsViewModel) {
    val state = viewModel.viewState.value

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        topBar = { TopBar { viewModel.setEvent(it) } },
    ) { paddingValues ->
        Content(
            paddingValues = paddingValues,
            navHostController = navHostController,
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            modalBottomSheetState = bottomSheetState
        )
    }
}

@Composable
private fun TopBar(onEventSend: (Event) -> Unit) {
    Row(
        modifier = Modifier
            .height(SIZE_XX_LARGE.dp)
            .fillMaxSize()
            .padding(SPACING_MEDIUM.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineLarge,
            text = stringResource(R.string.documents_screen_title)
        )
        WrapIcon(
            modifier = Modifier
                .clickable {
                    onEventSend(Event.ToggleAddDocumentBottomSheet(isOpen = true))
                }
                .align(Alignment.CenterVertically),
            iconData = AppIcons.Add
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    paddingValues: PaddingValues,
    navHostController: NavController,
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    modalBottomSheetState: SheetState,
) {
    val isBottomSheetOpen = state.showAddDocumentBottomSheet

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = paddingValues),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        item {
            val searchItem =
                SearchItem(searchLabel = stringResource(R.string.documents_screen_search_label))
            FiltersSearchBar(placeholder = searchItem.searchLabel,
                onValueChange = {},
                onFilterClick = {})
        }
        items(state.documents) { documentItemData ->
            WrapListItem(
                documentItemData,
                { onEventSend(Event.GoToDocumentDetails(documentItemData.itemId)) })
        }
    }

    if (isBottomSheetOpen) {
        AddDocumentBottomSheet(onEventSend, modalBottomSheetState)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        onEventSend(Event.GetDocuments)
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchScreen -> {
                    navHostController.navigate(effect.screenRoute) {
                        popUpTo(effect.popUpToScreenRoute) {
                            inclusive = effect.inclusive
                        }
                    }
                }
            }
        }.collect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDocumentBottomSheet(
    onEventSend: (Event) -> Unit,
    modalBottomSheetState: SheetState,
) {
    WrapModalBottomSheet(
        onDismissRequest = {
            onEventSend(Event.ToggleAddDocumentBottomSheet(isOpen = false))
        },
        sheetState = modalBottomSheetState
    ) {
        BottomSheetWithTwoBigIcons(
            textData = BottomSheetTextData(
                title = stringResource(R.string.documents_screen_add_document_title),
                message = stringResource(R.string.documents_screen_add_document_description)
            ),
            options = listOf(
                ModalOptionUi(
                    title = stringResource(R.string.documents_screen_add_document_option_list),
                    leadingIcon = AppIcons.AddDocumentFromList,
                    leadingIconTint = MaterialTheme.colorScheme.primary,
                    event = Event.GoToAddDocument,
                ),
                ModalOptionUi(
                    title = stringResource(R.string.documents_screen_add_document_option_qr),
                    leadingIcon = AppIcons.AddDocumentFromQr,
                    leadingIconTint = MaterialTheme.colorScheme.primary,
                    event = Event.GoToQrScan,
                )
            ),
            onEventSent = { onEventSend(it) }
        )
    }
}