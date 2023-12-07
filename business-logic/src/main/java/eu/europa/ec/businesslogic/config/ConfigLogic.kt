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

import eu.europa.ec.businesslogic.BuildConfig

interface ConfigLogic {

    /**
     * Build Type.
     */
    val appBuildType: AppBuildType get() = AppBuildType.getType()

    /**
     * Server Environment Configuration.
     */
    val environmentConfig: EnvironmentConfig

    /**
     * Deeplink Schema.
     */
    val deepLink: String get() = BuildConfig.DEEPLINK
}

enum class AppBuildType {
    DEBUG, RELEASE;

    companion object {
        fun getType(): AppBuildType {
            return when (BuildConfig.BUILD_TYPE) {
                "debug" -> DEBUG
                else -> RELEASE
            }
        }
    }
}

abstract class EnvironmentConfig {
    val environment: ServerConfig
        get() {
            return when (AppBuildType.getType()) {
                AppBuildType.DEBUG -> ServerConfig.Dev
                AppBuildType.RELEASE -> ServerConfig.Demo
            }
        }

    val connectTimeoutSeconds: Long get() = 60
    val readTimeoutSeconds: Long get() = 60

    abstract fun getServerHost(): String
    sealed class ServerConfig {
        data object Dev : ServerConfig()
        data object Demo : ServerConfig()
    }
}