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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.eudi.rqes.core.RqesSigningRecord
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import java.time.Instant

internal fun RqesSigningRecord.toSigningEntries(
    idProvider: () -> String,
    time: Instant,
): List<TransactionEntry.SigningSealing> {
    val result = when (val outcome = outcome) {
        is RqesSigningRecord.Outcome.Completed -> TransactionResult.Completed
        is RqesSigningRecord.Outcome.Failed -> TransactionResult.NotCompleted(outcome.reason)
    }

    val partyName = serviceName?.let { safeServiceName ->
        MultiLangString(
            lang = safeServiceName.languageTag,
            content = safeServiceName.name
        )
    }

    val signingTransactionIdentifier = idProvider()

    return documents
        .ifEmpty {
            listOf(null)
        }
        .map { document ->
            TransactionEntry.SigningSealing(
                transactionIdentifier = idProvider(),
                signingTransactionIdentifier = signingTransactionIdentifier,
                time = time,
                transactionResult = result,
                certificateIdentifier = certificateSerialNumber,
                dtbsr = document?.dtbsr,
                fileName = document?.label,
                fileSize = document?.sizeBytes?.toString(),
                interactingPartyName = partyName,
            )
        }
}