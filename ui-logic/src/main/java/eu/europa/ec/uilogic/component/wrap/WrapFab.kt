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

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import eu.europa.ec.resourceslogic.theme.values.allCorneredShapeLarge
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.preview.PreviewTheme

private val fabShape: Shape
    @Composable get() = MaterialTheme.shapes.allCorneredShapeLarge

private val primaryFabContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

private val primaryFabContentColor: Color
    @Composable get() = MaterialTheme.colorScheme.textPrimaryDark

private val secondaryFabContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.backgroundPaper

private val secondaryFabContentColor: Color
    @Composable get() = MaterialTheme.colorScheme.textPrimaryDark

/**
 * Data class that is used to construct and initialize a fab button.
 *
 * @param onClick  Operation to invoke when users click on the fab.
 * @param text  Composable that is used for the text of the fab content.
 * @param icon  Composable that is used for the icon of the fab content.
 */
data class FabData(
    val text: @Composable () -> Unit = {},
    val icon: @Composable () -> Unit = {},
    val onClick: () -> Unit,
)

@Composable
fun WrapPrimaryFab(
    data: FabData,
    modifier: Modifier = Modifier,
    shape: Shape = fabShape,
    containerColor: Color = primaryFabContainerColor,
    contentColor: Color = primaryFabContentColor,
) {
    FloatingActionButton(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        onClick = data.onClick,
        content = data.icon,
    )
}

@Composable
fun WrapSecondaryFab(
    data: FabData,
    modifier: Modifier = Modifier,
    shape: Shape = fabShape,
    containerColor: Color = secondaryFabContainerColor,
    contentColor: Color = secondaryFabContentColor,
) {
    WrapPrimaryFab(
        data = data,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
fun WrapPrimaryExtendedFab(
    data: FabData,
    modifier: Modifier = Modifier,
    shape: Shape = fabShape,
    containerColor: Color = primaryFabContainerColor,
    contentColor: Color = primaryFabContentColor,
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        onClick = data.onClick,
        icon = data.icon,
        text = data.text
    )
}

@Composable
fun WrapSecondaryExtendedFab(
    data: FabData,
    modifier: Modifier = Modifier,
    shape: Shape = fabShape,
    containerColor: Color = secondaryFabContainerColor,
    contentColor: Color = secondaryFabContentColor,
) {
    WrapPrimaryExtendedFab(
        data = data,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Preview
@Composable
fun WrapPrimaryFabPreview() {
    PreviewTheme {
        WrapPrimaryFab(
            data = FabData(
                icon = { WrapIcon(iconData = AppIcons.Add) },
                onClick = {}
            )
        )
    }
}