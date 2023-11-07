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

package eu.europa.ec.presentationfeature.ui.loading

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.ui.loading.CommonLoadingViewModel
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PresentationLoadingViewModel constructor(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider
) : CommonLoadingViewModel() {

    override fun getTitle(): String {
        return resourceProvider.getString(R.string.online_authentication_loading_title)
    }

    override fun getSubtitle(): String {
        return resourceProvider.getString(R.string.online_authentication_loading_subtitle)
    }

    override fun getPreviousScreen(): Screen {
        return PresentationScreens.CrossDevice
    }

    override fun getCallerScreen(): Screen {
        return PresentationScreens.Loading
    }

    override fun getNextScreen(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = generateComposableArguments(
                getSuccessConfig()
            )
        )
    }

    override fun doWork() {
        viewModelScope.launch {
            delay(5000)
            doNavigation(NavigationType.PUSH)
        }
    }

    private fun getSuccessConfig(): Map<String, String> =
        mapOf(
            SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                SuccessUIConfig(
                    header = resourceProvider.getString(R.string.online_authentication_success_config_title),
                    content = resourceProvider.getString(R.string.online_authentication_success_config_subtitle),
                    imageConfig = SuccessUIConfig.ImageConfig(
                        type = SuccessUIConfig.ImageConfig.Type.DEFAULT
                    ),
                    buttonConfig = listOf(
                        SuccessUIConfig.ButtonConfig(
                            text = resourceProvider.getString(R.string.online_authentication_success_config_primary_button_text),
                            style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                            navigation = ConfigNavigation(
                                navigationType = NavigationType.POP,
                                screenToNavigate = DashboardScreens.Dashboard
                            )
                        )
                    ),
                    onBackScreenToNavigate = ConfigNavigation(
                        navigationType = NavigationType.POP,
                        screenToNavigate = DashboardScreens.Dashboard
                    ),
                ),
                SuccessUIConfig.Parser
            ).orEmpty()
        )
}