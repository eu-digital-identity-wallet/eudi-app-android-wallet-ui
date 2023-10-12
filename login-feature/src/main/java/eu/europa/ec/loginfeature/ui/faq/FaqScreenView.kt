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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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
    Content(
        state = viewModel.viewState.value,
        effectFlow = viewModel.effect,
        onEventSend = { viewModel.setEvent(it) },
        onNavigationRequested = {
            when (it) {
                is Effect.Navigation.SwitchModule -> {
                    navController.navigate(it.moduleRoute.route) {
                        popUpTo(ModuleRoute.StartupModule.route) { inclusive = true }
                    }
                }

                is Effect.Navigation.SwitchScreen -> {
                    navController.navigate(it.screen) {
                        popUpTo(StartupScreens.Splash.screenRoute) { inclusive = true }
                    }
                }
            }
        }
    )
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>?,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit
) {

    ExpandableListScreen(
        sections = listOf(
            CollapsableSection(
                title = "Question A goes Here",
                rows = listOf("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.")
            ),
            CollapsableSection(
                title = "Question B goes Here",
                rows = listOf("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.")
            ),
            CollapsableSection(
                title = "Question C goes Here",
                rows = listOf("Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
            ),
            CollapsableSection(
                title = "Question D goes Here",
                rows = listOf("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.")
            ),
            CollapsableSection(
                title = "Question E goes Here",
                rows = listOf("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.")
            ),
            CollapsableSection(
                title = "Question F goes Here",
                rows = listOf("Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
            ),
        ),
    )

    LaunchedEffect(Unit) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchModule -> onNavigationRequested(effect)
                is Effect.Navigation.SwitchScreen -> onNavigationRequested(effect)
            }
        }?.collect()
    }
}

@Composable
fun CollapsableLazyColumn(
    sections: List<CollapsableSection>,
    modifier: Modifier = Modifier
) {

    var searchText by remember { mutableStateOf("") }

   // -----------------------------------

    val collapsedState = remember(sections) { sections.map { true }.toMutableStateList() }


    LazyColumn(modifier) {
        sections.forEachIndexed { i, dataItem ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            collapsedState[i] = !collapsed
                        }
                ) {
                    Icon(
                        Icons.Default.run {
                            if (collapsed)
                                KeyboardArrowDown
                            else
                                KeyboardArrowUp
                        },
                        contentDescription = "",
                        tint = Color.LightGray,
                    )
                    Text(
                        dataItem.title,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .weight(1f)
                    )
                }
                Divider()
            }
            if (!collapsed) {
                items(dataItem.rows) { row ->
                    Row {
                        Spacer(modifier = Modifier.size(MaterialIconDimension.dp))
                        Text(
                            row,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

data class CollapsableSection(val title: String, val rows: List<String>)

const val MaterialIconDimension = 24f

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableListScreen(sections: List<CollapsableSection>,
                         modifier: Modifier = Modifier) {
    var searchText by remember { mutableStateOf("") }
    var expandedItemIndex by remember { mutableStateOf(-1) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(" ") },
                navigationIcon = {
                    IconButton(onClick = {/* Handle back action here */}) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.White))
            Text(
                text = "FAQs",
                modifier = Modifier.padding(start = 20.dp, end = 10.dp, top = 50.dp, bottom = 10.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 24.sp)
        }) {


        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Search field
            var text by mutableStateOf("")
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 5.dp, end = 10.dp, top = 80.dp, bottom = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    //.border(2.dp, color = colorResource(R.color.super_light_gray))
                    .background(Color.Transparent)
            )

            val collapsedState = remember(sections) { sections.map { true }.toMutableStateList() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor =  MaterialTheme.colorScheme.onSecondary,
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                )
                   )
            {
            LazyColumn(modifier) {
                sections.forEachIndexed { i, dataItem ->
                    val collapsed = collapsedState[i]
                    item(key = "header_$i") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    collapsedState[i] = !collapsed
                                }
                        ) {
                            Text(
                                dataItem.title,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .weight(1f)
                            )
                            Icon(
                                Icons.Default.run {
                                    if (collapsed)
                                        KeyboardArrowDown
                                    else
                                        KeyboardArrowUp
                                },
                                contentDescription = "",
                                tint = Color.LightGray,
                            )

                        }
                        //Divider()
                    }
                    if (!collapsed) {
                        items(dataItem.rows) { row ->
                            Row {
                                Spacer(modifier = Modifier.size(MaterialIconDimension.dp))
                                Text(
                                    row,
                                    modifier = Modifier
                                        .padding(vertical = 10.dp)
                                )
                            }
                           // Divider()
                        }
                    }
                }
            }
//            val listA = listOf<String>(
//                    "Example FAQ",
//                    "Android FAQ",
//                    "Tutorial FAQ",
//                    "Jetpack",
//                    "Compose",
//                    "List",
//                    "Example",
//                    "Simple",
//                    "List")
//
//            LazyColumn(modifier = Modifier.fillMaxWidth()) {
//
//            listA.forEachIndexed { index, item ->
//                    var isExpanded by mutableStateOf(index == expandedItemIndex)
//
//                    item {
//                        Column(
//                            modifier =
//                            Modifier.fillMaxWidth()
//                                .height(120.dp)
//                                .padding(10.dp)
//                                .clip(RoundedCornerShape(16.dp))
//                                .background(MaterialTheme.colorScheme.primary)
//                                .clickable {
//                                    expandedItemIndex = if (isExpanded) -1 else index
//                                    isExpanded = !isExpanded
//                                }) {
//                            Text(text = item, style = MaterialTheme.typography.bodyMedium)
//
//                            if (isExpanded) {
//                                Box(
//                                    modifier =
//                                    Modifier.fillMaxWidth()
//                                        .padding(8.dp)
//                                        .clip(shape = MaterialTheme.shapes.small)
//                                        .background(Color.Gray.copy(alpha = 0.1f))
//                                        .padding(16.dp)) {
//                                    Text(text = "Additional content for $item")
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }
    }
    }
}
//
//@Composable
//fun CardArrow(
//    degrees: Float,
//    onClick: () -> Unit
//) {
//    IconButton(
//        onClick = onClick,
//        content = {
//            Icon(
//                painter = painterResource(id = R.drawable.ic_expand_less_24),
//                contentDescription = "Expandable Arrow",
//                modifier = Modifier.rotate(degrees),
//            )
//        }
//    )
//}