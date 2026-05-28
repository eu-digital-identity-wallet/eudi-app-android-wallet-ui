/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.commonfeature.config

import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.serializer.UiSerializable
import eu.europa.ec.uilogic.serializer.UiSerializableParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface BiometricMode {
    @Serializable
    @SerialName("Default")
    data class Default(
        val descriptionWhenBiometricsEnabled: String,
        val descriptionWhenBiometricsNotEnabled: String,
        val textAbovePin: String,
    ) : BiometricMode

    @Serializable
    @SerialName("Login")
    data class Login(
        val title: String,
        val subTitleWhenBiometricsEnabled: String,
        val subTitleWhenBiometricsNotEnabled: String,
    ) : BiometricMode
}

@Serializable
data class BiometricUiConfig(
    val mode: BiometricMode,
    val isPreAuthorization: Boolean = false,
    val shouldInitializeBiometricAuthOnCreate: Boolean = true,
    val onSuccessNavigation: ConfigNavigation,
    val onBackNavigationConfig: OnBackNavigationConfig
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "biometricConfig"
    }
}

@Serializable
data class OnBackNavigationConfig(
    val onBackNavigation: ConfigNavigation?,
    private val hasToolbarBackIcon: Boolean
) {
    val isBackable: Boolean get() = hasToolbarBackIcon && onBackNavigation != null
}