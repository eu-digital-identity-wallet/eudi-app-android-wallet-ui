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

package eu.europa.ec.testlogic.flow.interactor.quick_pin

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.validator.FormValidator
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorImpl
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorPinValidPartialState
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorSetPinPartialState
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.testlogic.tools.error.mockedException
import eu.europa.ec.testlogic.tools.error.plainErrorMessage
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Spy

class TestQuickPinInteractor {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Spy
    private lateinit var formValidator: FormValidator

    @Spy
    private lateinit var prefKeys: PrefKeys

    @Spy
    private lateinit var resourceProvider: ResourceProvider


    private lateinit var quickPinInteractor: QuickPinInteractor

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)

        quickPinInteractor = QuickPinInteractorImpl(
            formValidator = formValidator,
            prefKeys = prefKeys,
            resourceProvider = resourceProvider
        )

        Mockito.`when`(resourceProvider.genericErrorMessage()).thenReturn(plainErrorMessage)
    }

    //region setPin
    @Test
    fun `Given prefKeys' setDevicePin is Success, When setPin is called, Then it returns Success`() =
        coroutineRule.runTest {

            // When
            quickPinInteractor.setPin(
                newPin = mockedNewPin,
                initialPin = mockedCurrentPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Success,
                    awaitItem()
                )
            }

            Mockito.verify(prefKeys, Mockito.atLeastOnce())
                .setDevicePin(ArgumentMatchers.anyString())
        }

    @Test
    fun `Given prefKeys' setDevicePin throws Exception, When setPin is called, Then it returns Failed`() =
        coroutineRule.runTest {
            // Given
            Mockito.`when`(prefKeys.setDevicePin(ArgumentMatchers.anyString()))
                .thenThrow(mockedException).toFlow()

            // When
            quickPinInteractor.setPin(
                newPin = mockedNewPin,
                initialPin = mockedCurrentPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedException.localizedMessage ?: plainErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    //endregion

    //region changePin
    @Test
    fun `Given prefKeys' getDevicePin is Success, When changePin is called, Then it returns Success`() =
        coroutineRule.runTest {
            // Given
            getDevicePinInterceptor()

            // When
            quickPinInteractor.changePin(
                //currentPin = mockedCurrentPin,
                newPin = mockedNewPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Success,
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given prefKeys' getDevicePin is Failed, When changePin is called, Then it returns Failed with appropriate message`() =
        coroutineRule.runTest {
            // Given
            getDevicePinInterceptor()
            Mockito.`when`(resourceProvider.getString(ArgumentMatchers.anyInt())).thenReturn(mockedInvalidPinMessage)

            // When
            quickPinInteractor.changePin(
                //currentPin = mockedInvalidCurrentPin,
                newPin = mockedNewPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedInvalidPinMessage
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given prefKeys' getDevicePin throws Exception, When changePin is called, Then it returns Failed with exception's localized message`() =
        coroutineRule.runTest {
            // Given
            Mockito.`when`(prefKeys.getDevicePin())
                .thenThrow(mockedException).toFlow()

            // When
            quickPinInteractor.changePin(
                //currentPin = mockedInvalidCurrentPin,
                newPin = mockedNewPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorSetPinPartialState.Failed(
                        errorMessage = mockedException.localizedMessage ?: plainErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    //endregion

    //region isCurrentPinValid
    @Test
    fun `Given prefKeys' getDevicePin is Success, When isCurrentPinValid is called, Then it returns Success`() =
        coroutineRule.runTest {
            // Given
            getDevicePinInterceptor()

            // When
            quickPinInteractor.isCurrentPinValid(
                pin = mockedCurrentPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorPinValidPartialState.Success,
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given prefKeys' getDevicePin is Failed, When isCurrentPinValid is called, Then it returns Failed with appropriate message`() =
        coroutineRule.runTest {
            // Given
            getDevicePinInterceptor()
            Mockito.`when`(resourceProvider.getString(ArgumentMatchers.anyInt())).thenReturn(mockedInvalidPinMessage)

            // When
            quickPinInteractor.isCurrentPinValid(
                pin = mockedInvalidCurrentPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorPinValidPartialState.Failed(
                        errorMessage = mockedInvalidPinMessage
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given prefKeys' getDevicePin throws Exception, When isCurrentPinValid is called, Then it returns Failed with exception's localized message`() =
        coroutineRule.runTest {
            // Given
            Mockito.`when`(prefKeys.getDevicePin())
                .thenThrow(mockedException).toFlow()

            // When
            quickPinInteractor.isCurrentPinValid(
                pin = mockedCurrentPin
            ).runFlowTest {
                // Then
                assertEquals(
                    QuickPinInteractorPinValidPartialState.Failed(
                        errorMessage = mockedException.localizedMessage ?: plainErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    //endregion

    //region Interceptors
    private fun getDevicePinInterceptor() {
        Mockito.`when`(prefKeys.getDevicePin())
            .thenReturn(mockedCurrentPin).toFlow()
    }

    //endregion

    //region Mocked objects needed for tests.
    private val mockedNewPin = "1111"
    private val mockedCurrentPin = "3312"
    private val mockedInvalidCurrentPin = "8998"
    private val mockedInvalidPinMessage = "The quick pin is invalid"
    //endregion
}