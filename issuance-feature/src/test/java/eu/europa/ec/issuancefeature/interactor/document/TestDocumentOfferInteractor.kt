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

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.commonfeature.util.TestsData.mockedConfigNavigationTypePop
import eu.europa.ec.commonfeature.util.TestsData.mockedInvalidCodeFormatMessage
import eu.europa.ec.commonfeature.util.TestsData.mockedIssuanceErrorMessage
import eu.europa.ec.commonfeature.util.TestsData.mockedIssuerName
import eu.europa.ec.commonfeature.util.TestsData.mockedOfferTxCodeFourDigits
import eu.europa.ec.commonfeature.util.TestsData.mockedOfferedDocumentDocType
import eu.europa.ec.commonfeature.util.TestsData.mockedOfferedDocumentName
import eu.europa.ec.commonfeature.util.TestsData.mockedPendingMdlUi
import eu.europa.ec.commonfeature.util.TestsData.mockedPendingPidUi
import eu.europa.ec.commonfeature.util.TestsData.mockedPidId
import eu.europa.ec.commonfeature.util.TestsData.mockedPrimaryButtonText
import eu.europa.ec.commonfeature.util.TestsData.mockedRouteArguments
import eu.europa.ec.commonfeature.util.TestsData.mockedSuccessDescription
import eu.europa.ec.commonfeature.util.TestsData.mockedSuccessText
import eu.europa.ec.commonfeature.util.TestsData.mockedTxCode
import eu.europa.ec.commonfeature.util.TestsData.mockedTxCodeFourDigits
import eu.europa.ec.commonfeature.util.TestsData.mockedUriPath1
import eu.europa.ec.commonfeature.util.TestsData.mockedWalletActivationErrorMessage
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.ResolveDocumentOfferPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.getIssuerName
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerEndpoint
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata.Display
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.TxCode
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.issuancefeature.ui.document.offer.model.DocumentOfferItemUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.testfeature.mockedDefaultLocale
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedMainPid
import eu.europa.ec.testfeature.mockedMdlDocName
import eu.europa.ec.testfeature.mockedMdlDocType
import eu.europa.ec.testfeature.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testfeature.mockedPidDocType
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.utils.PERCENTAGE_25
import eu.europa.ec.uilogic.serializer.UiSerializer
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URL
import java.util.Locale

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

    @Mock
    private lateinit var resultHandler: DeviceAuthenticationResult

    @Mock
    private lateinit var context: Context

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
        whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region resolveDocumentOffer

    // Case 1:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns ResolveDocumentOfferPartialState.Success with:
    // - empty response.offer.offeredDocuments

    // Case 1 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.NoDocument state with:
    // - the issuer name
    @Test
    fun `Given Case 1, When resolveDocumentOffer is called, Then Case 1 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName
            )
            mockGetMainPidDocumentCall(
                mainPid = mockedMainPid
            )
            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(
                    offer = mockedOffer
                )
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult = ResolveDocumentOfferInteractorPartialState.NoDocument(
                    issuerName = mockedOffer.getIssuerName(mockedDefaultLocale),
                    issuerLogo = null,
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 2:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns ResolveDocumentOfferPartialState.Success with:
    // - valid response.offer.txCodeSpec?.inputMode (TxCodeSpec.InputMode.NUMERIC),
    // - invalid response.offer.txCodeSpec?.length (2), and
    // - response.offer.offeredDocuments has only one Offer.OfferedDocument item.

    // Case 2 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - an invalid code format error message
    @Test
    fun `Given Case 2, When resolveDocumentOffer is called, Then Case 2 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedTxCodeSpecLength = 2
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = mockedOfferedDocumentsList,
                txCodeSpec = mockOfferTxCodeSpec(
                    inputMode = TxCodeInputMode.NUMERIC,
                    length = mockedTxCodeSpecLength
                )
            )

            val codeMinLength = 4
            val codeMaxLength = 6
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_error_invalid_txcode_format,
                    codeMinLength,
                    codeMaxLength
                )
            ).thenReturn(mockedInvalidCodeFormatMessage)

            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Failure(
                    errorMessage = mockedInvalidCodeFormatMessage
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 3:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns ResolveDocumentOfferPartialState.Success with:
    // - invalid response.offer.txCodeSpec?.inputMode (TxCodeSpec.InputMode.TEXT),
    // - valid response.offer.txCodeSpec?.length (4), and
    // - response.offer.offeredDocuments has only one Offer.OfferedDocument item.

    // Case 3 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - an invalid code format error message
    @Test
    fun `Given Case 3, When resolveDocumentOffer is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedTxCodeSpecLength = 4
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = mockedOfferedDocumentsList,
                txCodeSpec = mockOfferTxCodeSpec(
                    inputMode = TxCodeInputMode.TEXT,
                    length = mockedTxCodeSpecLength
                )
            )

            val codeMinLength = 4
            val codeMaxLength = 6
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_error_invalid_txcode_format,
                    codeMinLength,
                    codeMaxLength
                )
            ).thenReturn(mockedInvalidCodeFormatMessage)

            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Failure(
                    errorMessage = mockedInvalidCodeFormatMessage
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns ResolveDocumentOfferPartialState.Success with:
    // - valid response.offer.txCodeSpec?.inputMode (TxCodeSpec.InputMode.NUMERIC),
    // - valid response.offer.txCodeSpec?.length (4), and
    // - response.offer.offeredDocuments has only one Offer.OfferedDocument item.
    // 2. walletCoreDocumentsController.getMainPidDocument() returns not null (i.e. hasMainPid == true).
    // 3. no PID in Offer (i.e hasPidInOffer == false).
    // 4. System Locale is not supported by Metadata.

    // Case 4 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Success state, with:
    // - DocumentUiItem list, with non-localized document names, using DocTypes.
    // - issuer name
    // - and txCodeLength
    @Test
    fun `Given Case 4, When resolveDocumentOffer is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = listOf(
                    mockOfferedDocument(
                        display = listOf(
                            eu.europa.ec.eudi.openid4vci.Display(
                                name = mockedOfferedDocumentName,
                                locale = Locale("es")
                            )
                        )
                    )
                ),
                txCodeSpec = mockedOfferTxCodeFourDigits
            )
            mockGetMainPidDocumentCall(
                mainPid = mockedMainPid
            )
            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedList = listOf(
                    DocumentOfferItemUi(
                        title = mockedOfferedDocumentName,
                    )
                )
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Success(
                    documents = expectedList,
                    issuerName = mockedIssuerName,
                    txCodeLength = mockedTxCodeFourDigits,
                    issuerLogo = null,
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 5:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns ResolveDocumentOfferPartialState.Success with:
    // - valid response.offer.txCodeSpec?.inputMode (TxCodeSpec.InputMode.NUMERIC),
    // - valid response.offer.txCodeSpec?.length (4), and
    // - response.offer.offeredDocuments has only one Offer.OfferedDocument item.
    // 2. walletCoreDocumentsController.getMainPidDocument() returns null (i.e. hasMainPid == false).
    // 3. a PID in Offer (i.e hasPidInOffer == true).
    // 4. System Locale is supported by Metadata.

    // Case 5 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Success state, with:
    // - DocumentUiItem list, with remote document names
    // - issuer name
    // - and txCodeLength
    @Test
    fun `Given Case 5, When resolveDocumentOffer is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = listOf(
                    mockOfferedDocument(
                        name = mockedOfferedDocumentName,
                        docType = mockedPidDocType
                    )
                ),
                txCodeSpec = mockedOfferTxCodeFourDigits
            )
            mockGetMainPidDocumentCall(
                mainPid = null
            )

            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedDocumentsUiList = listOf(
                    DocumentOfferItemUi(
                        title = mockedOfferedDocumentName,
                    )
                )
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Success(
                    documents = expectedDocumentsUiList,
                    issuerName = mockedIssuerName,
                    txCodeLength = mockedOffer.txCodeSpec?.length,
                    issuerLogo = null,
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 6:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() returns ResolveDocumentOfferPartialState.Success with:
    // - valid response.offer.txCodeSpec?.inputMode (TxCodeSpec.InputMode.NUMERIC),
    // - valid response.offer.txCodeSpec?.length (4), and
    // - response.offer.offeredDocuments has only one Offer.OfferedDocument item.
    // 2. walletCoreDocumentsController.getMainPidDocument() returns null (i.e. hasMainPid == false).
    // 3. no PID in Offer (i.e hasPidInOffer == false).
    // 4. System Locale is supported by Metadata.

    // Case 6 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - an invalid wallet activation error message
    @Test
    fun `Given Case 6, When resolveDocumentOffer is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = mockedOfferedDocumentsList,
                txCodeSpec = mockedOfferTxCodeFourDigits
            )
            mockGetMainPidDocumentCall(
                mainPid = null
            )

            whenever(resourceProvider.getString(R.string.issuance_document_offer_error_missing_pid_text))
                .thenReturn(mockedWalletActivationErrorMessage)
            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Failure(
                    errorMessage = mockedWalletActivationErrorMessage
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
            mockWalletDocumentsControllerResolveOfferEventEmission(
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

    // Case 8:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() throws:
    // a RuntimeException with a message

    // Case 8 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - the exception's localized message
    @Test
    fun `Given Case 8, When resolveDocumentOffer is called, Then Case 8 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.resolveDocumentOffer(mockedUriPath1))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                // Then
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Failure(
                    errorMessage = mockedExceptionWithMessage.localizedMessage!!
                )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 9:
    // 1. walletCoreDocumentsController.resolveDocumentOffer() throws:
    // a RuntimeException without message

    // Case 9 Expected Result:
    // ResolveDocumentOfferInteractorPartialState.Failure state, with:
    // - the generic error message
    @Test
    fun `Given Case 9, When resolveDocumentOffer is called, Then Case 9 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.resolveDocumentOffer(mockedUriPath1))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                // Then
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Failure(
                    errorMessage = mockedGenericErrorMessage
                )
                assertEquals(expectedResult, awaitItem())
            }
        }
    //endregion

    //region issueDocuments

    // Case 1:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri emits
    // IssueDocumentsPartialState.Failure with:
    // mockedPlainFailureMessage as the error message

    // Case 1 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - errorMessage equal to mockedPlainFailureMessage
    @Test
    fun `Given Case 1, When issueDocuments is called, Then Case 1 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
            )

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                // Then
                val expectedResult = IssueDocumentsInteractorPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 2:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri emits
    // IssueDocumentsPartialState.Failure with:
    // mockedPlainFailureMessage as the error message
    // 2. The controller issueDocumentsByOfferUri is called with a mocked offerUri and null txCode

    // Case 2 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - errorMessage equal to mockedPlainFailureMessage.
    @Test
    fun `Given Case 2, When issueDocuments is called, Then Case 2 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val failureResponse = IssueDocumentsPartialState.Failure(
                errorMessage = mockedPlainFailureMessage
            )
            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = failureResponse,
                txCode = null
            )

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop
            ).runFlowTest {
                val expectedResult = IssueDocumentsInteractorPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 3:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri emits
    // IssueDocumentsPartialState.UserAuthRequired with:
    // biometricCrypto object and resultHandler as DeviceAuthenticationResult
    // 2. required arguments are mocked

    // Case 3 Expected Result:
    // IssueDocumentsInteractorPartialState.UserAuthRequired state, with parameters of:
    // - biometricCrypto object and resultHandler as DeviceAuthenticationResult
    @Test
    fun `Given Case 3, When issueDocuments is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(resourceProvider.getString(R.string.issuance_generic_error))
                .thenReturn(mockedIssuanceErrorMessage)

            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.UserAuthRequired(
                    crypto = biometricCrypto,
                    resultHandler = resultHandler
                )
            )

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                // Then
                val expectedResult = IssueDocumentsInteractorPartialState.UserAuthRequired(
                    crypto = biometricCrypto,
                    resultHandler = resultHandler
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri emits
    // IssueDocumentsPartialState.Success with:
    // 1. some documentIds.

    // Case 4 Expected Result:
    // IssueDocumentsInteractorPartialState.Success state, with:
    // - the same documentIds.
    @Test
    fun `Given Case 4, When issueDocuments is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.Success(
                    documentIds = listOf(mockedPidId)
                )
            )

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                val expectedResult = IssueDocumentsInteractorPartialState.Success(
                    documentIds = listOf(mockedPidId)
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 5:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri emits
    // IssueDocumentsPartialState.DeferredSuccess with:
    // mocked deferred documents
    // 2. required strings are mocked
    // 3. triple object with warning tint color
    // 4. uiSerializer.toBase64() serializes the mockedSuccessUiConfig into mockedRouteArguments

    // Case 5 Expected Result:
    // IssueDocumentsInteractorPartialState.DeferredSuccess state, with:
    // - successRoute equal to "SUCCESS?successConfig=mockedArguments"
    @Test
    fun `Given Case 5, When issueDocuments is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_deferred_success_description,
                    mockedIssuerName
                )
            ).thenReturn(mockedSuccessDescription)

            mockIssuanceDocumentOfferDeferredSuccessStrings()
            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.DeferredSuccess(
                    deferredDocuments = mockDeferredDocumentsMap()
                )
            )

            val mockedTripleObject = Triple(
                first = SuccessUIConfig.TextElementsConfig(
                    text = mockedSuccessText,
                    description = mockedSuccessDescription,
                    color = ThemeColors.pending
                ),
                second = SuccessUIConfig.ImageConfig(
                    type = SuccessUIConfig.ImageConfig.Type.Drawable(icon = AppIcons.InProgress),
                    tint = ThemeColors.primary,
                    screenPercentageSize = PERCENTAGE_25,
                ),
                third = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_primary_button_text)
            )

            val config = SuccessUIConfig(
                textElementsConfig = mockedTripleObject.first,
                imageConfig = mockedTripleObject.second,
                buttonConfig = listOf(
                    SuccessUIConfig.ButtonConfig(
                        text = mockedTripleObject.third,
                        style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                        navigation = mockedConfigNavigationTypePop
                    )
                ),
                onBackScreenToNavigate = mockedConfigNavigationTypePop
            )

            whenever(
                uiSerializer.toBase64(
                    model = config,
                    parser = SuccessUIConfig.Parser
                )
            ).thenReturn(mockedRouteArguments)

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                val expectedResult = IssueDocumentsInteractorPartialState.DeferredSuccess(
                    successRoute = "SUCCESS?successConfig=$mockedRouteArguments"
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 6:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri emits
    //    IssueDocumentsPartialState.PartialSuccess with:
    //    - successfully issued documentIds.
    //    - nonIssuedDocuments map containing mockDeferredPendingDocId1 to mockDeferredPendingType1
    //      and mockDeferredPendingDocId2 to mockDeferredPendingType2.
    // 2. nonIssuedDocsNames is formed by combining the document types of non-issued documents:
    //    "eu.europa.ec.eudi.pid.1, org.iso.18013.5.1.mDL"
    // 3. mocked string resources
    // 7. uiSerializer.toBase64() serializes the SuccessUIConfig object into mockedArguments.

    // Case 6 Expected Result:
    // IssueDocumentsInteractorPartialState.Success state, with:
    // - successRoute equal to "SUCCESS?successConfig=mockedArguments"
    @Test
    fun `Given Case 6, When issueDocuments is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockSuccessfullyIssuedDocId = "0000"

            val mockDeferredPendingDocId1 = mockedPendingPidUi.documentId
            val mockDeferredPendingType1 = mockedPendingPidUi.documentIdentifier.formatType

            val mockDeferredPendingDocId2 = mockedPendingMdlUi.documentId
            val mockDeferredPendingType2 = mockedPendingMdlUi.documentIdentifier.formatType

            val nonIssuedDeferredDocuments: Map<DocumentId, FormatType> = mapOf(
                mockDeferredPendingDocId1 to mockDeferredPendingType1,
                mockDeferredPendingDocId2 to mockDeferredPendingType2
            )

            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf(mockSuccessfullyIssuedDocId),
                    nonIssuedDocuments = nonIssuedDeferredDocuments
                )
            )

            val config = SuccessUIConfig(
                textElementsConfig = mockedTripleObject.first,
                imageConfig = mockedTripleObject.second,
                buttonConfig = listOf(
                    SuccessUIConfig.ButtonConfig(
                        text = mockedTripleObject.third,
                        style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                        navigation = mockedConfigNavigationTypePop
                    )
                ),
                onBackScreenToNavigate = mockedConfigNavigationTypePop
            )

            whenever(
                uiSerializer.toBase64(
                    model = config,
                    parser = SuccessUIConfig.Parser
                )
            ).thenReturn(mockedRouteArguments)

            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                val expectedResult = IssueDocumentsInteractorPartialState.Success(
                    documentIds = listOf(mockSuccessfullyIssuedDocId)
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 7:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri emits IssueDocumentsPartialState.PartialSuccess
    // 2. The interactor is called with the given offerUri, issuerName, navigation and txCode.

    // Case 7 Expected Result:
    // IssueDocumentsInteractorPartialState.Success state, with:
    // - successRoute equal to "SUCCESS?successConfig=mockedArguments".
    @Test
    fun `Given Case 7, When issueDocuments is called, Then Case 7 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockSuccessfullyIssuedDocId = "0000"
            val mockDeferredPendingDocName = mockedMdlDocName
            val mockDeferredPendingType1 = mockedMdlDocType
            val nonIssuedDeferredDocuments: Map<FormatType, DocumentId> = mapOf(
                mockDeferredPendingType1 to mockDeferredPendingDocName
            )

            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf(mockSuccessfullyIssuedDocId),
                    nonIssuedDocuments = nonIssuedDeferredDocuments
                )
            )

            whenever(
                uiSerializer.toBase64(
                    model = mockedSuccessUiConfig,
                    parser = SuccessUIConfig.Parser
                )
            ).thenReturn(mockedRouteArguments)

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                val expectedResult = IssueDocumentsInteractorPartialState.Success(
                    documentIds = listOf(mockSuccessfullyIssuedDocId),
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 8:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri throws an exception with a message.

    // Case 8 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - errorMessage equal to exception's localized message.
    @Test
    fun `Given Case 8, When issueDocuments is called, Then Case 8 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.issueDocumentsByOfferUri(
                    offerUri = mockedUriPath1,
                    txCode = mockedTxCode
                )
            ).thenThrow(mockedExceptionWithMessage)

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                val expectedResult = IssueDocumentsInteractorPartialState.Failure(
                    errorMessage = mockedExceptionWithMessage.localizedMessage!!
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 9:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri() throws an exception with no message.

    // Case 9 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - the generic error message.
    @Test
    fun `Given Case 9, When issueDocuments is called, Then Case 9 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.issueDocumentsByOfferUri(
                    offerUri = mockedUriPath1,
                    txCode = mockedTxCode
                )
            ).thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.issueDocuments(
                offerUri = mockedUriPath1,
                issuerName = mockedIssuerName,
                navigation = mockedConfigNavigationTypePop,
                txCode = mockedTxCode
            ).runFlowTest {
                val expectedResult = IssueDocumentsInteractorPartialState.Failure(
                    errorMessage = mockedGenericErrorMessage
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }
    //endregion

    //region handleUserAuthentication
    //
    // Case 1:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.CanAuthenticate

    // Case 1 Expected Result:
    // deviceAuthenticationInteractor.authenticateWithBiometrics called once.
    @Test
    fun `Given Case 1, When handleUserAuthentication is called, Then Case 1 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.CanAuthenticate
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = biometricCrypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(
                context = context,
                crypto = biometricCrypto,
                notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
                resultHandler = resultHandler
            )
    }

    // Case 2:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.NonEnrolled

    // Case 2 Expected Result:
    // deviceAuthenticationInteractor.launchBiometricSystemScreen called once.
    @Test
    fun `Given Case 2, When handleUserAuthentication is called, Then Case 2 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.NonEnrolled
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = biometricCrypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .launchBiometricSystemScreen()
    }

    // Case 3:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.Failure

    // Case 3 Expected Result:
    // resultHandler.onAuthenticationFailure called once.
    @Test
    fun `Given Case 3, When handleUserAuthentication is called, Then Case 3 expected result is returned`() {
        // Given
        val mockedOnAuthenticationFailure: () -> Unit = {}
        whenever(resultHandler.onAuthenticationFailure)
            .thenReturn(mockedOnAuthenticationFailure)

        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.Failure(
                errorMessage = mockedPlainFailureMessage
            )
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = biometricCrypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(resultHandler, times(1))
            .onAuthenticationFailure
    }

    //endregion

    //region resumeOpenId4VciWithAuthorization
    @Test
    fun `when interactor resumeOpenId4VciWithAuthorization is called, then resumeOpenId4VciWithAuthorization should be invoked on the controller`() {
        interactor.resumeOpenId4VciWithAuthorization(mockedUriPath1)

        verify(walletCoreDocumentsController, times(1))
            .resumeOpenId4VciWithAuthorization(mockedUriPath1)
    }
    //endregion

    //region helper functions
    private fun mockGetMainPidDocumentCall(mainPid: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(mainPid)
    }

    private fun mockWalletDocumentsControllerResolveOfferEventEmission(event: ResolveDocumentOfferPartialState) {
        whenever(walletCoreDocumentsController.resolveDocumentOffer(mockedUriPath1))
            .thenReturn(event.toFlow())
    }

    private fun mockWalletDocumentsControllerIssueByUriEventEmission(
        event: IssueDocumentsPartialState,
        txCode: String? = mockedTxCode
    ) {
        whenever(
            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = mockedUriPath1,
                txCode = txCode
            )
        ).thenReturn(event.toFlow())
    }

    private fun mockBiometricsAvailabilityResponse(response: BiometricsAvailability) {
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(listener = any()))
            .thenAnswer {
                val bioAvailability = it.getArgument<(BiometricsAvailability) -> Unit>(0)
                bioAvailability(response)
            }
    }

    private fun mockDeferredDocumentsMap(): Map<String, String> {
        val mockDeferredPendingDocId1 = mockedPendingPidUi.documentId
        val mockDeferredPendingType1 = mockedPendingPidUi.documentIdentifier.formatType

        val mockDeferredPendingDocId2 = mockedPendingMdlUi.documentId
        val mockDeferredPendingType2 = mockedPendingMdlUi.documentIdentifier.formatType

        return mapOf(
            mockDeferredPendingDocId1 to mockDeferredPendingType1,
            mockDeferredPendingDocId2 to mockDeferredPendingType2
        )
    }

    private fun mockIssuanceDocumentOfferDeferredSuccessStrings() {
        whenever(resourceProvider.getString(R.string.issuance_document_offer_deferred_success_text))
            .thenReturn(mockedSuccessText)
        whenever(resourceProvider.getString(R.string.issuance_document_offer_deferred_success_primary_button_text))
            .thenReturn(mockedPrimaryButtonText)
    }

    private fun mockOffer(
        issuerName: String,
        offeredDocuments: List<Offer.OfferedDocument> = listOf(),
        txCodeSpec: TxCode? = mockOfferTxCodeSpec(),
        docType: String = mockedOfferedDocumentDocType,
    ): Offer {
        return mock(Offer::class.java).apply {
            whenever(this.offeredDocuments).thenReturn(offeredDocuments)
            whenever(this.txCodeSpec).thenReturn(txCodeSpec)
            whenever(this.issuerMetadata).thenReturn(
                mockCredentialIssuerMetadata(
                    issuerName,
                    docType
                )
            )
        }
    }

    private fun mockCredentialIssuerMetadata(
        issuerName: String,
        docType: String
    ): CredentialIssuerMetadata {
        return CredentialIssuerMetadata(
            credentialIssuerIdentifier = CredentialIssuerId.invoke(mockHttpUrl).getOrThrow(),
            credentialEndpoint = CredentialIssuerEndpoint(URL(mockHttpUrl)),
            credentialConfigurationsSupported = mapOf(
                CredentialConfigurationIdentifier("identifier") to MsoMdocCredential(
                    docType = docType,
                    isoPolicy = null
                )
            ),
            display = listOf(
                Display(
                    name = issuerName,
                    locale = mockedDefaultLocale.language
                )
            )
        )
    }

    private fun mockOfferedDocument(
        name: String = mockedOfferedDocumentName,
        docType: String = mockedOfferedDocumentDocType,
        display: List<eu.europa.ec.eudi.openid4vci.Display> = listOf(
            eu.europa.ec.eudi.openid4vci.Display(
                name = name,
                locale = mockedDefaultLocale
            )
        )
    ): Offer.OfferedDocument {
        return mock(Offer.OfferedDocument::class.java).apply {
            whenever(this.documentFormat).thenReturn(MsoMdocFormat(docType))
            whenever(this.configuration).thenReturn(
                MsoMdocCredential(
                    docType = mockedOfferedDocumentDocType,
                    isoPolicy = null,
                    display = display
                )
            )
        }
    }

    private fun mockOfferTxCodeSpec(
        inputMode: TxCodeInputMode = TxCodeInputMode.NUMERIC,
        length: Int? = mockedTxCodeFourDigits,
        description: String? = null
    ): TxCode {
        return TxCode(inputMode, length, description)
    }
    //endregion

    //region mocked objects
    private val mockedOfferedDocumentsList = listOf(
        mockOfferedDocument()
    )

    private val mockHttpUrl = "https://issuer.eudiw.dev"

    private val mockedTripleObject by lazy {
        Triple(
            first = SuccessUIConfig.TextElementsConfig(
                text = mockedSuccessText,
                description = mockedSuccessDescription,
                color = ThemeColors.success
            ),
            second = SuccessUIConfig.ImageConfig(),
            third = mockedPrimaryButtonText
        )
    }

    private val mockedSuccessUiConfig by lazy {
        SuccessUIConfig(
            textElementsConfig = mockedTripleObject.first,
            imageConfig = mockedTripleObject.second,
            buttonConfig = listOf(
                SuccessUIConfig.ButtonConfig(
                    text = mockedTripleObject.third,
                    style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                    navigation = mockedConfigNavigationTypePop
                )
            ),
            onBackScreenToNavigate = mockedConfigNavigationTypePop
        )
    }
    //endregion
}