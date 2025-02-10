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
     * Mock the call of [eu.europa.ec.commonfeature.ui.document_details.transformer.DocumentDetailsTransformer.transformToUiItem]
     */
    fun mockTransformToUiItemCall(resourceProvider: ResourceProvider) {
        mockTransformToDocumentDetailsUiCall(resourceProvider)
    }

    /**
     * Mock the call of [eu.europa.ec.commonfeature.ui.document_details.transformer.transformToDocumentDetailsUi]
     */
    fun mockTransformToDocumentDetailsUiCall(resourceProvider: ResourceProvider) {
        whenever(resourceProvider.getString(R.string.document_details_portrait_readable_identifier))
            .thenReturn("Shown above")

        mockGetKeyValueUiCall(resourceProvider)
    }

    /**
     * Mock the call of [eu.europa.ec.commonfeature.util.getKeyValueUi]
     */
    fun mockGetKeyValueUiCall(resourceProvider: ResourceProvider) {

        whenever(resourceProvider.getString(R.string.document_details_boolean_item_true_readable_value))
            .thenReturn("yes")
        whenever(resourceProvider.getString(R.string.document_details_boolean_item_false_readable_value))
            .thenReturn("no")

        mockGetGenderValueCall(resourceProvider)
    }

    /**
     * Mock the call of [eu.europa.ec.commonfeature.util.getGenderValue]
     */
    fun mockGetGenderValueCall(resourceProvider: ResourceProvider) {
        whenever(resourceProvider.getString(R.string.request_gender_male))
            .thenReturn("Male")
        whenever(resourceProvider.getString(R.string.request_gender_female))
            .thenReturn("Female")
        whenever(resourceProvider.getString(R.string.request_gender_not_known))
            .thenReturn("Not known")
        whenever(resourceProvider.getString(R.string.request_gender_not_applicable))
            .thenReturn("Not applicable")
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
}