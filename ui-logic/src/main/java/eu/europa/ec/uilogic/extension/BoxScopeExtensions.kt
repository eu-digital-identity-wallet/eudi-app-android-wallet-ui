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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconDataUi
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapIcon

@Composable
fun BoxScope.IconWarningIndicator(
    iconData: IconDataUi = AppIcons.Warning,
    customTint: Color = MaterialTheme.colorScheme.warning,
    backgroundColor: Color,
) {
    WrapIcon(
        iconData = iconData,
        customTint = customTint,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .padding(SIZE_EXTRA_SMALL.dp)
            .size(20.dp)
    )
}

@ThemeModePreviews
@Composable
private fun IconWarningIndicatorPreview() {
    PreviewTheme {
        Box {
            WrapIcon(
                iconData = AppIcons.Id,
                customTint = MaterialTheme.colorScheme.primary
            )
            IconWarningIndicator(
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}