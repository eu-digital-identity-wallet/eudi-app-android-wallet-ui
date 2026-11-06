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
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class TestWalletCoreLogController {

    private val logController: LogController = mock()

    private fun record(level: Int) = Logger.Record(level = level, message = "record")

    @Test
    fun `Given the release level, When Wallet Core logs a debug record, Then it is dropped`() {
        WalletCoreLogControllerImpl(
            logController,
            Logger.LEVEL_INFO
        ).log(record(Logger.LEVEL_DEBUG))

        verify(logController, never()).d(any<() -> String>())
    }

    @Test
    fun `Given the release level, When Wallet Core logs info and errors, Then they are forwarded`() {
        val controller = WalletCoreLogControllerImpl(logController, Logger.LEVEL_INFO)

        controller.log(record(Logger.LEVEL_INFO))
        controller.log(record(Logger.LEVEL_ERROR))

        verify(logController).i(any<() -> String>())
        verify(logController).e(any<() -> String>())
    }

    @Test
    fun `Given the debug level, When Wallet Core logs a debug record, Then it is forwarded`() {
        WalletCoreLogControllerImpl(
            logController,
            Logger.LEVEL_DEBUG
        ).log(record(Logger.LEVEL_DEBUG))

        verify(logController).d(any<() -> String>())
    }

    @Test
    fun `Given logging is off, When Wallet Core logs an error, Then it is dropped`() {
        WalletCoreLogControllerImpl(logController, Logger.OFF).log(record(Logger.LEVEL_ERROR))

        verify(logController, never()).e(any<() -> String>())
    }
}