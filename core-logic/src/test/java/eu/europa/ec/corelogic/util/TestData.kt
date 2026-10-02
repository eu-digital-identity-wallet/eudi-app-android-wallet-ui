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

package eu.europa.ec.corelogic.util

import eu.europa.ec.corelogic.model.ClaimPathSegment
import java.time.Instant
import java.util.Locale

internal const val mockedTransactionId = "transaction-id"
internal val mockedTransactionTime: Instant = Instant.parse("2026-09-08T12:30:00Z")
internal val mockedEnglishLocale: Locale = Locale.ENGLISH
internal val mockedGreekLocale: Locale = Locale.forLanguageTag("el-GR")
internal const val mockedCredentialType = "eu.europa.ec.eudi.pid.1"
internal const val mockedClaimName = "family_name"
internal val mockedClaimSegments = listOf(
    ClaimPathSegment.Key(mockedCredentialType),
    ClaimPathSegment.Key(mockedClaimName),
)