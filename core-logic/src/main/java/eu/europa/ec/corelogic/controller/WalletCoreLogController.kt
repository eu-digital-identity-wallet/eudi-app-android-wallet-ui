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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.eudi.wallet.logging.Logger

interface WalletCoreLogController : Logger

/**
 * Forwards Wallet Core's log records to [logController], dropping those above [logLevel], one of
 * the `Logger` levels. Wallet Core applies `EudiWalletConfig.logLevel` only in its own logger, so a
 * logger supplied with `withLogger` receives every record, including debug records that hold HTTP
 * bodies with tokens and credentials. Pass the configured `logLevel`.
 */
class WalletCoreLogControllerImpl(
    private val logController: LogController,
    private val logLevel: Int,
) : WalletCoreLogController {

    override fun log(record: Logger.Record) {
        if (record.level > logLevel) return
        when (record.level) {
            Logger.LEVEL_ERROR -> record.thrown?.let { logController.e(it) }
                ?: logController.e { record.message }

            Logger.LEVEL_INFO -> logController.i { record.message }
            Logger.LEVEL_DEBUG -> logController.d { record.message }
        }
    }
}