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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.zIndex
import eu.europa.ec.uilogic.component.utils.screenPaddings

enum class LoadingType {
    NORMAL, NONE
}

data class ToolbarAction(
    val icon: IconData,
    val order: Int = 100,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

data class ToolbarConfig(
    val title: String = "",
    val actions: List<ToolbarAction> = listOf()
)

enum class ScreenNavigateAction {
    BACKABLE, CANCELABLE, NONE
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContentScreen(
    loadingType: LoadingType = LoadingType.NONE,
    toolBarConfig: ToolbarConfig? = null,
    navigatableAction: ScreenNavigateAction = ScreenNavigateAction.BACKABLE,
    onBack: (() -> Unit)? = null,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    stickyBottom: @Composable (() -> Unit)? = null,
    fab: FabData? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    bodyContent: @Composable (PaddingValues) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (topBar != null) topBar.invoke()
            else toolBarConfig?.let {
                DefaultToolBar(
                    scrollBehavior = scrollBehavior,
                    navigatableAction = navigatableAction,
                    onBack = onBack,
                    keyboardController = keyboardController,
                    toolbarConfig = it,
                )
            } ?: {}
        },
        bottomBar = bottomBar ?: {},
        floatingActionButton = {
            fab?.let { fabData ->
                WrapFab(data = fabData)
            }
        },
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = {
            Snackbar.PlaceHolder(snackbarHostState = snackbarHostState)
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPaddings(padding))
        ) {

            Column(modifier = Modifier.fillMaxSize()) {

                Box(modifier = Modifier.weight(1f)) {
                    bodyContent(screenPaddings(padding))
                }

                stickyBottom?.let { stickyBottomContent ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(Z_STICKY),
                        contentAlignment = Alignment.Center
                    ) {
                        stickyBottomContent()
                    }
                }
            }

            if (loadingType == LoadingType.NORMAL) LoadingIndicator()
        }
    }

    BackHandler(enabled = true) {
        onBack?.invoke()
    }
}