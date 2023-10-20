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

package eu.europa.ec.startupfeature.di

import eu.europa.ec.networklogic.api.ApiClient
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.startupfeature.interactor.StartupInteractor
import eu.europa.ec.startupfeature.interactor.StartupInteractorImpl
import eu.europa.ec.startupfeature.interactor.splash.SplashInteractor
import eu.europa.ec.startupfeature.interactor.splash.SplashInteractorImpl
import eu.europa.ec.startupfeature.repository.StartupRepository
import eu.europa.ec.startupfeature.repository.StartupRepositoryImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("eu.europa.ec.startupfeature")
class FeatureStartupModule

@Factory
fun provideStartupRepository(apiClient: ApiClient): StartupRepository {
    return StartupRepositoryImpl(apiClient)
}

@Factory
fun provideStartupInteractor(
    startupRepository: StartupRepository,
    resourceProvider: ResourceProvider
): StartupInteractor {
    return StartupInteractorImpl(startupRepository, resourceProvider)
}

@Factory
fun provideSplashInteractor(): SplashInteractor {
    return SplashInteractorImpl()
}