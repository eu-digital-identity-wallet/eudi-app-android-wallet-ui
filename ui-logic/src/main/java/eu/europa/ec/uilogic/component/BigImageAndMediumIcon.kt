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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapImage

@Composable
fun BigImageAndMediumIcon(
    image: IconData,
    icon: IconData?,
) {
    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (bigImage, mediumIcon) = createRefs()
        val verticalGuideline = createGuidelineFromStart(0.50f)
        WrapImage(
            modifier = Modifier
                .size(width = 160.dp, height = 160.dp)
                .clip(RoundedCornerShape(SIZE_SMALL.dp))
                .constrainAs(bigImage) {
                    start.linkTo(parent.start)
                },
            iconData = image
        )
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
            image = AppIcons.User,
            icon = AppIcons.IdStroke
        )
    }
}