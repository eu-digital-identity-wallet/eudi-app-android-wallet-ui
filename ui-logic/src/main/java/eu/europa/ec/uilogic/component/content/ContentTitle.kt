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

package eu.europa.ec.uilogic.component.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.VSpacer

@Composable
fun ContentTitle(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        color = MaterialTheme.colorScheme.onSurface
    ),
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    ),
    subTitleMaxLines: Int = Int.MAX_VALUE,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = titleStyle,
        )
        VSpacer.Large()

        subtitle?.let { safeSubtitle ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = safeSubtitle,
                style = subtitleStyle,
                maxLines = subTitleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun ContentTitlePreview() {
    PreviewTheme {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.proximity_qr_title),
            subtitle = stringResource(id = R.string.proximity_qr_subtitle)
        )
    }
}

@ThemeModePreviews
@Composable
private fun ContentTitleNoSubtitlePreview() {
    PreviewTheme {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.proximity_qr_title),
            subtitle = null
        )
    }
}