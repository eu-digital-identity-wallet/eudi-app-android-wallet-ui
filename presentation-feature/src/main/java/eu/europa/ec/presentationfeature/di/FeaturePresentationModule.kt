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

package eu.europa.ec.presentationfeature.di

import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.presentationfeature.interactor.PresentationCrossDeviceInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationCrossDeviceInteractorImpl
import eu.europa.ec.presentationfeature.interactor.PresentationSameDeviceInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationSameDeviceInteractorImpl
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("eu.europa.ec.presentationfeature")
class FeaturePresentationModule

@Factory
fun providePresentationCrossDeviceInteractor(
    resourceProvider: ResourceProvider,
    eudiWallet: EudiWallet
): PresentationCrossDeviceInteractor {
    return PresentationCrossDeviceInteractorImpl(resourceProvider, eudiWallet)
}

@Factory
fun providePresentationSameDeviceInteractor(
    resourceProvider: ResourceProvider,
): PresentationSameDeviceInteractor {
    return PresentationSameDeviceInteractorImpl(resourceProvider)
}