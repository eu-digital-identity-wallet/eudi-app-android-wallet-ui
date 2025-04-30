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

import eu.europa.ec.businesslogic.util.toLocalDate
import eu.europa.ec.businesslogic.util.toLocalDateTime
import eu.europa.ec.corelogic.model.TransactionLogData
import eu.europa.ec.corelogic.model.TransactionLogData.PresentationLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentationTransactionLog

// TODO RETURN PROPER OBJECTS ONCE READY FROM CORE ISSUANCE,SIGNING
@Throws(IllegalArgumentException::class)
internal fun Any.toTransactionLogData(id: String): TransactionLogData = when (this) {
    is PresentationTransactionLog -> PresentationLog(
        id = id,
        name = relyingParty.name,
        status = status,
        creationLocalDateTime = timestamp.toLocalDateTime(),
        creationLocalDate = timestamp.toLocalDate(),
        relyingParty = relyingParty,
        documents = documents,
    )

    else -> throw IllegalArgumentException("Unknown transaction log type")
}