/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.utils.ALPHA_DISABLED
import eu.europa.ec.uilogic.component.utils.ALPHA_ENABLED

/**
 * Reusable Wrapper Composable to be used instead of plain Icon.
 * If you need control over its enabled/disabled state and/or want it to be clickable consider using [WrapIconButton] instead.
 * @param modifier the [Modifier] to be applied to the Composable.
 * @param iconData The actual data ([IconData]) the Icon has, like its resourceId and its contentDescriptionId.
 * @param customTint Nullable Color value to be applied as its tint. If null, it will use its default tint Color.
 * @param contentAlpha The alpha of its content. [ALPHA_ENABLED] by default.
 */
@Composable
fun WrapIcon(
    iconData: IconData,
    modifier: Modifier = Modifier,
    customTint: Color? = null,
    contentAlpha: Float = ALPHA_ENABLED,
) {
    val iconTint = customTint?.copy(alpha = contentAlpha) ?: LocalContentColor.current
    val iconContentDescription = stringResource(id = iconData.contentDescriptionId)

    iconData.resourceId?.let { resId ->
        Icon(
            modifier = modifier,
            painter = painterResource(id = resId),
            tint = iconTint,
            contentDescription = iconContentDescription
        )
    } ?: run {
        iconData.imageVector?.let { imageVector ->
            Icon(
                modifier = modifier,
                imageVector = imageVector,
                tint = iconTint,
                contentDescription = iconContentDescription
            )
        }
    }
}

/**
 * Reusable Wrapper Composable to be used instead of plain IconButton.
 * @param modifier the [Modifier] to be applied to this icon button
 * @param iconData The actual data ([IconData]) the Icon has, like its resourceId and its contentDescriptionId.
 * @param customTint Nullable Color value to be applied as its tint. If null, it will use its default tint Color.
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param onClick called when this icon button is clicked. Can be null.
 *
 * IMPORTANT NOTE: This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 */
@Composable
fun WrapIconButton(
    modifier: Modifier = Modifier,
    iconData: IconData,
    customTint: Color? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    IconButton(
        onClick = {
            if (onClick != null) {
                onClick()
            }
        },
        enabled = enabled,
        content = {
            WrapIcon(
                modifier = modifier,
                iconData = iconData,
                customTint = customTint,
                contentAlpha = if (enabled) {
                    ALPHA_ENABLED
                } else {
                    ALPHA_DISABLED
                }
            )
        }
    )
}