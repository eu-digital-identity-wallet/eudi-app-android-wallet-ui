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

package eu.europa.ec.uilogic.di

import eu.europa.ec.analyticslogic.controller.AnalyticsController
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.uilogic.config.ConfigUILogic
import eu.europa.ec.uilogic.config.ConfigUILogicImpl
import eu.europa.ec.uilogic.container.EudiComponentActivityViewModel
import eu.europa.ec.uilogic.navigation.RouterHost
import eu.europa.ec.uilogic.navigation.RouterHostImpl
import eu.europa.ec.uilogic.serializer.UiSerializer
import eu.europa.ec.uilogic.serializer.UiSerializerImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val logicUiModule = module {
    viewModelOf(::EudiComponentActivityViewModel)
    factoryOf(::provideRouterHost)
    factoryOf(::provideUiSerializer)
    singleOf(::provideConfigUILogic)
}

fun provideRouterHost(
    configUILogic: ConfigUILogic,
    analyticsController: AnalyticsController,
    sessionController: SessionController
): RouterHost = RouterHostImpl(configUILogic, analyticsController, sessionController)

fun provideUiSerializer(): UiSerializer = UiSerializerImpl()

fun provideConfigUILogic(): ConfigUILogic = ConfigUILogicImpl()