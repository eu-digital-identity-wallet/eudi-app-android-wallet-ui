/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_LARGE
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.extension.clickableNoRipple

@Composable
fun InlineSnackbar(
    error: ContentErrorConfig,
    modifier: Modifier = Modifier,
    maxMessageLines: Int = 4,
) {
    val message = error.errorTitle
        ?: error.errorSubTitle
        ?: stringResource(R.string.generic_error_message)
    InlineSnackbar(
        message = message,
        onRetry = error.onRetry,
        onDismiss = error.onCancel,
        modifier = modifier,
        maxMessageLines = maxMessageLines,
    )
}

@Composable
fun InlineSnackbar(
    message: String,
    onRetry: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxMessageLines: Int = 4,
) {
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(SIZE_SMALL.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SPACING_MEDIUM.dp, vertical = SPACING_SMALL.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compute “maxMessageLines” max height
                val style = MaterialTheme.typography.bodyMedium
                val lineHeightSp =
                    if (style.lineHeight.isUnspecified) 20.sp else style.lineHeight
                val maxHeight =
                    with(LocalDensity.current) { (lineHeightSp * maxMessageLines).toDp() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = message,
                        style = style
                    )
                }

                onRetry?.let { retry ->
                    Text(
                        text = stringResource(R.string.generic_error_button_retry),
                        color = MaterialTheme.colorScheme.inversePrimary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(vertical = SPACING_EXTRA_SMALL.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { retry() }
                            .padding(
                                horizontal = SPACING_SMALL.dp,
                                vertical = SPACING_EXTRA_SMALL.dp
                            )
                    )
                }
            }
        }

        WrapIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = SPACING_SMALL.dp, y = (-SPACING_SMALL).dp)
                .size(SIZE_LARGE.dp)
                .background(MaterialTheme.colorScheme.inverseOnSurface, CircleShape)
                .clickableNoRipple {
                    onDismiss()
                }
                .padding(SPACING_EXTRA_SMALL.dp),
            iconData = AppIcons.Close,
            customTint = MaterialTheme.colorScheme.inverseSurface,
        )
    }
}

@ThemeModePreviews
@Composable
private fun InlineSnackbar_Preview_NoAction() {
    PreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(SPACING_MEDIUM.dp)
        ) {
            InlineSnackbar(
                message = "Something went wrong.",
                onRetry = null,
                onDismiss = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun InlineSnackbar_Preview_WithAction() {
    PreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(SPACING_MEDIUM.dp)
        ) {
            InlineSnackbar(
                message = "Network error. Please try again.",
                onRetry = {},
                onDismiss = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun InlineSnackbar_Preview_LongScrollable() {
    PreviewTheme {
        val longText =
            "This is a very long message intended to demonstrate vertical scrolling inside the snackbar. " +
                    "If the content exceeds the configured maximum number of lines, it should become scrollable " +
                    "so the user can read everything without the snackbar growing too tall. ".repeat(
                        4
                    )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(SPACING_MEDIUM.dp)
        ) {
            InlineSnackbar(
                message = longText,
                onRetry = {},
                onDismiss = {},
                modifier = Modifier.align(Alignment.BottomCenter),
                maxMessageLines = 4
            )
        }
    }
}