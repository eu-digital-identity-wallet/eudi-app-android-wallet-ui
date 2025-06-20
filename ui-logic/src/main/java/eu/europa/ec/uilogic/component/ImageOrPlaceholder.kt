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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.wrap.WrapImage

@Composable
fun ImageOrPlaceholder(
    modifier: Modifier,
    base64Image: String,
    contentScale: ContentScale? = null,
    fallbackIcon: IconDataUi = AppIcons.User,
) {
    if (base64Image.isNotBlank()) {
        WrapImage(
            modifier = modifier,
            bitmap = rememberBase64DecodedBitmap(base64Image = base64Image),
            contentDescription = stringResource(id = R.string.content_description_image_or_placeholder_icon),
            contentScale = contentScale,
        )
    } else {
        WrapImage(
            modifier = modifier,
            iconData = fallbackIcon,
        )
    }
}

@ThemeModePreviews
@Composable
private fun ImageOrPlaceholderPreview() {
    PreviewTheme {
        ImageOrPlaceholder(
            base64Image = "",
            modifier = Modifier
        )
    }
}