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

package eu.europa.ec.networklogic.di

import eu.europa.ec.businesslogic.config.AppBuildType
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.networklogic.api.Api
import eu.europa.ec.networklogic.api.ApiClient
import eu.europa.ec.networklogic.api.ApiClientImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@ComponentScan("eu.europa.ec.networklogic")
class LogicNetworkModule

@Factory
fun providesHttpLoggingInterceptor(configLogic: ConfigLogic) = HttpLoggingInterceptor()
    .apply {
        level = when (configLogic.appBuildType) {
            AppBuildType.DEBUG -> HttpLoggingInterceptor.Level.BODY
            AppBuildType.RELEASE -> HttpLoggingInterceptor.Level.NONE
        }
    }

@Factory
fun provideOkHttpClient(
    httpLoggingInterceptor: HttpLoggingInterceptor,
    configLogic: ConfigLogic
): OkHttpClient {

    val client = OkHttpClient().newBuilder()
        .readTimeout(configLogic.environmentConfig.readTimeoutSeconds, TimeUnit.SECONDS)
        .connectTimeout(configLogic.environmentConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor(httpLoggingInterceptor)

    return client.build()
}

@Factory
fun provideApi(retrofit: Retrofit): Api = retrofit.create(Api::class.java)

@Factory
fun provideConverterFactory(): GsonConverterFactory = GsonConverterFactory.create()

@Single
fun provideApiClient(api: Api): ApiClient = ApiClientImpl(api)

@Single
fun provideRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder().baseUrl(configLogic.environmentConfig.getServerHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory).build()
}
