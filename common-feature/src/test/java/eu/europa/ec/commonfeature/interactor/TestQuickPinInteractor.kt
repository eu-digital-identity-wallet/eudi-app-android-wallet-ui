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

package eu.europa.ec.commonfeature.interactor

import eu.europa.ec.authenticationlogic.controller.storage.PinStorageController
import eu.europa.ec.businesslogic.model.SecurePin
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.securePin
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
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

class TestQuickPinInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var pinStorageController: PinStorageController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: QuickPinInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = QuickPinInteractorImpl(
            pinStorageController = pinStorageController,
            resourceProvider = resourceProvider
        )

        whenever(resourceProvider.genericErrorMessage())
            .thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region hasPin

    // Case 1:
    // pinStorageController.hasPin() returns false.
    @Test
    fun `Given Case 1, When hasPin is called, Then it returns false`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.hasPin())
                .thenReturn(false)

            // When
            val actual = interactor.hasPin()

            // Then
            val expected = false

            assertEquals(expected, actual)
            verify(pinStorageController, times(1))
                .hasPin()
        }
    }

    // Case 2:
    // pinStorageController.hasPin() returns true.
    @Test
    fun `Given Case 2, When hasPin is called, Then it returns true`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.hasPin())
                .thenReturn(true)

            // When
            val actual = interactor.hasPin()

            // Then
            val expected = true

            assertEquals(expected, actual)
            verify(pinStorageController, times(1))
                .hasPin()
        }
    }
    //endregion

    //region setPin

    // Case 1:
    // isPinMatched returns Success.
    // pinStorageController.setPin() throws no errors.
    @Test
    fun `Given Case 1, When setPin is called, Then it returns Success`() {
        coroutineRule.runTest {
            // Given
            val newPin = securePin(mockedPin)
            val initialPin = securePin(mockedPin)

            // When
            interactor.setPin(
                newPin = newPin,
                initialPin = initialPin,
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Success,
                    awaitItem()
                )
            }

            verify(pinStorageController, times(1))
                .setPin(any<SecurePin>())
        }
    }

    // Case 2:
    // isPinMatched returns Failed.
    @Test
    fun `Given Case 2, When setPin is called, Then it returns Failed with the appropriate error message`() {
        coroutineRule.runTest {
            // Given
            whenever(resourceProvider.getString(R.string.quick_pin_non_match))
                .thenReturn(mockedPinsDontMatchMessage)

            val newPin = securePin(mockedNewPin)
            val initialPin = securePin(mockedInitialPin)

            // When
            interactor.setPin(
                newPin = newPin,
                initialPin = initialPin,
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedPinsDontMatchMessage
                    ),
                    awaitItem()
                )

                verify(resourceProvider, times(1))
                    .getString(R.string.quick_pin_non_match)
            }
        }
    }

    // Case 3:
    // isPinMatched returns Success.
    // pinStorageController.setPin() throws an exception with a message.
    @Test
    fun `Given Case 3, When setPin is called, Then it returns Failed with exception's localized message`() {
        coroutineRule.runTest {
            // Given
            val newPin = securePin(mockedPin)
            val initialPin = securePin(mockedPin)
            whenever(pinStorageController.setPin(any<SecurePin>()))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.setPin(
                newPin = newPin,
                initialPin = initialPin,
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // isPinMatched returns Success.
    // pinStorageController.setPin() throws an exception with no message.
    @Test
    fun `Given Case 4, When setPin is called, Then it returns Failed with the generic error message`() {
        coroutineRule.runTest {
            // Given
            val newPin = securePin(mockedPin)
            val initialPin = securePin(mockedPin)
            whenever(pinStorageController.setPin(any<SecurePin>()))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.setPin(
                newPin = newPin,
                initialPin = initialPin,
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region changePin

    // Case 1:
    // pinStorageController.setPin() throws no errors.
    @Test
    fun `Given Case 1, When changePin is called, Then it returns Success`() {
        coroutineRule.runTest {

            // When
            interactor.changePin(
                newPin = securePin(mockedNewPin)
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // pinStorageController.setPin() throws an exception with a message.
    @Test
    fun `Given Case 2, When changePin is called, Then it returns Failed with exception's localized message`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.setPin(any<SecurePin>()))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.changePin(
                newPin = securePin(mockedNewPin)
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // pinStorageController.setPin() throws an exception with no message.
    @Test
    fun `Given Case 3, When changePin is called, Then it returns Failed with the generic error message`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.setPin(any<SecurePin>()))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.changePin(
                newPin = securePin(mockedNewPin)
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region isCurrentPinValid

    // Case 1:
    // pinStorageController.isPinValid() == pin is true.
    @Test
    fun `Given Case 1, When isCurrentPinValid is called, Then it returns Success`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.isPinValid(any<SecurePin>()))
                .thenReturn(true)

            // When
            interactor.isCurrentPinValid(
                pin = securePin(mockedPin)
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorPinValidPartialState.Success,
                    awaitItem()
                )
            }
        }
    }

    // Case 2:
    // pinStorageController.isPinValid() == pin is false.
    @Test
    fun `Given Case 2, When isCurrentPinValid is called, Then it returns Failed with the appropriate error message`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.isPinValid(any<SecurePin>()))
                .thenReturn(false)

            whenever(resourceProvider.getString(R.string.quick_pin_invalid_error))
                .thenReturn(mockedInvalidPinMessage)

            // When
            interactor.isCurrentPinValid(
                pin = securePin(mockedNewPin)
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorPinValidPartialState.Failed(
                        errorMessage = mockedInvalidPinMessage
                    ),
                    awaitItem()
                )

                verify(resourceProvider, times(1))
                    .getString(R.string.quick_pin_invalid_error)
            }
        }
    }

    // Case 3:
    // pinStorageController.isPinValid() throws an exception with a message.
    @Test
    fun `Given Case 3, When isCurrentPinValid is called, Then it returns Failed with exception's localized message`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.isPinValid(any<SecurePin>()))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.isCurrentPinValid(
                pin = securePin(mockedPin)
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorPinValidPartialState.Failed(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // pinStorageController.isPinValid() throws an exception with no message.
    @Test
    fun `Given Case 4, When isCurrentPinValid is called, Then it returns Failed with the generic error message`() {
        coroutineRule.runTest {
            // Given
            whenever(pinStorageController.isPinValid(any<SecurePin>()))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.isCurrentPinValid(
                pin = securePin(mockedPin)
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorPinValidPartialState.Failed(
                        errorMessage = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region Mocked objects needed for tests.
    private val mockedPin = "1234"
    private val mockedNewPin = "5678"
    private val mockedInitialPin = "0000"
    private val mockedPinsDontMatchMessage = "Pins do not match"
    private val mockedInvalidPinMessage = "Invalid quick pin"
    //endregion
}