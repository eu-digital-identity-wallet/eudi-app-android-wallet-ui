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

package eu.europa.ec.businesslogic.validator

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class TestFormValidator {

    private val plainErrorMessage = "error message"

    @get:org.junit.Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var formValidation: FormValidator

    @Mock
    private lateinit var logController: LogController

    private val validationError = FormValidationResult(
        false,
        plainErrorMessage
    )

    private val validationSuccess = FormValidationResult(true)

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)
        formValidation = FormValidatorImpl(logController)
    }

    @Test
    fun testValidateNotEmptyRule() = coroutineRule.runTest {
        val rules = listOf(Rule.ValidateNotEmpty(plainErrorMessage))
        validateForm(
            rules = rules,
            value = "",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "test",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateEmailRule() = coroutineRule.runTest {
        val rules = listOf(Rule.ValidateEmail(plainErrorMessage))
        validateForm(
            rules = rules,
            value = "test@test",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "test@test.com",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidatePhoneNumberRule() = coroutineRule.runTest {
        val greekPhoneRule = listOf(Rule.ValidatePhoneNumber(plainErrorMessage, "GR"))
        val usPhoneRule = listOf(Rule.ValidatePhoneNumber(plainErrorMessage, "US"))
        validateForm(
            rules = greekPhoneRule,
            value = "1111111111",
            validationResult = validationError
        )
        validateForm(
            rules = greekPhoneRule,
            value = "15223433333",
            validationResult = validationError
        )
        validateForm(
            rules = greekPhoneRule,
            value = "6941111111",
            validationResult = validationSuccess
        )
        validateForm(
            rules = usPhoneRule,
            value = "6941111111",
            validationResult = validationError
        )
        validateForm(
            rules = usPhoneRule,
            value = "6102458772",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateStringLengthRule() = coroutineRule.runTest {
        val rules = listOf(Rule.ValidateStringLength(listOf(10, 12), plainErrorMessage))
        validateForm(
            rules = rules,
            value = "123456789",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "12345678901",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1234567890",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "123456789012",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateStringMaxLengthRule() = coroutineRule.runTest {
        val rules = listOf(Rule.ValidateStringMaxLength(10, plainErrorMessage))
        validateForm(
            rules = rules,
            value = "aaaaaaaaaaa",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "            ",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaa",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaaa",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateStringMinLengthRule() = coroutineRule.runTest {
        val rules = listOf(Rule.ValidateStringMinLength(5, plainErrorMessage))
        validateForm(
            rules = rules,
            value = "",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaa",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaaa",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaa",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateContainsRegexRule() = coroutineRule.runTest {
        val atLeast2DigitsRule =
            listOf(Rule.ContainsRegex("(.*\\d){2,}".toRegex(), plainErrorMessage))
        val atLeast2SpecialCharsRule =
            listOf(Rule.ContainsRegex("(.*[^a-zA-Z0-9]){2,}".toRegex(), plainErrorMessage))
        val atLeast3UppercaseCharactersRule =
            listOf(Rule.ContainsRegex("(.*[A-Z]){3,}".toRegex(), plainErrorMessage))

        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test12",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test123",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test1",
            validationResult = validationError
        )
        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test",
            validationResult = validationError
        )

        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test!@",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test!@#",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test!",
            validationResult = validationError
        )
        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test",
            validationResult = validationError
        )

        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "TestAA",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "TAestAA",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "TestA",
            validationResult = validationError
        )
        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "Test",
            validationResult = validationError
        )
    }

    @Test
    fun testValidateRegexRule() = coroutineRule.runTest {
        val atLeastOneCapitalLetterRule =
            listOf(Rule.ValidateRegex(".*[A-Z].*".toRegex(), plainErrorMessage))
        val atLeastOneDigitRule =
            listOf(Rule.ValidateRegex(".*(\\d).*".toRegex(), plainErrorMessage))
        val atLeastOneSpecialCharRule =
            listOf(Rule.ValidateRegex(".*[^a-zA-Z0-9].*".toRegex(), plainErrorMessage))
        validateForm(
            rules = atLeastOneCapitalLetterRule,
            value = "test",
            validationResult = validationError
        )
        validateForm(
            rules = atLeastOneCapitalLetterRule,
            value = "Test",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeastOneDigitRule,
            value = "test",
            validationResult = validationError
        )
        validateForm(
            rules = atLeastOneDigitRule,
            value = "test1",
            validationResult = validationSuccess
        )
        validateForm(
            rules = atLeastOneSpecialCharRule,
            value = "test",
            validationResult = validationError
        )
        validateForm(
            rules = atLeastOneSpecialCharRule,
            value = "test1@",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateStringRangeRule() = coroutineRule.runTest {
        val rules = listOf(Rule.ValidateStringRange(0..10, plainErrorMessage))
        validateForm(
            rules = rules,
            value = "aaaaaaaaaaa",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaaa",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateStringMatchRule() = coroutineRule.runTest {
        val isNotCaseSensitiveRule = listOf(Rule.ValidateStringMatch("test", plainErrorMessage))
        val isCaseSensitiveRule = listOf(Rule.ValidateStringMatch("test", plainErrorMessage, true))
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "testt",
            validationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "t!@#$",
            validationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "",
            validationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "TEST",
            validationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "TEST",
            validationResult = validationError
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "",
            validationResult = validationError
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "test",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateStringNotMatchRule() = coroutineRule.runTest {
        val isNotCaseSensitiveRule = listOf(Rule.ValidateStringNotMatch("test", plainErrorMessage))
        val isCaseSensitiveRule =
            listOf(Rule.ValidateStringNotMatch("test", plainErrorMessage, true))
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "test",
            validationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "TEST",
            validationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "",
            validationResult = validationSuccess
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "testt",
            validationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "test",
            validationResult = validationError
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "Testss",
            validationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "TEST",
            validationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateDuplicateCharacterNotInConsecutiveOrderRule() = coroutineRule.runTest {
        val maxTimesOfConsecutiveOrder2Rule = listOf(
            Rule.ValidateDuplicateCharacterNotInConsecutiveOrder(
                2,
                plainErrorMessage
            )
        )
        val maxTimesOfConsecutiveOrder4Rule = listOf(
            Rule.ValidateDuplicateCharacterNotInConsecutiveOrder(
                4,
                plainErrorMessage
            )
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "1313",
            validationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "4356754",
            validationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "0191",
            validationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "1225",
            validationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "2332",
            validationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "454545454545454555444455455",
            validationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "0024",
            validationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "0024",
            validationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "0004",
            validationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "0000",
            validationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "35523456337777",
            validationResult = validationError
        )
    }

    @Test
    fun testValidateNumericNotInConsecutiveSequenceOrderRule() = coroutineRule.runTest {
        val rules = listOf(Rule.ValidateNumericNotInConsecutiveSequenceOrder(4, plainErrorMessage))
        validateForm(
            rules = rules,
            value = "1234",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "4321",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "9876",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "3210",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "0123",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1235",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "9875",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "0124",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "0923",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "8834834835939534",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "TEST",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "",
            validationResult = validationSuccess
        )
    }

    @Test
    fun validateMultipleForms() = coroutineRule.runTest {
        val forms = listOf(
            Form(
                mapOf(
                    listOf(
                        Rule.ValidateNumericNotInConsecutiveSequenceOrder(
                            2,
                            "ValidateNumericNotInConsecutiveSequenceOrder"
                        ),
                        Rule.ValidateNotEmpty(
                            "ValidateNotEmpty"
                        ),
                        Rule.ValidateDuplicateCharacterNotInConsecutiveOrder(
                            2,
                            "ValidateDuplicateCharacterNotInConsecutiveOrder"
                        ),
                        Rule.ValidateEmail(
                            "ValidateEmail"
                        ),
                        Rule.ValidateRegex(
                            ".*(\\d).*".toRegex(),
                            "ValidateRegex"
                        )
                    ) to "4412"
                )
            ),
            Form(
                mapOf(
                    listOf(
                        Rule.ValidateEmail(
                            "ValidateEmail"
                        ),
                        Rule.ValidateNotEmpty(
                            "ValidateNotEmpty"
                        ),
                        Rule.ValidateDuplicateCharacterNotInConsecutiveOrder(
                            2,
                            "ValidateDuplicateCharacterNotInConsecutiveOrder"
                        ),
                        Rule.ValidatePhoneNumber(
                            "ValidatePhoneNumber",
                            "GR"
                        ),
                        Rule.ValidateRegex(
                            ".*(\\d).*".toRegex(),
                            "ValidateRegex"
                        )
                    ) to "test@test.gr"
                )
            ),
            Form(
                mapOf(
                    listOf(
                        Rule.ValidateEmail(
                            "ValidateEmail"
                        ),
                        Rule.ValidateStringNotMatch(
                            "6991111111",
                            "ValidateStringNotMatch"
                        ),
                        Rule.ValidateDuplicateCharacterNotInConsecutiveOrder(
                            2,
                            "ValidateDuplicateCharacterNotInConsecutiveOrder"
                        ),
                        Rule.ValidatePhoneNumber(
                            "ValidatePhoneNumber",
                            "GR"
                        ),
                        Rule.ValidateRegex(
                            ".*[^a-zA-Z0-9].*".toRegex(),
                            "ValidateRegex"
                        )
                    ) to "6991111111"
                )
            )
        )

        formValidation.validateForms(forms).collect {
            assertEquals(
                FormsValidationResult(
                    false,
                    listOf(
                        "ValidateDuplicateCharacterNotInConsecutiveOrder",
                        "ValidateEmail",
                        "ValidatePhoneNumber",
                        "ValidateRegex",
                        "ValidateEmail",
                        "ValidateStringNotMatch",
                        "ValidateDuplicateCharacterNotInConsecutiveOrder",
                        "ValidateRegex"
                    )
                ), it
            )
        }
    }

    @Test
    fun testStringShouldEndWithNumber() = coroutineRule.runTest {
        val rules = listOf(Rule.StringShouldEndWithNumber(plainErrorMessage))
        validateForm(
            rules = rules,
            value = "1.",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1,",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1*",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1.00",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "1,00",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateFileExtension() = coroutineRule.runTest {
        val validExtensionList = listOf(".doc", ".ppt")

        val rules = listOf(
            Rule.ValidateFileExtension(
                errorMessage = plainErrorMessage,
                supportedExtensions = validExtensionList
            )
        )

        validateForm(
            rules = rules,
            value = "fileame.pdf",
            validationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "asdf",
            validationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "lalala.doc",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "lala.ppt",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateFileSizeMaxSize() = coroutineRule.runTest {

        val rules = listOf(
            Rule.ValidateFileSizeMaxSize(
                errorMessage = plainErrorMessage,
                size = 100L
            )
        )

        validateForm(
            rules = rules,
            value = "200",
            validationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "100",
            validationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "99",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateMinimumAmount() = coroutineRule.runTest {
        val rules = listOf(
            Rule.ValidateMinimumAmount(
                minimumAmount = 5,
                errorMessage = plainErrorMessage,
            )
        )

        validateForm(
            rules = rules,
            value = "test",
            validationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "4",
            validationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "5",
            validationResult = validationSuccess
        )

        validateForm(
            rules = rules,
            value = "6",
            validationResult = validationSuccess
        )
    }

    @Test
    fun testValidateMaximumAmount() = coroutineRule.runTest {
        val rules = listOf(
            Rule.ValidateMaximumAmount(
                maximumAmount = 5,
                errorMessage = plainErrorMessage,
            )
        )

        validateForm(
            rules = rules,
            value = "test",
            validationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "6",
            validationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "5",
            validationResult = validationSuccess
        )

        validateForm(
            rules = rules,
            value = "4",
            validationResult = validationSuccess
        )
    }

    private suspend fun validateForm(
        rules: List<Rule>,
        value: String,
        validationResult: FormValidationResult
    ) {
        formValidation.validateForm(
            Form(mapOf(rules to value))
        ).collect {
            assertEquals(validationResult, it)
        }
    }
}