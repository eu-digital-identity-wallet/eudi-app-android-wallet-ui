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

package eu.europa.ec.uilogic.component.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class TopSpacing {
    WithToolbar, WithoutToolbar
}

fun screenPaddings(
    append: PaddingValues? = null,
    topSpacing: TopSpacing = TopSpacing.WithToolbar
) = PaddingValues(
    start = SPACING_LARGE.dp,
    top = calculateTopSpacing(topSpacing).dp + (append?.calculateTopPadding() ?: 0.dp),
    end = SPACING_LARGE.dp,
    bottom = SPACING_LARGE.dp + (append?.calculateBottomPadding() ?: 0.dp)
)

internal fun stickyBottomPaddings(
    contentScreenPaddings: PaddingValues,
    layoutDirection: LayoutDirection
): PaddingValues {
    return PaddingValues(
        start = contentScreenPaddings.calculateStartPadding(layoutDirection),
        end = contentScreenPaddings.calculateEndPadding(layoutDirection),
        top = contentScreenPaddings.calculateBottomPadding(),
        bottom = contentScreenPaddings.calculateBottomPadding()
    )
}

private fun calculateTopSpacing(topSpacing: TopSpacing): Int = when (topSpacing) {
    TopSpacing.WithToolbar -> SPACING_SMALL
    TopSpacing.WithoutToolbar -> SPACING_XX_LARGE
}