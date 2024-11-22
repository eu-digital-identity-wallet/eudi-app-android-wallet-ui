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
import androidx.compose.ui.res.stringResource
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.wrap.WrapImage

@Composable
fun UserImageOrPlaceholder(userBase64Image: String, modifier: Modifier) {
    if (userBase64Image.isNotBlank()) {
        WrapImage(
            bitmap = rememberBase64DecodedBitmap(base64Image = userBase64Image),
            modifier = modifier,
            contentDescription = stringResource(id = R.string.content_description_user_image_icon)
        )
    } else {
        WrapImage(
            iconData = AppIcons.User,
            modifier = modifier,
        )
    }
}

@ThemeModePreviews
@Composable
private fun UserImageOrPlaceholderPreview() {
    PreviewTheme {
        UserImageOrPlaceholder(
            userBase64Image = "",
            modifier = Modifier
        )
    }
}