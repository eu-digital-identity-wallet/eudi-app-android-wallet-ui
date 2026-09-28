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

package eu.europa.ec.testfeature.util

import androidx.annotation.VisibleForTesting
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@VisibleForTesting(otherwise = VisibleForTesting.Companion.NONE)
object StringResourceProviderMocker {

    /**
     * Mocks ResourceProvider.getString(...) for each (resId → returnValue) pair.
     */
    fun mockResourceProviderStrings(
        resourceProvider: ResourceProvider,
        pairs: List<Pair<Int, String>>,
    ) {
        pairs.forEach { (resId, returnValue) ->
            whenever(resourceProvider.getString(resId)).thenReturn(returnValue)
        }
    }

    fun mockGetDocumentDetailsStrings(
        resourceProvider: ResourceProvider,
        availableCredentials: Int,
        totalCredentials: Int,
    ) {
        mockCreateDocumentCredentialsInfoStrings(
            resourceProvider = resourceProvider,
            availableCredentials = availableCredentials,
            totalCredentials = totalCredentials
        )

        mockTransformToDocumentDetailsDomainStrings(resourceProvider)
    }

    fun mockCreateDocumentCredentialsInfoStrings(
        resourceProvider: ResourceProvider,
        availableCredentials: Int,
        totalCredentials: Int,
    ) {
        whenever(
            resourceProvider.getString(
                R.string.document_details_document_credentials_info_text,
                availableCredentials,
                totalCredentials
            )
        ).thenReturn("$availableCredentials/$totalCredentials instances remaining")
    }

    fun mockTransformToDocumentDetailsDomainStrings(resourceProvider: ResourceProvider) {
        mockCreateKeyValueStrings(resourceProvider)
    }

    fun mockCreateKeyValueStrings(resourceProvider: ResourceProvider) {
        val mockedStrings = listOf(
            R.string.document_details_boolean_item_true_readable_value to "yes",
            R.string.document_details_boolean_item_false_readable_value to "no",
        )

        mockResourceProviderStrings(resourceProvider, mockedStrings)
        mockGetGenderValueStrings(resourceProvider)
    }

    fun mockGetGenderValueStrings(resourceProvider: ResourceProvider) {
        val mockedStrings = listOf(
            R.string.request_gender_male to "Male",
            R.string.request_gender_female to "Female",
            R.string.request_gender_not_known to "Not known",
            R.string.request_gender_not_applicable to "Not applicable",
        )

        mockResourceProviderStrings(resourceProvider, mockedStrings)
    }

    fun mockTransformToUiItemsStrings(
        resourceProvider: ResourceProvider,
    ) {
        mockCreateKeyValueStrings(resourceProvider)

        whenever(resourceProvider.getString(R.string.request_collapsed_supporting_text))
            .thenReturn(mockedRequestCollapsedSupportingText)

        whenever(resourceProvider.getLocale())
            .thenReturn(mockedDefaultLocale)
    }

    fun mockTransactionDataStrings(resourceProvider: ResourceProvider) {
        mockResourceProviderStrings(
            resourceProvider = resourceProvider,
            pairs = listOf(
                R.string.request_transaction_section_title to "Data to be signed",
                R.string.request_transaction_details_title to "Signature details",
                R.string.request_transaction_trust_framework to "Trust framework",
                R.string.request_transaction_trust_framework_value to "eIDAS",
                R.string.request_transaction_type to "Transaction type",
                R.string.request_transaction_signature_type to "Signature type",
                R.string.request_transaction_requested_credentials to "Requested credentials",
                R.string.request_transaction_signing_credential_id to "Signing credential ID",
                R.string.request_transaction_signature_count to "Number of signatures",
                R.string.request_transaction_document to "Document",
                R.string.request_transaction_document_location to "Document location",
                R.string.request_transaction_open_document to "Open document",
                R.string.request_transaction_expected_checksum to "Expected document checksum",
                R.string.request_transaction_checksum_algorithm to "Checksum algorithm",
                R.string.request_transaction_hash_representation to "Hash representation",
                R.string.request_transaction_dtbsr_hash to "DTBSR hash",
                R.string.request_transaction_dtbsr_algorithm to "DTBSR hash algorithm",
                R.string.request_transaction_sdr_hash to "SDR hash",
                R.string.request_transaction_sdr_algorithm to "SDR hash algorithm",
                R.string.request_transaction_sodr_hash to "SODR hash",
                R.string.request_transaction_sodr_algorithm to "SODR hash algorithm",
                R.string.request_transaction_document_hash to "Document hash",
                R.string.request_transaction_document_hash_algorithm to "Document hash algorithm",
                R.string.request_transaction_signature_format to "Signature format",
                R.string.request_transaction_conformance_level to "Conformance level",
                R.string.request_transaction_signed_attributes to "Signed attributes",
                R.string.request_transaction_otp to "One-time password (OTP)",
                R.string.request_transaction_response_uri to "Response URI",
                R.string.request_transaction_unavailable to "Details for this transaction are unavailable.",
                R.string.request_transaction_qes to "Qualified electronic signature (QES)",
                R.string.request_transaction_qeseal to "Qualified electronic seal",
                R.string.request_transaction_aes to "Advanced electronic signature",
                R.string.request_transaction_aeseal to "Advanced electronic seal",
                R.string.request_transaction_aesqc to "Advanced electronic signature with a qualified certificate",
                R.string.request_transaction_aesealqc to "Advanced electronic seal with a qualified certificate",
            ),
        )
        whenever(resourceProvider.getString(R.string.request_collapsed_supporting_text))
            .thenReturn(mockedRequestCollapsedSupportingText)
        whenever(resourceProvider.getString(eq(R.string.request_transaction_numbered), any()))
            .thenAnswer { invocation -> "Transaction ${invocation.getArgument<Any>(1)}" }
        whenever(resourceProvider.getString(eq(R.string.request_transaction_document_numbered), any()))
            .thenAnswer { invocation -> "Document ${invocation.getArgument<Any>(1)}" }
    }

    fun mockIssuerName(
        resourceProvider: ResourceProvider,
        name: String
    ) {
        whenever(resourceProvider.getString(R.string.issuance_success_header_issuer_default_name))
            .thenReturn(name)
    }

    fun mockGetUiItemsStrings(
        resourceProvider: ResourceProvider,
        supportingText: String,
    ) {
        whenever(resourceProvider.getString(R.string.document_success_collapsed_supporting_text))
            .thenReturn(supportingText)
    }
}