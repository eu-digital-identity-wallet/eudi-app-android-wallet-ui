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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.utils.ALPHA_DISABLED
import eu.europa.ec.uilogic.component.utils.ALPHA_ENABLED
import eu.europa.ec.uilogic.component.utils.DEFAULT_ICON_SIZE
import eu.europa.ec.uilogic.extension.throttledClickable

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
    val iconTint = (customTint ?: LocalContentColor.current).copy(alpha = contentAlpha)
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
 * @param modifier the [Modifier] to be applied to this icon button.
 * DO NOT set the size from the [modifier], instead, use the [size] parameter.
 * @param iconData The actual data ([IconData]) the Icon has, like its resourceId and its contentDescriptionId.
 * @param customTint Nullable Color value to be applied as its tint. If null, it will use its default tint Color. Default value is null.
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services. Default value is true.
 * @param size the size of the icon button. Has default value of [DEFAULT_ICON_SIZE]
 * Use this parameter instead, and NOT Modifier.size() .
 * @param throttleClicks Decides whether the clicks should be throttled or not. Default value is true.
 * @param throttleDuration The duration (in milliseconds) between each intercepted user click. Default value is 1 second.
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [PressInteraction.Press] when this clickable is pressed. Only the initial (first) press will be
 * recorded and dispatched with [MutableInteractionSource].
 * @param onClick called when this icon button is clicked.
 *
 * IMPORTANT NOTE: This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 */
@Composable
fun WrapIconButton(
    modifier: Modifier = Modifier,
    iconData: IconData,
    customTint: Color? = null,
    enabled: Boolean = true,
    size: Dp = DEFAULT_ICON_SIZE.dp,
    throttleClicks: Boolean = true,
    throttleDuration: Long = 1_000L,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
) {
    val role = Role.Button
    val rippleSize = if (size == DEFAULT_ICON_SIZE.dp) 40.dp else size
    val indication = ripple(
        bounded = false,
        radius = rippleSize / 2
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(rippleSize)
            .clip(CircleShape)
            .then(
                when (throttleClicks) {
                    true -> Modifier.throttledClickable(
                        onClick = onClick,
                        throttleDuration = throttleDuration,
                        enabled = enabled,
                        role = role,
                        interactionSource = interactionSource,
                        indication = indication
                    )

                    false -> Modifier.clickable(
                        onClick = onClick,
                        enabled = enabled,
                        role = role,
                        interactionSource = interactionSource,
                        indication = indication
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        WrapIcon(
            modifier = Modifier.size(size),
            iconData = iconData,
            customTint = customTint,
            contentAlpha = if (enabled) {
                ALPHA_ENABLED
            } else {
                ALPHA_DISABLED
            }
        )
    }
}