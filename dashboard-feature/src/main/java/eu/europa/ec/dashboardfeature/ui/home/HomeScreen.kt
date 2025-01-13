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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIconAndTextData
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ModalOptionUi
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
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.BottomSheetWithTwoBigIcons
import eu.europa.ec.uilogic.component.wrap.GenericBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapActionCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navHostController: NavController,
    viewModel: HomeViewModel
) {
    val state = viewModel.viewState.value
    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        topBar = { TopBar() }
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSent = { event -> viewModel.setEvent(event) },
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState,
            navController = navHostController,
            paddingValues = paddingValues
        )
    }

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
            HomeScreenSheetContent(
                sheetContent = state.sheetContent,
                onEventSent = { event -> viewModel.setEvent(event) },
                modalBottomSheetState = bottomSheetState
            )
        }
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
            iconData = AppIcons.Menu,
            shape = null
        ) {
            // invoke event
        }

        // wallet logo
        AppIconAndText(appIconAndTextData = AppIconAndTextData())

        HSpacer.ExtraLarge()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSent: ((event: Event) -> Unit),
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        VSpacer.Small()

        ContentTitle(
            title = state.welcomeUserMessage,
            titleStyle = MaterialTheme.typography.headlineMedium
        )

        WrapActionCard(
            config = state.authenticateCardConfig,
            onActionClick = {
                onEventSent(
                    Event.AuthenticateCard.AuthenticatePressed
                )
            },
            onLearnMoreClick = {
                onEventSent(
                    Event.AuthenticateCard.LearnMorePressed
                )
            }
        )

        WrapActionCard(
            config = state.signCardConfig,
            onActionClick = {
                onEventSent(
                    Event.SignDocumentCard.SignDocumentPressed
                )
            },
            onLearnMoreClick = {
                onEventSent(
                    Event.SignDocumentCard.LearnMorePressed
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }

                is Effect.Navigation.SwitchScreen -> navController.navigate(effect.screenRoute) {
                    popUpTo(effect.popUpToScreenRoute) {
                        inclusive = effect.inclusive
                    }
                }
            }
        }.collect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenSheetContent(
    sheetContent: HomeScreenBottomSheetContent,
    onEventSent: (event: Event) -> Unit,
    modalBottomSheetState: SheetState
) {
    when (sheetContent) {
        is HomeScreenBottomSheetContent.Authenticate -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                BottomSheetWithTwoBigIcons(
                    textData = BottomSheetTextData(
                        title = stringResource(R.string.home_screen_authenticate),
                        message = stringResource(R.string.home_screen_authenticate_description)
                    ),
                    options = listOf(
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_authenticate_option_in_person),
                            leadingIcon = AppIcons.PresentDocumentInPerson,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            event = Event.BottomSheet.Authenticate.OpenAuthenticateInPerson,
                        ),
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_add_document_option_online),
                            leadingIcon = AppIcons.PresentDocumentOnline,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            event = Event.BottomSheet.Authenticate.OpenAuthenticateOnLine,
                        )
                    ),
                    onEventSent = {
                        // invoke event
                    }
                )
            }
        }

        is HomeScreenBottomSheetContent.Sign -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                BottomSheetWithTwoBigIcons(
                    textData = BottomSheetTextData(
                        title = stringResource(R.string.home_screen_sign_document),
                        message = stringResource(R.string.home_screen_sign_document_description)
                    ),
                    options = listOf(
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_sign_document_option_from_device),
                            leadingIcon = AppIcons.SignDocumentFromDevice,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            event = Event.BottomSheet.SignDocument.OpenDocumentFromDevice,
                        ),
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_sign_document_option_scan_qr),
                            leadingIcon = AppIcons.SignDocumentFromQr,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            enabled = false,
                            event = Event.BottomSheet.SignDocument.OpenDocumentFromQr,
                        )
                    ),
                    onEventSent = {
                        // invoke event
                    }
                )
            }
        }

        is HomeScreenBottomSheetContent.LearnMoreAboutAuthenticate -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                GenericBottomSheet(
                    titleContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WrapIcon(
                                iconData = AppIcons.Info,
                                customTint = MaterialTheme.colorScheme.primary
                            )
                            HSpacer.Small()
                            Text(
                                text = stringResource(R.string.home_screen_authenticate),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                            )
                        }
                    },
                    bodyContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)) {
                            Text(
                                stringResource(R.string.home_screen_sign_learn_more_inner_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                stringResource(R.string.home_screen_sign_learn_more_description),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                )
            }
        }

        is HomeScreenBottomSheetContent.LearnMoreAboutSignDocument -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                GenericBottomSheet(
                    titleContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WrapIcon(
                                iconData = AppIcons.Info,
                                customTint = MaterialTheme.colorScheme.primary
                            )
                            HSpacer.Small()
                            Text(
                                stringResource(R.string.home_screen_sign),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    },
                    bodyContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)) {
                            Text(
                                stringResource(R.string.home_screen_authenticate_learn_more_inner_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                stringResource(R.string.home_screen_authenticate_learn_more_description),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun HomeScreenContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                isBottomSheetOpen = false,
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
            effectFlow = Channel<Effect>().receiveAsFlow(),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
            onEventSent = {},
            navController = rememberNavController(),
            paddingValues = PaddingValues(SPACING_MEDIUM.dp)
        )
    }
}