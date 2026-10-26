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

package eu.europa.ec.corelogic.di

import android.content.Context
import androidx.credentials.registry.provider.ClearCredentialRegistryRequest
import androidx.credentials.registry.provider.RegistryManager
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.di.LogicBusinessModule
import eu.europa.ec.businesslogic.extension.deleteContents
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.config.WalletCoreConfigImpl
import eu.europa.ec.corelogic.controller.WalletCoreDCAPIRegistrationController
import eu.europa.ec.corelogic.controller.WalletCoreDCAPIRegistrationControllerImpl
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsControllerImpl
import eu.europa.ec.corelogic.controller.WalletCoreLogController
import eu.europa.ec.corelogic.controller.WalletCoreLogControllerImpl
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.controller.WalletCorePresentationControllerImpl
import eu.europa.ec.corelogic.controller.WalletCoreResetController
import eu.europa.ec.corelogic.controller.WalletCoreResetControllerImpl
import eu.europa.ec.corelogic.controller.WalletCoreTransactionLogController
import eu.europa.ec.corelogic.controller.WalletCoreTransactionLogControllerImpl
import eu.europa.ec.corelogic.controller.WalletCoreTransactionRecordingController
import eu.europa.ec.corelogic.controller.WalletCoreTransactionRecordingControllerImpl
import eu.europa.ec.corelogic.controller.WalletCoreTrustMarkController
import eu.europa.ec.corelogic.controller.WalletCoreTrustMarkControllerImpl
import eu.europa.ec.corelogic.controller.WalletResetStatus
import eu.europa.ec.corelogic.provider.DCAPISettingProvider
import eu.europa.ec.corelogic.provider.DCAPISettingProviderImpl
import eu.europa.ec.corelogic.provider.RegistrationCheckProvider
import eu.europa.ec.corelogic.provider.RegistrationCheckProviderImpl
import eu.europa.ec.corelogic.provider.WalletCoreAttestationProvider
import eu.europa.ec.corelogic.provider.WalletCoreAttestationProviderImpl
import eu.europa.ec.corelogic.util.DocumentManagerWithoutExcluded
import eu.europa.ec.corelogic.worker.ReIssuanceWorkManager
import eu.europa.ec.corelogic.worker.RevocationWorkManager
import eu.europa.ec.corelogic.worker.WalletWorkScheduler
import eu.europa.ec.corelogic.worker.WalletWorkSchedulerImpl
import eu.europa.ec.eudi.rqes.core.RqesSigningLogger
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.dcapi.registration.DefaultDCAPIRegistration
import eu.europa.ec.eudi.wallet.transactionLogging.DefaultTransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.networklogic.repository.WalletAttestationRepository
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.dao.IssuerTrustDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import eu.europa.ec.storagelogic.service.DatabaseEraser
import io.ktor.client.HttpClient
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

val logicCoreModule = module {
    workerOf(::ReIssuanceWorkManager)
    workerOf(::RevocationWorkManager)

    singleOf(::provideWalletCoreDCAPIRegistrationController)
    factoryOf(::provideWalletCoreTrustMarkController)
    singleOf(::provideWalletCoreConfig)
    factoryOf(::provideWalletCoreLogController)
    // Single, not a factory: the controller owns the queue that keeps transaction writes in order,
    // so a second instance would be a second queue writing the same rows.
    singleOf(::provideWalletCoreTransactionLogController)
    singleOf(::provideWalletCoreTransactionLogManager)
    singleOf(::provideWalletCoreTransactionRecordingController) { bind<RqesSigningLogger>() }
    factoryOf(::provideWalletCoreAttestationProvider)
    factoryOf(::provideRegistrationCheckProvider)
    factoryOf(::provideDCAPISettingProvider)
    factoryOf(::provideWalletCoreDocumentsController)
    factoryOf(::provideWalletCoreResetController)
    singleOf(::provideWalletResetStatus)
    singleOf(::provideWalletWorkScheduler)

    scope<WalletCoreScope> {
        scopedOf(::provideEudiWallet)
    }
    scope<WalletPresentationScope> {
        scopedOf(::provideWalletCorePresentationController)
    }
}

