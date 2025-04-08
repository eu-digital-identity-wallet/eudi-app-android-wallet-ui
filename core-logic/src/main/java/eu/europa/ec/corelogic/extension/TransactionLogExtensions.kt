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

package eu.europa.ec.corelogic.extension

import com.nimbusds.jose.shaded.gson.Gson
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentationTransactionLog
import eu.europa.ec.storagelogic.model.TransactionLog as StorageTransaction

internal fun StorageTransaction.toCoreTransactionLog(): TransactionLog? = try {
    Gson().fromJson(
        this.value,
        TransactionLog::class.java
    )
} catch (_: Exception) {
    null
}

// TODO RETURN PROPER OBJECTS ONCE READY FROM CORE ISSUANCE,SIGNING
@Throws(IllegalArgumentException::class)
internal fun TransactionLog.parseTransactionLog(): Any? =
    when (this.type) {
        TransactionLog.Type.Presentation ->
            PresentationTransactionLog.fromTransactionLog(this)
                .getOrNull()

        TransactionLog.Type.Issuance ->
            throw IllegalArgumentException("UnSupported transaction log type")

        TransactionLog.Type.Signing ->
            throw IllegalArgumentException("UnSupported transaction log type")
    }