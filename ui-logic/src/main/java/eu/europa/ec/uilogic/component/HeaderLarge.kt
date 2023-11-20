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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.resourceslogic.theme.values.bottomCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.textSecondaryLight
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.VSpacer

data class HeaderData(
    val title: String,
    val subtitle: String,
    val image: IconData = AppIcons.User,
    val icon: IconData = AppIcons.IdStroke
)

@Composable
fun HeaderLarge(
    modifier: Modifier = Modifier,
    data: HeaderData,
    contentPadding: PaddingValues = PaddingValues(all = SPACING_LARGE.dp)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.bottomCorneredShapeSmall
            )
            .padding(contentPadding)
    ) {

        Text(
            text = data.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        VSpacer.Small()

        Text(
            text = data.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(0.8F)
        )

        VSpacer.Large()

        BigImageAndMediumIcon(
            image = data.image,
            icon = data.icon
        )
    }
}

@ThemeModePreviews
@Composable
private fun HeaderLargePreview() {
    PreviewTheme {
        HeaderLarge(
            modifier = Modifier.fillMaxWidth(),
            data = HeaderData(
                title = "Digital ID",
                subtitle = "Jane Doe",
                image = AppIcons.User,
                icon = AppIcons.IdStroke
            )
        )
    }
}