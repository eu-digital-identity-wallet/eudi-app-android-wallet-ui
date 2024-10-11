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

package eu.europa.ec.issuancefeature.interactor.document

import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.commonfeature.util.TestsData.mockedUriPath1
import eu.europa.ec.corelogic.controller.ResolveDocumentOfferPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedMainPid
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class TestDocumentOfferInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var deviceAuthenticationInteractor: DeviceAuthenticationInteractor

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uiSerializer: UiSerializer

    private lateinit var interactor: DocumentOfferInteractor

    private lateinit var closeable: AutoCloseable

    private lateinit var biometricCrypto: BiometricCrypto

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DocumentOfferInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            deviceAuthenticationInteractor = deviceAuthenticationInteractor,
            resourceProvider = resourceProvider,
            uiSerializer = uiSerializer
        )
        biometricCrypto = BiometricCrypto(cryptoObject = null)

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region resolveDocumentOffer
    //
    // Case 1:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns
    // ResolveDocumentOfferPartialState.Success

    // Case 1 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.NoDocument state with:
    // - the issuer name string
    @Test
    fun `Given Case 1, When resolveDocumentOffer is called, Then Case 1 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(mockedOffer.issuerName)
                .thenReturn(mockedIssuerName)
            mockGetMainPidDocumentCall(
                mainPid = mockedMainPid
            )
            mockWalletDocumentsControllerEventEmission(
                event = ResolveDocumentOfferPartialState.Success(
                    offer = mockedOffer
                )
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult =
                    ResolveDocumentOfferInteractorPartialState.NoDocument(
                        issuerName = mockedIssuerName
                    )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 2:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns
    // ResolveDocumentOfferPartialState.Success with:
    // an Offer item holding a document list of Offer.OfferedDocument

    // Case 2 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Success state, with:
    // - DocumentUiItem list
    // - issuer name (string)
    // - and txCodeLength (int)
    @Test
    fun `Given Case 2, When resolveDocumentOffer is called, Then Case 2 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOfferedDocumentsList = listOf(mockedOfferedDocument)
            whenever(mockedOffer.offeredDocuments).thenReturn(mockedOfferedDocumentsList)
            whenever(mockedOfferedDocument.name).thenReturn(mockedOfferDocumentName)
            whenever(mockedOfferedDocument.docType).thenReturn(mockedDocType)
            val mockedTxCodeSpecLength = 4
            val mockedOfferTxCodeSpec = Offer.TxCodeSpec(
                length = mockedTxCodeSpecLength
            )
            whenever(mockedOffer.issuerName)
                .thenReturn(mockedIssuerName)
            whenever(mockedOffer.txCodeSpec)
                .thenReturn(mockedOfferTxCodeSpec)
            mockGetMainPidDocumentCall(
                mainPid = mockedMainPid
            )
            mockWalletDocumentsControllerEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedList = mockedOfferedDocumentsList.map {
                    DocumentItemUi(title = mockedOfferDocumentName)
                }
                val expectedResult =
                    ResolveDocumentOfferInteractorPartialState.Success(
                        documents = expectedList,
                        issuerName = mockedIssuerName,
                        txCodeLength = mockedTxCodeSpecLength
                    )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 3:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() throws:
    // a RuntimeException without message

    // Case 3 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - a generic error message
    @Test
    fun `Given Case 3, When resolveDocumentOffer is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.resolveDocumentOffer(mockedUriPath1)).thenThrow(
                mockedExceptionWithNoMessage
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                // Then
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Failure(
                    errorMessage = mockedGenericErrorMessage
                )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns
    // ResolveDocumentOfferPartialState.Success with:
    // an Offer item holding a document list of Offer.OfferedDocument items
    // txCodeSpec with length of 2 (numeric type), less than given limits of 4 to 6 digits length

    // Case 4 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - an invalid code format error message
    @Test
    fun `Given Case 4, When resolveDocumentOffer is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOfferedDocumentsList = listOf(mockedOfferedDocument)
            whenever(mockedOffer.offeredDocuments).thenReturn(mockedOfferedDocumentsList)
            whenever(mockedOfferedDocument.name).thenReturn(mockedOfferDocumentName)
            whenever(mockedOfferedDocument.docType).thenReturn(mockedDocType)

            val mockedTxCodeSpecLength = 2
            val mockedOfferTxCodeSpec = Offer.TxCodeSpec(
                inputMode = Offer.TxCodeSpec.InputMode.NUMERIC,
                length = mockedTxCodeSpecLength
            )
            whenever(mockedOffer.txCodeSpec).thenReturn(mockedOfferTxCodeSpec)

            val codeMinLength = 4
            val codeMaxLength = 6
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_error_invalid_txcode_format,
                    codeMinLength,
                    codeMaxLength
                )
            ).thenReturn(mockedInvalidCodeFormatMessage)

            mockWalletDocumentsControllerEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult =
                    ResolveDocumentOfferInteractorPartialState.Failure(
                        errorMessage = mockedInvalidCodeFormatMessage
                    )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 5:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns
    // ResolveDocumentOfferPartialState.Success with:
    // an Offer item holding a document list of Offer.OfferedDocument items
    // one of the OfferedDocument list items with docType of "load_sample_documents"
    // (isSupported() returns false in this case)
    // txCodeSpec with length of 4 (numeric type)

    // Case 5 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - an invalid wallet activation error message
    @Test
    fun `Given Case 5, When resolveDocumentOffer is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOfferedDocumentsList = listOf(mockedOfferedDocument)
            whenever(mockedOffer.offeredDocuments).thenReturn(mockedOfferedDocumentsList)
            whenever(mockedOfferedDocument.name).thenReturn(mockedOfferDocumentName)
            whenever(mockedOfferedDocument.docType).thenReturn(mockedSampleDocumentType)

            val mockedTxCodeSpecLength = 4
            val mockedTxCodeSpec = Offer.TxCodeSpec(
                length = mockedTxCodeSpecLength
            )
            whenever(mockedOffer.issuerName).thenReturn(mockedIssuerName)
            whenever(mockedOffer.txCodeSpec).thenReturn(mockedTxCodeSpec)
            whenever(resourceProvider.getString(R.string.issuance_document_offer_error_missing_pid_text))
                .thenReturn(mockedWalletActivationErrorMessage)
            mockWalletDocumentsControllerEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult =
                    ResolveDocumentOfferInteractorPartialState.Failure(
                        mockedWalletActivationErrorMessage
                    )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 6:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns
    // ResolveDocumentOfferPartialState.Success with:
    // an Offer item holding a document list of Offer.OfferedDocument items
    // one of the OfferedDocument list items with docType of PID
    // txCodeSpec with length of 4 (numeric type)

    // Case 6 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Success state, with:
    // - the expected DocumentItemUi list
    // - issuer name string and
    // - txCodeLength (int)
    @Test
    fun `Given Case 6, When resolveDocumentOffer is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOfferedDocumentsList = listOf(mockedOfferedDocument)
            whenever(mockedOffer.offeredDocuments).thenReturn(mockedOfferedDocumentsList)
            whenever(mockedOfferedDocument.name)
                .thenReturn(mockedOfferDocumentName)
            whenever(mockedOfferedDocument.docType)
                .thenReturn(mockedPidDocType)
            whenever(resourceProvider.getString(R.string.pid))
                .thenReturn(mockedPidLabel)

            val mockedTxCodeSpecLength = 4
            val mockedOfferTxCodeSpec = Offer.TxCodeSpec(
                length = mockedTxCodeSpecLength
            )
            whenever(mockedOffer.issuerName).thenReturn(mockedIssuerName)
            whenever(mockedOffer.txCodeSpec).thenReturn(mockedOfferTxCodeSpec)

            mockGetMainPidDocumentCall(
                mainPid = mockedMainPid
            )
            mockWalletDocumentsControllerEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedDocumentsUiList = listOf(
                    DocumentItemUi(mockedPidLabel)
                )
                val expectedResult =
                    ResolveDocumentOfferInteractorPartialState.Success(
                        documents = expectedDocumentsUiList,
                        issuerName = mockedIssuerName,
                        txCodeLength = mockedTxCodeSpecLength
                    )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 7:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns
    // ResolveDocumentOfferPartialState.Failure with:
    // a plain failure message

    // Case 7 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - the same failure message
    @Test
    fun `Given Case 7, When resolveDocumentOffer is called, Then Case 7 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletDocumentsControllerEventEmission(
                event = ResolveDocumentOfferPartialState.Failure(mockedPlainFailureMessage)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
                //Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    //endregion

    //region helper functions
    private fun mockGetMainPidDocumentCall(mainPid: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(mainPid)
    }

    private fun mockWalletDocumentsControllerEventEmission(event: ResolveDocumentOfferPartialState) {
        whenever(walletCoreDocumentsController.resolveDocumentOffer(mockedUriPath1))
            .thenReturn(event.toFlow())
    }
    //endregion

    //region mocked objects
    private val mockedOffer = mock<Offer>()
    private val mockedOfferedDocument = mock<Offer.OfferedDocument>()
    private val mockedIssuerName = "mockedIssuerName"
    private val mockedOfferDocumentName = "offerDocumentName"
    private val mockedDocType = "mockedDocType"
    private val mockedSampleDocumentType = DocumentIdentifier.SAMPLE.docType
    private val mockedPidLabel = "mocked PID label"
    private val mockedPidDocType = DocumentIdentifier.PID.docType
    private val mockedInvalidCodeFormatMessage = "mocked invalid code format message"
    private val mockedWalletActivationErrorMessage = "mocked wallet activation error message"
    //endregion
}