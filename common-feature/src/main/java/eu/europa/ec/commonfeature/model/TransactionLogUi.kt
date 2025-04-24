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

package eu.europa.ec.commonfeature.model

import eu.europa.ec.corelogic.model.TransactionLogData
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

enum class TransactionUiStatus {
    Completed, Failed;

    companion object {
        fun TransactionUiStatus.toUiText(resourceProvider: ResourceProvider): String {
            return when (this) {
                Completed -> resourceProvider.getString(R.string.transactions_filter_item_status_completed)
                Failed -> resourceProvider.getString(R.string.transactions_filter_item_status_failed)
            }
        }
    }
}

enum class TransactionUiType {
    PRESENTATION,
    ISSUANCE,
    SIGNING;
}

fun TransactionLog.Status.toTransactionUiStatus(): TransactionUiStatus {
    return when (this) {
        TransactionLog.Status.Incomplete, TransactionLog.Status.Error -> TransactionUiStatus.Failed
        TransactionLog.Status.Completed -> TransactionUiStatus.Completed
    }
}

fun TransactionLogData.toTransactionUiType(): TransactionUiType {
    return when (this) {
        is TransactionLogData.IssuanceLog -> TransactionUiType.ISSUANCE
        is TransactionLogData.PresentationLog -> TransactionUiType.PRESENTATION
        is TransactionLogData.SigningLog -> TransactionUiType.SIGNING
    }
}