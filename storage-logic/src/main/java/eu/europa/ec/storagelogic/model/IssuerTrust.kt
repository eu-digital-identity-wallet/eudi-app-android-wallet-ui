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

package eu.europa.ec.storagelogic.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The issuer-trust verdict Wallet Core returned when a document was issued. Wallet Core reports
 * it only on the issuance event, so the app keeps it here for the document's lifetime.
 */
@Entity(tableName = "issuerTrustRecords")
data class IssuerTrust(
    @PrimaryKey
    val identifier: String,
    val verdict: IssuerTrustVerdict,
)

/**
 * [TRUSTED]: the issuer's certificate chain matched a trust anchor. [NOT_TRUSTED]: the chain was
 * evaluated and did not match one. [NOT_EVALUATED]: no evaluation took place, for example for an
 * attestation type that is not classified.
 */
enum class IssuerTrustVerdict {
    TRUSTED,
    NOT_TRUSTED,
    NOT_EVALUATED,
}