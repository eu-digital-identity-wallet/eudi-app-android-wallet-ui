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
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.commonfeature.util.TestsData.mockedAgeOptionItemUi
import eu.europa.ec.commonfeature.util.TestsData.mockedMdlOptionItemUi
import eu.europa.ec.commonfeature.util.TestsData.mockedPidId
import eu.europa.ec.commonfeature.util.TestsData.mockedPidOptionItemUi
import eu.europa.ec.commonfeature.util.TestsData.mockedSampleDataOptionItemUi
import eu.europa.ec.corelogic.controller.AddSampleDataPartialState
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.controller.IssueDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.MockResourceProviderForStringCalls.mockDocumentTypeUiToUiNameCall
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.serializer.UiSerializer
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
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
            uiSerializer = uiSerializer,
        )

        crypto = BiometricCrypto(cryptoObject = null)

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getAddDocumentOption

    // Case 1:
    // 1. flowType == IssuanceFlowUiConfig.NO_DOCUMENT

    // Case 1 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. a PID option, available to add.
    // 2. an mDL option, unavailable to add.
    // 3. a Load Sample Data option, available to add.
    @Test
    fun `Given Case 1, When getAddDocumentOption is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.NO_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedPidOptionItemUi,
                            mockedMdlOptionItemUi.copy(
                                available = false
                            ),
                            mockedAgeOptionItemUi.copy(
                                available = false
                            ),
                            mockedSampleDataOptionItemUi
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // 2. flowType == IssuanceFlowUiConfig.EXTRA_DOCUMENT

    // Case 2 Expected Result:
    // AddDocumentInteractorPartialState.Success state, with the following options:
    // 1. a PID option, available to add.
    // 2. an mDL option, available to add.
    // 3. no Load Sample Data option.
    @Test
    fun `Given Case 2, When getAddDocumentOption is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockDocumentTypeUiToUiNameCall(resourceProvider)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
            ).runFlowTest {
                // Then
                assertEquals(
                    AddDocumentInteractorPartialState.Success(
                        options = listOf(
                            mockedPidOptionItemUi,
                            mockedMdlOptionItemUi,
                            mockedAgeOptionItemUi
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // 1. resourceProvider.getString() throws an exception with a message.
    @Test
    fun `Given Case 3, When getAddDocumentOption is called, Then it returns Failure with exception's localized message`() {
        coroutineRule.runTest {
            // Given
            whenever(resourceProvider.getString(anyInt()))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
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

    // Case 4:
    // 1. resourceProvider.getString() throws an exception with no message.
    @Test
    fun `Given Case 4, When getAddDocumentOption is called, Then it returns Failure with the generic error message`() {
        coroutineRule.runTest {
            // Given
            whenever(resourceProvider.getString(anyInt()))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getAddDocumentOption(
                flowType = IssuanceFlowUiConfig.EXTRA_DOCUMENT
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
    //endregion

    //region issueDocument
    @Test
    fun `Given an issuance method and a document type, When issueDocument is called, Then it calls walletCoreDocumentsController#issueDocument`() {
        coroutineRule.runTest {
            // Given
            val mockedIssuanceMethod = IssuanceMethod.OPENID4VCI
            val mockedDocumentType = DocumentIdentifier.PID.docType

            whenever(
                walletCoreDocumentsController.issueDocument(
                    issuanceMethod = mockedIssuanceMethod,
                    documentType = mockedDocumentType
                )
            ).thenReturn(IssueDocumentPartialState.Success(mockedPidId).toFlow())

            // When
            interactor.issueDocument(
                issuanceMethod = mockedIssuanceMethod,
                documentType = mockedDocumentType
            ).runFlowTest {
                awaitItem()

                // Then
                verify(walletCoreDocumentsController, times(1))
                    .issueDocument(
                        issuanceMethod = mockedIssuanceMethod,
                        documentType = mockedDocumentType
                    )
            }
        }
    }
    //endregion

    //region addSampleData
    @Test
    fun `When addSampleData is called, Then it calls walletCoreDocumentsController#addSampleData`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.addSampleData())
                .thenReturn(AddSampleDataPartialState.Success.toFlow())

            // When
            interactor.addSampleData()
                .runFlowTest {
                    awaitItem()

                    // Then
                    verify(walletCoreDocumentsController, times(1))
                        .addSampleData()
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
    fun `Given case 1, When handleUserAuth is called, Then Case 1 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.CanAuthenticate
        )

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(context, crypto, resultHandler)
    }

    // Case 2:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.NonEnrolled

    // Case 2 Expected Result:
    // deviceAuthenticationInteractor.authenticateWithBiometrics called once.
    @Test
    fun `Given case 2, When handleUserAuth is called, Then Case 2 expected result is returned`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.NonEnrolled
        )

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(context, crypto, resultHandler)
    }

    // Case 3:
    // 1. deviceAuthenticationInteractor.getBiometricsAvailability returns:
    // BiometricsAvailability.Failed

    // Case 3 Expected Result:
    // resultHandler.onAuthenticationFailure called once.
    @Test
    fun `Given case 3, When handleUserAuth is called, Then Case 3 expected result is returned`() {
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
            resultHandler = resultHandler
        )

        // Then
        verify(resultHandler, times(1))
            .onAuthenticationFailure
    }
    //endregion

    //region helper functions
    private fun mockBiometricsAvailabilityResponse(response: BiometricsAvailability) {
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(listener = any()))
            .thenAnswer {
                val bioAvailability = it.getArgument<(BiometricsAvailability) -> Unit>(0)
                bioAvailability(response)
            }
    }
    //endregion
}