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
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentDetailsUi
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemDomainPayload
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.OptionalFieldItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequiredFieldsItemUi
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.DocItem
import eu.europa.ec.eudi.iso18013.transfer.DocRequest
import eu.europa.ec.eudi.iso18013.transfer.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer.TxCodeSpec
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.InfoTextWithNameAndImageData
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.DashboardScreens

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
object TestsData {

    data class TestFieldUi(
        val elementIdentifier: String,
        val value: String,
        val isAvailable: Boolean = true,
    )

    data class TestTransformedRequestDataUi(
        val documentId: String,
        val documentIdentifierUi: DocumentIdentifier,
        val documentTitle: String,
        val optionalFields: List<TestFieldUi>,
        val requiredFields: List<TestFieldUi>
    )

    val NotSupportedDocumentTypeException =
        RuntimeException("Currently not supported Document Type")

    const val mockedPidDocName = "EU PID"
    const val mockedMdlDocName = "mDL"
    const val mockedPidId = "000001"
    const val mockedMdlId = "000002"
    const val mockedAgeVerificationId = "000003"
    const val mockedPhotoId = "000004"
    const val mockedUserFirstName = "JAN"
    const val mockedUserBase64Portrait = "SE"
    const val mockedDocUiNamePid = "National ID"
    const val mockedDocUiNameMdl = "Driving License"
    const val mockedDocUiNameAge = "Age Verification"
    const val mockedDocUiNamePhotoId = "Photo ID"
    const val mockedDocUiNameSampleData = "Load Sample Documents"
    const val mockedNoUserFistNameFound = ""
    const val mockedNoUserBase64PortraitFound = ""
    const val mockedNoExpirationDateFound = ""
    const val mockedFormattedExpirationDate = "30 Mar 2050"
    const val mockedDocumentHasExpired = false
    const val mockedUserAuthentication = false
    const val mockedVerifierName = "EUDIW Verifier"
    const val mockedIssuerName = "EUDIW Issuer"
    const val mockedRequestRequiredFieldsTitle = "Verification Data"
    const val mockedRequestElementIdentifierNotAvailable = "Not available"
    const val mockedOfferedDocumentName = "Offered Document"
    const val mockedOfferedDocumentDocType = "mocked_offered_document_doc_type"
    const val mockedTxCodeSpecFourDigits = 4
    const val mockedSuccessTitle = "Success title"
    const val mockedSuccessSubtitle = "Success subtitle"
    const val mockedSuccessContentDescription = "Content description"
    const val mockedIssuanceErrorMessage = "Issuance error message"
    const val mockedInvalidCodeFormatMessage = "Invalid code format message"
    const val mockedWalletActivationErrorMessage = "Wallet activation error message"
    const val mockedPrimaryButtonText = "Primary button text"
    const val mockedRouteArguments = "mockedRouteArguments"
    const val mockedTxCode = "mockedTxCode"

    const val mockedPidDocType = "eu.europa.ec.eudi.pid.1"
    const val mockedPidNameSpace = "eu.europa.ec.eudi.pid.1"
    const val mockedMdlDocType = "org.iso.18013.5.1.mDL"
    const val mockedMdlNameSpace = "org.iso.18013.5.1"
    const val mockedAgeVerificationDocType = "eu.europa.ec.eudi.pseudonym.age_over_18.1"
    const val mockedAgeVerificationNameSpace = "eu.europa.ec.eudi.pseudonym.age_over_18.1"
    const val mockedPhotoIdDocType = "org.iso.23220.2.photoid.1"
    const val mockedPhotoIdNameSpace = "org.iso.23220.2.photoid.1"

    const val mockedUriPath1 = "eudi-wallet://example.com/path1"
    const val mockedUriPath2 = "eudi-wallet://example.com/path2"

    val mockedValidReaderAuth = ReaderAuth(
        readerAuth = byteArrayOf(),
        readerSignIsValid = true,
        readerCertificateChain = listOf(),
        readerCertificatedIsTrusted = true,
        readerCommonName = mockedVerifierName
    )

