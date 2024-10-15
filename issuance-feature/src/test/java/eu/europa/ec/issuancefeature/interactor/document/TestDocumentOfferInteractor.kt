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
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.commonfeature.util.TestsData.mockedConfigNavigationTypePop
import eu.europa.ec.commonfeature.util.TestsData.mockedDocUiNamePid
import eu.europa.ec.commonfeature.util.TestsData.mockedDocumentId
import eu.europa.ec.commonfeature.util.TestsData.mockedInvalidCodeFormatMessage
import eu.europa.ec.commonfeature.util.TestsData.mockedIssuanceErrorMessage
import eu.europa.ec.commonfeature.util.TestsData.mockedIssuerName
import eu.europa.ec.commonfeature.util.TestsData.mockedOfferTxCodeSpecFourDigits
import eu.europa.ec.commonfeature.util.TestsData.mockedOfferedDocumentDocType
import eu.europa.ec.commonfeature.util.TestsData.mockedOfferedDocumentName
import eu.europa.ec.commonfeature.util.TestsData.mockedPendingMdlUi
import eu.europa.ec.commonfeature.util.TestsData.mockedPendingPidUi
import eu.europa.ec.commonfeature.util.TestsData.mockedPrimaryButtonText
import eu.europa.ec.commonfeature.util.TestsData.mockedRouteArguments
import eu.europa.ec.commonfeature.util.TestsData.mockedSuccessContentDescription
import eu.europa.ec.commonfeature.util.TestsData.mockedSuccessSubtitle
import eu.europa.ec.commonfeature.util.TestsData.mockedSuccessTitle
import eu.europa.ec.commonfeature.util.TestsData.mockedTxCode
import eu.europa.ec.commonfeature.util.TestsData.mockedTxCodeSpecFourDigits
import eu.europa.ec.commonfeature.util.TestsData.mockedUriPath1
import eu.europa.ec.commonfeature.util.TestsData.mockedWalletActivationErrorMessage
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.ResolveDocumentOfferPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer.TxCodeSpec
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedMainPid
import eu.europa.ec.testfeature.mockedPidDocType
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.serializer.UiSerializer
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
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
                    issuerName = mockedOffer.issuerName
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
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = mockedOfferedDocumentsList,
                txCodeSpec = mockedOfferTxCodeSpecFourDigits
            )
            mockGetMainPidDocumentCall(
                mainPid = mockedMainPid
            )
            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedList = mockedOfferedDocumentsList.map {
                    DocumentItemUi(title = mockedOfferedDocumentName)
                }
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Success(
                    documents = expectedList,
                    issuerName = mockedIssuerName,
                    txCodeLength = mockedOfferTxCodeSpecFourDigits.length
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
            val mockedTxCodeSpecLength = 2
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = mockedOfferedDocumentsList,
                txCodeSpec = mockOfferTxCodeSpec(
                    inputMode = TxCodeSpec.InputMode.NUMERIC,
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
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = listOf(
                    mockOfferedDocument(
                        name = mockedOfferedDocumentName,
                        docType = DocumentIdentifier.SAMPLE.docType
                    )
                ),
                txCodeSpec = mockedOfferTxCodeSpecFourDigits
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
            val mockedOffer = mockOffer(
                issuerName = mockedIssuerName,
                offeredDocuments = mockedOfferedDocumentsList,
                txCodeSpec = mockedOfferTxCodeSpecFourDigits
            )
            whenever(resourceProvider.getString(R.string.pid))
                .thenReturn(mockedOfferedDocumentName)

            mockGetMainPidDocumentCall(
                mainPid = mockedMainPid
            )
            mockWalletDocumentsControllerResolveOfferEventEmission(
                event = ResolveDocumentOfferPartialState.Success(mockedOffer)
            )

            // When
            interactor.resolveDocumentOffer(mockedUriPath1).runFlowTest {
                val expectedDocumentsUiList = listOf(
                    DocumentItemUi(mockedOfferedDocumentName)
                )
                val expectedResult = ResolveDocumentOfferInteractorPartialState.Success(
                    documents = expectedDocumentsUiList,
                    issuerName = mockedIssuerName,
                    txCodeLength = mockedOffer.txCodeSpec?.length
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

    //endregion

    //region issueDocuments
    // Case 1:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri() is called with:
    // parameters being mocked
    // 2. A mockedExceptionWithMessage (RuntimeException) is thrown with message

    // Case 1 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - errorMessage equal to the localized message of mockedExceptionWithMessage
    @Test
    fun `Given Case 1, When issueDocuments is called, Then Case 1 Expected Result is returned`() =
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

    // Case 2:
    // 1. walletCoreDocumentsController.issueDocumentsByOfferUri() is called with:
    // parameters being mocked
    // 2. A mockedExceptionWithNoMessage (RuntimeException) is thrown

    // Case 2 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - a mockedGenericErrorMessage
    @Test
    fun `Given Case 2, When issueDocuments is called, Then Case 2 Expected Result is returned`() =
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

    // Case 3:
    // 1. mockWalletDocumentsControllerIssueByUriEventEmission() emits
    // IssueDocumentsPartialState.Failure with:
    // mockedPlainFailureMessage as the error message

    // Case 3 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - errorMessage equal to mockedPlainFailureMessage
    @Test
    fun `Given Case 3, When issueDocuments is called, Then Case 3 Expected Result is returned`() =
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

                val expectedResult = IssueDocumentsInteractorPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // 1. mockWalletDocumentsControllerIssueByUriEventEmission() emits
    // IssueDocumentsPartialState.UserAuthRequired with:
    // biometricCrypto object and resultHandler as DeviceAuthenticationResult
    // 2. required arguments are mocked

    // Case 4 Expected Result:
    // IssueDocumentsInteractorPartialState.UserAuthRequired state, with parameters of:
    // - biometricCrypto object and resultHandler as DeviceAuthenticationResult
    @Test
    fun `Given Case 4, When issueDocuments is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(resourceProvider.getString(R.string.issuance_generic_error))
                .thenReturn(mockedIssuanceErrorMessage)
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_success_subtitle,
                    mockedIssuerName
                )
            ).thenReturn(mockedSuccessSubtitle)

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
                val expectedResult = IssueDocumentsInteractorPartialState.UserAuthRequired(
                    crypto = biometricCrypto,
                    resultHandler = resultHandler
                )
                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 5:
    // 1. mockWalletDocumentsControllerIssueByUriEventEmission() emits
    // IssueDocumentsPartialState.Success with:
    // biometricCrypto object and resultHandler as DeviceAuthenticationResult
    // 2. required strings are mocked
    // 3. uiSerializer.toBase64() serializes the mockedSuccessUiConfig into mockedArguments

    // Case 5 Expected Result:
    // IssueDocumentsInteractorPartialState.Success state, with:
    // - successRoute equal to "SUCCESS?successConfig=mockedArguments"
    @Test
    fun `Given Case 5, When issueDocuments is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.Success(
                    documentIds = listOf(mockedDocumentId)
                )
            )

            mockIssuanceDocumentOfferSuccessStrings()
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_success_subtitle,
                    mockedIssuerName
                )
            ).thenReturn(mockedSuccessSubtitle)

            val mockedSuccessUiConfig = SuccessUIConfig(
                headerConfig = mockedTripleObject.first,
                content = mockedSuccessSubtitle,
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
                    successRoute = "SUCCESS?successConfig=$mockedRouteArguments"
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 6:
    // 1. mockWalletDocumentsControllerIssueByUriEventEmission() emits
    // IssueDocumentsPartialState.DeferredSuccess with:
    // mocked deferred documents
    // 2. required strings are mocked
    // 3. triple object with warning tint color
    // 4. uiSerializer.toBase64() serializes the mockedSuccessUiConfig into mockedRouteArguments

    // Case 6 Expected Result:
    // IssueDocumentsInteractorPartialState.DeferredSuccess state, with:
    // - successRoute equal to "SUCCESS?successConfig=mockedArguments"
    @Test
    fun `Given Case 6, When issueDocuments is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_deferred_success_subtitle,
                    mockedIssuerName
                )
            ).thenReturn(mockedSuccessSubtitle)

            mockIssuanceDocumentOfferDeferredSuccessStrings()
            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.DeferredSuccess(
                    deferredDocuments = mockDeferredDocumentsMap()
                )
            )

            val mockedTripleObject = Triple(
                first = SuccessUIConfig.HeaderConfig(
                    title = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_title),
                    color = ThemeColors.warning
                ),
                second = SuccessUIConfig.ImageConfig(
                    type = SuccessUIConfig.ImageConfig.Type.DRAWABLE,
                    drawableRes = AppIcons.ClockTimer.resourceId,
                    tint = ThemeColors.warning,
                    contentDescription = resourceProvider.getString(AppIcons.ClockTimer.contentDescriptionId)
                ),
                third = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_primary_button_text)
            )

            val config = SuccessUIConfig(
                headerConfig = mockedTripleObject.first,
                content = mockedSuccessSubtitle,
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

    // Case 7:
    // 1. mockWalletDocumentsControllerIssueByUriEventEmission() emits
    //    IssueDocumentsPartialState.PartialSuccess with:
    //    - documentIds containing mockedDocumentId.
    //    - nonIssuedDocuments map containing mockDeferredPendingDocId1 to mockDeferredPendingType1
    //      and mockDeferredPendingDocId2 to mockDeferredPendingType2.
    // 2. nonIssuedDocsNames is formed by combining the document types of non-issued documents:
    //    "eu.europa.ec.eudi.pid.1, org.iso.18013.5.1.mDL"
    // 3. mocked string resources
    // 7. uiSerializer.toBase64() serializes the SuccessUIConfig object into mockedArguments.

    // Case 7 Expected Result:
    // IssueDocumentsInteractorPartialState.Success state, with:
    // - successRoute equal to "SUCCESS?successConfig=mockedArguments"
    @Test
    fun `Given Case 7, When issueDocuments is called, Then Case 7 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId1 = mockedPendingPidUi.documentId
            val mockDeferredPendingType1 = mockedPendingPidUi.documentIdentifier.docType

            val mockDeferredPendingDocId2 = mockedPendingMdlUi.documentId
            val mockDeferredPendingType2 = mockedPendingMdlUi.documentIdentifier.docType

            val nonIssuedDeferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockDeferredPendingDocId1 to mockDeferredPendingType1,
                mockDeferredPendingDocId2 to mockDeferredPendingType2
            )

            val nonIssuedDocsNames =
                "${mockedPendingPidUi.documentIdentifier.docType}, ${mockedPendingMdlUi.documentIdentifier.docType}"

            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_partial_success_subtitle,
                    mockedIssuerName,
                    nonIssuedDocsNames
                )
            ).thenReturn(mockedSuccessSubtitle)

            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf(mockedDocumentId),
                    nonIssuedDocuments = nonIssuedDeferredDocuments
                )
            )

            mockIssuanceDocumentOfferSuccessStrings()
            whenever(resourceProvider.getString(R.string.content_description_success))
                .thenReturn(mockedSuccessContentDescription)

            val config = SuccessUIConfig(
                headerConfig = mockedTripleObject.first,
                content = mockedSuccessSubtitle,
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
                    successRoute = "SUCCESS?successConfig=$mockedRouteArguments"
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 8:
    // 1. IssueDocumentsPartialState.PartialSuccess is returned by issueDocumentsByOfferUri
    // 2. The interactor is called with the given offerUri, issuerName, navigation and txCode.

    // Case 8 Expected Result:
    // IssueDocumentsInteractorPartialState.Success state, with:
    // - successRoute equal to "SUCCESS?successConfig=mockedArguments".
    @Test
    fun `Given Case 8, When issueDocuments is called, Then Case 8 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockedDeferredPendingDocId1 = mockedPidDocType
            val mockDeferredPendingType1 = mockedPendingPidUi.documentIdentifier.docType
            val nonIssuedDeferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockedDeferredPendingDocId1 to mockDeferredPendingType1
            )

            val nonIssuedDocsNames = mockedDocUiNamePid
            whenever(resourceProvider.getString(R.string.pid)).thenReturn(nonIssuedDocsNames)
            whenever(
                resourceProvider.getString(
                    R.string.issuance_document_offer_partial_success_subtitle,
                    mockedIssuerName,
                    nonIssuedDocsNames
                )
            ).thenReturn(mockedSuccessSubtitle)

            mockWalletDocumentsControllerIssueByUriEventEmission(
                event = IssueDocumentsPartialState.PartialSuccess(
                    documentIds = listOf(mockedDocumentId),
                    nonIssuedDocuments = nonIssuedDeferredDocuments
                )
            )

            mockIssuanceDocumentOfferSuccessStrings()
            whenever(resourceProvider.getString(R.string.content_description_success))
                .thenReturn(mockedSuccessContentDescription)

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
                    successRoute = "SUCCESS?successConfig=$mockedRouteArguments"
                )

                // Then
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 9:
    // 1. IssueDocumentsPartialState.Failure is returned by issueDocumentsByOfferUri
    // 2. The controller issueDocumentsByOfferUri is called with a mocked offerUri and null txCode

    // Case 9 Expected Result:
    // IssueDocumentsInteractorPartialState.Failure state, with:
    // - errorMessage equal to mockedPlainFailureMessage.
    @Test
    fun `Given Case 9, When issueDocuments is called, Then Case 9 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val failureResponse =
                IssueDocumentsPartialState.Failure(errorMessage = mockedPlainFailureMessage)
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

    //endregion

    //region handleUserAuthentication
    //
    // Case 1:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.CanAuthenticate

    // Case 1 Expected Result:
    // deviceAuthenticationInteractor.authenticateWithBiometrics called once.
    @Test
    fun `Given case 1, When handleUserAuthentication is called, Then Case 1 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.CanAuthenticate
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = biometricCrypto,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(
                context = context,
                crypto = biometricCrypto,
                resultHandler = resultHandler
            )
    }

    // Case 2:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.NonEnrolled

    // Case 2 Expected Result:
    // deviceAuthenticationInteractor.authenticateWithBiometrics called once.
    @Test
    fun `Given case 2, When handleUserAuthentication is called, Then Case 2 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.NonEnrolled
        )

        // When
        interactor.handleUserAuthentication(
            context = context,
            crypto = biometricCrypto,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(
                context = context,
                crypto = biometricCrypto,
                resultHandler = resultHandler
            )
    }

    // Case 3:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.Failure

    // Case 3 Expected Result:
    // resultHandler.onAuthenticationFailure called once.
    @Test
    fun `Given case 3, When handleUserAuthentication is called, Then Case 3 expected result is returned`() {
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
        val mockDeferredPendingType1 = mockedPendingPidUi.documentIdentifier.docType

        val mockDeferredPendingDocId2 = mockedPendingMdlUi.documentId
        val mockDeferredPendingType2 = mockedPendingMdlUi.documentIdentifier.docType

        return mapOf(
            mockDeferredPendingDocId1 to mockDeferredPendingType1,
            mockDeferredPendingDocId2 to mockDeferredPendingType2
        )
    }

    private fun mockIssuanceDocumentOfferSuccessStrings() {
        whenever(resourceProvider.getString(R.string.issuance_document_offer_success_title))
            .thenReturn(mockedSuccessTitle)
        whenever(resourceProvider.getString(R.string.issuance_document_offer_success_primary_button_text))
            .thenReturn(mockedPrimaryButtonText)
    }

    private fun mockIssuanceDocumentOfferDeferredSuccessStrings() {
        whenever(resourceProvider.getString(R.string.issuance_document_offer_deferred_success_title))
            .thenReturn(mockedSuccessTitle)
        whenever(resourceProvider.getString(R.string.issuance_document_offer_deferred_success_primary_button_text))
            .thenReturn(mockedPrimaryButtonText)
    }

    private fun mockOffer(
        issuerName: String,
        offeredDocuments: List<Offer.OfferedDocument> = listOf(),
        txCodeSpec: TxCodeSpec? = mockOfferTxCodeSpec()
    ): Offer {
        return mock(Offer::class.java).apply {
            whenever(this.issuerName).thenReturn(issuerName)
            whenever(this.offeredDocuments).thenReturn(offeredDocuments)
            whenever(this.txCodeSpec).thenReturn(txCodeSpec)
        }
    }

    private fun mockOfferedDocument(
        name: String = mockedOfferedDocumentName,
        docType: String = mockedOfferedDocumentDocType
    ): Offer.OfferedDocument {
        return mock(Offer.OfferedDocument::class.java).apply {
            whenever(this.name).thenReturn(name)
            whenever(this.docType).thenReturn(docType)
        }
    }

    private fun mockOfferTxCodeSpec(
        inputMode: TxCodeSpec.InputMode = TxCodeSpec.InputMode.NUMERIC,
        length: Int? = mockedTxCodeSpecFourDigits,
        description: String? = null
    ): TxCodeSpec {
        return TxCodeSpec(inputMode, length, description)
    }
    //endregion

    //region mocked objects
    private val mockedOfferedDocumentsList =
        listOf(
            mockOfferedDocument(docType = DocumentIdentifier.SAMPLE.docType)
        )

    private val mockedTripleObject by lazy {
        Triple(
            first = SuccessUIConfig.HeaderConfig(
                title = resourceProvider.getString(R.string.issuance_document_offer_success_title),
                color = ThemeColors.success
            ),
            second = SuccessUIConfig.ImageConfig(
                type = SuccessUIConfig.ImageConfig.Type.DEFAULT,
                drawableRes = null,
                tint = ThemeColors.success,
                contentDescription = resourceProvider.getString(R.string.content_description_success)
            ),
            third =
            resourceProvider.getString(R.string.issuance_document_offer_success_primary_button_text)
        )
    }

    private val mockedSuccessUiConfig by lazy {
        SuccessUIConfig(
            headerConfig = mockedTripleObject.first,
            content = mockedSuccessSubtitle,
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