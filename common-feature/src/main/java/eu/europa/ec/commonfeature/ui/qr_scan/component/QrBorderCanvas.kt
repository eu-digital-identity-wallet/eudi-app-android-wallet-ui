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

package eu.europa.ec.commonfeature.ui.qr_scan.component

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import eu.europa.ec.uilogic.component.utils.SIZE_LARGE

fun DrawScope.qrBorderCanvas(
    borderColor: Color = Color.White,
    curve: Dp,
    strokeWidth: Dp,
    capSize: Dp,
    gapAngle: Int = SIZE_LARGE,
    shadowSize: Dp = strokeWidth * 2,
    drawShadowLinesOnTheEdges: Boolean = false,
    cap: StrokeCap = StrokeCap.Square,
    lineCap: StrokeCap = StrokeCap.Round,
) {

    val curvePx = curve.toPx()

    val mCapSize = capSize.toPx()

    val width = size.width
    val height = size.height

    val sweepAngle = 90 / 2 - gapAngle / 2f

    strokeWidth.toPx().toInt()
    if (drawShadowLinesOnTheEdges) {
        for (i in 4..shadowSize.toPx().toInt() step 2) {
            drawRoundRect(
                color = Color(0x05000000),
                size = size,
                topLeft = Offset(0f, 0f),
                style = Stroke(width = i * 1f),
                cornerRadius = CornerRadius(
                    x = curvePx,
                    y = curvePx
                ),
            )
        }
    }

    val mCurve = curvePx * 2

    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 0f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width - mCurve, height - mCurve
        )
    )
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 90 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width - mCurve, height - mCurve
        )
    )

    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            0f, height - mCurve
        )
    )
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 180 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            0f, height - mCurve
        )
    )

    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 180f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            0f, 0f
        )
    )

    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 270 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            0f, 0f
        )
    )


    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 270f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width - mCurve, 0f
        )
    )

    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 360 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width - mCurve, 0f
        )
    )


    drawLine(
        SolidColor(borderColor), Offset(width, height - mCapSize), Offset(width, height - curvePx),
        strokeWidth.toPx(), lineCap,
    )

    drawLine(
        SolidColor(borderColor), Offset(width - mCapSize, height), Offset(width - curvePx, height),
        strokeWidth.toPx(), lineCap,
    )

    drawLine(
        SolidColor(borderColor), Offset(mCapSize, height), Offset(curvePx, height),
        strokeWidth.toPx(), lineCap,
    )

    drawLine(
        SolidColor(borderColor), Offset(0f, height - curvePx), Offset(0f, height - mCapSize),
        strokeWidth.toPx(), lineCap
    )

    drawLine(
        SolidColor(borderColor), Offset(0f, curvePx), Offset(0f, mCapSize),
        strokeWidth.toPx(), lineCap,
    )

    drawLine(
        SolidColor(borderColor), Offset(curvePx, 0f), Offset(mCapSize, 0f),
        strokeWidth.toPx(), lineCap,
    )

    drawLine(
        SolidColor(borderColor), Offset(width - curvePx, 0f), Offset(width - mCapSize, 0f),
        strokeWidth.toPx(), lineCap,
    )

    drawLine(
        SolidColor(borderColor), Offset(width, curvePx), Offset(width, mCapSize),
        strokeWidth.toPx(), lineCap
    )

}