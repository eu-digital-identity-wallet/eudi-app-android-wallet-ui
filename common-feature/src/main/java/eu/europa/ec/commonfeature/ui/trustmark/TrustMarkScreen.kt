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

package eu.europa.ec.commonfeature.ui.trustmark

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.TrustMarkMode
import eu.europa.ec.commonfeature.config.TrustMarkUiConfig
import eu.europa.ec.commonfeature.ui.trustmark.model.TrustMarkParagraphUi
import eu.europa.ec.commonfeature.ui.trustmark.model.TrustMarkUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIconAndTextDataUi
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconDataUi
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.loader.LoadingIndicator
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.DEFAULT_BIG_ICON_SIZE
import eu.europa.ec.uilogic.component.utils.OncePerViewModelEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.WrapAsyncImage
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapTextButton
import eu.europa.ec.uilogic.extension.finish
import eu.europa.ec.uilogic.extension.tryOpenUrl
import eu.europa.ec.uilogic.navigation.CommonScreens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun TrustMarkScreen(
    navController: NavController,
    viewModel: TrustMarkViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isWelcome = state.config.mode is TrustMarkMode.Welcome
    val snackbarHostState = remember { SnackbarHostState() }

    ContentScreen(
        navigatableAction = if (isWelcome) {
            ScreenNavigateAction.NONE
        } else {
            ScreenNavigateAction.BACKABLE
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(snackbarData = snackbarData)
                }
            )
        },
        onBack = { viewModel.setEvent(Event.Back) },
        stickyBottom = if (isWelcome) {
            { paddingValues ->
                ContinueSection(
                    modifier = Modifier.padding(paddingValues),
                    enabled = !state.isCompleting && !state.isNavigating,
                    error = state.completionError,
                    onContinue = { viewModel.setEvent(Event.Continue) },
                )
            }
        } else null,
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = viewModel::setEvent,
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.Pop -> navController.popBackStack()
                    is Effect.Navigation.Finish -> context.finish()
                    is Effect.Navigation.Continue -> {
                        navController.navigate(navigationEffect.route) {
                            popUpTo(CommonScreens.TrustMark.screenRoute) {
                                inclusive = true
                            }
                        }
                    }
                }
            },
            context = context,
            snackbarHostState = snackbarHostState,
            paddingValues = paddingValues,
        )
    }

    OncePerViewModelEffect(viewModel) {
        viewModel.setEvent(Event.Init)
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    context: Context,
    snackbarHostState: SnackbarHostState,
    paddingValues: PaddingValues,
) {
    val linksEnabled = !state.isNavigating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
    ) {
        if (state.config.mode is TrustMarkMode.About) {
            ContentTitle(
                title = stringResource(R.string.trust_mark_about_title)
            )
        }

        TrustMarkHeading(
            modifier = Modifier.fillMaxWidth(),
            isWelcome = state.config.mode is TrustMarkMode.Welcome,
        )

        when {
            state.isLoading -> LoadingSection(
                modifier = Modifier.fillMaxWidth(),
            )

            state.loadError != null -> StatusSection(
                modifier = Modifier.fillMaxWidth(),
                message = state.loadError,
                onRetry = { onEventSend(Event.Retry) },
            )

            else -> state.trustMark?.let { safeTrustMark ->
                TrustMarkBadge(
                    modifier = Modifier.fillMaxWidth(),
                    imageUrl = safeTrustMark.imageUrl,
                    text = safeTrustMark.text,
                )
                LinkedParagraph(
                    modifier = Modifier.fillMaxWidth(),
                    paragraph = safeTrustMark.certificationDescription,
                    trailingIcon = AppIcons.OpenNew,
                    onClick = safeTrustMark.certifiedWalletsUrl?.takeIf { linksEnabled }
                        ?.let { url ->
                            { onEventSend(Event.OpenLink(url)) }
                        },
                )
                LinkedParagraph(
                    modifier = Modifier.fillMaxWidth(),
                    paragraph = safeTrustMark.certificationInformationDescription,
                    trailingIcon = AppIcons.OpenNew,
                    onClick = safeTrustMark.walletSolutionUrl?.takeIf { linksEnabled }
                        ?.let { url ->
                            { onEventSend(Event.OpenLink(url)) }
                        },
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
                is Effect.OpenUrl -> {
                    if (!context.tryOpenUrl(effect.url.toUri())) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.trust_mark_browser_error),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustMarkHeading(
    modifier: Modifier,
    isWelcome: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
    ) {
        if (isWelcome) {
            AppIconAndText(
                modifier = Modifier.fillMaxWidth(),
                appIconAndTextData = AppIconAndTextDataUi(),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
        ) {
            if (isWelcome) {
                Text(
                    text = stringResource(R.string.trust_mark_welcome_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Text(
                text = stringResource(R.string.trust_mark_wallet_name),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }
}

private enum class ImageLoadState {
    LOADING, LOADED, FAILED
}

@Composable
private fun TrustMarkBadge(
    modifier: Modifier,
    imageUrl: String?,
    text: String?,
) {
    var imageState by remember(imageUrl) {
        mutableStateOf(ImageLoadState.LOADING)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TrustMarkImage(
                modifier = Modifier.fillMaxWidth(),
                url = imageUrl,
                imageState = imageState,
                onStateChanged = { newImageState ->
                    imageState = newImageState
                },
            )
            text?.let { safeText ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = safeText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        if (imageUrl == null || imageState == ImageLoadState.FAILED) {
            StatusSection(
                modifier = Modifier.fillMaxWidth(),
                message = stringResource(R.string.trust_mark_image_error),
                onRetry = if (imageUrl == null) null else {
                    { imageState = ImageLoadState.LOADING }
                },
            )
        }
    }
}

@Composable
private fun TrustMarkImage(
    modifier: Modifier,
    url: String?,
    imageState: ImageLoadState,
    onStateChanged: (ImageLoadState) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (url == null || imageState == ImageLoadState.FAILED) {
            WrapIcon(iconData = AppIcons.Info)
        } else {
            WrapAsyncImage(
                modifier = Modifier.fillMaxSize(),
                source = url,
                contentDescription = stringResource(R.string.trust_mark_image_description),
                contentScale = ContentScale.Fit,
                placeholder = null,
                error = null,
                fallback = null,
                onLoading = { onStateChanged(ImageLoadState.LOADING) },
                onSuccess = { onStateChanged(ImageLoadState.LOADED) },
                onError = { onStateChanged(ImageLoadState.FAILED) },
            )
            if (imageState == ImageLoadState.LOADING) LoadingIndicator()
        }
    }
}

@Composable
private fun LinkedParagraph(
    modifier: Modifier,
    paragraph: TrustMarkParagraphUi,
    trailingIcon: IconDataUi?,
    onClick: (() -> Unit)?,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val iconId = "trust_mark_link_icon"
    val iconDescription = trailingIcon?.let { icon -> stringResource(icon.contentDescriptionId) }
    val annotatedText = remember(paragraph, linkColor, onClick != null, iconDescription) {
        buildAnnotatedString {
            val range = paragraph.linkRange
            if (range == null) {
                append(text = paragraph.text)
            } else {
                append(
                    text = paragraph.text,
                    start = 0,
                    end = range.last + 1
                )
                iconDescription?.let { description ->
                    append(char = '\u00A0')
                    appendInlineContent(id = iconId, alternateText = description)
                }
                if (onClick != null) {
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = "trust_mark_link",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = { currentOnClick?.invoke() },
                        ),
                        start = range.first,
                        end = length,
                    )
                }
                append(
                    text = paragraph.text,
                    start = range.last + 1,
                    end = paragraph.text.length
                )
            }
        }
    }
    Text(
        modifier = modifier,
        text = annotatedText,
        color = textColor,
        inlineContent = trailingIcon?.let { icon ->
            mapOf(
                iconId to InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    WrapIcon(
                        modifier = Modifier.fillMaxSize(),
                        iconData = icon,
                        customTint = if (onClick != null) linkColor
                        else textColor,
                    )
                }
            )
        } ?: emptyMap(),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun LoadingSection(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DEFAULT_BIG_ICON_SIZE.dp)
        ) {
            LoadingIndicator()
        }
        Text(
            text = stringResource(R.string.trust_mark_loading),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun StatusSection(
    modifier: Modifier,
    message: String,
    onRetry: (() -> Unit)?
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
        onRetry?.let { retry ->
            WrapTextButton(
                text = stringResource(R.string.generic_error_button_retry),
                enabled = true,
                contentAlignment = Alignment.Start,
                trailingIcon = null,
                onClick = retry,
            )
        }
    }
}

@Composable
private fun ContinueSection(
    modifier: Modifier,
    enabled: Boolean,
    error: String?,
    onContinue: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
    ) {
        error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                enabled = enabled,
                onClick = onContinue,
            ),
        ) {
            Text(text = stringResource(R.string.generic_continue))
        }
    }
}

private class TrustMarkPreviewProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State> = sequenceOf(
        State(
            config = TrustMarkUiConfig(
                mode = TrustMarkMode.Welcome(continuationRoute = "QUICK_PIN")
            ),
            trustMark = previewTrustMark(),
        ),
        State(
            config = TrustMarkUiConfig(
                mode = TrustMarkMode.About
            ),
            trustMark = previewTrustMark(),
        ),
        State(
            config = TrustMarkUiConfig(
                mode = TrustMarkMode.Welcome(continuationRoute = "QUICK_PIN")
            ),
            isLoading = true
        ),
        State(
            config = TrustMarkUiConfig(
                mode = TrustMarkMode.About
            ),
            trustMark = previewTrustMark().copy(imageUrl = null),
        ),
        State(
            config = TrustMarkUiConfig(
                mode = TrustMarkMode.About
            ),
            trustMark = previewTrustMark().copy(text = null),
        ),
        State(
            config = TrustMarkUiConfig(
                mode = TrustMarkMode.Welcome(continuationRoute = "QUICK_PIN")
            ),
            loadError = "Unable to load Trust Mark information. Please try again.",
        ),
    )
}

@ThemeModePreviews
@Composable
private fun TrustMarkPreview(@PreviewParameter(TrustMarkPreviewProvider::class) state: State) {
    PreviewTheme {
        ContentScreen(
            navigatableAction = if (state.config.mode is TrustMarkMode.Welcome) {
                ScreenNavigateAction.NONE
            } else ScreenNavigateAction.BACKABLE,
            stickyBottom = if (state.config.mode is TrustMarkMode.Welcome) {
                { paddingValues ->
                    ContinueSection(
                        modifier = Modifier.padding(paddingValues),
                        enabled = true,
                        error = null,
                        onContinue = {},
                    )
                }
            } else null,
        ) { paddingValues ->
            Content(
                state = state,
                effectFlow = emptyFlow(),
                onEventSend = {},
                onNavigationRequested = {},
                context = LocalContext.current,
                snackbarHostState = remember { SnackbarHostState() },
                paddingValues = paddingValues,
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun TrustMarkBadgeLongTextPreview() {
    PreviewTheme {
        TrustMarkBadge(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SPACING_LARGE.dp),
            imageUrl = null,
            text = (1..25).joinToString(separator = "\n") { line -> "Detail $line" },
        )
    }
}

private fun previewTrustMark(): TrustMarkUi = TrustMarkUi(
    imageUrl = "https://example.com/trust-mark.svg",
    text = "Localized wallet information provided by the Trust Mark resource.\n\n" +
            "Additional information is available by scrolling the screen.",
    certifiedWalletsUrl = "https://example.com/wallets",
    walletSolutionUrl = "https://example.com/wallets/example",
    certificationDescription = TrustMarkParagraphUi(
        text = "This wallet application has been certified conformant with the EUDI Wallet’s " +
                "security and privacy requirements and is part of the EUDI Wallet Provider Trusted List.",
        linkRange = 130..162,
    ),
    certificationInformationDescription = TrustMarkParagraphUi(
        text = "See the Certification information page for detailed certification information of this wallet.",
        linkRange = 8..37,
    ),
)