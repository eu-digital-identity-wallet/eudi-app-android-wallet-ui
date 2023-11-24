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

package eu.europa.ec.proximityfeature.di

import eu.europa.ec.commonfeature.di.PRESENTATION_SCOPE_ID
import eu.europa.ec.commonfeature.interactor.EudiWalletInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximityRequestInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityRequestInteractorImpl
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.ScopeId

@Module
@ComponentScan("eu.europa.ec.proximityfeature")
class FeatureProximityModule

@Factory
fun provideProximityQRInteractor(
    resourceProvider: ResourceProvider,
    @ScopeId(name = PRESENTATION_SCOPE_ID) eudiWalletInteractor: EudiWalletInteractor
): ProximityQRInteractor =
    ProximityQRInteractorImpl(resourceProvider, eudiWalletInteractor)

@Factory
fun provideProximityRequestInteractor(
    resourceProvider: ResourceProvider,
    @ScopeId(name = PRESENTATION_SCOPE_ID) eudiWalletInteractor: EudiWalletInteractor
): ProximityRequestInteractor =
    ProximityRequestInteractorImpl(resourceProvider, eudiWalletInteractor)

@Factory
fun provideProximityLoadingInteractor(
    resourceProvider: ResourceProvider,
    @ScopeId(name = PRESENTATION_SCOPE_ID) eudiWalletInteractor: EudiWalletInteractor
): ProximityLoadingInteractor =
    ProximityLoadingInteractorImpl(resourceProvider, eudiWalletInteractor)