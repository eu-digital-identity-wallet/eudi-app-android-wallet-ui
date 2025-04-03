/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.businesslogic.config

import android.content.Context
import eu.europa.ec.eudi.rqesui.infrastructure.config.EudiRQESUiConfig

class ConfigLogicImpl(val context: Context) : ConfigLogic {
    override val appFlavor: AppFlavor
        get() = AppFlavor.DEV

    override val environmentConfig: EnvironmentConfig
        get() = DevEnvironmentConfig()

    override val rqesConfig: EudiRQESUiConfig
        get() = RQESConfigImpl(context)

    override val changelogUrl: String?
        get() = null
}

private class DevEnvironmentConfig : EnvironmentConfig() {
    override fun getServerHost(): String = when (environment) {
        ServerConfig.Debug -> ""
        ServerConfig.Release -> ""
    }
}