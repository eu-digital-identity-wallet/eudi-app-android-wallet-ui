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

package eu.europa.ec.uilogic.component.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object VSpacer {

    @Composable
    fun Custom(space: Int) = Spacer(modifier = Modifier.height(space.dp))

    @Composable
    fun ExtraSmall() = Spacer(modifier = Modifier.height(SPACING_EXTRA_SMALL.dp))

    @Composable
    fun Small() = Spacer(modifier = Modifier.height(SPACING_SMALL.dp))

    @Composable
    fun Medium() = Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

    @Composable
    fun Large() = Spacer(modifier = Modifier.height(SPACING_LARGE.dp))

    @Composable
    fun ExtraLarge() = Spacer(modifier = Modifier.height(SPACING_EXTRA_LARGE.dp))

}

object HSpacer {

    @Composable
    fun Custom(space: Int) = Spacer(modifier = Modifier.width(space.dp))

    @Composable
    fun ExtraSmall() = Spacer(modifier = Modifier.width(SPACING_EXTRA_SMALL.dp))

    @Composable
    fun Small() = Spacer(modifier = Modifier.width(SPACING_SMALL.dp))

    @Composable
    fun Medium() = Spacer(modifier = Modifier.width(SPACING_MEDIUM.dp))

    @Composable
    fun Large() = Spacer(modifier = Modifier.width(SPACING_LARGE.dp))

    @Composable
    fun ExtraLarge() = Spacer(modifier = Modifier.width(SPACING_EXTRA_LARGE.dp))

}