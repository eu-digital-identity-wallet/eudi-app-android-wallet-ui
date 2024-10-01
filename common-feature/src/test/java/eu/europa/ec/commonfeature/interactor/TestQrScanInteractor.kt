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

package eu.europa.ec.commonfeature.interactor

import eu.europa.ec.businesslogic.validator.Form
import eu.europa.ec.businesslogic.validator.FormValidationResult
import eu.europa.ec.businesslogic.validator.FormValidator
import eu.europa.ec.businesslogic.validator.FormsValidationResult
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestQrScanInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    lateinit var formValidator: FormValidator

    private lateinit var closeable: AutoCloseable

    private lateinit var interactor: QrScanInteractor

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = QrScanInteractorImpl(
            formValidator = formValidator
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    // Case: validateForm behavior
    // When FormValidationResult is returned by validateForm on formValidator,
    // the expected result should be returned from the interactor
    @Test
    fun `Given a valid form, When validateForm is called, Then the expected FormValidationResult is returned`() =
        coroutineRule.runTest {
            // Given
            val form = mock<Form>()
            val expectedResult = FormValidationResult(isValid = true)
            whenever(formValidator.validateForm(form)).thenReturn(expectedResult)

            // When
            val result = interactor.validateForm(form = form)

            // Then
            assertEquals(expectedResult, result)
            verify(formValidator).validateForm(form)
        }

    // Case: validateForms behavior
    // When FormValidationResult is returned by validateForms with a list of forms on formValidator,
    // the expected result should be returned from the interactor
    @Test
    fun `Given a valid list of forms, When validateForms is called, Then the expected FormValidationResult is returned`() =
        coroutineRule.runTest {
            // Given
            val forms = mock<List<Form>>()
            val expectedResult = FormsValidationResult(isValid = true)
            whenever(formValidator.validateForms(forms)).thenReturn(expectedResult)

            // When
            val result = interactor.validateForms(forms = forms)

            // Then
            assertEquals(expectedResult, result)
            verify(formValidator).validateForms(forms)
        }
}