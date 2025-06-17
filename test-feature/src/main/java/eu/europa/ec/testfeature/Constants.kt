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

package eu.europa.ec.testfeature

import com.android.identity.document.NameSpacedData
import com.android.identity.securearea.software.SoftwareSecureArea
import com.android.identity.storage.EphemeralStorageEngine
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import java.time.Instant
import java.util.Locale

const val mockedGenericErrorMessage = "resourceProvider's genericErrorMessage"
const val mockedPlainFailureMessage = "failure message"

val mockedExceptionWithMessage = RuntimeException("Exception to test interactor.")
val mockedExceptionWithNoMessage = RuntimeException()

val mockedDefaultLocale: Locale = Locale.ENGLISH

const val mockedOldestDocumentCreationDate = "2000-01-25T14:25:00.073Z"
const val mockedDocumentCreationDate = "2024-01-25T14:25:00.073Z"
const val mockedDocumentValidUntilDate = "2025-05-13T14:25:00.073Z"
const val mockedOldestPidId = "000000"
const val mockedPidId = "000001"
const val mockedMdlId = "000002"
const val mockedPidDocName = "EU PID"
const val mockedMdlDocName = "mDL"
const val mockedBookmarkId = "mockedBookmarkId"
const val mockedVerifierIsTrusted = true
const val mockedNotifyOnAuthenticationFailure = false
val mockedPidFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "birth_date" to byteArrayOf(-39, 3, -20, 106, 49, 57, 56, 53, 45, 48, 51, 45, 51, 48),
    "age_over_18" to byteArrayOf(-11),
    "age_over_15" to byteArrayOf(-11),
    "age_over_21" to byteArrayOf(-11),
    "age_over_60" to byteArrayOf(-12),
    "age_over_65" to byteArrayOf(-12),
    "age_over_68" to byteArrayOf(-12),
    "age_in_years" to byteArrayOf(24, 38),
    "age_birth_year" to byteArrayOf(25, 7, -63),
    "family_name_birth" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name_birth" to byteArrayOf(99, 74, 65, 78),
    "birth_place" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "birth_country" to byteArrayOf(98, 83, 69),
    "birth_state" to byteArrayOf(98, 83, 69),
    "birth_city" to byteArrayOf(107, 75, 65, 84, 82, 73, 78, 69, 72, 79, 76, 77),
    "resident_address" to byteArrayOf(
        111,
        70,
        79,
        82,
        84,
        85,
        78,
        65,
        71,
        65,
        84,
        65,
        78,
        32,
        49,
        53
    ),
    "resident_country" to byteArrayOf(98, 83, 69),
    "resident_state" to byteArrayOf(98, 83, 69),
    "resident_city" to byteArrayOf(107, 75, 65, 84, 82, 73, 78, 69, 72, 79, 76, 77),
    "resident_postal_code" to byteArrayOf(101, 54, 52, 49, 51, 51),
    "resident_street" to byteArrayOf(108, 70, 79, 82, 84, 85, 78, 65, 71, 65, 84, 65, 78),
    "resident_house_number" to byteArrayOf(98, 49, 50),
    "gender" to byteArrayOf(1),
    "nationality" to byteArrayOf(98, 83, 69),
    "issuance_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        48,
        57,
        45,
        48,
        49,
        45,
        48,
        49,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "issuing_authority" to byteArrayOf(99, 85, 84, 79),
    "document_number" to byteArrayOf(105, 49, 49, 49, 49, 49, 49, 49, 49, 52),
    "administrative_number" to byteArrayOf(106, 57, 48, 49, 48, 49, 54, 55, 52, 54, 52),
    "issuing_country" to byteArrayOf(98, 83, 69),
    "issuing_jurisdiction" to byteArrayOf(100, 83, 69, 45, 73),
)
val mockedMdlFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "birth_date" to byteArrayOf(-39, 3, -20, 106, 49, 57, 56, 53, 45, 48, 51, 45, 51, 48),
    "issue_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        48,
        57,
        45,
        48,
        49,
        45,
        48,
        49,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "issuing_country" to byteArrayOf(98, 83, 69),
    "issuing_authority" to byteArrayOf(99, 85, 84, 79),
    "document_number" to byteArrayOf(105, 49, 49, 49, 49, 49, 49, 49, 49, 52),
    "portrait" to byteArrayOf(98, 83, 69),
    "un_distinguishing_sign" to byteArrayOf(97, 83),
    "administrative_number" to byteArrayOf(106, 57, 48, 49, 48, 49, 54, 55, 52, 54, 52),
    "sex" to byteArrayOf(1),
    "height" to byteArrayOf(24, -76),
    "weight" to byteArrayOf(24, 91),
    "eye_colour" to byteArrayOf(101, 98, 108, 97, 99, 107),
    "hair_colour" to byteArrayOf(101, 98, 108, 97, 99, 107),
    "birth_place" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "resident_address" to byteArrayOf(
        111,
        70,
        79,
        82,
        84,
        85,
        78,
        65,
        71,
        65,
        84,
        65,
        78,
        32,
        49,
        53
    ),
    "portrait_capture_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        50,
        51,
        45,
        48,
        51,
        45,
        50,
        51,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "signature_usual_mark" to byteArrayOf(98, 83, 69),
    "age_in_years" to byteArrayOf(24, 38),
    "age_birth_year" to byteArrayOf(25, 7, -63),
    "issuing_jurisdiction" to byteArrayOf(100, 83, 69, 45, 73),
    "nationality" to byteArrayOf(98, 83, 69),
    "resident_city" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "resident_state" to byteArrayOf(98, 83, 69),
    "resident_postal_code" to byteArrayOf(101, 54, 52, 49, 51, 51),
    "resident_country" to byteArrayOf(98, 83, 69),
    "family_name_national_character" to byteArrayOf(
        105,
        65,
        78,
        68,
        69,
        82,
        83,
        83,
        79,
        78
    ),
    "given_name_national_character" to byteArrayOf(99, 74, 65, 78),
    "age_over_15" to byteArrayOf(-11),
    "age_over_18" to byteArrayOf(-11),
    "age_over_21" to byteArrayOf(-11),
    "age_over_60" to byteArrayOf(-12),
    "age_over_65" to byteArrayOf(-12),
    "age_over_68" to byteArrayOf(-12),
)

val mockedPidBasicFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "age_over_18" to byteArrayOf(-11),
    "age_over_65" to byteArrayOf(-12),
    "age_birth_year" to byteArrayOf(25, 7, -63),
    "birth_city" to byteArrayOf(107, 75, 65, 84, 82, 73, 78, 69, 72, 79, 76, 77),
    "gender" to byteArrayOf(1),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
)

val mockedMdlBasicFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "birth_place" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "portrait" to byteArrayOf(98, 83, 69),

    "signature_usual_mark" to byteArrayOf(98, 83, 69),
    "sex" to byteArrayOf(1),
)

const val mockedPidDocType = "eu.europa.ec.eudi.pid.1"
const val mockedPidNameSpace = "eu.europa.ec.eudi.pid.1"
const val mockedMdlDocType = "org.iso.18013.5.1.mDL"
const val mockedMdlNameSpace = "org.iso.18013.5.1"

val secureArea = SoftwareSecureArea(EphemeralStorageEngine())

fun createMockedNamespaceData(
    documentNamespace: String,
    nameSpacedData: Map<String, ByteArray>,
): NameSpacedData {
    val builder = NameSpacedData.Builder()
    nameSpacedData.forEach {
        builder.putEntry(documentNamespace, it.key, it.value)
    }
    return builder.build()
}

val mockedFullPid = IssuedDocument(
    id = mockedPidId,
    name = mockedPidDocName,
    documentManagerId = "fabulas",
    isCertified = false,
    keyAlias = "massa",
    secureArea = secureArea,
    createdAt = Instant.parse(mockedDocumentCreationDate),
    issuedAt = Instant.parse(mockedDocumentCreationDate),
    validFrom = Instant.now(),
    validUntil = Instant.parse(mockedDocumentValidUntilDate),
    issuerProvidedData = byteArrayOf(),
    data = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        metadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            mockedPidFields
        )
    )
)

val mockedUnsignedPid = UnsignedDocument(
    id = mockedPidId,
    name = mockedPidDocName,
    createdAt = Instant.parse(mockedDocumentCreationDate),
    format = MsoMdocFormat(mockedPidDocType),
    documentManagerId = "viderer",
    isCertified = false,
    keyAlias = "movet",
    secureArea = secureArea,
    metadata = null
)

val mockedMainPid = mockedFullPid

val mockedPidWithBasicFields = mockedFullPid.copy(
    data = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        metadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            mockedPidBasicFields
        )
    )
)

val mockedOldestPidWithBasicFields = mockedPidWithBasicFields.copy(
    id = mockedOldestPidId,
    createdAt = Instant.parse(mockedOldestDocumentCreationDate)
)

val mockedEmptyPid = mockedFullPid.copy(
    data = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        metadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            emptyMap()
        )
    )
)

val mockedFullMdl = IssuedDocument(
    id = mockedMdlId,
    name = mockedMdlDocName,
    documentManagerId = "fabulas",
    isCertified = false,
    keyAlias = "massa",
    secureArea = secureArea,
    createdAt = Instant.parse(mockedDocumentCreationDate),
    issuedAt = Instant.parse(mockedDocumentCreationDate),
    validFrom = Instant.now(),
    validUntil = Instant.parse(mockedDocumentValidUntilDate),
    issuerProvidedData = byteArrayOf(),
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        metadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlFields
        )
    )
)

val mockedMdlWithBasicFields = mockedFullMdl.copy(
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        metadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlBasicFields
        )
    )
)

val mockedMdlWithNoExpirationDate: IssuedDocument = mockedFullMdl.copy(
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        metadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlFields
                .minus("expiry_date"),
        )
    )
)

val mockedMdlWithNoUserNameAndNoUserImage: IssuedDocument = mockedFullMdl.copy(
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        metadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlFields
                .minus("given_name")
                .minus("portrait")
        )
    )
)

val mockedFullDocuments: List<IssuedDocument> = listOf(
    mockedFullPid, mockedFullMdl
)