fun provideEudiWallet(
    context: Context,
    walletCoreConfig: WalletCoreConfig,
    walletCoreLogController: WalletCoreLogController,
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
    walletCoreAttestationProvider: WalletCoreAttestationProvider,
    httpClient: HttpClient,
    walletCoreDCAPIRegistrationController: WalletCoreDCAPIRegistrationController,
): EudiWallet {
    val eudiWallet = EudiWallet(
        context = context,
        config = walletCoreConfig.config,
        walletProvider = walletCoreAttestationProvider,
        trustMarkSource = walletCoreConfig.trustMarkSource,
    ) {
        withLogger(walletCoreLogController)
        withTransactionLogger(walletCoreTransactionLogController)
        withKtorHttpClientFactory { httpClient }
        withDCAPIRegistration(walletCoreDCAPIRegistrationController)
    }
    eudiWallet.addTransferEventListener(walletCoreTransactionLogController)
    walletCoreDCAPIRegistrationController.attach {
        DefaultDCAPIRegistration(
            context = context,
            documentManager = DocumentManagerWithoutExcluded(eudiWallet) {
                walletCoreDCAPIRegistrationController.excludedDocumentIds
            },
            supportedProtocols = walletCoreConfig.config.dcapiConfig?.supportedProtocols?.toList()
                .orEmpty(),
            logger = walletCoreLogController,
        )
    }
    return eudiWallet
}

fun provideWalletCoreDCAPIRegistrationController(
    context: Context,
    dcapiSettingProvider: DCAPISettingProvider,
    revokedDocumentDao: RevokedDocumentDao,
): WalletCoreDCAPIRegistrationController = WalletCoreDCAPIRegistrationControllerImpl(
    clearRegistry = {
        RegistryManager.create(context).clearCredentialRegistry(
            ClearCredentialRegistryRequest(isDeleteAll = true)
        )
    },
    isEnabled = { dcapiSettingProvider.isAvailable && dcapiSettingProvider.isEnabled() },
    getBlockedDocumentIds = { revokedDocumentDao.retrieveAll().map { it.identifier }.toSet() },
)

fun provideWalletCoreTrustMarkController(
    prefKeys: PrefKeys,
    resourceProvider: ResourceProvider,
): WalletCoreTrustMarkController = WalletCoreTrustMarkControllerImpl(
    prefKeys = prefKeys,
    resourceProvider = resourceProvider,
)

fun provideWalletCoreConfig(
    registrationCheckProvider: RegistrationCheckProvider
): WalletCoreConfig = WalletCoreConfigImpl(
    registrationCheckProvider = registrationCheckProvider
)

fun provideWalletCoreLogController(
    logController: LogController,
    walletCoreConfig: WalletCoreConfig,
): WalletCoreLogController = WalletCoreLogControllerImpl(
    logController = logController,
    logLevel = walletCoreConfig.config.logLevel,
)

fun provideWalletCoreTransactionLogController(
    transactionLogDao: TransactionLogDao,
    resourceProvider: ResourceProvider,
    walletCoreConfig: WalletCoreConfig,
): WalletCoreTransactionLogController = WalletCoreTransactionLogControllerImpl(
    transactionLogDao = transactionLogDao,
    resourceProvider = resourceProvider,
    transactionDataTypes = walletCoreConfig.config.openId4VpConfig?.transactionDataTypes.orEmpty(),
)

fun provideWalletCoreTransactionLogManager(
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
): TransactionLogManager = DefaultTransactionLogManager(
    storage = walletCoreTransactionLogController,
)

fun provideWalletCoreTransactionRecordingController(
    uuidProvider: UuidProvider,
    transactionLogManager: TransactionLogManager,
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
    resourceProvider: ResourceProvider,
): WalletCoreTransactionRecordingController = WalletCoreTransactionRecordingControllerImpl(
    uuidProvider = uuidProvider,
    transactionLogManager = transactionLogManager,
    walletCoreTransactionLogController = walletCoreTransactionLogController,
    resourceProvider = resourceProvider,
)

fun provideWalletCoreAttestationProvider(
    walletAttestationRepository: WalletAttestationRepository,
    walletCoreConfig: WalletCoreConfig
): WalletCoreAttestationProvider =
    WalletCoreAttestationProviderImpl(
        walletCoreConfig = walletCoreConfig,
        walletAttestationRepository = walletAttestationRepository
    )

fun provideRegistrationCheckProvider(
    prefKeys: PrefKeys
): RegistrationCheckProvider = RegistrationCheckProviderImpl(
    prefKeys = prefKeys
)

