/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.corelogic.service

import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.wallet.EudiWallet
import org.koin.android.ext.android.inject
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService as BaseService

class NfcEngagementService : BaseService() {

    val wallet: EudiWallet by inject()

    override val transferManager: TransferManager
        get() = wallet.transferManager
}