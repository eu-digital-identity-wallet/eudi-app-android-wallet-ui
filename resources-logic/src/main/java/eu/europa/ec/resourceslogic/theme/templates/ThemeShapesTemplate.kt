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

package eu.europa.ec.resourceslogic.theme.templates

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

data class ThemeShapesTemplate(
    val extraSmall: Double,
    val small: Double,
    val medium: Double,
    val large: Double,
    val extraLarge: Double
) {
    companion object {
        fun ThemeShapesTemplate.toShapes(): Shapes = Shapes(
            extraSmall = RoundedCornerShape(extraSmall.dp),
            small = RoundedCornerShape(small.dp),
            medium = RoundedCornerShape(medium.dp),
            large = RoundedCornerShape(large.dp),
            extraLarge = RoundedCornerShape(extraLarge.dp),
        )
    }
}