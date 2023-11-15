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

package eu.europa.ec.commonfeature.di

import eu.europa.ec.businesslogic.controller.biometry.BiometricController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.validator.FormValidator
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.commonfeature.interactor.BiometricInteractorImpl
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorImpl
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("eu.europa.ec.commonfeature")
class FeatureCommonModule

@Factory
fun provideQuickPinInteractor(
    formValidator: FormValidator,
    prefKeys: PrefKeys,
    resourceProvider: ResourceProvider
): QuickPinInteractor {
    return QuickPinInteractorImpl(formValidator, prefKeys, resourceProvider)
}

@Factory
fun provideBiometricInteractor(
    prefKeys: PrefKeys,
    biometricController: BiometricController,
    quickPinInteractor: QuickPinInteractor
): BiometricInteractor {
    return BiometricInteractorImpl(prefKeys, biometricController, quickPinInteractor)
}

@Single
fun provideEudiWallet(): EudiWallet = EudiWallet