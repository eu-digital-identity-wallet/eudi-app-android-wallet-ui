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

package eu.europa.ec.uilogic.extension

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

private data class ClickState(
    val event: () -> Unit,
    var currentTimeInMillis: Long
)

fun Modifier.throttledClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    indication: Indication? = null,
    interactionSource: MutableInteractionSource? = null,
    throttleDuration: Long = 1_000L,
    onClick: () -> Unit,
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {

    var lastClicked: Long by remember { mutableLongStateOf(0) }

    val debounceState = remember {
        MutableSharedFlow<ClickState>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    LaunchedEffect(Unit) {
        debounceState
            .collect {
                if (lastClicked <= 0 || (it.currentTimeInMillis - lastClicked) >= throttleDuration) {
                    it.event()
                    lastClicked = it.currentTimeInMillis
                }
            }
    }

    Modifier
        .clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = { debounceState.tryEmit(ClickState(onClick, System.currentTimeMillis())) },
            role = role,
            indication = indication ?: LocalIndication.current,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }
        )
}

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show a default
 * indication when it's pressed.
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, default indication from
 * [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use another
 * overload.
 *
 * If you need to support double click or long click alongside the single click, consider
 * using [combinedClickable].
 *
 * Same as [clickable]. Difference here is that ripple effect will not be shown to element modifier
 * applied at.
 *
 * @param onClick will be called when user clicks on the element
 */
inline fun Modifier.clickableNoRipple(
    enabled: Boolean = true,
    crossinline onClick: () -> Unit
): Modifier = composed {
    clickable(
        enabled = enabled,
        indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

fun Modifier.dashedBorder(
    brush: Brush,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = this.drawWithContent {
    val outline = shape.createOutline(size, layoutDirection, density = this)

    val dashedStroke = Stroke(
        cap = cap,
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx())
        )
    )

    // Draw the content
    drawContent()

    // Draw the border
    drawOutline(
        outline = outline,
        style = dashedStroke,
        brush = brush
    )
}

fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = dashedBorder(brush = SolidColor(color), shape, strokeWidth, dashLength, gapLength, cap)