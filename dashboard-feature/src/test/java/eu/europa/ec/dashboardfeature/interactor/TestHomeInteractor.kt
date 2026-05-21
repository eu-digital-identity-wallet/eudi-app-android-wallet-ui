/*
 * Copyright (c) 2026 European Commission
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
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.getMockedFullPid
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedUserFirstName
import eu.europa.ec.testfeature.walletcore.getMockedEudiWalletConfig
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.base.getMockedContext
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBluetoothAdapter

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class TestHomeInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var walletCoreConfig: WalletCoreConfig

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var shadowBluetoothAdapter: ShadowBluetoothAdapter

    private lateinit var interactor: HomeInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = HomeInteractorImpl(
            resourceProvider = resourceProvider,
            walletCoreDocumentsController = walletCoreDocumentsController,
            walletCoreConfig = walletCoreConfig,
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

    // Case 3:
    // Context.getSystemService(BLUETOOTH_SERVICE) returns null (or non-BluetoothManager) →
    // the `as? BluetoothManager` cast yields null → `bluetoothManager?.adapter?.isEnabled == true`
    // short-circuits to false. Exercises both the `as?` null branch and the `?.adapter` short-circuit.
    @Test
    fun `Given Case 3, When the system returns no BluetoothManager, Then isBleAvailable returns false`() {
        // Given
        val nullBluetoothContext = org.mockito.kotlin.mock<Context>()
        whenever(nullBluetoothContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(null)
        whenever(resourceProvider.provideContext()).thenReturn(nullBluetoothContext)

        // When
        val actual = interactor.isBleAvailable()

        // Then
        assertEquals(false, actual)
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
            configureProximityPresentation(enableBleCentralMode = true)
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
            configureProximityPresentation(enableBlePeripheralMode = true)
        }

        whenever(walletCoreConfig.config).thenReturn(mockedConfig)

        // When
        val actual = interactor.isBleCentralClientModeEnabled()

        // Then
        assertEquals(expectedBleCentralClientModeEnabled, actual)
    }
    //endregion

    //region getUserNameViaMainPidDocument

    // Case 1:
    // walletCoreDocumentsController.getMainPidDocument() returns a real PID document with
    // a non-empty FIRST_NAME claim.

    // Case 1 Expected Result:
    // Success with the extracted first name.
    @Test
    fun `Given Case 1, When getUserNameViaMainPidDocument is called, Then Success with the document's first name is returned`() {
        coroutineRule.runTest {
            // Given
            val pid = getMockedFullPid()
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(pid)

            // When
            interactor.getUserNameViaMainPidDocument().runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is HomeInteractorGetUserNameViaMainPidDocumentPartialState.Success)
                result as HomeInteractorGetUserNameViaMainPidDocumentPartialState.Success
                assertEquals(mockedUserFirstName, result.userFirstName)
            }
        }
    }

    // Case 2:
    // walletCoreDocumentsController.getMainPidDocument() returns null.

    // Case 2 Expected Result:
    // Success with an empty userFirstName (the `.orEmpty()` fallback on the nullable let).
    @Test
    fun `Given Case 2, When getUserNameViaMainPidDocument is called with no PID, Then Success with empty name is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(null)

            // When
            interactor.getUserNameViaMainPidDocument().runFlowTest {
                // Then
                assertEquals(
                    HomeInteractorGetUserNameViaMainPidDocumentPartialState.Success(userFirstName = ""),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // walletCoreDocumentsController.getMainPidDocument() throws an exception with a message.

    // Case 3 Expected Result:
    // Failure with the exception's localized message.
    @Test
    fun `Given Case 3, When getUserNameViaMainPidDocument is called and throws with message, Then Failure with localized message is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getMainPidDocument())
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getUserNameViaMainPidDocument().runFlowTest {
                // Then
                assertEquals(
                    HomeInteractorGetUserNameViaMainPidDocumentPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // walletCoreDocumentsController.getMainPidDocument() throws an exception with no message.

    // Case 4 Expected Result:
    // Failure with the generic error message.
    @Test
    fun `Given Case 4, When getUserNameViaMainPidDocument is called and throws without message, Then Failure with generic message is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getMainPidDocument())
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getUserNameViaMainPidDocument().runFlowTest {
                // Then
                assertEquals(
                    HomeInteractorGetUserNameViaMainPidDocumentPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
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
    //endregion
}