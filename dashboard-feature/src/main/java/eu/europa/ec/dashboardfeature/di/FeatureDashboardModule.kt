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

package eu.europa.ec.dashboardfeature.di

import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.document.AddDocumentInteractor
import eu.europa.ec.dashboardfeature.interactor.document.AddDocumentInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.document.DocumentDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.document.DocumentDetailsInteractorImpl
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("eu.europa.ec.dashboardfeature")
class FeatureDashboardModule

@Factory
fun provideDashboardInteractor(
    resourceProvider: ResourceProvider,
    eudiWallet: EudiWallet
): DashboardInteractor = DashboardInteractorImpl(resourceProvider, eudiWallet)

@Factory
fun provideDocumentDetailsInteractor(
    resourceProvider: ResourceProvider,
): DocumentDetailsInteractor = DocumentDetailsInteractorImpl(resourceProvider)

@Factory
fun provideAddDocumentInteractor(
    resourceProvider: ResourceProvider,
    eudiWallet: EudiWallet
): AddDocumentInteractor = AddDocumentInteractorImpl(resourceProvider, eudiWallet)