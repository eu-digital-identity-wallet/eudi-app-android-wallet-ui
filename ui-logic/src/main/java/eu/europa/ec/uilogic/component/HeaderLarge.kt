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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.resourceslogic.theme.values.bottomCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.textSecondaryLight
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapImage

data class HeaderData(
    val title: String,
    val subtitle: String,
    val image: IconData,
    val icon: IconData? = null
)

@Composable
fun HeaderLarge(
    modifier: Modifier = Modifier,
    data: HeaderData,
    contentPadding: PaddingValues = PaddingValues(all = SPACING_LARGE.dp)
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.bottomCorneredShapeSmall
            )
            .padding(contentPadding)
    ) {

        VSpacer.Large()

        Text(
            text = data.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.backgroundPaper
        )

        VSpacer.Small()

        Text(
            text = data.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.textSecondaryLight
        )

        VSpacer.Large()

        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
            val (image, icon) = createRefs()
            val verticalGuideline = createGuidelineFromStart(0.50f)
            WrapImage(
                modifier = Modifier
                    .size(160.dp, 160.dp)
                    .constrainAs(image) {
                        start.linkTo(parent.start)
                    },
                iconData = data.image
            )
            if (data.icon != null) {
                WrapImage(
                    modifier = Modifier
                        .clip(RoundedCornerShape(SIZE_SMALL.dp))
                        .constrainAs(icon) {
                            start.linkTo(verticalGuideline)
                            top.linkTo(image.top)
                            bottom.linkTo(image.bottom)
                            end.linkTo(verticalGuideline)
                        },
                    iconData = data.icon,
                )
            }
        }
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