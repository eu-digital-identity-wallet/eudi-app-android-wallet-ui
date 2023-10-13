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

package eu.europa.ec.uilogic.extension

import androidx.navigation.NavController
import eu.europa.ec.uilogic.config.FlowCompletion

private const val FLOW_CANCELLATION = "FLOW_CANCELLATION"
private const val FLOW_SUCCESS = "FLOW_SUCCESS"

fun NavController.setBackStackFlowCancelled(screenRouter: String) {
    try {
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_SUCCESS)
        getBackStackEntry(screenRouter).savedStateHandle[FLOW_CANCELLATION] = true
    } catch (_: Exception) {
    }
}

fun NavController.setBackStackFlowSuccess(screenRouter: String) {
    try {
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_CANCELLATION)
        getBackStackEntry(screenRouter).savedStateHandle[FLOW_SUCCESS] = true
    } catch (_: Exception) {
    }
}

fun NavController.resetBackStack(screenRouter: String) {
    try {
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_CANCELLATION)
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_SUCCESS)
    } catch (_: Exception) {
    }
}

fun NavController.wasFlowCancelled(): Boolean {
    return currentBackStackEntry
        ?.savedStateHandle
        ?.remove<Boolean>(FLOW_CANCELLATION)
        ?: false
}

fun NavController.wasFlowSucceeded(): Boolean {
    return currentBackStackEntry
        ?.savedStateHandle
        ?.remove<Boolean>(FLOW_SUCCESS)
        ?: false
}

fun NavController.getFlowCompletion(): FlowCompletion {
    return if (wasFlowCancelled()) {
        FlowCompletion.CANCEL
    } else if (wasFlowSucceeded()) {
        FlowCompletion.SUCCESS
    } else {
        FlowCompletion.NONE
    }
}