fun provideDCAPISettingProvider(
    prefKeys: PrefKeys,
    walletCoreConfig: WalletCoreConfig,
): DCAPISettingProvider = DCAPISettingProviderImpl(
    prefKeys = prefKeys,
    walletCoreConfig = walletCoreConfig,
)

fun provideWalletCoreDocumentsController(
    resourceProvider: ResourceProvider,
    walletCoreConfig: WalletCoreConfig,
    bookmarkDao: BookmarkDao,
    revokedDocumentDao: RevokedDocumentDao,
    failedReIssuedDocumentDao: FailedReIssuedDocumentDao,
    issuerTrustDao: IssuerTrustDao,
    statusRefreshFailureDao: StatusRefreshFailureDao,
    prefKeys: PrefKeys,
    walletWorkScheduler: WalletWorkScheduler,
): WalletCoreDocumentsController =
    WalletCoreDocumentsControllerImpl(
        resourceProvider = resourceProvider,
        walletCoreConfig = walletCoreConfig,
        bookmarkDao = bookmarkDao,
        revokedDocumentDao = revokedDocumentDao,
        failedReIssuedDocumentDao = failedReIssuedDocumentDao,
        issuerTrustDao = issuerTrustDao,
        statusRefreshFailureDao = statusRefreshFailureDao,
        prefKeys = prefKeys,
        walletWorkScheduler = walletWorkScheduler,
    )

fun provideWalletCoreResetController(
    context: Context,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    walletCoreDCAPIRegistrationController: WalletCoreDCAPIRegistrationController,
    prefKeys: PrefKeys,
    sessionController: SessionController,
    resourceProvider: ResourceProvider,
    walletResetStatus: WalletResetStatus,
    walletWorkScheduler: WalletWorkScheduler,
    databaseEraser: DatabaseEraser,
    logController: LogController,
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
): WalletCoreResetController = WalletCoreResetControllerImpl(
    walletCoreDocumentsController = walletCoreDocumentsController,
    walletCoreDCAPIRegistrationController = walletCoreDCAPIRegistrationController,
    prefKeys = prefKeys,
    sessionController = sessionController,
    resourceProvider = resourceProvider,
    resetStatus = walletResetStatus,
    stopBackgroundWork = { walletWorkScheduler.stop() },
    scheduleBackgroundWork = { walletWorkScheduler.schedule() },
    clearTransactionLogs = { walletCoreTransactionLogController.deleteAllTransactionLogs() },
    clearDatabase = { databaseEraser.clearAllTables() },
    deleteLogFiles = { logController.deleteLogFiles() },
    deleteCacheFiles = { context.cacheDir.deleteContents(recursively = true) },
)

fun provideWalletResetStatus(): WalletResetStatus = WalletResetStatus()

fun provideWalletWorkScheduler(
    context: Context,
    walletCoreConfig: WalletCoreConfig,
    resetStatus: WalletResetStatus,
): WalletWorkScheduler = WalletWorkSchedulerImpl(
    context = context,
    walletCoreConfig = walletCoreConfig,
    resetStatus = resetStatus,
)

fun provideWalletCorePresentationController(
    resourceProvider: ResourceProvider,
    prefKeys: PrefKeys,
): WalletCorePresentationController =
    WalletCorePresentationControllerImpl(
        resourceProvider = resourceProvider,
        prefKeys = prefKeys,
    )

/**
 * Koin scope that lives for all the document presentation flow. It is manually handled from the
 * ViewModels that start and participate on the presentation process
 * */
class WalletPresentationScope

/**
 * Koin scope that defines the lifecycle for core wallet components.
 * This scope is used to manage the EUDI Wallet instance and its related
 * dependencies, ensuring they are preserved across the core logic operations.
 */
class WalletCoreScope

/**
 * Get Koin scope that lives during document presentation flow
 * */
inline fun <reified T : Any> getOrCreateKoinScope(scopeId: String): org.koin.core.scope.Scope =
    KoinPlatform.getKoin().getOrCreateScope<T>(scopeId)

/**
 * Retrieves an existing Koin scope by its identifier.
 *
 * @param scopeId The unique identifier of the scope to retrieve.
 * @return The [org.koin.core.scope.Scope] instance if it exists, or null if no scope with the given ID is found
 * or Koin has not been started.
 */
fun getOrNullKoinScope(scopeId: String): org.koin.core.scope.Scope? =
    KoinPlatform.getKoinOrNull()?.getScopeOrNull(scopeId)