    val mockedPidWithBasicFieldsDocRequest = DocRequest(
        docType = mockedPidDocType,
        requestItems = listOf(
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "family_name"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "given_name"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "age_over_18"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "age_over_65"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "age_birth_year"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "birth_city"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "gender"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "expiry_date"
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "portrait",
            ),
            DocItem(
                namespace = mockedPidNameSpace,
                elementIdentifier = "issuing_country",
            ),
        ),
        readerAuth = mockedValidReaderAuth
    )

    val mockedPhotoIdWithBasicFieldsDocRequest = DocRequest(
        docType = mockedPhotoIdDocType,
        requestItems = listOf(
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "family_name"
            ),
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "given_name"
            ),
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "age_over_18"
            ),
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "age_birth_year"
            ),
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "birth_city"
            ),
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "expiry_date"
            ),
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "portrait",
            ),
            DocItem(
                namespace = mockedPhotoIdNameSpace,
                elementIdentifier = "issuing_country",
            ),
        ),
        readerAuth = mockedValidReaderAuth
    )

    val mockedMdlWithBasicFieldsDocRequest = DocRequest(
        docType = mockedMdlDocType,
        requestItems = listOf(
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "family_name"
            ),
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "given_name"
            ),
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "birth_place"
            ),
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "expiry_date"
            ),
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "portrait"
            ),
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "driving_privileges"
            ),
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "signature_usual_mark"
            ),
            DocItem(
                namespace = mockedMdlNameSpace,
                elementIdentifier = "sex"
            )
        ),
        readerAuth = mockedValidReaderAuth
    )

    val mockedAgeVerificationWithBasicFieldsDocRequest = DocRequest(
        docType = mockedAgeVerificationDocType,
        requestItems = listOf(
            DocItem(
                namespace = mockedAgeVerificationNameSpace,
                elementIdentifier = "age_over_18"
            ),
            DocItem(
                namespace = mockedAgeVerificationNameSpace,
                elementIdentifier = "expiry_date"
            ),
            DocItem(
                namespace = mockedAgeVerificationNameSpace,
                elementIdentifier = "issuing_country",
            )
        ),
        readerAuth = mockedValidReaderAuth
    )

    val mockedValidPidWithBasicFieldsRequestDocument = RequestDocument(
        documentId = mockedPidId,
        docType = mockedPidDocType,
        docName = mockedPidDocName,
        userAuthentication = mockedUserAuthentication,
        docRequest = mockedPidWithBasicFieldsDocRequest
    )

    val mockedValidMdlWithBasicFieldsRequestDocument = RequestDocument(
        documentId = mockedMdlId,
        docType = mockedMdlDocType,
        docName = mockedMdlDocName,
        userAuthentication = mockedUserAuthentication,
        docRequest = mockedMdlWithBasicFieldsDocRequest
    )

    val mockedFullPidUi = DocumentUi(
        documentId = mockedPidId,
        documentName = mockedDocUiNamePid,
        documentIdentifier = DocumentIdentifier.PID,
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
        documentImage = "",
        documentDetails = emptyList(),
        documentIssuanceState = DocumentUiIssuanceState.Issued,
    )

    val mockedPendingPidUi = mockedFullPidUi.copy(
        documentIssuanceState = DocumentUiIssuanceState.Pending
    )

    val mockedUnsignedPidUi = mockedFullPidUi.copy(
        documentName = mockedPidDocName,
        documentIssuanceState = DocumentUiIssuanceState.Pending,
        documentIdentifier = DocumentIdentifier.OTHER(
            nameSpace = "",
            docType = mockedFullPidUi.documentIdentifier.docType
        ),
        documentExpirationDateFormatted = ""
    )

    val mockedBasicPidUi = mockedFullPidUi.copy(
        documentDetails = listOf(
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "birth_city",
                        infoValues = arrayOf("KATRINEHOLM")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "gender",
                        infoValues = arrayOf("male")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "age_over_18",
                        infoValues = arrayOf("yes")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "age_birth_year",
                        infoValues = arrayOf("1985")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "expiry_date",
                        infoValues = arrayOf("30 Mar 2050")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "given_name",
                        infoValues = arrayOf("JAN")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "family_name",
                        infoValues = arrayOf("ANDERSSON")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "age_over_65",
                        infoValues = arrayOf("no")
                    )
            ),
        ),
        userFullName = "JAN ANDERSSON"
    )

    val mockedFullMdlUi = DocumentUi(
        documentId = mockedMdlId,
        documentName = mockedDocUiNameMdl,
        documentIdentifier = DocumentIdentifier.MDL,
        documentExpirationDateFormatted = mockedFormattedExpirationDate,
        documentHasExpired = mockedDocumentHasExpired,
        documentImage = "",
        documentDetails = emptyList(),
        documentIssuanceState = DocumentUiIssuanceState.Issued,
    )

    val mockedPendingMdlUi = mockedFullMdlUi.copy(
        documentIssuanceState = DocumentUiIssuanceState.Pending
    )

    val mockedBasicMdlUi = mockedFullMdlUi.copy(
        documentDetails = listOf(
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "driving_privileges",
                        infoValues = arrayOf(
                            "issue_date: 1 Jul 2010\n" +
                                    "expiry_date: 30 Mar 2050\n" +
                                    "vehicle_category_code: A\n" +
                                    "issue_date: 19 May 2008\n" +
                                    "expiry_date: 30 Mar 2050\n" +
                                    "vehicle_category_code: B"
                        )
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "expiry_date",
                        infoValues = arrayOf("30 Mar 2050")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "sex",
                        infoValues = arrayOf("male")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "birth_place",
                        infoValues = arrayOf("SWEDEN")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "given_name",
                        infoValues = arrayOf("JAN")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "portrait",
                        infoValues = arrayOf("Shown above")
                    )
            ),
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData
                    .create(
                        title = "family_name",
                        infoValues = arrayOf("ANDERSSON")
                    )
            ),
            DocumentDetailsUi.SignatureItem(
                itemData = InfoTextWithNameAndImageData(
                    title = "signature_usual_mark",
                    base64Image = "SE"
                )
            ),
        ),
        userFullName = "JAN ANDERSSON"
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
        type = DocumentIdentifier.PID,
        available = true
    )

    val mockedMdlOptionItemUi = DocumentOptionItemUi(
        text = mockedDocUiNameMdl,
        icon = AppIcons.Id,
        type = DocumentIdentifier.MDL,
        available = true
    )

    val mockedAgeOptionItemUi = DocumentOptionItemUi(
        text = mockedDocUiNameAge,
        icon = AppIcons.Id,
        type = DocumentIdentifier.AGE,
        available = true
    )

    val mockedPhotoIdOptionItemUi = DocumentOptionItemUi(
        text = mockedDocUiNamePhotoId,
        icon = AppIcons.Id,
        type = DocumentIdentifier.PHOTOID,
        available = true
    )

    val mockedSampleDataOptionItemUi = DocumentOptionItemUi(
        text = mockedDocUiNameSampleData,
        icon = AppIcons.Id,
        type = DocumentIdentifier.SAMPLE,
        available = true
    )

    val mockedConfigNavigationTypePop = ConfigNavigation(navigationType = NavigationType.Pop)
    val mockedConfigNavigationTypePush = ConfigNavigation(
        navigationType = NavigationType.PushRoute(
            route = DashboardScreens.Dashboard.screenRoute
        )
    )
    val mockedConfigNavigationTypePopToScreen = ConfigNavigation(
        navigationType = NavigationType.PopTo(
            screen = DashboardScreens.Dashboard
        )
    )

    val mockedOfferTxCodeSpecFourDigits =
        TxCodeSpec(
            inputMode = TxCodeSpec.InputMode.NUMERIC,
            length = mockedTxCodeSpecFourDigits
        )

    val mockedOptionalFieldsForPidWithBasicFields = listOf(
        TestFieldUi(
            elementIdentifier = "family_name",
            value = "ANDERSSON",
        ),
        TestFieldUi(
            elementIdentifier = "given_name",
            value = "JAN",
        ),
        TestFieldUi(
            elementIdentifier = "age_over_18",
            value = "yes",
        ),
        TestFieldUi(
            elementIdentifier = "age_over_65",
            value = "no",
        ),
        TestFieldUi(
            elementIdentifier = "age_birth_year",
            value = "1985",
        ),
        TestFieldUi(
            elementIdentifier = "birth_city",
            value = "KATRINEHOLM",
        ),
        TestFieldUi(
            elementIdentifier = "gender",
            value = "male",
        ),
    )

    val mockedRequiredFieldsForPidWithBasicFields = listOf(
        TestFieldUi(
            elementIdentifier = "expiry_date",
            value = mockedFormattedExpirationDate,
            isAvailable = true
        ),
        TestFieldUi(
            elementIdentifier = "portrait",
            value = mockedRequestElementIdentifierNotAvailable,
            isAvailable = false
        ),
        TestFieldUi(
            elementIdentifier = "issuing_country",
            value = mockedRequestElementIdentifierNotAvailable,
            isAvailable = false
        ),
    )

    val mockedOptionalFieldsForMdlWithBasicFields = listOf(
        TestFieldUi(
            elementIdentifier = "family_name",
            value = "ANDERSSON",
        ),
        TestFieldUi(
            elementIdentifier = "given_name",
            value = "JAN",
        ),
        TestFieldUi(
            elementIdentifier = "birth_place",
            value = "SWEDEN",
        ),
        TestFieldUi(
            elementIdentifier = "expiry_date",
            value = mockedFormattedExpirationDate,
        ),
        TestFieldUi(
            elementIdentifier = "portrait",
            value = "SE",
        ),
        TestFieldUi(
            elementIdentifier = "driving_privileges",
            value = "issue_date: 1 Jul 2010\n" +
                    "expiry_date: 30 Mar 2050\n" +
                    "vehicle_category_code: A\n" +
                    "issue_date: 19 May 2008\n" +
                    "expiry_date: 30 Mar 2050\n" +
                    "vehicle_category_code: B",
        ),
        TestFieldUi(
            elementIdentifier = "signature_usual_mark",
            value = "SE",
        ),
        TestFieldUi(
            elementIdentifier = "sex",
            value = "male",
        ),
    )

    val mockedTransformedRequestDataUiForPidWithBasicFields = TestTransformedRequestDataUi(
        documentId = mockedPidId,
        documentIdentifierUi = DocumentIdentifier.PID,
        documentTitle = mockedDocUiNamePid,
        optionalFields = mockedOptionalFieldsForPidWithBasicFields,
        requiredFields = mockedRequiredFieldsForPidWithBasicFields
    )

    fun createTransformedRequestDataUi(
        items: List<TestTransformedRequestDataUi>
    ): List<RequestDataUi<Event>> {
        val resultList = mutableListOf<RequestDataUi<Event>>()

        items.forEachIndexed { itemsIndex, transformedRequestDataUi ->
            resultList.add(
                RequestDataUi.Document(
                    documentItemUi = DocumentItemUi(
                        title = transformedRequestDataUi.documentTitle
                    )
                )
            )
            resultList.add(RequestDataUi.Space())

            transformedRequestDataUi.optionalFields.forEachIndexed { index, testFieldUi ->
                val optionalField = when (transformedRequestDataUi.documentIdentifierUi) {
                    is DocumentIdentifier.PID -> mockCreateOptionalFieldForPid(
                        docId = transformedRequestDataUi.documentId,
                        elementIdentifier = testFieldUi.elementIdentifier,
                        value = testFieldUi.value
                    )

                    is DocumentIdentifier.MDL -> mockCreateOptionalFieldForMdl(
                        docId = transformedRequestDataUi.documentId,
                        elementIdentifier = testFieldUi.elementIdentifier,
                        value = testFieldUi.value,
                    )

                    is DocumentIdentifier.AGE -> mockCreateOptionalFieldForAgeVerification(
                        docId = transformedRequestDataUi.documentId,
                        elementIdentifier = testFieldUi.elementIdentifier,
                        value = testFieldUi.value,
                    )

                    is DocumentIdentifier.PHOTOID -> mockCreateOptionalFieldForPhotoId(
                        docId = transformedRequestDataUi.documentId,
                        elementIdentifier = testFieldUi.elementIdentifier,
                        value = testFieldUi.value
                    )

                    is DocumentIdentifier.SAMPLE, is DocumentIdentifier.OTHER -> throw NotSupportedDocumentTypeException
                }

                resultList.add(RequestDataUi.Space())
                resultList.add(optionalField)

                if (index != (transformedRequestDataUi.optionalFields.size + transformedRequestDataUi.requiredFields.size) - 1) {
                    resultList.add(RequestDataUi.Space())
                    resultList.add(RequestDataUi.Divider())
                }
            }

            resultList.add(RequestDataUi.Space())

            if (transformedRequestDataUi.requiredFields.isNotEmpty()) {
                resultList.add(
                    mockCreateRequiredFieldsForPid(
                        docId = transformedRequestDataUi.documentId,
                        requiredFieldsWholeSectionId = itemsIndex,
                        requiredFields = transformedRequestDataUi.requiredFields
                    )
                )
                resultList.add(RequestDataUi.Space())
            }
        }

        return resultList
    }

    val mockedTransformedRequestDataUiForMdlWithBasicFields = TestTransformedRequestDataUi(
        documentId = mockedMdlId,
        documentIdentifierUi = DocumentIdentifier.MDL,
        documentTitle = mockedDocUiNameMdl,
        optionalFields = mockedOptionalFieldsForMdlWithBasicFields,
        requiredFields = emptyList()
    )

    private fun mockCreateOptionalFieldForPid(
        docId: String,
        elementIdentifier: String,
        value: String,
        checked: Boolean = true,
        enabled: Boolean = true,
    ): RequestDataUi.OptionalField<Event> {
        val uniqueId = mockedPidDocType + elementIdentifier + docId
        return mockCreateOptionalField(
            documentIdentifierUi = DocumentIdentifier.PID,
            uniqueId = uniqueId,
            elementIdentifier = elementIdentifier,
            value = value,
            checked = checked,
            enabled = enabled,
            event = Event.UserIdentificationClicked(itemId = uniqueId)
        )
    }

    private fun mockCreateOptionalFieldForPhotoId(
        docId: String,
        elementIdentifier: String,
        value: String,
        checked: Boolean = true,
        enabled: Boolean = true,
    ): RequestDataUi.OptionalField<Event> {
        val uniqueId = mockedPhotoIdDocType + elementIdentifier + docId
        return mockCreateOptionalField(
            documentIdentifierUi = DocumentIdentifier.PHOTOID,
            uniqueId = uniqueId,
            elementIdentifier = elementIdentifier,
            value = value,
            checked = checked,
            enabled = enabled,
            event = Event.UserIdentificationClicked(itemId = uniqueId)
        )
    }

    private fun mockCreateOptionalFieldForMdl(
        docId: String,
        elementIdentifier: String,
        value: String,
        checked: Boolean = true,
        enabled: Boolean = true,
    ): RequestDataUi.OptionalField<Event> {
        val uniqueId = mockedMdlDocType + elementIdentifier + docId
        return mockCreateOptionalField(
            documentIdentifierUi = DocumentIdentifier.MDL,
            uniqueId = uniqueId,
            elementIdentifier = elementIdentifier,
            value = value,
            checked = checked,
            enabled = enabled,
            event = Event.UserIdentificationClicked(itemId = uniqueId)
        )
    }

    private fun mockCreateOptionalFieldForAgeVerification(
        docId: String,
        elementIdentifier: String,
        value: String,
        checked: Boolean = true,
        enabled: Boolean = true,
    ): RequestDataUi.OptionalField<Event> {
        val uniqueId = mockedAgeVerificationDocType + elementIdentifier + docId
        return mockCreateOptionalField(
            documentIdentifierUi = DocumentIdentifier.AGE,
            uniqueId = uniqueId,
            elementIdentifier = elementIdentifier,
            value = value,
            checked = checked,
            enabled = enabled,
            event = Event.UserIdentificationClicked(itemId = uniqueId)
        )
    }

    private fun mockCreateOptionalField(
        documentIdentifierUi: DocumentIdentifier,
        uniqueId: String,
        elementIdentifier: String,
        value: String,
        checked: Boolean,
        enabled: Boolean,
        event: Event,
    ): RequestDataUi.OptionalField<Event> {
        return RequestDataUi.OptionalField(
            optionalFieldItemUi = OptionalFieldItemUi(
                requestDocumentItemUi = mockCreateRequestDocumentItemUi(
                    documentIdentifierUi = documentIdentifierUi,
                    uniqueId = uniqueId,
                    elementIdentifier = elementIdentifier,
                    value = value,
                    checked = checked,
                    enabled = enabled,
                    event = event
                )
            )
        )
    }

    private fun mockCreateRequestDocumentItemUi(
        documentIdentifierUi: DocumentIdentifier,
        uniqueId: String,
        elementIdentifier: String,
        value: String,
        checked: Boolean,
        enabled: Boolean,
        event: Event?,
    ): RequestDocumentItemUi<Event> {

        val namespace: String
        val docId: String
        val docType: DocType
        val docRequest: DocRequest

        when (documentIdentifierUi) {
            is DocumentIdentifier.PID -> {
                namespace = mockedPidNameSpace
                docId = mockedPidId
                docType = mockedPidDocType
                docRequest = mockedPidWithBasicFieldsDocRequest
            }

            is DocumentIdentifier.MDL -> {
                namespace = mockedMdlNameSpace
                docId = mockedMdlId
                docType = mockedMdlDocType
                docRequest = mockedMdlWithBasicFieldsDocRequest
            }

            is DocumentIdentifier.AGE -> {
                namespace = mockedAgeVerificationNameSpace
                docId = mockedAgeVerificationId
                docType = mockedAgeVerificationDocType
                docRequest = mockedAgeVerificationWithBasicFieldsDocRequest
            }

            is DocumentIdentifier.PHOTOID -> {
                namespace = mockedPhotoIdNameSpace
                docId = mockedPhotoId
                docType = mockedPhotoIdDocType
                docRequest = mockedPhotoIdWithBasicFieldsDocRequest
            }

            is DocumentIdentifier.SAMPLE, is DocumentIdentifier.OTHER -> throw NotSupportedDocumentTypeException
        }

        return RequestDocumentItemUi(
            id = uniqueId,
            domainPayload = DocumentItemDomainPayload(
                docId = docId,
                docType = docType,
                docRequest = docRequest,
                namespace = namespace,
                elementIdentifier = elementIdentifier
            ),
            readableName = elementIdentifier,
            value = value,
            checked = checked,
            enabled = enabled,
            docItem = DocItem(
                namespace = namespace,
                elementIdentifier = elementIdentifier
            ),
            event = event
        )
    }

    private fun mockCreateRequiredFieldsForPid(
        docId: String,
        requiredFieldsWholeSectionId: Int,
        requiredFields: List<TestFieldUi>,
    ): RequestDataUi.RequiredFields<Event> {
        val requestDocumentItemsUi: MutableList<RequestDocumentItemUi<Event>> = mutableListOf()
        requiredFields.forEach {
            val uniqueId = mockedPidDocType + it.elementIdentifier + docId
            requestDocumentItemsUi.add(
                mockCreateRequestDocumentItemUi(
                    documentIdentifierUi = DocumentIdentifier.PID,
                    uniqueId = uniqueId,
                    elementIdentifier = it.elementIdentifier,
                    value = it.value,
                    checked = it.isAvailable,
                    enabled = false,
                    event = null
                )
            )
        }

        return RequestDataUi.RequiredFields(
            requiredFieldsItemUi = RequiredFieldsItemUi(
                id = requiredFieldsWholeSectionId,
                requestDocumentItemsUi = requestDocumentItemsUi,
                expanded = false,
                title = mockedRequestRequiredFieldsTitle,
                event = Event.ExpandOrCollapseRequiredDataList(id = requiredFieldsWholeSectionId)
            )
        )
    }
}