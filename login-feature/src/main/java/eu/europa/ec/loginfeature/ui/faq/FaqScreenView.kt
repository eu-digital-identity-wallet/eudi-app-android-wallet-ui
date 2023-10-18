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

package eu.europa.ec.loginfeature.ui.faq

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.theme.values.backgroundDefault
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.StartupScreens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun FaqScreen(
    navController: NavController,
    viewModel: FaqScreenViewModel
) {

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.BACKABLE
    ) { paddingValues ->
        FaqScreenView(
            state = viewModel.viewState.value,
            effectFlow = viewModel.effect,
            onEventSend = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.SwitchModule -> {
                        navController.navigate(navigationEffect.moduleRoute.route) {
                            popUpTo(ModuleRoute.StartupModule.route) { inclusive = true }
                        }
                    }

                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screen) {
                            popUpTo(StartupScreens.Splash.screenRoute) { inclusive = true }
                        }
                    }
                }
            },
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun FaqScreenView(
    state: State,
    effectFlow: Flow<Effect>?,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    )
    {
        var text by remember { mutableStateOf("") }
        ContentTitle("FAQs")

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text("Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.inversePrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, end = 10.dp, top = 10.dp, bottom = 20.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        ExpandableListScreen(
            sections = state.faqItems
        )
    }

    LaunchedEffect(Unit) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchModule -> onNavigationRequested(effect)
                is Effect.Navigation.SwitchScreen -> onNavigationRequested(effect)
            }
        }?.collect()
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ExpandableListScreen(
    sections: List<CollapsableSection>,
) {
    var expandedItemIndex by remember { mutableStateOf(-1) }

    LazyColumn {
        sections.forEachIndexed { i, dataItem ->
            var isExpanded by mutableStateOf(i == expandedItemIndex)

            item(key = "header_$i") {
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.backgroundDefault,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            expandedItemIndex = if (isExpanded) -1 else i
                            isExpanded = !isExpanded
                        }) {

                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically

                    ) {
                        Text(
                            text = dataItem.title,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Black
                        )
                        Icon(
                            Icons.Default.run {
                                if (isExpanded)
                                    KeyboardArrowDown
                                else
                                    KeyboardArrowUp
                            },
                            contentDescription = "arrow-down",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }


                    if (isExpanded) {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = dataItem.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

