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

package eu.europa.ec.dashboardfeature.interactor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.Uri
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.util.TestsData.mockedFullDocumentsUi
import eu.europa.ec.commonfeature.util.TestsData.mockedMdlUiWithNoExpirationDate
import eu.europa.ec.commonfeature.util.TestsData.mockedMdlUiWithNoUserNameAndNoUserImage
import eu.europa.ec.commonfeature.util.TestsData.mockedNoExpirationDateFound
import eu.europa.ec.commonfeature.util.TestsData.mockedNoUserBase64PortraitFound
import eu.europa.ec.commonfeature.util.TestsData.mockedNoUserFistNameFound
import eu.europa.ec.commonfeature.util.TestsData.mockedPendingDocumentUi
import eu.europa.ec.commonfeature.util.TestsData.mockedUriPath1
import eu.europa.ec.commonfeature.util.TestsData.mockedUriPath2
import eu.europa.ec.commonfeature.util.TestsData.mockedUserBase64Portrait
import eu.europa.ec.commonfeature.util.TestsData.mockedUserFirstName
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.MockResourceProviderForStringCalls.mockDocumentTypeUiToUiNameCall
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedFullDocuments
import eu.europa.ec.testfeature.mockedFullPid
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedMainPid
import eu.europa.ec.testfeature.mockedMdlWithNoExpirationDate
import eu.europa.ec.testfeature.mockedMdlWithNoUserNameAndNoUserImage
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testfeature.mockedUnsignedDocument
import eu.europa.ec.testfeature.walletcore.getMockedEudiWalletConfig
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.base.getMockedContext
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBluetoothAdapter

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class TestDashboardInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var walletCoreConfig: WalletCoreConfig

    @Mock
    private lateinit var configLogic: ConfigLogic

    @Mock
    private lateinit var logController: LogController

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var shadowBluetoothAdapter: ShadowBluetoothAdapter

    private lateinit var interactor: DashboardInteractor

    private lateinit var closeable: AutoCloseable

    private lateinit var mockDocumentId: String
    private lateinit var mockDocumentName: String

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DashboardInteractorImpl(
            resourceProvider = resourceProvider,
            walletCoreDocumentsController = walletCoreDocumentsController,
            walletCoreConfig = walletCoreConfig,
            configLogic = configLogic,
            logController = logController
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(resourceProvider.provideContext()).thenReturn(getMockedContext())

        bluetoothManager =
            getMockedContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        shadowBluetoothAdapter = Shadows.shadowOf(bluetoothManager.adapter)

        mockDocumentId = "mockDocumentId"
        mockDocumentName = "mockDocumentName"
    }

    @After
    fun after() {
        closeable.close()
    }

    //region isBleAvailable

    // Case 1:
    // BluetoothAdapter.getDefaultAdapter()?.isEnabled returns true.
    @Test
    fun `Given Case 1, When isBleAvailable is called, Then it returns true`() {
        // Given
        val expectedBluetoothAdapterEnabled = true
        mockBluetoothAdapterEnabledState(enabled = expectedBluetoothAdapterEnabled)

        // When
        val actual = interactor.isBleAvailable()

        // Then
        assertEquals(expectedBluetoothAdapterEnabled, actual)
    }

    // Case 2:
    // BluetoothAdapter.getDefaultAdapter()?.isEnabled returns false.
    @Test
    fun `Given Case 2, When isBleAvailable is called, Then it returns false`() {
        // Given
        val expectedBluetoothAdapterEnabled = false
        mockBluetoothAdapterEnabledState(enabled = expectedBluetoothAdapterEnabled)

        // When
        val actual = interactor.isBleAvailable()

        // Then
        assertEquals(expectedBluetoothAdapterEnabled, actual)
    }
    //endregion

    //region isBleCentralClientModeEnabled

    // Case 1:
    // Configuration of Wallet Core has BLE_CLIENT_CENTRAL_MODE for its bleTransferMode.
    @Test
    fun `Given Case 1, When isBleCentralClientModeEnabled is called, Then it returns true`() {
        // Given
        val expectedBleCentralClientModeEnabled = true

        val mockedConfig = getMockedEudiWalletConfig {
            bleTransferMode(EudiWalletConfig.BLE_CLIENT_CENTRAL_MODE)
        }

        whenever(walletCoreConfig.config).thenReturn(mockedConfig)

        // When
        val actual = interactor.isBleCentralClientModeEnabled()

        // Then
        assertEquals(expectedBleCentralClientModeEnabled, actual)
    }

    // Case 2:
    // Configuration of Wallet Core has BLE_SERVER_PERIPHERAL_MODE for its bleTransferMode.
    @Test
    fun `Given Case 2, When isBleCentralClientModeEnabled is called, Then it returns false`() {
        // Given
        val expectedBleCentralClientModeEnabled = false

        val mockedConfig = getMockedEudiWalletConfig {
            bleTransferMode(EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE)
        }

        whenever(walletCoreConfig.config).thenReturn(mockedConfig)

        // When
        val actual = interactor.isBleCentralClientModeEnabled()

        // Then
        assertEquals(expectedBleCentralClientModeEnabled, actual)
    }
    //endregion

    // region deleteDocument
    // Case 1:
    // walletCoreDocumentsController.getAllDocuments() returns a list of Documents
    // with a size of two.

    // Case 1 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.SingleDocumentDeleted state.
    @Test
    fun `Given Case 1, When deleteDocument is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)
            assert(walletCoreDocumentsController.getAllDocuments().size == 2)
            mockDeleteDocumentCall()

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                val expectedFlow =
                    DashboardInteractorDeleteDocumentPartialState.SingleDocumentDeleted

                assertEquals(expectedFlow, awaitItem())
            }
        }
    }

    // Case 2:
    // walletCoreDocumentsController.getAllDocuments() returns returns an empty list.

    // Case 2 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.AllDocumentsDeleted state
    @Test
    fun `Given Case 2, When deleteDocument is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockDocumentsList = mock<List<Document>>()
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(mockDocumentsList)
            whenever(mockDocumentsList.isEmpty()).thenReturn(true)
            assert(walletCoreDocumentsController.getAllDocuments().isEmpty())

            mockDeleteDocumentCall()

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                val expectedFlow = DashboardInteractorDeleteDocumentPartialState.AllDocumentsDeleted

                // Then
                assertEquals(expectedFlow, awaitItem())
            }
        }
    }

    // Case 3:
    // RuntimeException was thrown when deleteDocument was called.

    // Case 3 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.Failure state.
    @Test
    fun `Given Case 3, When deleteDocument is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            //val exceptionMessage = "Unexpected error thrown"
            whenever(walletCoreDocumentsController.deleteDocument(mockDocumentId)).thenThrow(
                RuntimeException(
                    mockedPlainFailureMessage
                )
            )

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorDeleteDocumentPartialState.Failure(mockedPlainFailureMessage),
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given Case 3, When deleteDocument is called, Then Case 3 Expected Result is returned gf`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockDocumentId)).thenThrow(
                mockedExceptionWithMessage
            )

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorDeleteDocumentPartialState.Failure(
                        mockedExceptionWithMessage.message ?: ""
                    ),
                    awaitItem()
                )
            }
        }

    // Case 4:
    // DeleteDocumentPartialState.Failure was emitted when deleteDocument was called.

    // Case 4 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.Failure state.
    @Test
    fun `Given Case 4, When deleteDocument is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockDocumentId)).thenReturn(
                DeleteDocumentPartialState.Failure(mockedPlainFailureMessage).toFlow()
            )

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorDeleteDocumentPartialState.Failure(mockedPlainFailureMessage),
                    awaitItem()
                )
            }
        }

    // Case 5:
    // RuntimeException with error message was emitted when deleteDocument was called.

    // Case 5 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.Failure state.
    @Test
    fun `Given Case 5, When deleteDocument is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(any())).thenReturn(flow {
                throw RuntimeException(mockedPlainFailureMessage)
            })

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }

    // Case 6:
    // RuntimeException without error message was emitted when deleteDocument was called.

    // Case 6 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.Failure state.
    @Test
    fun `Given Case 6, When deleteDocument is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(any())).thenReturn(flow {
                throw RuntimeException()
            })

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }

    // end region

    // region tryIssuingDeferredDocumentsFlow
    // Case 1:
    // IssueDeferredDocumentPartialState.Issued was emitted when issueDeferredDocument was called.

    // Case 1 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result state.
    @Test
    fun `Given Case 1, When tryIssuingDeferredDocumentsFlow is called, Then Case 1 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val documentId: DocumentId = mockDocumentId
            val deferredDocuments = mapOf(documentId to DocumentIdentifier.SAMPLE.docType)
            val successData =
                DeferredDocumentData(documentId, DocumentIdentifier.SAMPLE.docType, mockDocumentName)

            whenever(walletCoreDocumentsController.issueDeferredDocument(any())).thenReturn(
                IssueDeferredDocumentPartialState.Issued(successData).toFlow()
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments)
                .runFlowTest {
                    // Then
                    val expectedResult =
                        DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                            successfullyIssuedDeferredDocuments = listOf(successData),
                            failedIssuedDeferredDocuments = listOf()
                        )
                    assertEquals(expectedResult, awaitItem())
                }
        }

    // Case 2:
    // IssueDeferredDocumentPartialState.Expired was emitted when issueDeferredDocument was called.

    // Case 2 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result state.
    @Test
    fun `Given Case 2, When tryIssuingDeferredDocumentsFlow is called, Then Case 2 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val documentId: DocumentId = mockDocumentId
            val deferredDocuments = mapOf(documentId to DocumentIdentifier.SAMPLE.docType)
            val mockExpiredDocumentId: DocumentId = "expiredDocumentId"

            whenever(walletCoreDocumentsController.issueDeferredDocument(any()))
                .thenReturn(
                    IssueDeferredDocumentPartialState.Expired(mockExpiredDocumentId).toFlow()
                )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments)
                .runFlowTest {
                    // Then
                    val expectedResult =
                        DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                            successfullyIssuedDeferredDocuments = listOf(),
                            failedIssuedDeferredDocuments = listOf()
                        )
                    assertEquals(expectedResult, awaitItem())
                }
        }

    // Case 3:
    // IssueDeferredDocumentPartialState.NotReady was emitted when issueDeferredDocument was called.

    // Case 3 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result state.
    @Test
    fun `Given Case 3, When tryIssuingDeferredDocumentsFlow is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            val documentId: DocumentId = mockDocumentId
            val deferredDocuments = mapOf(documentId to DocumentIdentifier.SAMPLE.docType)
            val successData = DeferredDocumentData(
                documentId,
                DocumentIdentifier.SAMPLE.docType,
                mockDocumentName
            )

            whenever(walletCoreDocumentsController.issueDeferredDocument(any()))
                .thenReturn(IssueDeferredDocumentPartialState.NotReady(successData).toFlow())

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                val expectedResult =
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                        successfullyIssuedDeferredDocuments = listOf(),
                        failedIssuedDeferredDocuments = listOf()
                    )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // IssueDeferredDocumentPartialState.Failed was emitted when issueDeferredDocument was called.

    // Case 4 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result state.
    @Test
    fun `Given Case 4, When tryIssuingDeferredDocumentsFlow is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val documentId: DocumentId = mockDocumentId
            val deferredDocuments = mapOf(documentId to DocumentIdentifier.SAMPLE.docType)
            whenever(walletCoreDocumentsController.issueDeferredDocument(any()))
                .thenReturn(
                    IssueDeferredDocumentPartialState.Failed(
                        documentId = documentId,
                        errorMessage = mockedPlainFailureMessage
                    ).toFlow()
                )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                val expectedResult =
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                        successfullyIssuedDeferredDocuments = listOf(),
                        failedIssuedDeferredDocuments = listOf(documentId)
                    )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 5:
    // RuntimeException was thrown when issueDeferredDocument was called.

    // Case 5 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure state.
    @Test
    fun `Given Case 5, When tryIssuingDeferredDocumentsFlow is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val documentId: DocumentId = mockDocumentId
            val deferredDocuments = mapOf(documentId to DocumentIdentifier.SAMPLE.docType)
            whenever(walletCoreDocumentsController.issueDeferredDocument(documentId)).thenThrow(
                RuntimeException(
                    mockedPlainFailureMessage
                )
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
                        mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }

    // Case 6:
    // RuntimeException was thrown when issueDeferredDocument was called.

    // Case 6 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure state.
    @Test
    fun `Given Case 6, When tryIssuingDeferredDocumentsFlow is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {

            // Given
            val documentId: DocumentId = mockDocumentId
            val deferredDocuments = mapOf(documentId to DocumentIdentifier.SAMPLE.docType)
            whenever(walletCoreDocumentsController.issueDeferredDocument(documentId)).thenThrow(
                mockedExceptionWithNoMessage
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
                        mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }

    // Case 7:
    // emptyFlow was returned when issueDeferredDocument was called.

    // Case 7 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result state.
    @Test
    fun `Given Case 7, When tryIssuingDeferredDocumentsFlow is called, Then Case 7 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val documentId: DocumentId = mockDocumentId
            val deferredDocuments = mapOf(documentId to DocumentIdentifier.SAMPLE.docType)
            whenever(walletCoreDocumentsController.issueDeferredDocument(documentId))
                .thenReturn(emptyFlow())

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                val expectedResult =
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                        successfullyIssuedDeferredDocuments = listOf(),
                        failedIssuedDeferredDocuments = listOf(documentId)
                    )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // end region

    //region getDocuments

    // Case 1:
    // walletCoreDocumentsController.getAllDocuments() returns
    // a full PID and a full mDL, and
    // walletCoreDocumentsController.getMainPidDocument() returns a main PID.

    // Case 1 Expected Result:
    // DashboardInteractorGetDocumentsPartialState.Success state, with:
    // 1. the list of Documents transformed to DocumentUi objects,
    // 2. an actual user name, and
    // 3. an actual (base64 encoded) user image, and
    // 4. the main PID.
    @Test
    fun `Given Case 1, When getDocuments is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetStringForDocumentsCall(resourceProvider)
            mockGetMainPidDocumentCall(mockedMainPid)

            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorGetDocumentsPartialState.Success(
                        documentsUi = mockedFullDocumentsUi,
                        userFirstName = mockedUserFirstName,
                        userBase64Portrait = mockedUserBase64Portrait,
                        mainPid = mockedFullPid,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // walletCoreDocumentsController.getAllDocuments() returns
    // an mDL with no user name,
    // no user image, and
    // walletCoreDocumentsController.getMainPidDocument() returns null.

    // Case 2 Expected Result:
    // DashboardInteractorGetDocumentsPartialState.Success state, with:
    // 1. the DeferredDocument transformed to DocumentUi object,
    // 2. empty string for the user name,
    // 3. empty string for the user image, and
    // 4. null for the main PID.
    @Test
    fun `Given Case 2, When getDocuments is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetStringForDocumentsCall(resourceProvider)

            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(listOf(mockedMdlWithNoUserNameAndNoUserImage))

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorGetDocumentsPartialState.Success(
                        documentsUi = listOf(mockedMdlUiWithNoUserNameAndNoUserImage),
                        userFirstName = mockedNoUserFistNameFound,
                        userBase64Portrait = mockedNoUserBase64PortraitFound,
                        mainPid = null,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // walletCoreDocumentsController.getAllDocuments() returns
    // a full PID and a full mDL, and
    // walletCoreDocumentsController.getMainPidDocument() returns a null PID.

    // Case 3 Expected Result:
    // DashboardInteractorGetDocumentsPartialState.Success state, with:
    // 1. the list of Documents transformed to DocumentUi objects,
    // 2. a user name
    // 3. an empty user image, and
    // 4. null PID.
    @Test
    fun `Given Case 3, When getDocuments is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetStringForDocumentsCall(resourceProvider)
            mockGetMainPidDocumentCall(null)
            mockGetAllDocumentsCall()

            val emptyUserName = ""
            val emptyBase64Portrait = ""
            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val mockDocumentsUi: List<DocumentUi> = listOf(mockedPendingDocumentUi)
                val expectedResult = DashboardInteractorGetDocumentsPartialState.Success(
                    documentsUi = mockDocumentsUi,
                    userFirstName = emptyUserName,
                    userBase64Portrait = emptyBase64Portrait,
                    mainPid = null,
                )
                assertEquals(
                    expectedResult,
                    awaitItem()
                )
            }
        }
    }


    // Case 4:
    // walletCoreDocumentsController.getAllDocuments() returns
    // an mDL with no expiration date.

    // Case 4 Expected Result:
    // DashboardInteractorGetDocumentsPartialState.Success state, with:
    // 1. the DeferredDocument transformed to DocumentUi object,
    // 2. an actual user name, and
    // 3. an actual (base64 encoded) user image, and
    // 4. null for the main PID.
    @Test
    fun `Given Case 4, When getDocuments is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetStringForDocumentsCall(resourceProvider)

            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(listOf(mockedMdlWithNoExpirationDate))

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorGetDocumentsPartialState.Success(
                        documentsUi = listOf(mockedMdlUiWithNoExpirationDate),
                        userFirstName = mockedUserFirstName,
                        userBase64Portrait = mockedUserBase64Portrait,
                        mainPid = null,
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 5:
    // walletCoreDocumentsController.getAllDocuments() throws an exception with a message.
    @Test
    fun `Given Case 5, When getDocuments is called, Then it returns Failed with exception's localized message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorGetDocumentsPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 6:
    // walletCoreDocumentsController.getAllDocuments() throws an exception with no message.
    @Test
    fun `Given Case 6, When getDocuments is called, Then it returns Failed with the generic error message`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorGetDocumentsPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region getAppVersion
    @Test
    fun `Given an App Version, When getAppVersion is called, Then it returns the Apps Version`() {
        // Given
        val expectedAppVersion = "2024.01.1"
        whenever(configLogic.appVersion)
            .thenReturn(expectedAppVersion)

        // When
        val actualAppVersion = interactor.getAppVersion()

        // Then
        assertEquals(expectedAppVersion, actualAppVersion)
        verify(configLogic, times(1))
            .appVersion
    }
    //endregion

    // region retrieveLogFileUris
    @Test
    fun `Given a list of logs via logController, When retrieveLogFileUris is called, Then expected logs are returned`() {
        // Given
        val mockedArrayList = arrayListOf(
            Uri.parse(mockedUriPath1),
            Uri.parse(mockedUriPath2)
        )
        whenever(logController.retrieveLogFileUris()).thenReturn(mockedArrayList)

        // When
        val expectedLogFileUris = interactor.retrieveLogFileUris()

        // Then
        assertEquals(mockedArrayList, expectedLogFileUris)
    }
    // end region

    //region Mock Calls of the Dependencies
    private fun mockBluetoothAdapterEnabledState(enabled: Boolean) {
        val newBluetoothAdapterState = if (enabled) {
            BluetoothAdapter.STATE_ON
        } else {
            BluetoothAdapter.STATE_OFF
        }
        shadowBluetoothAdapter.setState(newBluetoothAdapterState)
    }

    private fun mockGetStringForDocumentsCall(resourceProvider: ResourceProvider) {
        mockDocumentTypeUiToUiNameCall(resourceProvider)

        whenever(resourceProvider.getString(R.string.dashboard_document_no_expiration_found))
            .thenReturn(mockedNoExpirationDateFound)
    }

    private fun mockGetMainPidDocumentCall(mainPid: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(mainPid)
    }

    private fun mockDeleteDocumentCall() {
        whenever(walletCoreDocumentsController.deleteDocument(any()))
            .thenReturn(DeleteDocumentPartialState.Success.toFlow())
    }

    private fun mockGetAllDocumentsCall() {
        val mockedDocuments: List<Document> = listOf(mockedUnsignedDocument)
        whenever(walletCoreDocumentsController.getAllDocuments())
            .thenReturn(mockedDocuments)
    }
    //endregion
}