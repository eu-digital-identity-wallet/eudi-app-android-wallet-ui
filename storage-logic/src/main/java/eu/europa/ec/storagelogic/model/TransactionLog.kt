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

package eu.europa.ec.storagelogic.model

import eu.europa.ec.storagelogic.model.type.StoredObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class RealmTransactionLog : RealmObject {
    @PrimaryKey
    var identifier: String = ""
    var value: String = ""
}

data class TransactionLog(
    val identifier: String,
    val value: String
) : StoredObject

internal fun TransactionLog.toRealm() = RealmTransactionLog().apply {
    identifier = this@toRealm.identifier
    value = this@toRealm.value
}

internal fun RealmTransactionLog?.toTransactionLog() = this?.let {
    TransactionLog(
        it.identifier,
        it.value
    )
}

internal fun List<RealmTransactionLog>.toTransactionLogs() = this.map {
    TransactionLog(
        it.identifier,
        it.value
    )
}