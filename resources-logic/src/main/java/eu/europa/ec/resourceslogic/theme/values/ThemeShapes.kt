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

package eu.europa.ec.resourceslogic.theme.values

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.templates.ThemeShapesTemplate
import eu.europa.ec.resourceslogic.theme.values.ThemeShapes.Companion.LARGE
import eu.europa.ec.resourceslogic.theme.values.ThemeShapes.Companion.SMALL

class ThemeShapes {
    companion object {
        const val EXTRA_SMALL = 16.0
        const val SMALL = 16.0
        const val MEDIUM = 16.0
        const val LARGE = 32.0
        const val EXTRA_LARGE = 32.0

        val shapes = ThemeShapesTemplate(
            extraSmall = EXTRA_SMALL,
            small = SMALL,
            medium = MEDIUM,
            large = LARGE,
            extraLarge = EXTRA_LARGE
        )
    }
}

val Shapes.bottomCorneredShapeSmall: Shape
    @Composable get() = RoundedCornerShape(bottomStart = SMALL.dp, bottomEnd = SMALL.dp)

val Shapes.topCorneredShapeSmall: Shape
    @Composable get() = RoundedCornerShape(topStart = SMALL.dp, topEnd = SMALL.dp)

val Shapes.allCorneredShapeSmall: Shape
    @Composable get() = RoundedCornerShape(SMALL.dp)

val Shapes.allCorneredShapeLarge: Shape
    @Composable get() = RoundedCornerShape(LARGE.dp)