/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.issuancefeature.interactor

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.commonfeature.config.IssuanceFlowType
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.FetchScopedDocumentsPartialState
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.controller.IssueDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.issuancefeature.util.mockedAgeOptionItemUi
import eu.europa.ec.issuancefeature.util.mockedConfigNavigationTypePopToScreen
import eu.europa.ec.issuancefeature.util.mockedConfigNavigationTypePush
import eu.europa.ec.issuancefeature.util.mockedMdlOptionItemUi
import eu.europa.ec.issuancefeature.util.mockedPhotoIdOptionItemUi
import eu.europa.ec.issuancefeature.util.mockedPidOptionItemUi
import eu.europa.ec.issuancefeature.util.mockedPrimaryButtonText
import eu.europa.ec.issuancefeature.util.mockedRouteArguments
import eu.europa.ec.issuancefeature.util.mockedScopedDocuments
import eu.europa.ec.issuancefeature.util.mockedSuccessContentDescription
import eu.europa.ec.issuancefeature.util.mockedSuccessDescription
import eu.europa.ec.issuancefeature.util.mockedSuccessText
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedMdocPidFormat
import eu.europa.ec.testfeature.util.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testfeature.util.mockedUriPath1
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
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestAddDocumentInteractor {

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
    private lateinit var context: Context

    @Mock
    private lateinit var resultHandler: DeviceAuthenticationResult

    private lateinit var interactor: AddDocumentInteractor

    private lateinit var closeable: AutoCloseable

    private lateinit var crypto: BiometricCrypto

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = AddDocumentInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            deviceAuthenticationInteractor = deviceAuthenticationInteractor,
            resourceProvider = resourceProvider,
            uiSerializer = uiSerializer
        )

        crypto = BiometricCrypto(cryptoObject = null)

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getAddDocumentOption

    // Case 1:
    // 1. flowType == IssuanceFlowType.NoDocument
    // 2. walletCoreDocumentsController.getScopedDocuments() returns a non-empty list

    // Case 1 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. a PID
    @Test
    fun `Given Case 1, When getAddDocumentOption is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val expectedResponse = FetchScopedDocumentsPartialState.Success(
                documents = mockedScopedDocuments
            )
            mockGetScopedDocumentsResponse(expectedResponse)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.NoDocument
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedPidOptionItemUi
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // 1. flowType == IssuanceFlowType.ExtraDocument with formatType == null
    // 2. walletCoreDocumentsController.getScopedDocuments() returns a non-empty list

    // Case 2 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. an Age Verification,
    // 2. a PID,
    // 3. an mDL,
    // 4. a Photo ID
    @Test
    fun `Given Case 2, When getAddDocumentOption is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val expectedResponse = FetchScopedDocumentsPartialState.Success(
                documents = mockedScopedDocuments
            )
            mockGetScopedDocumentsResponse(expectedResponse)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.ExtraDocument(
                    formatType = null
                )
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedAgeOptionItemUi,
                            mockedPidOptionItemUi,
                            mockedMdlOptionItemUi,
                            mockedPhotoIdOptionItemUi
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // 1. flowType == IssuanceFlowType.ExtraDocument with formatType == PID format type
    // 2. walletCoreDocumentsController.getScopedDocuments() returns an empty list

    // Case 3 Expected Result:
    // AddDocumentInteractorPartialState.NoOptions state.
    @Test
    fun `Given Case 3, When getAddDocumentOption is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val expectedResponse = FetchScopedDocumentsPartialState.Success(
                documents = emptyList()
            )
            mockGetScopedDocumentsResponse(expectedResponse)

            val noDocumentsMsg = mockNoDocumentsString()

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.ExtraDocument(
                    formatType = mockedMdocPidFormat.docType
                )
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.NoOptions(
                        errorMsg = noDocumentsMsg
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // 1. flowType == IssuanceFlowType.ExtraDocument with a non-matching formatType ("NO_MATCHES_FORMAT_TYPE")
    // 2. walletCoreDocumentsController.getScopedDocuments() returns a non-empty list

    // Case 4 Expected Result:
    // AddDocumentInteractorPartialState.NoOptions state.
    @Test
    fun `Given Case 4, When getAddDocumentOption is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val expectedResponse = FetchScopedDocumentsPartialState.Success(
                documents = mockedScopedDocuments
            )
            mockGetScopedDocumentsResponse(expectedResponse)

            val noDocumentsMsg = mockNoDocumentsString()

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.ExtraDocument(
                    formatType = "NO_MATCHES_FORMAT_TYPE"
                )
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.NoOptions(
                        errorMsg = noDocumentsMsg
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 5:
    // 1. flowType == IssuanceFlowType.NoDocument
    // 2. walletCoreDocumentsController.getScopedDocuments() returns only non-PID documents

    // Case 5 Expected Result:
    // AddDocumentInteractorPartialState.NoOptions state.
    @Test
    fun `Given Case 5, When getAddDocumentOption is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val expectedResponse = FetchScopedDocumentsPartialState.Success(
                documents = mockedScopedDocuments
                    .filterNot {
                        it.isPid
                    }
            )
            mockGetScopedDocumentsResponse(expectedResponse)

            val noDocumentsMsg = mockNoDocumentsString()

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.NoDocument
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.NoOptions(
                        errorMsg = noDocumentsMsg
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 6:
    // 1. flowType == IssuanceFlowType.NoDocument
    // 2. walletCoreDocumentsController.getScopedDocuments() returns
    //    FetchScopedDocumentsPartialState.Failure with an error message.

    // Case 6 Expected Result:
    // AddDocumentInteractorPartialState.Failure with the same error message.
    @Test
    fun `Given Case 6, When getAddDocumentOption is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val expectedResponse = FetchScopedDocumentsPartialState.Failure(
                errorMessage = mockedPlainFailureMessage
            )
            mockGetScopedDocumentsResponse(expectedResponse)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.NoDocument
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Failure(
                        error = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 7:
    // 1. flowType == IssuanceFlowType.NoDocument
    // 2. walletCoreDocumentsController.getScopedDocuments() throws an exception with no error message.

    // Case 7 Expected Result:
    // AddDocumentInteractorPartialState.Failure with the generic error message.
    @Test
    fun `Given Case 7, When getAddDocumentOption is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.getScopedDocuments(any())
            ).thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.NoDocument
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 8:
    // 1. flowType == IssuanceFlowType.NoDocument
    // 2. walletCoreDocumentsController.getScopedDocuments() throws an exception with an error message.

    // Case 8 Expected Result:
    // AddDocumentInteractorPartialState.Failure with the exception's error message.
    @Test
    fun `Given Case 8, When getAddDocumentOption is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreDocumentsController.getScopedDocuments(any())
            ).thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowType.NoDocument
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    //region issueDocument
    @Test
    fun `Given an issuance method and a document type, When issueDocument is called, Then it calls walletCoreDocumentsController#issueDocument`() {
        coroutineRule.runTest {
            // Given
            val mockedIssuanceMethod = IssuanceMethod.OPENID4VCI
            val mockedConfigId = "id"

            whenever(
                walletCoreDocumentsController.issueDocument(
                    issuanceMethod = mockedIssuanceMethod,
                    configId = mockedConfigId
                )
            ).thenReturn(IssueDocumentPartialState.Success(mockedPidId).toFlow())

            // When
            interactor.issueDocument(
                issuanceMethod = mockedIssuanceMethod,
                configId = mockedConfigId
            ).runFlowTest {
                awaitItem()

                // Then
                verify(walletCoreDocumentsController, times(1))
                    .issueDocument(
                        issuanceMethod = mockedIssuanceMethod,
                        configId = mockedConfigId
                    )
            }
        }
    }
    //endregion

    //region handleUserAuth

    // Case 1:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.CanAuthenticate

    // Case 1 Expected Result:
    // deviceAuthenticationInteractor.authenticateWithBiometrics called once.
    @Test
    fun `Given Case 1, When handleUserAuth is called, Then Case 1 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.CanAuthenticate
        )

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(
                context,
                crypto,
                mockedNotifyOnAuthenticationFailure,
                resultHandler
            )
    }

    // Case 2:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.NonEnrolled

    // Case 2 Expected Result:
    // deviceAuthenticationInteractor.launchBiometricSystemScreen called once.
    @Test
    fun `Given Case 2, When handleUserAuth is called, Then Case 2 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.NonEnrolled
        )

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .launchBiometricSystemScreen()
    }

    // Case 3:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.Failed

    // Case 3 Expected Result:
    // resultHandler.onAuthenticationFailure called once.
    @Test
    fun `Given Case 3, When handleUserAuth is called, Then Case 3 expected result is returned`() {
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
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(resultHandler, times(1))
            .onAuthenticationFailure
    }
    //endregion

    //region buildGenericSuccessRouteForDeferred

    // Case 1:
    // 1. ConfigNavigation with NavigationType.PushRoute
    // 2. string resources mocked
    @Test
    fun `Given Case 1, When buildGenericSuccessRouteForDeferred is called, Then the expected string result is returned`() {
        // Given
        mockDocumentIssuanceStrings()

        val config = SuccessUIConfig(
            textElementsConfig = mockedTripleObject.first,
            imageConfig = mockedTripleObject.second,
            buttonConfig = listOf(
                SuccessUIConfig.ButtonConfig(
                    text = mockedTripleObject.third,
                    style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                    navigation = mockedConfigNavigationTypePush
                )
            ),
            onBackScreenToNavigate = mockedConfigNavigationTypePush
        )

        whenever(
            uiSerializer.toBase64(
                model = config,
                parser = SuccessUIConfig.Parser
            )
        ).thenReturn(mockedRouteArguments)

        val flowType = IssuanceFlowType.NoDocument

        // When
        val result = interactor.buildGenericSuccessRouteForDeferred(flowType = flowType)

        // Then
        val expectedResult = "SUCCESS?successConfig=$mockedRouteArguments"
        assertEquals(expectedResult, result)
    }

    // Case 2:
    // 1. ConfigNavigation with NavigationType.PopRoute
    // 2. string resources mocked
    @Test
    fun `When buildGenericSuccessRouteForDeferred (PopRoute) is called, then the expected string result is returned`() {
        // Given
        mockDocumentIssuanceStrings()

        val config = SuccessUIConfig(
            textElementsConfig = mockedTripleObject.first,
            imageConfig = mockedTripleObject.second,
            buttonConfig = listOf(
                SuccessUIConfig.ButtonConfig(
                    text = mockedTripleObject.third,
                    style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                    navigation = mockedConfigNavigationTypePopToScreen
                )
            ),
            onBackScreenToNavigate = mockedConfigNavigationTypePopToScreen
        )

        whenever(
            uiSerializer.toBase64(
                model = config,
                parser = SuccessUIConfig.Parser
            )
        ).thenReturn(mockedRouteArguments)

        val flowType = IssuanceFlowType.ExtraDocument(
            formatType = null
        )

        // When
        val result = interactor.buildGenericSuccessRouteForDeferred(flowType = flowType)

        // Then
        val expectedResult = "SUCCESS?successConfig=$mockedRouteArguments"
        assertEquals(expectedResult, result)
    }
    //endregion

    //region resumeOpenId4VciWithAuthorization

    // Case of resumeOpenId4VciWithAuthorization being called on the interactor
    // the expected result is the resumeOpenId4VciWithAuthorization function to be executed on
    // the walletCoreDocumentsController
    @Test
    fun `When interactor resumeOpenId4VciWithAuthorization is called, Then resumeOpenId4VciWithAuthorization should be invoked on the controller`() {
        // When
        interactor.resumeOpenId4VciWithAuthorization(mockedUriPath1)

        verify(walletCoreDocumentsController, times(1))
            .resumeOpenId4VciWithAuthorization(mockedUriPath1)
    }
    //endregion

    //region helper functions
    private suspend fun mockGetScopedDocumentsResponse(response: FetchScopedDocumentsPartialState) {
        whenever(
            walletCoreDocumentsController.getScopedDocuments(any())
        ).thenReturn(response)
    }

    private fun mockBiometricsAvailabilityResponse(response: BiometricsAvailability) {
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(listener = any()))
            .thenAnswer {
                val bioAvailability = it.getArgument<(BiometricsAvailability) -> Unit>(0)
                bioAvailability(response)
            }
    }

    private fun mockNoDocumentsString(): String {
        val noDocumentsMsg = "No available documents"

        whenever(
            resourceProvider.getString(R.string.issuance_add_document_no_options)
        ).thenReturn(noDocumentsMsg)

        return noDocumentsMsg
    }

    private fun mockDocumentIssuanceStrings() {
        whenever(resourceProvider.getString(R.string.issuance_add_document_deferred_success_text))
            .thenReturn(mockedSuccessText)
        whenever(resourceProvider.getString(R.string.issuance_add_document_deferred_success_primary_button_text))
            .thenReturn(mockedPrimaryButtonText)
        whenever(resourceProvider.getString(AppIcons.InProgress.contentDescriptionId))
            .thenReturn(mockedSuccessContentDescription)
        whenever(resourceProvider.getString(R.string.issuance_add_document_deferred_success_description))
            .thenReturn(mockedSuccessDescription)
    }
    //endregion

    //region mocked objects
    private val mockedTripleObject by lazy {
        Triple(
            first = SuccessUIConfig.TextElementsConfig(
                text = resourceProvider.getString(R.string.issuance_add_document_deferred_success_text),
                description = resourceProvider.getString(R.string.issuance_add_document_deferred_success_description),
                color = ThemeColors.pending
            ),
            second = SuccessUIConfig.ImageConfig(
                type = SuccessUIConfig.ImageConfig.Type.Drawable(icon = AppIcons.InProgress),
                tint = ThemeColors.primary,
                screenPercentageSize = PERCENTAGE_25,
            ),
            third = resourceProvider.getString(R.string.issuance_add_document_deferred_success_primary_button_text)
        )
    }
    //endregion
}