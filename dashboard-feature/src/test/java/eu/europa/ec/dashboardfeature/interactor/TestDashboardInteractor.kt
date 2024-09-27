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
import eu.europa.ec.commonfeature.util.TestsData.mockedNoUserBase64PortraitFound
import eu.europa.ec.commonfeature.util.TestsData.mockedNoUserFistNameFound
import eu.europa.ec.commonfeature.util.TestsData.mockedPendingMdlUi
import eu.europa.ec.commonfeature.util.TestsData.mockedPendingPidUi
import eu.europa.ec.commonfeature.util.TestsData.mockedUnsignedPidUi
import eu.europa.ec.commonfeature.util.TestsData.mockedUriPath1
import eu.europa.ec.commonfeature.util.TestsData.mockedUriPath2
import eu.europa.ec.commonfeature.util.TestsData.mockedUserBase64Portrait
import eu.europa.ec.commonfeature.util.TestsData.mockedUserFirstName
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
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
import eu.europa.ec.testfeature.mockedUnsignedPid
import eu.europa.ec.testfeature.walletcore.getMockedEudiWalletConfig
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.base.getMockedContext
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
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
            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

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
    // walletCoreDocumentsController.getAllDocuments() returns an empty list.

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

            mockDeleteDocumentCall(response = DeleteDocumentPartialState.Success)

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                val expectedFlow = DashboardInteractorDeleteDocumentPartialState.AllDocumentsDeleted

                // Then
                assertEquals(expectedFlow, awaitItem())
            }
        }
    }

    // Case 3:
    // walletCoreDocumentsController.getAllDocuments() returns Failure

    // Case 3 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.Failure state.
    @Test
    fun `Given Case 3, When deleteDocument is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            mockDeleteDocumentCall(
                response = DeleteDocumentPartialState.Failure(
                    errorMessage = mockedPlainFailureMessage
                )
            )

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

    // Case 4:
    // walletCoreDocumentsController.deleteDocument() throws an exception with a message.

    // Case 4 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.Failure state with exception's localized message.
    @Test
    fun `Given Case 4, When deleteDocument is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockDocumentId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.deleteDocument(mockDocumentId).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorDeleteDocumentPartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }

    // Case 5:
    // walletCoreDocumentsController.deleteDocument() throws an exception with no message.

    // Case 5 Expected Result:
    // DashboardInteractorDeleteDocumentPartialState.Failure state with the generic error message.
    @Test
    fun `Given Case 5, When deleteDocument is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.deleteDocument(mockDocumentId))
                .thenThrow(mockedExceptionWithNoMessage)

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
    //endregion

    //region tryIssuingDeferredDocumentsFlow

    // Case 1:
    // When issueDeferredDocument was called:
    // 1. IssueDeferredDocumentPartialState.Issued was emitted, with
    //  - successData, the successfully issued deferred document's DeferredDocumentData, and also,
    // 2. IssueDeferredDocumentPartialState.Failed was emitted, with
    //  - documentId, the failed deferred document's DocumentId.

    // Case 1 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result state, with
    //  - successfullyIssuedDeferredDocuments: a list with the successfully issued deferred document's DeferredDocumentData,
    //  - failedIssuedDeferredDocuments: a list with the failed deferred document's DocumentId.
    @Test
    fun `Given Case 1, When tryIssuingDeferredDocumentsFlow is called, Then Case 1 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId1 = mockedPendingPidUi.documentId
            val mockDeferredPendingType1 = mockedPendingPidUi.documentIdentifier.docType
            val mockDeferredPendingName1 = mockedPendingPidUi.documentName

            val mockDeferredPendingDocId2 = mockedPendingMdlUi.documentId
            val mockDeferredPendingType2 = mockedPendingMdlUi.documentIdentifier.docType

            val deferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockDeferredPendingDocId1 to mockDeferredPendingType1,
                mockDeferredPendingDocId2 to mockDeferredPendingType2
            )
            val successData = DeferredDocumentData(
                documentId = mockDeferredPendingDocId1,
                docType = mockDeferredPendingType1,
                docName = mockDeferredPendingName1
            )

            mockIssueDeferredDocumentCall(
                docId = mockDeferredPendingDocId1,
                response = IssueDeferredDocumentPartialState.Issued(
                    deferredDocumentData = successData
                )
            )
            mockIssueDeferredDocumentCall(
                docId = mockDeferredPendingDocId2,
                response = IssueDeferredDocumentPartialState.Failed(
                    documentId = mockDeferredPendingDocId2,
                    errorMessage = mockedPlainFailureMessage
                )
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments)
                .runFlowTest {
                    // Then
                    val expectedResult =
                        DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                            successfullyIssuedDeferredDocuments = listOf(successData),
                            failedIssuedDeferredDocuments = listOf(mockDeferredPendingDocId2)
                        )
                    assertEquals(expectedResult, awaitItem())
                }
        }

    // Case 2:
    // IssueDeferredDocumentPartialState.Expired was emitted when issueDeferredDocument was called.

    // Case 2 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result with,
    // - successfullyIssuedDeferredDocuments = emptyList.
    // - failedIssuedDeferredDocuments = emptyList.
    @Test
    fun `Given Case 2, When tryIssuingDeferredDocumentsFlow is called, Then Case 2 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredExpiredDocId = mockedPendingPidUi.documentId
            val mockDeferredExpiredDocType = mockedPendingPidUi.documentIdentifier.docType

            val deferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockDeferredExpiredDocId to mockDeferredExpiredDocType
            )

            mockIssueDeferredDocumentCall(
                docId = mockDeferredExpiredDocId,
                response = IssueDeferredDocumentPartialState.Expired(
                    documentId = mockDeferredExpiredDocId
                )
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments)
                .runFlowTest {
                    // Then
                    val expectedResult =
                        DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                            successfullyIssuedDeferredDocuments = emptyList(),
                            failedIssuedDeferredDocuments = emptyList()
                        )
                    assertEquals(expectedResult, awaitItem())
                }
        }

    // Case 3:
    // IssueDeferredDocumentPartialState.NotReady was emitted when issueDeferredDocument was called.

    // Case 3 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result with,
    // - successfullyIssuedDeferredDocuments = emptyList.
    // - failedIssuedDeferredDocuments = emptyList.
    @Test
    fun `Given Case 3, When tryIssuingDeferredDocumentsFlow is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.docType
            val mockDeferredPendingName = mockedPendingPidUi.documentName

            val deferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            val successData = DeferredDocumentData(
                documentId = mockDeferredPendingDocId,
                docType = mockDeferredPendingType,
                docName = mockDeferredPendingName
            )

            mockIssueDeferredDocumentCall(
                docId = mockDeferredPendingDocId,
                response = IssueDeferredDocumentPartialState.NotReady(
                    deferredDocumentData = successData
                )
            )

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                val expectedResult =
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                        successfullyIssuedDeferredDocuments = emptyList(),
                        failedIssuedDeferredDocuments = emptyList()
                    )
                assertEquals(expectedResult, awaitItem())
            }
        }

    // Case 4:
    // walletCoreDocumentsController.issueDeferredDocument() throws an exception with a message.

    // Case 4 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure state with exception's localized message.
    @Test
    fun `Given Case 4, When tryIssuingDeferredDocumentsFlow is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.docType

            val deferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            whenever(walletCoreDocumentsController.issueDeferredDocument(mockDeferredPendingDocId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }

    // Case 5:
    // walletCoreDocumentsController.issueDeferredDocument() throws an exception with no message.

    // Case 5 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure state with the generic error message.
    @Test
    fun `Given Case 5, When tryIssuingDeferredDocumentsFlow is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.docType

            val deferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            whenever(walletCoreDocumentsController.issueDeferredDocument(mockDeferredPendingDocId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                assertEquals(
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }

    // Case 6:
    // emptyFlow was returned when issueDeferredDocument was called.

    // Case 6 Expected Result:
    // DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result state.
    @Test
    fun `Given Case 6, When tryIssuingDeferredDocumentsFlow is called, Then Case 6 Expected Result is returned`() =
        coroutineRule.runTest {
            // Given
            val mockDeferredPendingDocId = mockedPendingPidUi.documentId
            val mockDeferredPendingType = mockedPendingPidUi.documentIdentifier.docType

            val deferredDocuments: Map<DocumentId, DocType> = mapOf(
                mockDeferredPendingDocId to mockDeferredPendingType
            )
            whenever(walletCoreDocumentsController.issueDeferredDocument(mockDeferredPendingDocId))
                .thenReturn(emptyFlow())

            // When
            interactor.tryIssuingDeferredDocumentsFlow(deferredDocuments).runFlowTest {
                // Then
                val expectedResult =
                    DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                        successfullyIssuedDeferredDocuments = emptyList(),
                        failedIssuedDeferredDocuments = listOf(mockDeferredPendingDocId)
                    )
                assertEquals(expectedResult, awaitItem())
            }
        }

    //endregion

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
    // an Unsigned PID,
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
            mockGetMainPidDocumentCall(null)

            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(listOf(mockedUnsignedPid))

            val emptyUserName = ""
            val emptyBase64Portrait = ""

            // When
            interactor.getDocuments().runFlowTest {
                // Then
                val mockDocumentsUi: List<DocumentUi> = listOf(mockedUnsignedPidUi)
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

    //region retrieveLogFileUris
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
    //endregion

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
    }

    private fun mockGetMainPidDocumentCall(mainPid: IssuedDocument?) {
        whenever(walletCoreDocumentsController.getMainPidDocument())
            .thenReturn(mainPid)
    }

    private fun mockDeleteDocumentCall(response: DeleteDocumentPartialState) {
        whenever(walletCoreDocumentsController.deleteDocument(anyString()))
            .thenReturn(response.toFlow())
    }

    private fun mockIssueDeferredDocumentCall(
        docId: DocumentId,
        response: IssueDeferredDocumentPartialState
    ) {
        whenever(walletCoreDocumentsController.issueDeferredDocument(docId))
            .thenReturn(response.toFlow())
    }
    //endregion
}