/*
 * Copyright (c) 2025 European Commission
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