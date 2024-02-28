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

private const val mockedDocUiNamePid = "National ID"
private const val mockedDocUiNameMdl = "Driving License"
private const val mockedDocUiNameConferenceBadge = "EUDI Conference Badge"
private const val mockedDocUiNameSampleData = "Load Sample Documents"

/**
 * Mock the call of [eu.europa.ec.commonfeature.model.toUiName]
 */
@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun mockDocumentTypeUiToUiNameCall(resourceProvider: ResourceProvider) {
    whenever(resourceProvider.getString(R.string.pid))
        .thenReturn(mockedDocUiNamePid)

    whenever(resourceProvider.getString(R.string.mdl))
        .thenReturn(mockedDocUiNameMdl)

    whenever(resourceProvider.getString(R.string.conference_badge))
        .thenReturn(mockedDocUiNameConferenceBadge)

    whenever(resourceProvider.getString(R.string.load_sample_data))
        .thenReturn(mockedDocUiNameSampleData)
}