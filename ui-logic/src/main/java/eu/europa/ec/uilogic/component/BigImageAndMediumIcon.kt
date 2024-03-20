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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.WrapImage

@Composable
fun BigImageAndMediumIcon(
    base64Image: String,
    icon: IconData?,
    docHasExpired: Boolean,
) {
    val bigImageWidth = 160.dp
    val bigImageHeight = 160.dp
    val smallIconWidth = 96.dp
    val smallIconHeight = 72.dp
    val smallIconStartOffset = bigImageWidth + SPACING_MEDIUM.dp

    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (bigImage, mediumIcon) = createRefs()
        val verticalGuideline = createGuidelineFromStart(smallIconStartOffset)
        val imageModifier = Modifier
            .size(width = bigImageWidth, height = bigImageHeight)
            .clip(RoundedCornerShape(SIZE_SMALL.dp))
            .constrainAs(bigImage) {
                start.linkTo(parent.start)
            }

        UserImageOrPlaceholder(
            userBase64Image = base64Image,
            modifier = imageModifier
        )

        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(width = smallIconWidth, height = smallIconHeight)
                    .clip(RoundedCornerShape(SIZE_SMALL.dp))
                    .constrainAs(mediumIcon) {
                        start.linkTo(verticalGuideline)
                        top.linkTo(bigImage.top)
                        bottom.linkTo(bigImage.bottom)
                        end.linkTo(verticalGuideline)
                    },
            ) {
                WrapImage(
                    iconData = icon
                )
                if (docHasExpired) {
                    DocumentHasExpiredIndicator(
                        backgroundColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun BigImageAndMediumIconWithDocExpiredPreview() {
    PreviewTheme {
        BigImageAndMediumIcon(
            base64Image = "",
            icon = AppIcons.IdStroke,
            docHasExpired = true,
        )
    }
}

@ThemeModePreviews
@Composable
private fun BigImageAndMediumIconWithDocNotExpiredPreview() {
    PreviewTheme {
        BigImageAndMediumIcon(
            base64Image = "",
            icon = AppIcons.IdStroke,
            docHasExpired = false,
        )
    }
}