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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import eu.europa.ec.resourceslogic.theme.values.allCorneredShapeLarge
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews

private val fabShape: Shape
    @Composable get() = MaterialTheme.shapes.allCorneredShapeLarge

private val primaryFabContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary

private val primaryFabContentColor: Color
    @Composable get() = contentColorFor(primaryFabContainerColor)

private val secondaryFabContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

private val secondaryFabContentColor: Color
    @Composable get() = contentColorFor(secondaryFabContainerColor)

/**
 * Data class that is used to construct and initialize a fab button.
 *
 * @param onClick  Operation to invoke when users click on the fab.
 * @param text  Composable that is used for the text of the fab content.
 * @param icon  Composable that is used for the icon of the fab content.
 */
data class FabDataUi(
    val text: @Composable () -> Unit = {},
    val icon: @Composable () -> Unit = {},
    val onClick: () -> Unit,
)

@Composable
fun WrapPrimaryFab(
    data: FabDataUi,
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
    data: FabDataUi,
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
    data: FabDataUi,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = fabShape,
    containerColor: Color = primaryFabContainerColor,
    contentColor: Color = primaryFabContentColor,
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        expanded = expanded,
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
    data: FabDataUi,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = fabShape,
    containerColor: Color = secondaryFabContainerColor,
    contentColor: Color = secondaryFabContentColor,
) {
    WrapPrimaryExtendedFab(
        data = data,
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryFabPreview() {
    PreviewTheme {
        WrapPrimaryFab(
            data = FabDataUi(
                icon = { WrapIcon(iconData = AppIcons.Add) },
                onClick = {}
            )
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryExtendedFabPreview() {
    PreviewTheme {
        WrapPrimaryExtendedFab(
            data = FabDataUi(
                text = { Text(text = "Primary Extended FAB") },
                icon = { WrapIcon(iconData = AppIcons.Add) },
                onClick = {}
            )
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryFabPreview() {
    PreviewTheme {
        WrapSecondaryFab(
            data = FabDataUi(
                icon = { WrapIcon(iconData = AppIcons.Add) },
                onClick = {}
            )
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryExtendedFabPreview() {
    PreviewTheme {
        WrapSecondaryExtendedFab(
            data = FabDataUi(
                text = { Text(text = "Secondary Extended FAB") },
                icon = { WrapIcon(iconData = AppIcons.Add) },
                onClick = {}
            )
        )
    }
}