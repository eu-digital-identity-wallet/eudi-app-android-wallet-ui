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

package eu.europa.ec.networklogic.di

import eu.europa.ec.businesslogic.config.AppBuildType
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.networklogic.config.NetworkConfig
import eu.europa.ec.networklogic.config.NetworkConfigImpl
import eu.europa.ec.networklogic.extension.installNetworkLimits
import eu.europa.ec.networklogic.repository.WalletAttestationRepository
import eu.europa.ec.networklogic.repository.WalletAttestationRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val logicNetworkModule = module {
    singleOf(::provideJson)
    singleOf(::provideNetworkConfig)
    singleOf(::provideHttpClient)
    singleOf(::provideWalletAttestationRepository)
}

fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

fun provideNetworkConfig(): NetworkConfig = NetworkConfigImpl()

/**
 * The HTTP client of the wallet, which Wallet Core uses for every issuer, verifier, trusted-list
 * and status-list request. It applies the timeouts and the response-size limit of
 * [NetworkConfig] ([installNetworkLimits]), so that a request a link starts cannot hang or fill
 * memory.
 */
fun provideHttpClient(
    json: Json,
    configLogic: ConfigLogic,
    networkConfig: NetworkConfig,
): HttpClient {
    return HttpClient(Android) {

        installNetworkLimits(networkConfig)

        install(Logging) {
            logger = Logger.DEFAULT
            level = when (configLogic.appBuildType) {
                AppBuildType.DEBUG -> LogLevel.BODY
                AppBuildType.RELEASE -> LogLevel.NONE
            }
        }

        install(ContentNegotiation) {
            json(
                json = json,
                contentType = ContentType.Application.Json
            )
        }
    }
}

fun provideWalletAttestationRepository(httpClient: HttpClient): WalletAttestationRepository =
    WalletAttestationRepositoryImpl(httpClient)