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

package eu.europa.ec.issuancefeature.ui.offer

import androidx.lifecycle.ViewModelStore
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.corelogic.model.IssuerRegistrationDomain
import eu.europa.ec.corelogic.model.RegistrationDetailsDomain
import eu.europa.ec.issuancefeature.interactor.DocumentOfferInteractor
import eu.europa.ec.issuancefeature.interactor.ResolveDocumentOfferInteractorPartialState
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestDocumentOfferViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var interactor: DocumentOfferInteractor

    @Mock
    private lateinit var resources: ResourceProvider

    @Mock
    private lateinit var serializer: UiSerializer

    private val store = ViewModelStore()

    private lateinit var viewModel: DocumentOfferViewModel

    private lateinit var closeable: AutoCloseable

    /**
     * Starts an empty Koin graph: clearing the view model closes the credential-offer scope, which
     * it looks up through Koin.
     */
    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        startKoin {}

        whenever(
            serializer.fromBase64(
                "config",
                OfferUiConfig::class.java,
                OfferUiConfig.Parser
            )
        ).thenReturn(
            OfferUiConfig(
                offerUri = OFFER_URI,
                onSuccessNavigation = ConfigNavigation(NavigationType.Pop),
                onCancelNavigation = ConfigNavigation(NavigationType.Pop),
            )
        )

        viewModel = DocumentOfferViewModel(
            resourceProvider = resources,
            uiSerializer = serializer,
            offerSerializedConfig = "config",
            documentOfferInteractor = interactor,
        )
        store.put("offer", viewModel)
    }

    @After
    fun after() {
        try {
            store.clear()
            coroutineRule.testScope.runCurrent()
        } finally {
            stopKoin()
            closeable.close()
        }
    }

    @Test
    fun `Given a verified issuer with an https privacy policy, When the offer is resolved, Then the policy link is shown`() =
        coroutineRule.runTest {
            resolveOffer(privacyPolicyUrl = "https://issuer.example/privacy")

            assertEquals(
                "https://issuer.example/privacy",
                viewModel.viewState.value.relyingPartyHeader?.privacyPolicyUrl,
            )
        }

    @Test
    fun `Given a verified issuer with a privacy policy that is not an https web address, When the offer is resolved, Then no policy link is shown`() =
        coroutineRule.runTest {
            listOf(
                "http://issuer.example/privacy",
                "javascript:alert(1)",
                "eudi-wallet://dashboard",
                "intent://issuer.example#Intent;scheme=https;end",
            ).forEach { url ->
                resolveOffer(privacyPolicyUrl = url)

                val header = viewModel.viewState.value.relyingPartyHeader
                assertEquals(url, ISSUER_NAME, header?.relyingParty?.name)
                assertNull(url, header?.privacyPolicyUrl)
            }
            verify(interactor, times(4)).resolveDocumentOffer(OFFER_URI)
        }

    private fun resolveOffer(privacyPolicyUrl: String) {
        whenever(interactor.resolveDocumentOffer(OFFER_URI)).thenReturn(
            flowOf(
                ResolveDocumentOfferInteractorPartialState.NoDocument(
                    issuerName = ISSUER_NAME,
                    issuerLogo = null,
                    issuerRegistration = IssuerRegistrationDomain.Verified(
                        RegistrationDetailsDomain(
                            tradeName = ISSUER_NAME,
                            uniqueId = null,
                            logoUri = null,
                            intendedUse = null,
                            privacyPolicyUrl = privacyPolicyUrl,
                            serviceDescription = null,
                        )
                    ),
                )
            )
        )
        viewModel.setEvent(Event.Init(deepLink = null))
        coroutineRule.testScope.runCurrent()
    }

    private companion object {
        const val OFFER_URI =
            "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fissuer.example"
        const val ISSUER_NAME = "Issuer"
    }
}
