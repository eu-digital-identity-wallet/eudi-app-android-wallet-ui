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
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.commonfeature.util.TestsData.mockedFullDocumentsUi
import eu.europa.ec.commonfeature.util.TestsData.mockedMdlUiWithNoExpirationDate
import eu.europa.ec.commonfeature.util.TestsData.mockedMdlUiWithNoUserNameAndNoUserImage
import eu.europa.ec.commonfeature.util.TestsData.mockedNoExpirationDateFound
import eu.europa.ec.commonfeature.util.TestsData.mockedNoUserBase64PortraitFound
import eu.europa.ec.commonfeature.util.TestsData.mockedNoUserFistNameFound
import eu.europa.ec.commonfeature.util.TestsData.mockedUserBase64Portrait
import eu.europa.ec.commonfeature.util.TestsData.mockedUserFirstName
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.eudi.wallet.EudiWalletConfig
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
import eu.europa.ec.testfeature.walletcore.getMockedEudiWalletConfig
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.base.getMockedContext
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
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
    // an mDL with no expiration date.

    // Case 3 Expected Result:
    // DashboardInteractorGetDocumentsPartialState.Success state, with:
    // 1. the DeferredDocument transformed to DocumentUi object,
    // 2. an actual user name, and
    // 3. an actual (base64 encoded) user image, and
    // 4. null for the main PID.
    @Test
    fun `Given Case 3, When getDocuments is called, Then Case 3 Expected Result is returned`() {
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

    // Case 4:
    // walletCoreDocumentsController.getAllDocuments() throws an exception with a message.
    @Test
    fun `Given Case 4, When getDocuments is called, Then it returns Failed with exception's localized message`() {
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

    // Case 5:
    // walletCoreDocumentsController.getAllDocuments() throws an exception with no message.
    @Test
    fun `Given Case 5, When getDocuments is called, Then it returns Failed with the generic error message`() {
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
    //endregion
}