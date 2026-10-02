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

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.commonfeature.ui.trustmark.model.TrustMarkParagraphUi
import eu.europa.ec.commonfeature.ui.trustmark.model.TrustMarkUi
import eu.europa.ec.corelogic.controller.WalletCoreTrustMarkController
import eu.europa.ec.corelogic.model.TrustMarkDomain
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestTrustMarkInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreTrustMarkController: WalletCoreTrustMarkController

    @Mock
    private lateinit var prefKeys: PrefKeys

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: TrustMarkInteractor
    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        interactor = TrustMarkInteractorImpl(
            walletCoreTrustMarkController = walletCoreTrustMarkController,
            prefKeys = prefKeys,
            resourceProvider = resourceProvider,
        )
        mockParagraphCall(
            textRes = R.string.trust_mark_certification_description,
            linkRes = R.string.trust_mark_certified_wallets_link,
            label = mockedTrustedListLabel,
            text = mockedCertificationDescription.text,
        )
        mockParagraphCall(
            textRes = R.string.trust_mark_certification_information_description,
            linkRes = R.string.trust_mark_certification_information_link,
            label = mockedInformationLabel,
            text = mockedInformationDescription.text,
        )
        whenever(resourceProvider.getString(R.string.trust_mark_load_error))
            .thenReturn(mockedTrustMarkLoadError)
        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getTrustMark

    // Case 1:
    // 1. The domain value includes localized text and absolute web URLs.
    // Case 1 Expected Result:
    // Display data preserves the text and targets without completing the introduction.
    @Test
    fun `Given Case 1, When getTrustMark is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkCall(mockedTrustMark)

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(LoadTrustMarkPartialState.Success(mockedTrustMarkUi), result)
            verify(prefKeys, never()).setTrustMarkIntroductionCompleted(any())
        }
    }

    // Case 2:
    // 1. The domain value has no localized text.
    // Case 2 Expected Result:
    // The paragraph is omitted while the image and links remain available.
    @Test
    fun `Given Case 2, When getTrustMark is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkCall(mockedTrustMark.copy(localisedText = null))

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(
                LoadTrustMarkPartialState.Success(mockedTrustMarkUi.copy(text = null)),
                result,
            )
        }
    }

    // Case 3:
    // 1. The image has a relative path and an SVG extension.
    // Case 3 Expected Result:
    // Its URL is resolved against the resource location, independently of the image name.
    @Test
    fun `Given Case 3, When getTrustMark is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkCall(mockedTrustMark.copy(imageUrl = "../assets/logo.svg"))

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(LoadTrustMarkPartialState.Success(mockedTrustMarkUi), result)
        }
    }

    // Case 4:
    // 1. The image URL is blank, malformed or uses a non-web scheme.
    // Case 4 Expected Result:
    // Only the image is unavailable.
    @Test
    fun `Given Case 4, When getTrustMark is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockedInvalidImageUrls.forEach { imageUrl ->
                mockTrustMarkCall(mockedTrustMark.copy(imageUrl = imageUrl))

                // When
                val result = interactor.getTrustMark()

                // Then
                assertEquals(
                    LoadTrustMarkPartialState.Success(mockedTrustMarkUi.copy(imageUrl = null)),
                    result,
                )
            }
        }
    }

    // Case 5:
    // 1. Link targets are relative, malformed, non-web or include user information.
    // Case 5 Expected Result:
    // Invalid targets are omitted, retaining the usable content.
    @Test
    fun `Given Case 5, When getTrustMark is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockedInvalidLinkUrls.forEach { url ->
                mockTrustMarkCall(
                    mockedTrustMark.copy(certifiedWalletsUrl = url, walletSolutionUrl = url)
                )

                // When
                val result = interactor.getTrustMark()

                // Then
                assertEquals(
                    LoadTrustMarkPartialState.Success(
                        mockedTrustMarkUi.copy(certifiedWalletsUrl = null, walletSolutionUrl = null)
                    ),
                    result,
                )
            }
        }
    }

    // Case 6:
    // 1. The controller reports a missing Trust Mark manager.
    // Case 6 Expected Result:
    // The Trust Mark load error is returned without completing the introduction.
    @Test
    fun `Given Case 6, When getTrustMark is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mockedMissingManagerException = IllegalStateException("Missing Trust Mark manager")
            mockTrustMarkFailure(mockedMissingManagerException)

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(
                LoadTrustMarkPartialState.Failure(mockedTrustMarkLoadError),
                result,
            )
            verify(prefKeys, never()).setTrustMarkIntroductionCompleted(any())
        }
    }

    // Case 7:
    // 1. Core reports a resource fetch or parsing failure.
    // Case 7 Expected Result:
    // The Trust Mark load error is returned without completing the introduction.
    @Test
    fun `Given Case 7, When getTrustMark is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkFailure(mockedExceptionWithMessage)

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(
                LoadTrustMarkPartialState.Failure(mockedTrustMarkLoadError),
                result,
            )
            verify(prefKeys, never()).setTrustMarkIntroductionCompleted(any())
        }
    }

    // Case 8:
    // 1. The controller throws before returning a result.
    // Case 8 Expected Result:
    // The interactor returns the Trust Mark load error at its asynchronous boundary.
    @Test
    fun `Given Case 8, When getTrustMark is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreTrustMarkController.getTrustMark()).thenThrow(
                mockedExceptionWithMessage
            )

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(
                LoadTrustMarkPartialState.Failure(mockedTrustMarkLoadError),
                result,
            )
        }
    }

    // Case 9:
    // 1. A translation places the linked phrase first and includes literal markup characters.
    // Case 9 Expected Result:
    // The full text is preserved and only the named phrase is marked as actionable.
    @Test
    fun `Given Case 9, When getTrustMark is called, Then Case 9 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkCall(mockedTrustMark)
            val translatedText = "Details <wallet> & privacy: learn more."
            mockParagraphCall(
                textRes = R.string.trust_mark_certification_information_description,
                linkRes = R.string.trust_mark_certification_information_link,
                label = "Details <wallet>",
                text = translatedText,
            )

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(
                LoadTrustMarkPartialState.Success(
                    mockedTrustMarkUi.copy(
                        certificationInformationDescription = TrustMarkParagraphUi(
                            text = translatedText,
                            linkRange = 0..15,
                        )
                    )
                ),
                result,
            )
        }
    }

    // Case 10:
    // 1. The linked label is absent from the translated paragraph or blank.
    // Case 10 Expected Result:
    // The paragraph remains plain text, without an invalid actionable range.
    @Test
    fun `Given Case 10, When getTrustMark is called, Then Case 10 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkCall(mockedTrustMark)
            val translatedText = "Information is available online."
            listOf(mockedInformationLabel, " ").forEach { label ->
                mockParagraphCall(
                    textRes = R.string.trust_mark_certification_information_description,
                    linkRes = R.string.trust_mark_certification_information_link,
                    label = label,
                    text = translatedText,
                )

                // When
                val result = interactor.getTrustMark()

                // Then
                assertEquals(
                    LoadTrustMarkPartialState.Success(
                        mockedTrustMarkUi.copy(
                            certificationInformationDescription = TrustMarkParagraphUi(
                                text = translatedText,
                                linkRange = null,
                            )
                        )
                    ),
                    result,
                )
            }
        }
    }

    // Case 11:
    // 1. Loading fails with an exception that has no message.
    // Case 11 Expected Result:
    // The same Trust Mark load error is returned without completing the introduction.
    @Test
    fun `Given Case 11, When getTrustMark is called, Then Case 11 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkFailure(mockedExceptionWithNoMessage)

            // When
            val result = interactor.getTrustMark()

            // Then
            assertEquals(
                LoadTrustMarkPartialState.Failure(mockedTrustMarkLoadError),
                result
            )
            verify(prefKeys, never()).setTrustMarkIntroductionCompleted(any())
        }
    }

    //endregion

    //region completeIntroduction

    // Case 1:
    // 1. Resource loading has failed.
    // Case 1 Expected Result:
    // Completion is still saved successfully.
    @Test
    fun `Given Case 1, When completeIntroduction is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockTrustMarkFailure(mockedExceptionWithMessage)
            interactor.getTrustMark()

            // When
            val result = interactor.completeIntroduction()

            // Then
            assertEquals(CompleteTrustMarkIntroductionPartialState.Success, result)
            verify(prefKeys).setTrustMarkIntroductionCompleted(value = true)
        }
    }

    // Case 2:
    // 1. Saving the completion flag fails.
    // Case 2 Expected Result:
    // The exception's localized message is returned as a completion failure.
    @Test
    fun `Given Case 2, When completeIntroduction is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(prefKeys.setTrustMarkIntroductionCompleted(value = true))
                .thenThrow(mockedExceptionWithMessage)

            // When
            val result = interactor.completeIntroduction()

            // Then
            assertEquals(
                CompleteTrustMarkIntroductionPartialState.Failure(
                    mockedExceptionWithMessage.localizedMessage!!
                ),
                result,
            )
        }
    }

    // Case 3:
    // 1. Saving the completion flag fails with an exception that has no message.
    // Case 3 Expected Result:
    // The generic error message is returned as a completion failure.
    @Test
    fun `Given Case 3, When completeIntroduction is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(prefKeys.setTrustMarkIntroductionCompleted(value = true))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            val result = interactor.completeIntroduction()

            // Then
            assertEquals(
                CompleteTrustMarkIntroductionPartialState.Failure(mockedGenericErrorMessage),
                result,
            )
        }
    }

    //endregion

    //region helper functions

    private suspend fun mockTrustMarkCall(trustMark: TrustMarkDomain) {
        whenever(walletCoreTrustMarkController.getTrustMark()).thenReturn(Result.success(trustMark))
    }

    private suspend fun mockTrustMarkFailure(error: Throwable) {
        whenever(walletCoreTrustMarkController.getTrustMark())
            .thenReturn(Result.failure(error))
    }

    private fun mockParagraphCall(textRes: Int, linkRes: Int, label: String, text: String) {
        whenever(resourceProvider.getString(linkRes)).thenReturn(label)
        whenever(resourceProvider.getString(textRes, label)).thenReturn(text)
    }

    private val mockedTrustMarkLoadError =
        "Unable to load Trust Mark information. Please try again."
    private val mockedEnglishText = "SampleText-for-Users"
    private val mockedTrustedListLabel = "Trusted wallets"
    private val mockedInformationLabel = "details"
    private val mockedCertificationDescription = TrustMarkParagraphUi(
        text = "Listed in Trusted wallets.",
        linkRange = 10..24,
    )
    private val mockedInformationDescription = TrustMarkParagraphUi(
        text = "Read details.",
        linkRange = 5..11,
    )
    private val mockedTrustMark = TrustMarkDomain(
        resourceUrl = "https://example.com/resources/TrustMarkResource.json",
        imageName = "trust-mark.png",
        imageUrl = "https://example.com/assets/logo.svg",
        localisedText = mockedEnglishText,
        certifiedWalletsUrl = "https://eidas.ec.europa.eu/efda/wallet/certified",
        walletSolutionUrl = "https://eidas.ec.europa.eu/efda/wallet/certified?id=WALLET_SOLUTION_ID",
    )
    private val mockedTrustMarkUi = TrustMarkUi(
        imageUrl = mockedTrustMark.imageUrl,
        text = mockedEnglishText,
        certifiedWalletsUrl = mockedTrustMark.certifiedWalletsUrl,
        walletSolutionUrl = mockedTrustMark.walletSolutionUrl,
        certificationDescription = mockedCertificationDescription,
        certificationInformationDescription = mockedInformationDescription,
    )
    private val mockedInvalidImageUrls = listOf(
        "", " ", "file:///logo.svg", "data:image/svg+xml,logo", "https://example.com/bad url.svg",
    )
    private val mockedInvalidLinkUrls = listOf(
        "/wallets", "javascript:alert(1)", "https://", "https://user@example.com/wallets",
    )

    //endregion
}