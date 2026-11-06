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

package eu.europa.ec.dashboardfeature.di

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.WalletCoreDCAPIRegistrationController
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCoreResetController
import eu.europa.ec.corelogic.controller.WalletCoreTransactionLogController
import eu.europa.ec.corelogic.controller.WalletCoreTransactionRecordingController
import eu.europa.ec.corelogic.provider.DCAPISettingProvider
import eu.europa.ec.corelogic.worker.WalletWorkScheduler
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.DocumentSignInteractor
import eu.europa.ec.dashboardfeature.interactor.DocumentSignInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.HomeInteractor
import eu.europa.ec.dashboardfeature.interactor.HomeInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.ResetWalletInteractor
import eu.europa.ec.dashboardfeature.interactor.ResetWalletInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.SettingsInteractor
import eu.europa.ec.dashboardfeature.interactor.SettingsInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.TransactionHistoryInteractor
import eu.europa.ec.dashboardfeature.interactor.TransactionHistoryInteractorImpl
import eu.europa.ec.dashboardfeature.interactor.TransactionsInteractor
import eu.europa.ec.dashboardfeature.interactor.TransactionsInteractorImpl
import eu.europa.ec.dashboardfeature.ui.dashboard.DashboardViewModel
import eu.europa.ec.dashboardfeature.ui.document_sign.DocumentSignViewModel
import eu.europa.ec.dashboardfeature.ui.documents.detail.DocumentDetailsViewModel
import eu.europa.ec.dashboardfeature.ui.documents.list.DocumentsViewModel
import eu.europa.ec.dashboardfeature.ui.home.HomeViewModel
import eu.europa.ec.dashboardfeature.ui.reset_wallet.ResetWalletViewModel
import eu.europa.ec.dashboardfeature.ui.settings.SettingsViewModel
import eu.europa.ec.dashboardfeature.ui.transactions.data_deletion.DataDeletionRequestViewModel
import eu.europa.ec.dashboardfeature.ui.transactions.detail.TransactionDetailsViewModel
import eu.europa.ec.dashboardfeature.ui.transactions.dpa_report.DpaReportViewModel
import eu.europa.ec.dashboardfeature.ui.transactions.history.TransactionHistoryViewModel
import eu.europa.ec.dashboardfeature.ui.transactions.list.TransactionsViewModel
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureDashboardModule = module {
    factoryOf(::provideDashboardInteractor)
    factoryOf(::provideSettingsInteractor)
    factoryOf(::provideResetWalletInteractor)
    factoryOf(::provideHomeInteractor)
    factoryOf(::provideDocumentsInteractor)
    factoryOf(::provideTransactionInteractor)
    factoryOf(::provideDocumentSignInteractor)
    factoryOf(::provideDocumentDetailsInteractor)
    factoryOf(::provideTransactionHistoryInteractor)
    factoryOf(::provideTransactionDetailsInteractor)

    viewModelOf(::DashboardViewModel)
    viewModelOf(::DocumentSignViewModel)
    viewModel { (documentId: DocumentId) ->
        DocumentDetailsViewModel(get(), get(), get(), get(), documentId)
    }
    viewModelOf(::DocumentsViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ResetWalletViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { (transactionId: String) ->
        DataDeletionRequestViewModel(get(), get(), transactionId)
    }
    viewModel { (transactionId: String) ->
        TransactionDetailsViewModel(get(), get(), transactionId)
    }
    viewModel { (transactionId: String) -> DpaReportViewModel(get(), get(), transactionId) }
    viewModel { (transactionId: String, actionType: String) ->
        TransactionHistoryViewModel(get(), transactionId, actionType)
    }
    viewModelOf(::TransactionsViewModel)
}

fun provideDashboardInteractor(
    resourceProvider: ResourceProvider,
    walletWorkScheduler: WalletWorkScheduler,
    sessionController: SessionController,
): DashboardInteractor = DashboardInteractorImpl(
    resourceProvider,
    walletWorkScheduler,
    sessionController,
)

fun provideSettingsInteractor(
    biometricInteractor: BiometricInteractor,
    configLogic: ConfigLogic,
    logController: LogController,
    resourceProvider: ResourceProvider,
    prefKeys: PrefKeys,
    dcapiSettingProvider: DCAPISettingProvider,
    walletCoreDCAPIRegistrationController: WalletCoreDCAPIRegistrationController,
): SettingsInteractor = SettingsInteractorImpl(
    biometricInteractor,
    configLogic,
    logController,
    resourceProvider,
    prefKeys,
    dcapiSettingProvider,
    walletCoreDCAPIRegistrationController,
)

fun provideResetWalletInteractor(
    walletCoreResetController: WalletCoreResetController,
    sessionController: SessionController,
): ResetWalletInteractor = ResetWalletInteractorImpl(
    walletCoreResetController,
    sessionController,
)

fun provideHomeInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    walletCoreConfig: WalletCoreConfig,
): HomeInteractor = HomeInteractorImpl(
    resourceProvider,
    walletCoreDocumentsController,
    walletCoreConfig,
)

fun provideDocumentsInteractor(
    resourceProvider: ResourceProvider,
    documentsController: WalletCoreDocumentsController,
    filterValidator: FilterValidator,
    configLogic: ConfigLogic,
    prefKeys: PrefKeys,
): DocumentsInteractor =
    DocumentsInteractorImpl(
        resourceProvider,
        documentsController,
        filterValidator,
        configLogic,
        prefKeys,
    )

fun provideTransactionInteractor(
    resourceProvider: ResourceProvider,
    filterValidator: FilterValidator,
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
): TransactionsInteractor = TransactionsInteractorImpl(
    resourceProvider,
    filterValidator,
    walletCoreTransactionLogController
)

fun provideDocumentSignInteractor(
    resourceProvider: ResourceProvider,
): DocumentSignInteractor = DocumentSignInteractorImpl(
    resourceProvider,
)

fun provideDocumentDetailsInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    configLogic: ConfigLogic,
    prefKeys: PrefKeys,
    sessionController: SessionController,
): DocumentDetailsInteractor =
    DocumentDetailsInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uuidProvider,
        configLogic,
        prefKeys,
        sessionController,
    )

fun provideTransactionHistoryInteractor(
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
    resourceProvider: ResourceProvider,
): TransactionHistoryInteractor = TransactionHistoryInteractorImpl(
    walletCoreTransactionLogController,
    resourceProvider,
)

fun provideTransactionDetailsInteractor(
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
    walletCoreTransactionRecordingController: WalletCoreTransactionRecordingController,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
): TransactionDetailsInteractor =
    TransactionDetailsInteractorImpl(
        walletCoreTransactionLogController,
        walletCoreTransactionRecordingController,
        resourceProvider,
        uuidProvider,
    )