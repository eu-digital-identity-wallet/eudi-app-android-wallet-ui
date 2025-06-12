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

import androidx.annotation.VisibleForTesting
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.mockito.kotlin.whenever

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
object MockResourceProviderForStringCalls {

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

    /**
     * Mock the call of [eu.europa.ec.commonfeature.ui.document_details.transformer.DocumentDetailsTransformer.transformToUiItem]
     */
    fun mockTransformToUiItemCall(
        resourceProvider: ResourceProvider,
        availableCredentials: Int = mockedDocumentAvailableCredentials,
        totalCredentials: Int = mockedDocumentTotalCredentials,
    ) {
        val mockedStrings = listOf(
            R.string.document_details_document_credentials_info_more_info_text to "More info",
            R.string.document_details_document_credentials_info_expanded_text_subtitle to "For security reasons, this document can be shared a limited number of times before it needs to be re-issued by the issuing authority.",
            R.string.document_details_document_credentials_info_expanded_button_hide_text to "Hide",
        )
        mockResourceProviderStrings(resourceProvider, mockedStrings)
        whenever(
            resourceProvider.getString(
                R.string.document_details_document_credentials_info_text,
                availableCredentials,
                totalCredentials
            )
        ).thenReturn("$availableCredentials/$totalCredentials instances remaining")

        mockTransformToDocumentDetailsUiCall(resourceProvider)
    }

    /**
     * Mock the call of [eu.europa.ec.commonfeature.ui.document_details.transformer.transformToDocumentDetailsUi]
     */
    fun mockTransformToDocumentDetailsUiCall(resourceProvider: ResourceProvider) {
        mockGetKeyValueUiCall(resourceProvider)
    }

    /**
     * Mock the call of [eu.europa.ec.commonfeature.util.getKeyValueUi]
     */
    fun mockGetKeyValueUiCall(resourceProvider: ResourceProvider) {
        val mockedStrings = listOf(
            R.string.document_details_boolean_item_true_readable_value to "yes",
            R.string.document_details_boolean_item_false_readable_value to "no",
        )

        mockResourceProviderStrings(resourceProvider, mockedStrings)
        mockGetGenderValueCall(resourceProvider)
    }

    /**
     * Mock the call of [eu.europa.ec.commonfeature.util.getGenderValue]
     */
    fun mockGetGenderValueCall(resourceProvider: ResourceProvider) {
        val mockedStrings = listOf(
            R.string.request_gender_male to "Male",
            R.string.request_gender_female to "Female",
            R.string.request_gender_not_known to "Not known",
            R.string.request_gender_not_applicable to "Not applicable",
        )

        mockResourceProviderStrings(resourceProvider, mockedStrings)
    }

    /**
     * Mock the call of [eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer.transformToUiItems]
     */
    fun mockTransformToUiItemsCall(
        resourceProvider: ResourceProvider,
        notAvailableString: String
    ) {
        whenever(resourceProvider.getString(R.string.request_element_identifier_not_available))
            .thenReturn(notAvailableString)

        mockGetKeyValueUiCall(resourceProvider)

        whenever(resourceProvider.getLocale())
            .thenReturn(mockedDefaultLocale)
    }

    fun mockIssuerName(
        resourceProvider: ResourceProvider,
        name: String
    ) {
        whenever(resourceProvider.getString(R.string.issuance_success_header_issuer_default_name))
            .thenReturn(name)
    }

    fun mockGetUiItemsCall(
        resourceProvider: ResourceProvider,
        supportingText: String,
    ) {
        whenever(resourceProvider.getString(R.string.document_success_collapsed_supporting_text))
            .thenReturn(supportingText)
    }
}