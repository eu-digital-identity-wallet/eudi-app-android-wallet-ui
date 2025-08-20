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

package eu.europa.ec.uilogic.component.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

@Composable
fun screenWidthInDp(): Dp =
    with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

@Composable
fun screenHeightInDp(): Dp =
    with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

@Composable
fun screenWidthInDp(excludeSafeArea: Boolean): Dp {
    if (!excludeSafeArea) return screenWidthInDp()
    val layoutDir = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    return screenWidthInDp() - safe.calculateStartPadding(layoutDir) - safe.calculateEndPadding(
        layoutDir
    )
}

@Composable
fun screenHeightInDp(excludeSafeArea: Boolean): Dp {
    if (!excludeSafeArea) return screenHeightInDp()
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    return screenHeightInDp() - safe.calculateTopPadding() - safe.calculateBottomPadding()
}

@Composable
fun screenSizeInDp(excludeSafeArea: Boolean = false): DpSize =
    DpSize(screenWidthInDp(excludeSafeArea), screenHeightInDp(excludeSafeArea))
