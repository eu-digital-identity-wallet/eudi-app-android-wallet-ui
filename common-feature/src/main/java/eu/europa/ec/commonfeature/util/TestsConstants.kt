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

package eu.europa.ec.commonfeature.util

import androidx.annotation.VisibleForTesting
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.uilogic.component.AppIcons

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
object TestsConstants {
    const val mockedId1 = "000001"
    const val mockedId2 = "000002"
    const val mockedUserFirstName = "JAN"
    const val mockedUserBase64Portrait = "SE"
    const val mockedDocUiNamePid = "National ID"
    const val mockedDocUiNameMdl = "Driving License"
    const val mockedDocUiNameSampleData = "Load Sample Documents"
    const val mockedNoUserFistNameFound = ""
    const val mockedNoUserBase64PortraitFound = ""
    const val mockedNoExpirationDateFound = "-"
    const val mockedFormattedExpirationDate = "30 Mar 2050"

    val mockedFullPidUi = DocumentUi(
        documentId = mockedId1,
        documentName = mockedDocUiNamePid,
        documentType = DocumentTypeUi.PID,
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentImage = "",
        documentDetails = emptyList(),
    )

    val mockedFullMdlUi = DocumentUi(
        documentId = mockedId2,
        documentName = mockedDocUiNameMdl,
        documentType = DocumentTypeUi.MDL,
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentImage = "",
        documentDetails = emptyList(),
    )

    val mockedMdlUiWithNoUserNameAndNoUserImage: DocumentUi = mockedFullMdlUi

    val mockedMdlUiWithNoExpirationDate: DocumentUi = mockedFullMdlUi.copy(
        documentExpirationDateFormatted = mockedNoExpirationDateFound
    )

    val mockedFullDocumentsUi: List<DocumentUi> = listOf(
        mockedFullPidUi, mockedFullMdlUi
    )

    val mockedPidOptionItemUi = DocumentOptionItemUi(
        text = mockedDocUiNamePid,
        icon = AppIcons.Id,
        type = DocumentTypeUi.PID,
        available = true
    )

    val mockedMdlOptionItemUi = DocumentOptionItemUi(
        text = mockedDocUiNameMdl,
        icon = AppIcons.Id,
        type = DocumentTypeUi.MDL,
        available = true
    )

    val mockedSampleDataOptionItemUi = DocumentOptionItemUi(
        text = mockedDocUiNameSampleData,
        icon = AppIcons.Id,
        type = DocumentTypeUi.SAMPLE_DOCUMENTS,
        available = true
    )
}