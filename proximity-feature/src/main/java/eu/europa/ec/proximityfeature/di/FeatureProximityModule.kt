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

package eu.europa.ec.proximityfeature.di

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximityRequestInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityRequestInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximitySuccessInteractor
import eu.europa.ec.proximityfeature.interactor.ProximitySuccessInteractorImpl
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan("eu.europa.ec.proximityfeature")
class FeatureProximityModule

@Factory
fun provideProximityQRInteractor(
    resourceProvider: ResourceProvider
): ProximityQRInteractor =
    ProximityQRInteractorImpl(resourceProvider)

@Factory
fun provideProximityRequestInteractor(
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController
): ProximityRequestInteractor =
    ProximityRequestInteractorImpl(
        resourceProvider,
        uuidProvider,
        walletCoreDocumentsController
    )

@Factory
fun provideProximityLoadingInteractor(
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor
): ProximityLoadingInteractor =
    ProximityLoadingInteractorImpl(deviceAuthenticationInteractor)

@Factory
fun provideProximitySuccessInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider
): ProximitySuccessInteractor {
    return ProximitySuccessInteractorImpl(
        walletCoreDocumentsController,
        resourceProvider,
        uuidProvider
    )
}