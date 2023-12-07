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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapImage

@Composable
fun BigImageAndMediumIcon(
    base64Image: String,
    icon: IconData?,
) {
    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (bigImage, mediumIcon) = createRefs()
        val verticalGuideline = createGuidelineFromStart(0.50f)
        val imageModifier = Modifier
            .size(width = 160.dp, height = 160.dp)
            .clip(RoundedCornerShape(SIZE_SMALL.dp))
            .constrainAs(bigImage) {
                start.linkTo(parent.start)
            }

        if (base64Image.isNotBlank()) {
            WrapImage(
                bitmap = rememberBase64DecodedBitmap(base64Image = base64Image),
                modifier = imageModifier,
                contentDescription = stringResource(id = R.string.content_description_user_image)
            )
        } else {
            WrapImage(
                iconData = AppIcons.User,
                modifier = imageModifier,
            )
        }
        if (icon != null) {
            WrapImage(
                modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .clip(RoundedCornerShape(SIZE_SMALL.dp))
                    .constrainAs(mediumIcon) {
                        start.linkTo(verticalGuideline)
                        top.linkTo(bigImage.top)
                        bottom.linkTo(bigImage.bottom)
                        end.linkTo(verticalGuideline)
                    },
                iconData = icon
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun BigImageAndMediumIconPreview() {
    PreviewTheme {
        BigImageAndMediumIcon(
            base64Image = "",
            icon = AppIcons.IdStroke
        )
    }
}