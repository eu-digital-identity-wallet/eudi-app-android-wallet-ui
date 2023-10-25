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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper

/**
 * Data class that is used to construct and initialize the fab button.
 *
 * @param onClick  Operation to invoke when users click on the fab.
 * @param content  Composable that is used to construct the fab content.
 */
data class FabData(
    val onClick: () -> Unit,
    val content: @Composable () -> Unit,
)

@Composable
fun WrapFab(modifier: Modifier = Modifier, data: FabData) {
    FloatingActionButton(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        onClick = data.onClick
    ) {
        data.content()
    }
}

@Composable
fun WrapPrimaryFab(modifier: Modifier = Modifier, data: FabData) {
    FloatingActionButton(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.backgroundPaper,
        contentColor = MaterialTheme.colorScheme.backgroundPaper,
        onClick = data.onClick
    ) {
        data.content()
    }
}

@Composable
fun WrapSecondaryFab(modifier: Modifier = Modifier, data: FabData) {
    FloatingActionButton(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        onClick = data.onClick
    ) {
        data.content()
    }
}

@Composable
fun WrapExtendedFab(
    modifier: Modifier = Modifier,
    data: FabData
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        onClick = data.onClick
    ) {
        data.content()
    }
}