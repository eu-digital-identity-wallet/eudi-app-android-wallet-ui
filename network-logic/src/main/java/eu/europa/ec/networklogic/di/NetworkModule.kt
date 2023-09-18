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

package eu.europa.ec.networklogic.di

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
fun providesHttpLoggingInterceptor() = HttpLoggingInterceptor()
    .apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

@Factory
fun provideOkHttpClient(
    httpLoggingInterceptor: HttpLoggingInterceptor,
    configLogic: ConfigLogic
): OkHttpClient {
    return OkHttpClient().newBuilder()
        .readTimeout(configLogic.environmentConfig.readTimeoutSeconds, TimeUnit.SECONDS)
        .connectTimeout(configLogic.environmentConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor(httpLoggingInterceptor)
        .build()
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
    return Retrofit.Builder().baseUrl(configLogic.environmentConfig.getServerHost()).client(okHttpClient)
        .addConverterFactory(converterFactory).build()
}
