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

package eu.europa.ec.businesslogic.validator

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        formValidation = FormValidatorImpl(logController)
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given a ValidateNotEmpty rule, When empty and non-empty values are validated, Then only the empty one fails`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateNotEmpty(plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "test",
            expectedValidationResult = validationSuccess
        )
    }

    /**
     * Cases: a valid URL, no schema, no host, no path, no query, an empty URL and two invalid URLs.
     */
    @Test
    fun `Given a URL rule that checks schema, host, path and query, When URLs are validated, Then only complete URLs pass`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateUrl(
                shouldValidateSchema = true,
                shouldValidateHost = true,
                shouldValidatePath = true,
                shouldValidateQuery = true,
                errorMessage = plainErrorMessage
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "https://www.example.com/path/to/resource?query=parameter&another=example",
            expectedValidationResult = validationSuccess
        )

        validateForm(
            rules = rules,
            value = "www.example.com/path/to/resource?query=parameter&another=example",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "https:///path/to/resource?query=parameter&another=example",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "https://www.example.com?query=parameter&another=example",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "https://www.example.com/path/to/resource",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "invalid-url",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "123456789",
            expectedValidationResult = validationError
        )
    }

    /**
     * Invalid URLs are validated first, then valid ones.
     */
    @Test
    fun `Given a URL rule that checks schema and query, When custom-scheme and web URLs are validated, Then only custom-scheme links with a query pass`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateUrl(
                shouldValidateSchema = true,
                shouldValidateHost = false,
                shouldValidatePath = false,
                shouldValidateQuery = true,
                errorMessage = plainErrorMessage
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "invalid_url",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "123456789",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "http://example.com",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "https://notarealproject.com/otherpath",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "https://notarealproject.com/bad_query_param?",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "ftp://projectsite.com",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "mocked_scheme://?mocked_query_param=some_value",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "mocked_scheme://mocked_host?mocked_query_param=some_value",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "mocked_scheme://mocked_host?mocked_query_param1=some_value1&mocked_query_param2=some_value2",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "mocked-scheme://mocked-host?mocked-query-param=some-value",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "mocked.scheme://mocked.host?mocked.query.param=some.value",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "eudi-openid4vp%3A%2F%2Fdev.verifier-backend.eudiw.dev%3Fclient_id%3Ddev.verifier-backend.eudiw.dev%26request_uri%3Dhttps%3A%2F%2Fdev.verifier-backend.eudiw.dev%2Fwallet%2Frequest.jwt%2F1234",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "openid-credential-offer://credential_offer?credential_offer=%7B%22credential_issuer%22:%20%22https://dev.issuer.eudiw.dev%22%2C%20%22credential_configuration_ids%22:%20%5B%22eu.europa.ec.eudi.pid_mdoc%22%5D%2C%20%22grants%22:%20%7B%22urn:ietf:params:oauth:grant-type:pre-authorized_code%22:%20%7B%22pre-authorized_code%22:%20%22some_code%22%2C%20%22tx_code%22:%20%7B%22length%22:%205%2C%20%22input_mode%22:%20%22numeric%22%2C%20%22description%22:%20%22Please%20provide%20the%20one-time%20code.%22%7D%7D%7D%7D",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "openid-credential-offer://?credential_offer=%7B%22credential_issuer%22%3A%22https%3A%2F%2Fdev.issuer-backend.eudiw.dev%22%2C%22credential_configuration_ids%22%3A%5B%22eu.europa.ec.eudi.pid_mso_mdoc%22%5D%2C%22grants%22%3A%7B%22authorization_code%22%3A%7B%22authorization_server%22%3A%22https%3A%2F%2Fdev.auth.eudiw.dev%2Frealms%2Fpid-issuer-realm%22%7D%7D%7D",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "eudi-openid4vp://dev.verifier-backend.eudiw.dev?client_id=dev.verifier-backend.eudiw.dev&request_uri=https://dev.verifier-backend.eudiw.dev/wallet/request.jwt/1234",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "openid-credential-offer://credential_offer?credential_offer={\"credential_issuer\": \"https://dev.issuer.eudiw.dev\", \"credential_configuration_ids\": [\"eu.europa.ec.eudi.pid_mdoc\"], \"grants\": {\"urn:ietf:params:oauth:grant-type:pre-authorized_code\": {\"pre-authorized_code\": \"some_code\", \"tx_code\": {\"length\": 5, \"input_mode\": \"numeric\", \"description\": \"Please provide the one-time code.\"}}}}",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "openid-credential-offer://?credential_offer={\"credential_issuer\":\"https://dev.issuer-backend.eudiw.dev\",\"credential_configuration_ids\":[\"eu.europa.ec.eudi.pid_mso_mdoc\"],\"grants\":{\"authorization_code\":{\"authorization_server\":\"https://dev.auth.eudiw.dev/realms/pid-issuer-realm\"}}}",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given an email rule, When addresses are validated, Then only a complete address passes`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateEmail(plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "test@test",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "test@test.com",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given Greek and US phone rules, When numbers are validated, Then only numbers valid for the region pass`() = coroutineRule.runTest {
        // Given
        val greekPhoneRule = listOf(Rule.ValidatePhoneNumber(plainErrorMessage, "GR"))
        val usPhoneRule = listOf(Rule.ValidatePhoneNumber(plainErrorMessage, "US"))

        // Then
        validateForm(
            rules = greekPhoneRule,
            value = "1111111111",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = greekPhoneRule,
            value = "15223433333",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = greekPhoneRule,
            value = "6941111111",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = usPhoneRule,
            value = "6941111111",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = usPhoneRule,
            value = "6102458772",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a rule for lengths 10 and 12, When strings are validated, Then only those lengths pass`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateStringLength(listOf(10, 12), plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "123456789",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "12345678901",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1234567890",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "123456789012",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a maximum length of 10, When strings are validated, Then longer strings fail`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateStringMaxLength(10, plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "aaaaaaaaaaa",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "            ",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaa",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaaa",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a minimum length of 5, When strings are validated, Then shorter strings fail`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateStringMinLength(5, plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaa",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaaa",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaa",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given contains-regex rules for digits, special characters and capitals, When values are validated, Then only values with enough matches pass`() = coroutineRule.runTest {
        // Given
        val atLeast2DigitsRule =
            listOf(Rule.ContainsRegex("(.*\\d){2,}".toRegex(), plainErrorMessage))
        val atLeast2SpecialCharsRule =
            listOf(Rule.ContainsRegex("(.*[^a-zA-Z0-9]){2,}".toRegex(), plainErrorMessage))
        val atLeast3UppercaseCharactersRule =
            listOf(Rule.ContainsRegex("(.*[A-Z]){3,}".toRegex(), plainErrorMessage))

        // Then
        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test12",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test123",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test1",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = atLeast2DigitsRule,
            value = "Test",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test!@",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test!@#",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test!",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = atLeast2SpecialCharsRule,
            value = "Test",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "TestAA",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "TAestAA",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "TestA",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = atLeast3UppercaseCharactersRule,
            value = "Test",
            expectedValidationResult = validationError
        )
    }

    @Test
    fun `Given regex rules for a capital, a digit and a special character, When values are validated, Then only values that match pass`() = coroutineRule.runTest {
        // Given
        val atLeastOneCapitalLetterRule =
            listOf(Rule.ValidateRegex(".*[A-Z].*".toRegex(), plainErrorMessage))
        val atLeastOneDigitRule =
            listOf(Rule.ValidateRegex(".*(\\d).*".toRegex(), plainErrorMessage))
        val atLeastOneSpecialCharRule =
            listOf(Rule.ValidateRegex(".*[^a-zA-Z0-9].*".toRegex(), plainErrorMessage))

        // Then
        validateForm(
            rules = atLeastOneCapitalLetterRule,
            value = "test",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = atLeastOneCapitalLetterRule,
            value = "Test",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeastOneDigitRule,
            value = "test",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = atLeastOneDigitRule,
            value = "test1",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = atLeastOneSpecialCharRule,
            value = "test",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = atLeastOneSpecialCharRule,
            value = "test1@",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a string range of 0 to 10, When strings are validated, Then strings beyond 10 characters fail`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateStringRange(0..10, plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "aaaaaaaaaaa",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "aaaaaaaaaa",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given case-insensitive and case-sensitive match rules, When values are validated, Then only matching values pass`() = coroutineRule.runTest {
        // Given
        val isNotCaseSensitiveRule = listOf(Rule.ValidateStringMatch("test", plainErrorMessage))
        val isCaseSensitiveRule = listOf(Rule.ValidateStringMatch("test", plainErrorMessage, true))

        // Then
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "testt",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "t!@#$",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "TEST",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "TEST",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "test",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given case-insensitive and case-sensitive not-match rules, When values are validated, Then matching values fail`() = coroutineRule.runTest {
        // Given
        val isNotCaseSensitiveRule = listOf(Rule.ValidateStringNotMatch("test", plainErrorMessage))
        val isCaseSensitiveRule =
            listOf(Rule.ValidateStringNotMatch("test", plainErrorMessage, true))

        // Then
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "test",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "TEST",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = isNotCaseSensitiveRule,
            value = "testt",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "test",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "Testss",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "TEST",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = isCaseSensitiveRule,
            value = "",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given duplicate-character rules for 2 and 4 repeats, When values are validated, Then values with too many consecutive repeats fail`() = coroutineRule.runTest {
        // Given
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

        // Then
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "1313",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "4356754",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "0191",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "1225",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "2332",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "454545454545454555444455455",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder2Rule,
            value = "0024",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "0024",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "0004",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "0000",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = maxTimesOfConsecutiveOrder4Rule,
            value = "35523456337777",
            expectedValidationResult = validationError
        )
    }

    @Test
    fun `Given a consecutive-sequence rule of length 4, When values are validated, Then ascending and descending sequences fail`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateNumericNotInConsecutiveSequenceOrder(4, plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "1234",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "4321",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "9876",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "3210",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "0123",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1235",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "9875",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "0124",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "0923",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "8834834835939534",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "TEST",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given three forms with several rules, When they are validated together, Then the messages of all failed rules are returned in order`() = coroutineRule.runTest {
        // Given
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

        val expectedValidationResult = FormsValidationResult(
            isValid = false,
            messages = listOf(
                "ValidateDuplicateCharacterNotInConsecutiveOrder",
                "ValidateEmail",
                "ValidatePhoneNumber",
                "ValidateRegex",
                "ValidateEmail",
                "ValidateStringNotMatch",
                "ValidateDuplicateCharacterNotInConsecutiveOrder",
                "ValidateRegex"
            )
        )

        // When
        val actualValidationResult = formValidation.validateForms(forms)

        // Then
        assertEquals(expectedValidationResult, actualValidationResult)
    }

    @Test
    fun `Given a rule that a string ends with a number, When values are validated, Then only values ending in a digit pass`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.StringShouldEndWithNumber(plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "1.",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1,",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1*",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "1.00",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "1,00",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given the supported extensions doc and ppt, When file names are validated, Then only supported extensions pass`() = coroutineRule.runTest {
        // Given
        val validExtensionList = listOf(".doc", ".ppt")

        val rules = listOf(
            Rule.ValidateFileExtension(
                errorMessage = plainErrorMessage,
                supportedExtensions = validExtensionList
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "fileame.pdf",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "asdf",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "lalala.doc",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "lala.ppt",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a maximum file size of 100, When sizes are validated, Then larger sizes fail`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateFileSizeMaxSize(
                errorMessage = plainErrorMessage,
                size = 100L
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "200",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "100",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "99",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a minimum amount of 5, When values are validated, Then smaller and non-numeric values fail`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateMinimumAmount(
                minimumAmount = 5,
                errorMessage = plainErrorMessage,
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "test",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "4",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "5",
            expectedValidationResult = validationSuccess
        )

        validateForm(
            rules = rules,
            value = "6",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a maximum amount of 5, When values are validated, Then larger and non-numeric values fail`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateMaximumAmount(
                maximumAmount = 5,
                errorMessage = plainErrorMessage,
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "test",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "6",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "5",
            expectedValidationResult = validationSuccess
        )

        validateForm(
            rules = rules,
            value = "4",
            expectedValidationResult = validationSuccess
        )
    }

    /**
     * An empty value: isNotEmpty() is false, which short-circuits the &&.
     */
    @Test
    fun `Given an email rule, When an empty value is validated, Then validation fails`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateEmail(plainErrorMessage))

        // Then
        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationError
        )
    }

    /**
     * shouldValidateHost = true (only), everything else off — covers the host-guard's `&&
     * uri.host.isNullOrEmpty()` second-operand-false branch (host non-empty) AND the
     * `shouldValidatePath/Query = false` short-circuit on the first operand.
     */
    @Test
    fun `Given a URL rule that validates only the host, When URLs are validated, Then a URL with a host passes and one without fails`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateUrl(
                shouldValidateSchema = false,
                shouldValidateHost = true,
                shouldValidatePath = false,
                shouldValidateQuery = false,
                errorMessage = plainErrorMessage,
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "https://www.example.com",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "https:///path",
            expectedValidationResult = validationError
        )
    }

    /**
     * Mirrors the host test above for the path guard.
     */
    @Test
    fun `Given a URL rule that validates only the path, When URLs are validated, Then a URL with a path passes and one without fails`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateUrl(
                shouldValidateSchema = false,
                shouldValidateHost = false,
                shouldValidatePath = true,
                shouldValidateQuery = false,
                errorMessage = plainErrorMessage,
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "https://www.example.com/some/path",
            expectedValidationResult = validationSuccess
        )
        validateForm(
            rules = rules,
            value = "https://www.example.com",
            expectedValidationResult = validationError
        )
    }

    /**
     * Each `if (shouldValidate* && ...)` short-circuits to false when shouldValidate* is false.
     * Empty value → early return false (still validationError because isValidUrl returns false)
     * Non-empty but no components — none of the guards fire because shouldValidate* are all false,
     * so isValidUrl returns true → validationSuccess.
     */
    @Test
    fun `Given a URL rule that validates nothing, When an empty and a non-empty value are validated, Then the empty one fails and the other passes`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateUrl(
                shouldValidateSchema = false,
                shouldValidateHost = false,
                shouldValidatePath = false,
                shouldValidateQuery = false,
                errorMessage = plainErrorMessage,
            )
        )

        // Then
        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationError
        )
        validateForm(
            rules = rules,
            value = "abc",
            expectedValidationResult = validationSuccess
        )
    }

    @Test
    fun `Given a string range of 2 to 5, When strings are validated, Then only strings inside the range pass`() = coroutineRule.runTest {
        // Given
        val rules = listOf(Rule.ValidateStringRange(range = 2..5, errorMessage = plainErrorMessage))

        // Then
        validateForm(rules = rules, value = "a", expectedValidationResult = validationError)
        validateForm(rules = rules, value = "ab", expectedValidationResult = validationSuccess)
        validateForm(rules = rules, value = "abcdef", expectedValidationResult = validationError)
    }

    /**
     * maxTimesOfConsecutiveOrder < 2 → always returns true (always valid).
     */
    @Test
    fun `Given a duplicate-character rule below its threshold, When repeated and distinct values are validated, Then both pass`() =
        coroutineRule.runTest {
            // Given
            val rules = listOf(
                Rule.ValidateDuplicateCharacterNotInConsecutiveOrder(
                    maxTimesOfConsecutiveOrder = 1,
                    errorMessage = plainErrorMessage,
                )
            )

            // Then
            validateForm(
                rules = rules,
                value = "aaaa",
                expectedValidationResult = validationSuccess
            )
            validateForm(
                rules = rules,
                value = "abcd",
                expectedValidationResult = validationSuccess
            )
        }

    /**
     * The guard `value.isEmpty() || minLength < 2 || value.toIntOrNull() == null || value.length !=
     * minLength` returns true for any of these conditions, so the rule is satisfied.
     * An empty value makes the first OR-arm true. A non-numeric value makes the third true
     * (toIntOrNull() == null). A length mismatch makes the fourth true. A minLength below 2 makes
     * the second true.
     * A genuinely consecutive sequence at the correct length, also reversed, returns false
     * (validationError).
     */
    @Test
    fun `Given a consecutive-sequence rule, When empty, non-numeric and wrong-length values and a rule below length 2 are checked, Then they pass and real sequences fail`() =
        coroutineRule.runTest {
            // Given
            val rules = listOf(
                Rule.ValidateNumericNotInConsecutiveSequenceOrder(
                    minLength = 4,
                    errorMessage = plainErrorMessage,
                )
            )

            // Then
            validateForm(rules = rules, value = "", expectedValidationResult = validationSuccess)
            validateForm(
                rules = rules,
                value = "abcd",
                expectedValidationResult = validationSuccess
            )
            validateForm(rules = rules, value = "123", expectedValidationResult = validationSuccess)

            val tinyMinLengthRule = listOf(
                Rule.ValidateNumericNotInConsecutiveSequenceOrder(
                    minLength = 1,
                    errorMessage = plainErrorMessage,
                )
            )
            validateForm(
                rules = tinyMinLengthRule,
                value = "1",
                expectedValidationResult = validationSuccess
            )

            validateForm(rules = rules, value = "1234", expectedValidationResult = validationError)
            validateForm(rules = rules, value = "4321", expectedValidationResult = validationError)
        }

    /**
     * Valid: alphanumerics and the allowed punctuation. Invalid: a character outside the allowed
     * set (for example a dot for the extension), or an empty value (the regex requires at least one
     * allowed character).
     */
    @Test
    fun `Given a file-name rule, When names are validated, Then only names of allowed characters pass`() = coroutineRule.runTest {
        // Given
        val rules = listOf(
            Rule.ValidateFileName(errorMessage = plainErrorMessage)
        )

        // Then
        validateForm(
            rules = rules,
            value = "my-file_name 01!",
            expectedValidationResult = validationSuccess
        )

        validateForm(
            rules = rules,
            value = "file.txt",
            expectedValidationResult = validationError
        )

        validateForm(
            rules = rules,
            value = "",
            expectedValidationResult = validationError
        )
    }

    private suspend fun validateForm(
        rules: List<Rule>,
        value: String,
        expectedValidationResult: FormValidationResult
    ) {
        val actualValidationResult = formValidation.validateForm(
            Form(mapOf(rules to value))
        )
        assertEquals(expectedValidationResult, actualValidationResult)
    }
}